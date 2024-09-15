package com.yupi.yupao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.yupi.yupao.common.BaseResponse;
import com.yupi.yupao.common.ErrorCode;
import com.yupi.yupao.common.ResultUtils;
import com.yupi.yupao.domain.Team;
import com.yupi.yupao.domain.User;
import com.yupi.yupao.domain.UserTeam;
import com.yupi.yupao.domain.dto.TeamQuery;
import com.yupi.yupao.domain.request.TeamAddRequest;
import com.yupi.yupao.domain.request.TeamJoinRequest;
import com.yupi.yupao.domain.vo.TeamUserVO;
import com.yupi.yupao.exception.BussinessException;
import com.yupi.yupao.service.TeamService;
import com.yupi.yupao.service.UserService;
import com.yupi.yupao.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@Slf4j
public class TeamController {

    @Resource
    private TeamService teamService;
    @Resource
    private UserService userService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);

        long teamId = teamService.addTeam(team, loginUser);

        return ResultUtils.success(teamId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody Long teamId) {
        if (teamId <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isDelete = teamService.removeById(teamId);
        if (!isDelete) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody Team team) {

        if (team == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.updateById(team);
        if (!result) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "修改队伍失败");

        }
        return ResultUtils.success(true);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        /*
         * 为什么还要在这里判断一下
         * */
        if (teamQuery == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        List<TeamUserVO> teamUserVOList = teamService.listTeams(teamQuery, isAdmin);
        /*
         * 这里TeamUserVO.Id 是被Team拷贝上去的
         * */
        List<Long> idList = teamUserVOList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        /*
         * 为每一个VO添加,hasJoin字段
         * */
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        User loginUser = userService.getLoginUser(request);
        long userId = loginUser.getId();
        queryWrapper.eq("userId", userId);
        queryWrapper.in("id", idList);
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getId).collect(Collectors.toSet());
        teamUserVOList.forEach(team -> {
                    boolean isContains = hasJoinTeamIdSet.contains(team.getId());
                    team.setHasJoin(isContains);
                }

        );
        /*
         * 为查询到的每一个队伍添加人数字段
         * 1.现在关联表中,广撒网,然后根据id分组
         * */

        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.in("teamId", idList);
        userTeamList = userTeamService.list(userTeamQueryWrapper);
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        /*
         * 然后根据分组,对每个进行数量赋值
         * */
        teamUserVOList.forEach(teamUserVO -> teamUserVO.setHasJoinNum(teamIdUserTeamList.getOrDefault(teamUserVO.getId(), new ArrayList<>()).size()));


        return ResultUtils.success(teamUserVOList);


    }


    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);

        }
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        System.err.println(team);
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>(team);
        Page<Team> teamPage = teamService.page(page, teamQueryWrapper);
        return ResultUtils.success(teamPage);


    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }
}

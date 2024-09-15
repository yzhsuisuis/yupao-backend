package com.yupi.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupao.common.ErrorCode;
import com.yupi.yupao.domain.Team;
import com.yupi.yupao.domain.User;
import com.yupi.yupao.domain.UserTeam;
import com.yupi.yupao.domain.dto.TeamQuery;
import com.yupi.yupao.domain.enums.TeamStatusEnum;
import com.yupi.yupao.domain.request.TeamJoinRequest;
import com.yupi.yupao.domain.vo.TeamUserVO;
import com.yupi.yupao.domain.vo.UserVO;
import com.yupi.yupao.exception.BussinessException;
import com.yupi.yupao.service.TeamService;
import com.yupi.yupao.mapper.TeamMapper;
import com.yupi.yupao.service.UserService;
import com.yupi.yupao.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
* @author yangz
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2024-09-13 20:38:12
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{
    @Resource
    private UserTeamService userTeamService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserService userService;

    @Override
    public long addTeam(Team team, User loginUser) {
//        Long id = team.getId();
//        String name = team.getName();
//        String description = team.getDescription();
//        Integer maxNum = team.getMaxNum();
//        Date expireTime = team.getExpireTime();
//        Long userId = team.getUserId();
//        Integer status = team.getStatus();
//        String password = team.getPassword();
//        Date createTime = team.getCreateTime();
//        Date updateTime = team.getUpdateTime();
//        Integer isDelete = team.getIsDelete();

        /*
        * 1. 请求参数是否为空
        *
        * */
        if(team==null)
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        /*
        * 2. 判断是否登录
        * */
        if(loginUser==null)
        {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = team.getUserId();
        /*
        * 1.队伍人数
        * */
        Integer maxNum = team.getMaxNum();
        Integer count = Optional.ofNullable(maxNum).orElse(0);
        if(count<1 || count>20)
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"人数上限太高");
        }
        /*
        *
        * 2. 队伍标题小于20
        * */
        String name = team.getName();
        if(Strings.isBlank(name)||name.length()>20)
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"队伍名称不满足要求");
        }
        /*
        * 3. 队伍描述不能少于512
        * 可以没有描述,但是一旦有描述的话,就不能超字数
        * */
        String description = team.getDescription();
        if(Strings.isNotBlank(description) && description.length()>512)
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"队伍描述不满足要求");
        }
        /*
        * 检查传入的状态是否符合要求
        * */
        Integer integer = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(integer);
        if(enumByValue==null)
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"队伍状态不符合要求");
        }

        /*
        * 如果是加密状态下需要
        * */
        if(TeamStatusEnum.SECRET.equals(enumByValue))
        {
            String password = team.getPassword();
            if(Strings.isBlank(password) || password.length() >32)
            {
                throw new BussinessException(ErrorCode.PARAMS_ERROR,"密码不符合要求");
            }
        }

        Date expireTime = team.getExpireTime();
        if(new Date().after(expireTime))
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");

        }

        /*
        * 一个用户最多创建5个队伍
        * todo 这里需要加一个锁,这里可能存在并发的情况,涉及到数的增减
        * */
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }

        // 8. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        // 9. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        Team team = getTeamById(teamId);
        Date expireTime = team.getExpireTime();

        boolean before = expireTime.before(new Date());
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BussinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        // 该用户已加入的队伍数量
        long userId = loginUser.getId();
        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock("yupao:join_team");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
                    if (hasJoinNum > 5) {
                        throw new BussinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
                    }
                    // 不能重复加入已加入的队伍
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    userTeamQueryWrapper.eq("teamId", teamId);
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                    if (hasUserJoinTeam > 0) {
                        throw new BussinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
                    }
                    // 已加入队伍的人数
                    long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
                    if (teamHasJoinNum >= team.getMaxNum()) {
                        throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    // 修改队伍信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }

    }
    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
//        Long id = teamQuery.getId();
//        List<Long> idList = teamQuery.getIdList();
//        String searchText = teamQuery.getSearchText();
//        String name = teamQuery.getName();
//        String description = teamQuery.getDescription();
//        Integer maxNum = teamQuery.getMaxNum();
//        Long userId = teamQuery.getUserId();
//        Integer status = teamQuery.getStatus();
//        int pageSize = teamQuery.getPageSize();
//        int pageNum = teamQuery.getPageNum();
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (!CollectionUtils.isEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 根据状态来查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BussinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        if(CollectionUtils.isEmpty(teamList))
        {
            return new ArrayList<TeamUserVO>();
        }
        /*
        * 为查询到的队伍,添加userVo ,createUser字段
        * */
        ArrayList<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if(userId==null)
            {
                continue;
            }
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team,teamUserVO);
            User user = userService.getById(userId);
            if(user!=null)
            {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user,userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);



        }
        return teamUserVOList;

    }

    private Team getTeamById(Long teamId) {
        if(teamId == null || teamId <=0)
        {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if(team == null)
        {
            throw new BussinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        return team;
    }
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }
}





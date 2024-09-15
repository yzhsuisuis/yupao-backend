package com.yupi.yupao.service;

import com.yupi.yupao.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupao.domain.User;
import com.yupi.yupao.domain.dto.TeamQuery;
import com.yupi.yupao.domain.request.TeamAddRequest;
import com.yupi.yupao.domain.request.TeamJoinRequest;
import com.yupi.yupao.domain.vo.TeamUserVO;

import java.util.List;

/**
* @author yangz
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-09-13 20:38:12
*/
public interface TeamService extends IService<Team> {
    long addTeam(Team team, User loginUser );

    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);
}

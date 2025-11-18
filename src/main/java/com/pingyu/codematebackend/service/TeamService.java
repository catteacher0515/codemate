package com.pingyu.codematebackend.service;

import com.pingyu.codematebackend.dto.*;
import com.pingyu.codematebackend.model.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pingyu.codematebackend.model.User;
import jakarta.servlet.http.HttpServletRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
* @author 花萍雨
* @description 针对表【team(队伍表)】的数据库操作Service
* @createDate 2025-11-06 17:41:58
*/
public interface TeamService extends IService<Team> {
    public Long createTeam(TeamCreateDTO teamCreateDTO, User loginUser);

    /**
     * 获取队伍详情（包含创建人信息、权限校验）
     *
     * @param teamId    队伍 ID
     * @param loginUser 当前登录用户 (用于权限校验)
     * @return TeamVO 或 null
     */
    TeamVO getTeamDetails(long teamId, User loginUser);

    /**
     * 【【【 案卷 #18：SOP (搜索/分页) 】】】
     * V3.x 聚合搜索
     *
     * @param teamSearchDTO 搜索合约 (包含分页)
     * @param loginUser     当前登录用户
     * @return 聚合后的 TeamVO 分页
     */
    Page<TeamVO> searchTeams(TeamSearchDTO teamSearchDTO, User loginUser);

    /**
     * 【【【 案卷 #004：SOP (加入队伍) 】】】
     * (SOP 1 契约)
     *
     * @param teamJoinDTO 包含 teamId 和 (可选的) password
     * @param loginUser   当前登录用户
     * @return boolean (是否加入成功)
     */
    boolean joinTeam(TeamJoinDTO teamJoinDTO, User loginUser);

    /**
     * 【【【 案卷 #005：SOP (邀请用户) 】】】
     *
     * @param teamInviteDTO 包含 teamId 和 targetUserAccount
     * @param loginUser     当前登录用户 (发起人)
     * @return boolean
     */
    boolean inviteUser(TeamInviteDTO teamInviteDTO, User loginUser);
}

package com.pingyu.codematebackend.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 【【【 案卷 #005：邀请用户合约 (DTO) 】】】
 * (SOP 1 契约: POST /api/team/invite)
 */
@Data
public class TeamInviteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 队伍 ID
     */
    private Long teamId;

    /**
     * 目标用户账号 (用于查找目标用户)
     */
    private String targetUserAccount;
}
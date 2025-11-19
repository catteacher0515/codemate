package com.pingyu.codematebackend.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 【【【 案卷 #006：退出队伍合约 (DTO) 】】】
 * (SOP 1 契约: POST /api/team/quit)
 */
@Data
public class TeamQuitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 队伍 ID
     */
    private Long teamId;
}
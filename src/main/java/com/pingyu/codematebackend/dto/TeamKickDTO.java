package com.pingyu.codematebackend.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 踢出成员请求体
 * 案卷 #008 核心证物
 */
@Data
public class TeamKickDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 队伍 ID
     */
    private Long teamId;

    /**
     * 目标用户账号 (前端只传账号，我们需要去查 ID)
     */
    private String targetUserAccount;
}
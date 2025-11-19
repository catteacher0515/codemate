package com.pingyu.codematebackend.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 【【【 案卷 #007：更新队伍合约 (DTO) 】】】
 * (SOP 1 契约: POST /api/team/update)
 */
@Data
public class TeamUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 队伍ID (必需)
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 最大人数 (注意：通常不建议随意修改最大人数，防止当前人数溢出，此处暂保留)
     */
    // private Integer maxNum;

    /**
     * 过期时间
     */
    // private Date expireTime;

    /**
     * 0-公开, 1-私有, 2-加密
     */
    private Integer status;

    /**
     * 密码 (仅当 status=2 时必需)
     */
    private String password;
}
package com.pingyu.codematebackend.dto;

import com.pingyu.codematebackend.model.User;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 队伍“证物袋” (View Object)
 */
@Data
public class TeamVO {
    private Long id;
    private String name;
    private String description;
    private Integer maxNum;

    // 【新增】已加入人数 (专门用于列表展示)
    private Integer hasJoinNum;

    private Long userId;
    private Integer status;
    private List<String> tags;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;

    private User teamCaptain;
    private List<User> members;
}
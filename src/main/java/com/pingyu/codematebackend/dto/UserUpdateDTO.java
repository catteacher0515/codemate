package com.pingyu.codematebackend.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserUpdateDTO {
    private String username;
    private String bio;
    private String email;
    private String phone; // 必须有
    private Integer gender;
    private String avatarUrl;
    private List<String> tags; // 必须有
}
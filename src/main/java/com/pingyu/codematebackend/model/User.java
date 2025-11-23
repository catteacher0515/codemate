package com.pingyu.codematebackend.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户表
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号
     */
    @TableField("userAccount")
    private String userAccount;

    /**
     * 头像
     */
    @TableField("avatarUrl")
    private String avatarUrl;

    /**
     * 性别 (0-女, 1-男, 2-保密)
     */
    private Integer gender;

    /**
     * 密码
     */
    @TableField("userPassword")
    private String userPassword;

    // 【关键修复 1】: 确保 phone 字段存在且映射正确
    /**
     * 手机号
     */
    private String phone;

    // 【关键修复 2】: 确保 email 字段存在且映射正确
    /**
     * 邮箱
     */
    private String email;

    // 【关键修复 3】: 确保 bio (个人简介) 字段存在
    // (如果数据库里没有 bio 列，你需要先去数据库加一列！)
    // ALTER TABLE user ADD COLUMN bio VARCHAR(512) NULL COMMENT '个人简介';
    /**
     * 个人简介
     */
    private String bio;

    /**
     * 状态 0-正常
     */
    @TableField("userStatus")
    private Integer userStatus;

    /**
     * 用户角色 0-普通用户 1-管理员
     */
    @TableField("userRole")
    private Integer userRole;

    /**
     * 星球编号
     */
    @TableField("planetCode")
    private String planetCode;

    /**
     * 标签列表 (这是一个虚拟字段，数据库里没有，存在 user_tag_relation 表)
     * 所以必须加 exist = false
     */
    @TableField(exist = false)
    private java.util.List<String> tags;

    /**
     * 创建时间
     */
    @TableField("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableField("isDelete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
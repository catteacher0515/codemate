-- Flyway V1: 创建项目的初始表结构

-- 1. 创建 'user' 表
CREATE TABLE user
(
    id           BIGINT AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    username     VARCHAR(255)                       NULL COMMENT '用户昵称',
    userAccount  VARCHAR(256)                       NOT NULL COMMENT '账号',
    avatarUrl    VARCHAR(1024)                      NULL COMMENT '头像',
    gender       TINYINT                            NULL COMMENT '性别 (0-女, 1-男)',
    userPassword VARCHAR(256)                       NOT NULL COMMENT '密码',
    phone        VARCHAR(128)                       NULL COMMENT '手机号',
    email        VARCHAR(512)                       NULL COMMENT '邮箱',
    userStatus   TINYINT  DEFAULT 0                 NOT NULL COMMENT '状态 0-正常',
    userRole     INT      DEFAULT 0                 NOT NULL COMMENT '用户角色 0-普通用户 1-管理员',
    planetCode   VARCHAR(512)                       NULL COMMENT '星球编号',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',

    CONSTRAINT uniIdx_userAccount
        UNIQUE (userAccount)
)
    COMMENT '用户表';


-- 2. 创建 'tag' 表 (中心化标签库，树形结构)
CREATE TABLE tag
(
    id         BIGINT AUTO_INCREMENT COMMENT '标签id'
        PRIMARY KEY,
    tagName    VARCHAR(255)                       NOT NULL COMMENT '标签名称',
    parentId   BIGINT                             NOT NULL COMMENT '父标签id (0表示根标签)',
    isParent   TINYINT                            NOT NULL COMMENT '0-子标签, 1-父标签',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',

    CONSTRAINT uniIdx_tagName_parentId
        UNIQUE (tagName, parentId)
)
    COMMENT '标签表（中心化标签库）';


-- 3. 创建 'user_tag_relation' 表 (用户-标签关系表)
CREATE TABLE user_tag_relation
(
    id         BIGINT AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    userId     BIGINT                             NOT NULL COMMENT '用户id (外键, 关联 user.id)',
    tagId      BIGINT                             NOT NULL COMMENT '标签id (外键, 关联 tag.id)',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',

    CONSTRAINT uniIdx_userId_tagId
        UNIQUE (userId, tagId)
)
    COMMENT '用户标签关系表';
create table if not exists team_chat
(
    id          bigint auto_increment comment '主键' primary key,
    teamId      bigint                             not null comment '队伍ID',
    userId      bigint                             not null comment '发言人ID',
    content     varchar(1024)                      null comment '聊天内容',
    createTime  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    index idx_teamId (teamId) -- 关键索引：加速“查历史消息”
) comment '队伍聊天室';
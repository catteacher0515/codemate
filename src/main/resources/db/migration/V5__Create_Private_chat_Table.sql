create table if not exists private_chat
(
    id           bigint auto_increment comment '主键' primary key,
    sender_id    bigint                             not null comment '发送者ID',
    receiver_id  bigint                             not null comment '接收者ID',
    content      varchar(1024)                      null comment '聊天内容',
    create_time  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    is_delete    tinyint  default 0                 not null comment '是否删除',
    is_read      tinyint  default 0                 not null comment '是否已读'
) comment '私聊消息表';
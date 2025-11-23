package com.pingyu.codematebackend.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "private_chat")
@Data
public class PrivateChat implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 【关键修复】强制指定映射到下划线列名 "sender_id"
    @TableField("sender_id")
    private Long senderId;

    // 【关键修复】强制指定映射到下划线列名 "receiver_id"
    @TableField("receiver_id")
    private Long receiverId;

    private String content;

    // 强制映射 is_read
    @TableField("is_read")
    private Integer isRead;

    // 强制映射 create_time
    @TableField("create_time")
    private LocalDateTime createTime;

    // 强制映射 is_delete
    @TableField("is_delete")
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息已读回执实体
 */
@Data
@TableName("message_read")
public class MessageRead {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime readTime;
}

package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息表情回应实体
 */
@Data
@TableName("message_reaction")
public class MessageReaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;
    private Long userId;
    private String emoji;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

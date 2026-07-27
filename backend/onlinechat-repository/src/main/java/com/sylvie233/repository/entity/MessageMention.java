package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息 @提及实体
 */
@Data
@TableName("message_mention")
public class MessageMention {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;
    private Long fromUserId;
    private Long toUserId;
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息撤回记录实体
 */
@Data
@TableName("message_recall")
public class MessageRecall {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;
    private Long recallBy;
    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime recallTime;
}

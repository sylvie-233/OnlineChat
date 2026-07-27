package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息收藏实体
 */
@Data
@TableName("message_bookmark")
public class MessageBookmark {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long messageId;
    private String tag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

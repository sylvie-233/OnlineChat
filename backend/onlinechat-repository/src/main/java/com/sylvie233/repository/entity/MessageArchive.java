package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息归档表实体（冷数据）
 */
@Data
@TableName("message_archive")
public class MessageArchive {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long seq;
    private Integer conversationType;
    private Long conversationId;
    private Long fromUserId;
    private Long toId;
    private Integer msgType;
    private String content;
    private String extra;
    private LocalDateTime sendTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime archiveTime;
}

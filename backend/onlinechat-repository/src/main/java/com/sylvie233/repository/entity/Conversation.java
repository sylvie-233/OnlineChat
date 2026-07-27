package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话表实体（用户维度的聊天列表）
 */
@Data
@TableName("conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer type;
    private Long targetId;
    private Long lastMessageId;
    private Long lastMessageSeq;
    private Integer unreadCount;
    private Integer isPinned;
    private Integer isMuted;
    private Integer isHidden;
    private String draft;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群公告已读确认实体
 */
@Data
@TableName("group_announcement_read")
public class GroupAnnouncementRead {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long announcementId;
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime readTime;
}

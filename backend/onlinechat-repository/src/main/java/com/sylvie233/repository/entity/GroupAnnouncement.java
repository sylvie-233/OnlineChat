package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群公告实体
 */
@Data
@TableName("group_announcement")
public class GroupAnnouncement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;
    private Long publisherId;
    private String title;
    private String content;
    private Integer isPinned;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

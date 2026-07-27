package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群组信息实体
 */
@Data
@TableName("group_info")
public class GroupInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupName;
    private String avatar;
    private Long ownerId;
    private String announcement;
    private String description;
    private Integer maxMembers;
    private Integer memberCount;
    private Integer joinType;
    private Integer isMutedAll;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 联系人分组实体
 */
@Data
@TableName("contact_group")
public class ContactGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String groupName;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

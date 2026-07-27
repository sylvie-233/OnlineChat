package com.sylvie233.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件上传记录实体
 */
@Data
@TableName("file_upload")
public class FileUpload {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private Integer duration;
    private Integer width;
    private Integer height;
    private String thumbnailUrl;
    private String storageType;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

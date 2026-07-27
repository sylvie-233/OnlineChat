package com.sylvie233.service.file;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.repository.entity.FileUpload;
import com.sylvie233.repository.mapper.FileUploadMapper;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件服务 — MinIO 对象存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService extends ServiceImpl<FileUploadMapper, FileUpload> {

    private final MinioClient minioClient;

    @Value("${minio.bucket:onlinechat}")
    private String bucket;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 上传文件
     */
    public FileUpload upload(MultipartFile file, Long userId, String fileType) throws Exception {
        // 生成存储路径
        String date = LocalDateTime.now().toLocalDate().toString();
        String objectName = fileType + "/" + date + "/" + UUID.randomUUID()
                + "_" + file.getOriginalFilename();

        // 上传到 MinIO
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());

        // 记录到数据库
        FileUpload upload = new FileUpload();
        upload.setUserId(userId);
        upload.setFileName(file.getOriginalFilename());
        upload.setFileUrl(endpoint + "/" + bucket + "/" + objectName);
        upload.setFileType(fileType);
        upload.setMimeType(file.getContentType());
        upload.setFileSize(file.getSize());
        upload.setStorageType("minio");
        upload.setStatus(1);

        save(upload);
        return upload;
    }

    /**
     * 生成预签名访问 URL（有效期 7 天）
     */
    public String getPresignedUrl(Long fileId) throws Exception {
        FileUpload upload = getById(fileId);
        if (upload == null) return null;

        // 从完整 URL 中提取 objectName
        String objectName = upload.getFileUrl()
                .substring(upload.getFileUrl().indexOf(bucket) + bucket.length() + 1);

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectName)
                        .expiry(7, TimeUnit.DAYS)
                        .build());
    }

    /**
     * 删除文件
     */
    public void deleteFile(Long fileId) throws Exception {
        FileUpload upload = getById(fileId);
        if (upload == null) return;

        String objectName = upload.getFileUrl()
                .substring(upload.getFileUrl().indexOf(bucket) + bucket.length() + 1);

        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build());

        removeById(fileId);
    }
}

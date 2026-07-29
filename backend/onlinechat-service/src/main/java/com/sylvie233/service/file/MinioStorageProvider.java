package com.sylvie233.service.file;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 存储实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioStorageProvider implements StorageProvider {

    private final MinioClient minioClient;

    @Value("${minio.bucket:onlinechat}")
    private String bucket;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Override
    public String upload(MultipartFile file, String objectName) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        return endpoint + "/" + bucket + "/" + objectName;
    }

    @Override
    public String upload(byte[] data, String objectName, String contentType) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(new ByteArrayInputStream(data), data.length, -1)
                .contentType(contentType)
                .build());
        return endpoint + "/" + bucket + "/" + objectName;
    }

    @Override
    public String getPresignedUrl(String objectName, long expireSeconds) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectName)
                        .expiry((int) expireSeconds, TimeUnit.SECONDS)
                        .build());
    }

    @Override
    public void delete(String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build());
    }

    @Override
    public String getType() {
        return "minio";
    }
}

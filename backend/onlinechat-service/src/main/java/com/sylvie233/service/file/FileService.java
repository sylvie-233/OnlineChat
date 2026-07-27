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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件服务 — MinIO 对象存储，支持上传/下载/删除/预签名/缩略图
 * <p>可通过实现 StorageProvider 接口扩展到 OSS / COS / 本地存储</p>
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
     * 上传文件到 MinIO
     *
     * @param file     上传文件
     * @param userId   上传者ID
     * @param fileType 文件分类: image / audio / video / file
     * @return 上传记录（含 MinIO URL）
     */
    public FileUpload upload(MultipartFile file, Long userId, String fileType) throws Exception {
        String date = LocalDateTime.now().toLocalDate().toString();
        String objectName = fileType + "/" + date + "/" + UUID.randomUUID()
                + "_" + file.getOriginalFilename();

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());

        FileUpload upload = new FileUpload();
        upload.setUserId(userId);
        upload.setFileName(file.getOriginalFilename());
        upload.setFileUrl(endpoint + "/" + bucket + "/" + objectName);
        upload.setFileType(fileType);
        upload.setMimeType(file.getContentType());
        upload.setFileSize(file.getSize());
        upload.setStorageType("minio");
        upload.setStatus(1);

        // 图片/视频：提取宽高并生成缩略图
        if ("image".equals(fileType)) {
            try {
                BufferedImage img = ImageIO.read(file.getInputStream());
                if (img != null) {
                    upload.setWidth(img.getWidth());
                    upload.setHeight(img.getHeight());
                    // 生成缩略图
                    String thumbName = fileType + "/" + date + "/thumb_" + UUID.randomUUID() + ".jpg";
                    upload.setThumbnailUrl(generateThumbnailAndUpload(img, thumbName));
                }
            } catch (Exception e) {
                log.warn("缩略图生成失败: {}", e.getMessage());
            }
        }

        save(upload);
        log.info("文件上传成功: fileId={}, name={}, size={}KB, type={}",
                upload.getId(), file.getOriginalFilename(), file.getSize() / 1024, fileType);
        return upload;
    }

    /**
     * 生成预签名访问 URL（7 天有效）
     */
    public String getPresignedUrl(Long fileId) throws Exception {
        FileUpload upload = getById(fileId);
        if (upload == null) {
            log.warn("文件不存在: fileId={}", fileId);
            return null;
        }
        String objectName = upload.getFileUrl()
                .substring(upload.getFileUrl().indexOf(bucket) + bucket.length() + 1);
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder().method(Method.GET)
                        .bucket(bucket).object(objectName).expiry(7, TimeUnit.DAYS).build());
        log.debug("生成预签名URL: fileId={}", fileId);
        return url;
    }

    /**
     * 删除文件（MinIO + DB）
     */
    public void deleteFile(Long fileId) throws Exception {
        FileUpload upload = getById(fileId);
        if (upload == null) return;

        String objectName = upload.getFileUrl()
                .substring(upload.getFileUrl().indexOf(bucket) + bucket.length() + 1);
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
        removeById(fileId);
        log.info("文件已删除: fileId={}, objectName={}", fileId, objectName);
    }

    /**
     * 更新上传状态（上传中/成功/失败）
     */
    public void updateStatus(Long fileId, int status) {
        FileUpload upload = new FileUpload();
        upload.setId(fileId);
        upload.setStatus(status);
        updateById(upload);
    }

    // ==================== 内部 ====================

    /**
     * 生成缩略图并上传到 MinIO，返回缩略图 URL
     */
    private String generateThumbnailAndUpload(BufferedImage original, String objectName) throws Exception {
        int thumbWidth = 200;
        int thumbHeight = (int) (original.getHeight() * (200.0 / original.getWidth()));
        BufferedImage thumb = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        thumb.createGraphics().drawImage(
                original.getScaledInstance(thumbWidth, thumbHeight, java.awt.Image.SCALE_SMOOTH), 0, 0, null);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(thumb, "jpg", os);
        byte[] bytes = os.toByteArray();

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket).object(objectName)
                .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                .contentType("image/jpeg").build());

        log.debug("缩略图已生成: {}", objectName);
        return endpoint + "/" + bucket + "/" + objectName;
    }
}

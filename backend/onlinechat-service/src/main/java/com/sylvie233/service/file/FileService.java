package com.sylvie233.service.file;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.repository.entity.FileUpload;
import com.sylvie233.repository.mapper.FileUploadMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件服务 — 通过 StorageProvider 接口接入存储后端
 * <p>默认使用 MinIO，切换存储只需更换注入的 StorageProvider 实现</p>
 */
@Slf4j
@Service
public class FileService extends ServiceImpl<FileUploadMapper, FileUpload> {

    private final StorageProvider storage;

    public FileService(FileUploadMapper mapper, StorageProvider storage) {
        this.storage = storage;
    }

    /**
     * 上传文件
     */
    public FileUpload upload(MultipartFile file, Long userId, String fileType) throws Exception {
        String date = LocalDateTime.now().toLocalDate().toString();
        String objectName = fileType + "/" + date + "/" + UUID.randomUUID()
                + "_" + file.getOriginalFilename();

        String fileUrl = storage.upload(file, objectName);

        FileUpload upload = new FileUpload();
        upload.setUserId(userId);
        upload.setFileName(file.getOriginalFilename());
        upload.setFileUrl(fileUrl);
        upload.setFileType(fileType);
        upload.setMimeType(file.getContentType());
        upload.setFileSize(file.getSize());
        upload.setStorageType(storage.getType());
        upload.setStatus(1);

        // 图片：提取宽高 + 生成缩略图
        if ("image".equals(fileType)) {
            try {
                BufferedImage img = ImageIO.read(file.getInputStream());
                if (img != null) {
                    upload.setWidth(img.getWidth());
                    upload.setHeight(img.getHeight());
                    String thumbName = fileType + "/" + date + "/thumb_" + UUID.randomUUID() + ".jpg";
                    String thumbUrl = generateThumbnailAndUpload(img, thumbName);
                    upload.setThumbnailUrl(thumbUrl);
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
        String objectName = extractObjectName(upload.getFileUrl());
        return storage.getPresignedUrl(objectName, 7 * 86400);
    }

    /**
     * 删除文件
     */
    public void deleteFile(Long fileId) throws Exception {
        FileUpload upload = getById(fileId);
        if (upload == null) return;
        String objectName = extractObjectName(upload.getFileUrl());
        storage.delete(objectName);
        removeById(fileId);
        log.info("文件已删除: fileId={}, objectName={}", fileId, objectName);
    }

    /**
     * 更新上传状态
     */
    @Transactional
    public void updateStatus(Long fileId, int status) {
        FileUpload upload = new FileUpload();
        upload.setId(fileId);
        upload.setStatus(status);
        updateById(upload);
    }

    // ==================== 内部 ====================

    private String generateThumbnailAndUpload(BufferedImage original, String objectName) throws Exception {
        int thumbWidth = 200;
        int thumbHeight = (int) (original.getHeight() * (200.0 / original.getWidth()));

        // JPEG 不支持透明通道，统一用 RGB + 白底
        BufferedImage thumb = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumb.createGraphics();
        try {
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, thumbWidth, thumbHeight);
            g2d.drawImage(
                    original.getScaledInstance(thumbWidth, thumbHeight, Image.SCALE_SMOOTH), 0, 0, null);
        } finally {
            g2d.dispose();
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        boolean written = ImageIO.write(thumb, "jpg", os);
        if (!written) {
            throw new java.io.IOException("JPEG 编码器不可用");
        }
        byte[] bytes = os.toByteArray();

        String thumbUrl = storage.upload(bytes, objectName, "image/jpeg");
        log.debug("缩略图已生成并上传: {} ({}B)", objectName, bytes.length);
        return thumbUrl;
    }

    private String extractObjectName(String fileUrl) {
        if (fileUrl == null) return "";
        int idx = fileUrl.indexOf("onlinechat/");
        if (idx >= 0) return fileUrl.substring(idx + "onlinechat/".length());
        // fallback: 取最后两段
        String[] parts = fileUrl.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        }
        return fileUrl;
    }
}

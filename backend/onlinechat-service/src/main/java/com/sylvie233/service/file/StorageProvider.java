package com.sylvie233.service.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * 存储后端抽象 — 支持 MinIO / 阿里云OSS / 腾讯云COS / 本地存储
 * <p>
 * 实现本接口即可接入新的存储后端，
 * FileService 通过构造函数注入具体的 Provider 实例
 * </p>
 */
public interface StorageProvider {

    /**
     * 上传文件
     * @param file     文件
     * @param objectName 对象名（含路径，如 image/2024-01/img_xxx.jpg）
     * @return 文件访问 URL
     */
    String upload(MultipartFile file, String objectName) throws Exception;

    /**
     * 上传字节数组（用于缩略图等内存生成的图片）
     * @param data        文件字节
     * @param objectName  对象名
     * @param contentType MIME 类型
     * @return 文件访问 URL
     */
    String upload(byte[] data, String objectName, String contentType) throws Exception;

    /**
     * 生成预签名访问 URL
     * @param objectName 对象名
     * @param expireSeconds 有效期（秒）
     * @return 预签名 URL
     */
    String getPresignedUrl(String objectName, long expireSeconds) throws Exception;

    /**
     * 删除文件
     * @param objectName 对象名
     */
    void delete(String objectName) throws Exception;

    /**
     * 存储后端标识
     */
    String getType();
}

package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.FileUpload;
import com.sylvie233.service.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口 — 上传/下载/删除
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<FileUpload> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "file") String fileType) {
        Long userId = StpUtil.getLoginIdAsLong();
        try {
            FileUpload upload = fileService.upload(file, userId, fileType);
            return Result.ok(upload);
        } catch (Exception e) {
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件访问 URL（预签名）
     */
    @GetMapping("/{fileId}/url")
    public Result<String> getFileUrl(@PathVariable Long fileId) {
        try {
            String url = fileService.getPresignedUrl(fileId);
            return Result.ok(url);
        } catch (Exception e) {
            return Result.fail("获取文件URL失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public Result<?> deleteFile(@PathVariable Long fileId) {
        try {
            fileService.deleteFile(fileId);
            return Result.ok();
        } catch (Exception e) {
            return Result.fail("删除失败: " + e.getMessage());
        }
    }
}

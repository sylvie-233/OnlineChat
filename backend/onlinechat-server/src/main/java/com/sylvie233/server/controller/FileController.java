package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.FileUpload;
import com.sylvie233.service.file.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口 — 上传/下载/删除
 */
@Tag(name = "文件")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<FileUpload> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "file") String fileType) {
        try {
            return Result.ok(fileService.upload(file, StpUtil.getLoginIdAsLong(), fileType));
        } catch (Exception e) {
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取文件访问URL")
    @GetMapping("/{fileId}/url")
    public Result<String> getFileUrl(@PathVariable Long fileId) {
        try {
            return Result.ok(fileService.getPresignedUrl(fileId));
        } catch (Exception e) {
            return Result.fail("获取文件URL失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除文件")
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

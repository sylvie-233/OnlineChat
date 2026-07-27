package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sylvie233.common.model.resp.PageResult;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.Notification;
import com.sylvie233.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知接口 — 通知列表、未读数、标记已读
 */
@Tag(name = "通知")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "获取通知列表(分页)")
    @GetMapping("/list")
    public Result<?> getNotifications(@RequestParam(name = "page", defaultValue = "1") int page,
                                       @RequestParam(name = "size", defaultValue = "20") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<Notification> pageResult = notificationService.getNotifications(userId, page, size);
        return PageResult.of(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }

    @Operation(summary = "获取未读通知数")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        return Result.ok(notificationService.getUnreadCount(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "标记单条已读")
    @PutMapping("/{id}/read")
    public Result<?> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<?> markAllAsRead() {
        notificationService.markAllAsRead(StpUtil.getLoginIdAsLong());
        return Result.ok();
    }
}

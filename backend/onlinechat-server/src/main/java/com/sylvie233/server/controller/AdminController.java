package com.sylvie233.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.User;
import com.sylvie233.service.user.UserService;
import com.sylvie233.service.group.GroupService;
import com.sylvie233.service.message.MessageQueueProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台接口 — 用户管理/封禁/统计
 */
@Tag(name = "管理后台")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final GroupService groupService;
    private final MessageQueueProducer queueProducer;

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<?> listUsers(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getNickname, keyword)
                    .or().like(User::getUsername, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> userPage = userService.page(new Page<>(page, size), wrapper);
        userPage.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(userPage);
    }

    @Operation(summary = "封禁/解封用户")
    @PutMapping("/users/{userId}/ban")
    public Result<?> banUser(@PathVariable Long userId,
                              @RequestBody Map<String, Object> body) {
        Integer targetStatus = (Integer) body.getOrDefault("status", 2);
        User user = userService.getById(userId);
        if (user == null) return Result.notFound();
        // 封禁：设为2；解封：仅当当前是封禁状态时才恢复为0
        if (targetStatus == 2) {
            user.setStatus(2);
        } else {
            user.setStatus(user.getStatus() == 2 ? 0 : user.getStatus());
        }
        userService.updateById(user);
        return Result.ok(targetStatus == 2 ? "已封禁" : "已解封");
    }

    @Operation(summary = "禁言/取消禁言用户")
    @PutMapping("/users/{userId}/mute")
    public Result<?> muteUser(@PathVariable Long userId,
                               @RequestBody Map<String, Boolean> body) {
        Boolean muted = body.getOrDefault("muted", true);
        User user = userService.getById(userId);
        if (user == null) return Result.notFound();
        // 禁言仅对正常用户生效，已封禁的用户不应被禁言覆盖
        if (muted && user.getStatus() == 0) {
            user.setStatus(1); // 禁言
        } else if (!muted && user.getStatus() == 1) {
            user.setStatus(0); // 取消禁言，恢复正常
        }
        userService.updateById(user);
        return Result.ok(muted ? "已禁言" : "已取消禁言");
    }

    @Operation(summary = "系统统计概览")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        long userCount = userService.count();
        long groupCount = groupService.count();
        long onlineCount = userService.lambdaQuery()
                .eq(User::getOnlineStatus, 1).count();

        return Result.ok(Map.of(
                "userCount", userCount,
                "groupCount", groupCount,
                "onlineCount", onlineCount));
    }

    @Operation(summary = "在线用户列表")
    @GetMapping("/users/online")
    public Result<?> getOnlineUsers() {
        return Result.ok(userService.lambdaQuery()
                .eq(User::getOnlineStatus, 1)
                .list().stream()
                .peek(u -> u.setPassword(null))
                .toList());
    }

    @Operation(summary = "消息队列状态")
    @GetMapping("/queue-stats")
    public Result<?> getQueueStats() {
        return Result.ok(Map.of(
                "enabled", queueProducer.isEnabled(),
                "queueSize", queueProducer.queueSize()));
    }
}

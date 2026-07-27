package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sylvie233.common.model.dto.UpdateProfileRequest;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.User;
import com.sylvie233.repository.entity.UserSetting;
import com.sylvie233.service.user.UserService;
import com.sylvie233.service.user.UserSettingService;
import com.sylvie233.service.user.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户接口 — 信息/资料/设置/在线状态/会话管理
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSettingService userSettingService;
    private final SessionService sessionService;

    @Operation(summary = "获取用户信息")
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) return Result.notFound();
        user.setPassword(null);
        return Result.ok(user);
    }

    @Operation(summary = "搜索用户")
    @GetMapping("/search")
    public Result<User> searchByUsername(@RequestParam String username) {
        User user = userService.getByUsername(username);
        if (user == null) return Result.notFound();
        user.setPassword(null);
        return Result.ok(user);
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody UpdateProfileRequest body) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) return Result.notFound();
        if (body.getNickname() != null) user.setNickname(body.getNickname());
        if (body.getAvatar() != null) user.setAvatar(body.getAvatar());
        if (body.getPhone() != null) user.setPhone(body.getPhone());
        if (body.getEmail() != null) user.setEmail(body.getEmail());
        if (body.getGender() != null) user.setGender(body.getGender());
        if (body.getBio() != null) user.setBio(body.getBio());
        if (body.getRegion() != null) user.setRegion(body.getRegion());
        userService.updateById(user);
        return Result.ok("更新成功");
    }

    @Operation(summary = "更新在线状态")
    @PutMapping("/online-status")
    public Result<?> updateOnlineStatus(@RequestBody Map<String, Integer> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        Integer status = body.get("status");
        if (status == null || status < 0 || status > 3) {
            return Result.fail("无效的在线状态");
        }
        userService.updateOnlineStatus(userId, status);
        return Result.ok();
    }

    @Operation(summary = "获取个人设置")
    @GetMapping("/settings")
    public Result<UserSetting> getSettings() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userSettingService.getOrInit(userId));
    }

    @Operation(summary = "更新个人设置")
    @PutMapping("/settings")
    public Result<?> updateSettings(@RequestBody UserSetting body) {
        Long userId = StpUtil.getLoginIdAsLong();
        userSettingService.updateSetting(userId, body);
        return Result.ok("更新成功");
    }

    @Operation(summary = "获取活跃会话列表")
    @GetMapping("/sessions")
    public Result<?> getSessions() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(sessionService.getActiveSessions(userId));
    }

    @Operation(summary = "强制登出会话")
    @PostMapping("/sessions/{sessionId}/kick")
    public Result<?> kickSession(@PathVariable Long sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        sessionService.kickSession(sessionId, userId);
        return Result.ok();
    }
}

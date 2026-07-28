package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.sylvie233.common.model.dto.LoginRequest;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.User;
import com.sylvie233.service.cache.RedisCacheService;
import com.sylvie233.service.user.UserService;
import com.sylvie233.service.user.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口 — 登录/注册/登出/Token刷新
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SessionService sessionService;
    private final RedisCacheService redisCacheService;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<?> register(@RequestBody LoginRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            return Result.fail("用户名不能为空");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            return Result.fail("密码至少 6 位");
        }
        User exist = userService.getByUsername(req.getUsername());
        if (exist != null) return Result.fail("用户名已存在");

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(BCrypt.hashpw(req.getPassword(), BCrypt.gensalt()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        user.setStatus(0);
        userService.save(user);
        return Result.ok("注册成功");
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest req,
                                              @RequestHeader(value = "X-Device-Type", defaultValue = "web") String deviceType,
                                              @RequestHeader(value = "X-Device-Name", defaultValue = "") String deviceName,
                                              @RequestHeader(value = "X-Device-Id", defaultValue = "") String deviceId,
                                              @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {
        User user = userService.getByUsername(req.getUsername());
        if (user == null || !BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }
        if (user.getStatus() == 2) return Result.fail("账号已被封禁");

        StpUtil.login(user.getId());
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        user.setOnlineStatus(1);
        userService.updateById(user);

        String token = StpUtil.getTokenValue();
        sessionService.createSession(user.getId(), token, deviceType,
                deviceName, deviceId, clientIp);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        return Result.ok(result);
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<?> logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        userService.offline(userId);
        redisCacheService.setOffline(userId);
        return Result.ok();
    }

    @Operation(summary = "刷新Token — 退出当前token并重新登录")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refreshToken() {
        Long userId = StpUtil.getLoginIdAsLong();
        String oldToken = StpUtil.getTokenValue();
        StpUtil.logout(userId);
        StpUtil.login(userId);
        String newToken = StpUtil.getTokenValue();

        // 更新 user_session 表中的 token
        sessionService.updateToken(userId, oldToken, newToken);

        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("userId", userId);
        return Result.ok(result);
    }
}

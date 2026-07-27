package com.sylvie233.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.User;
import com.sylvie233.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口 — 登录/注册/登出
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        User exist = userService.getByUsername(user.getUsername());
        if (exist != null) {
            return Result.fail("用户名已存在");
        }
        // TODO: 密码 BCrypt 加密
        user.setStatus(0);
        userService.save(user);
        return Result.ok("注册成功");
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.fail("用户名或密码错误");
        }
        // TODO: BCrypt 密码校验
        if (!password.equals(user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }
        if (user.getStatus() == 2) {
            return Result.fail("账号已被封禁");
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());

        // 更新最后登录时间和在线状态
        user.setLastLoginTime(LocalDateTime.now());
        user.setOnlineStatus(1);
        userService.updateById(user);

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        return Result.ok(result);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        userService.offline(userId);
        return Result.ok();
    }
}

package com.sylvie233.server.controller;

import com.sylvie233.common.model.resp.Result;
import com.sylvie233.repository.entity.User;
import com.sylvie233.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.notFound();
        }
        user.setPassword(null); // 脱敏
        return Result.ok(user);
    }

    /**
     * 根据用户名搜索
     */
    @GetMapping("/search")
    public Result<User> searchByUsername(@RequestParam String username) {
        User user = userService.getByUsername(username);
        if (user == null) {
            return Result.notFound();
        }
        user.setPassword(null);
        return Result.ok(user);
    }
}

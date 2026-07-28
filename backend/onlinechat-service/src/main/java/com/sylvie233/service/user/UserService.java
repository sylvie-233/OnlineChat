package com.sylvie233.service.user;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.common.enums.OnlineStatus;
import com.sylvie233.common.exception.BizException;
import com.sylvie233.repository.entity.User;
import com.sylvie233.repository.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务 — 用户查询、在线状态管理、封禁/禁言校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    /**
     * 根据用户名精确查询
     */
    public User getByUsername(String username) {
        User user = lambdaQuery().eq(User::getUsername, username).one();
        if (user != null) log.debug("查询用户: username={}", username);
        return user;
    }

    /**
     * 模糊搜索用户（匹配 username 或 nickname）
     */
    public List<User> searchUsers(String keyword) {
        return lambdaQuery()
                .like(User::getUsername, keyword)
                .or()
                .like(User::getNickname, keyword)
                .last("LIMIT 20")
                .list();
    }

    /**
     * 用户上线 — 将 onlineStatus 设为 1 (在线)
     */
    @Transactional
    public void online(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setOnlineStatus(OnlineStatus.ONLINE.getCode());
        updateById(user);
        log.info("用户上线: userId={}", userId);
    }

    /**
     * 用户离线 — 将 onlineStatus 设为 0 (离线)
     */
    @Transactional
    public void offline(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setOnlineStatus(OnlineStatus.OFFLINE.getCode());
        updateById(user);
        log.info("用户离线: userId={}", userId);
    }

    /**
     * 校验用户状态 — 封禁/禁言时抛出 BizException
     */
    public void checkStatus(Long userId) {
        User user = getById(userId);
        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            throw BizException.of(404, "用户不存在");
        }
        if (user.getStatus() == 2) {
            log.warn("用户已封禁: userId={}", userId);
            throw BizException.of(403, "账号已被封禁");
        }
        if (user.getStatus() == 1) {
            log.debug("用户已禁言: userId={}", userId);
        }
    }

    /**
     * 更新在线状态 (在线/离线/隐身/忙碌)
     */
    @Transactional
    public void updateOnlineStatus(Long userId, Integer status) {
        User user = new User();
        user.setId(userId);
        user.setOnlineStatus(status);
        updateById(user);
        log.info("在线状态变更: userId={}, status={}", userId, status);
    }
}

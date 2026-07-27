package com.sylvie233.service.user;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.common.enums.OnlineStatus;
import com.sylvie233.common.exception.BizException;
import com.sylvie233.repository.entity.User;
import com.sylvie233.repository.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final UserMapper userMapper;

    /**
     * 根据用户名查询
     */
    public User getByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    /**
     * 用户上线
     */
    @Transactional
    public void online(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setOnlineStatus(OnlineStatus.ONLINE.getCode());
        updateById(user);
    }

    /**
     * 用户离线
     */
    @Transactional
    public void offline(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setOnlineStatus(OnlineStatus.OFFLINE.getCode());
        updateById(user);
    }

    /**
     * 检查用户是否被封禁/禁言
     */
    public void checkStatus(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw BizException.of(404, "用户不存在");
        }
        if (user.getStatus() == 2) {
            throw BizException.of(403, "账号已被封禁");
        }
    }
}

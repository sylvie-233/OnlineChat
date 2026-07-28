package com.sylvie233.service.user;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sylvie233.repository.entity.UserSetting;
import com.sylvie233.repository.mapper.UserSettingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户设置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingService extends ServiceImpl<UserSettingMapper, UserSetting> {

    /**
     * 获取或初始化用户设置
     */
    public UserSetting getOrInit(Long userId) {
        UserSetting setting = lambdaQuery().eq(UserSetting::getUserId, userId).one();
        if (setting == null) {
            setting = new UserSetting();
            setting.setUserId(userId);
            setting.setMsgNotifyEnabled(1);
            setting.setSoundEnabled(1);
            setting.setVibrateEnabled(1);
            setting.setShowDetailEnabled(1);
            setting.setFriendVerifyType(1);
            setting.setGroupInviteVerify(1);
            setting.setTheme("light");
            setting.setLanguage("zh-CN");
            setting.setFontSize("medium");
            save(setting);
        }
        return setting;
    }

    /**
     * 更新用户设置
     */
    @Transactional
    public void updateSetting(Long userId, UserSetting setting) {
        UserSetting exist = getOrInit(userId);
        setting.setId(exist.getId());
        updateById(setting);
    }
}

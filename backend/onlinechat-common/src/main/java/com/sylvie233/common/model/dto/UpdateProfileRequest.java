package com.sylvie233.common.model.dto;

import lombok.Data;

/**
 * 更新个人资料请求
 */
@Data
public class UpdateProfileRequest {
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private Integer gender;
    private String bio;
    private String birthday;
    private String region;
}

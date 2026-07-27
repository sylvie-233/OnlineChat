package com.sylvie233.common.model.dto;

import lombok.Data;

/**
 * 登录/注册请求
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
    private String nickname;
}

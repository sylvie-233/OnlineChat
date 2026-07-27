package com.sylvie233.common.model.dto;

import lombok.Data;

/**
 * 注册请求
 */
@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;
}

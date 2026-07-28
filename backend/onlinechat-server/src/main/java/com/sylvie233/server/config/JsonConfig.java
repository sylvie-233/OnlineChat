package com.sylvie233.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 序列化配置 — 统一 Long 序列化行为
 * <p>
 * Jackson（HTTP REST）和 Fastjson2（WebSocket）均保持 Long 写为数字，
 * HTTP 和 WebSocket 两端 ID 类型一致，前端无需做 string/number 转换。
 * <p>
 * 当前项目使用 AUTO_INCREMENT ID，值远小于 JS 安全整数 2^53，不存在精度丢失。
 */
@Slf4j
@Configuration
public class JsonConfig {
    // 使用默认序列化行为：Long → number，不做任何全局转换
}

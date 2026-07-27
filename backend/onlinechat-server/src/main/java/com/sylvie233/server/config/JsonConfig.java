package com.sylvie233.server.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * JSON 序列化配置 — 仅 Snowflake ID (Long) 转 String，防止 JS 精度丢失
 */
@Slf4j
@Configuration
public class JsonConfig {

    /**
     * Jackson（HTTP REST）— Message.id 序列化为 String
     */
    @Bean
    public com.fasterxml.jackson.databind.Module snowflakeIdModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, new JsonSerializer<Long>() {
            @Override
            public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                // 只对超过 JS 安全整数范围的 Long 转 String（Snowflake ID）
                if (value > 9007199254740992L) { // 2^53
                    gen.writeString(value.toString());
                } else {
                    gen.writeNumber(value);
                }
            }
        });
        return module;
    }

    /**
     * Fastjson2（WebSocket / ImPacket）— 全局 Long 转 String
     */
    @PostConstruct
    public void configureFastjson2() {
        com.alibaba.fastjson2.JSON.config(
                com.alibaba.fastjson2.JSONWriter.Feature.WriteLongAsString
        );
        log.info("Fastjson2 已配置: Long -> String（防止 JS 精度丢失）");
    }
}

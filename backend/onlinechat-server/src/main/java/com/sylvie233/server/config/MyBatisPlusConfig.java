package com.sylvie233.server.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.sylvie233.common.util.SnowflakeIdWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置 — 注入自定义 ID 生成器
 * <p>
 * 替换默认的 64-bit Snowflake，确保 Message 等实体的 ASSIGN_ID
 * 也生成小 ID（&lt; 2^53），与 MessageQueueProducer 共用一个 Worker。
 */
@Configuration
public class MyBatisPlusConfig {

    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Bean
    public IdentifierGenerator customIdGenerator() {
        return new IdentifierGenerator() {
            @Override
            public Number nextId(Object entity) {
                return idWorker.nextId();
            }

            @Override
            public String nextUUID(Object entity) {
                return IdentifierGenerator.super.nextUUID(entity);
            }
        };
    }
}

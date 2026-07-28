package com.sylvie233.server.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.sylvie233.common.util.SnowflakeIdWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置 — 自定义 ID 生成器 + 分页插件
 */
@Configuration
public class MyBatisPlusConfig {

    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 指定数据库类型为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

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

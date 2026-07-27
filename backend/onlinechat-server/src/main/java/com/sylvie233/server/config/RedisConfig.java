package com.sylvie233.server.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.StandardCharsets;

/**
 * Redis 配置
 * <p>
 * 序列化策略: Key 用 String, Value 用 Fastjson2 JSON
 * <br>
 * 用途: 在线状态、Channel 映射、消息 seq、分布式锁、缓存
 * </p>
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate — 通用 KV 操作
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Fastjson2 序列化器
        FastJsonRedisSerializer<Object> valueSerializer = new FastJsonRedisSerializer<>(Object.class);

        // Key 用 String
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redis 消息监听容器（跨节点广播、Key 过期监听）
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        // 线程池大小
        container.setSubscriptionExecutor(
                java.util.concurrent.Executors.newFixedThreadPool(4));
        return container;
    }

    // ==================== Fastjson2 序列化器 ====================

    public static class FastJsonRedisSerializer<T> implements RedisSerializer<T> {

        private static final FastJsonConfig CONFIG = new FastJsonConfig();

        static {
            CONFIG.setReaderFeatures(
                    JSONReader.Feature.FieldBased,
                    JSONReader.Feature.SupportArrayToBean
            );
            CONFIG.setWriterFeatures(
                    JSONWriter.Feature.WriteClassName,
                    JSONWriter.Feature.NotWriteDefaultValue
            );
        }

        private final Class<T> clazz;

        public FastJsonRedisSerializer(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public byte[] serialize(T obj) throws SerializationException {
            if (obj == null) {
                return new byte[0];
            }
            return JSON.toJSONBytes(obj, CONFIG.getWriterFeatures());
        }

        @Override
        public T deserialize(byte[] bytes) throws SerializationException {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return JSON.parseObject(new String(bytes, StandardCharsets.UTF_8),
                    clazz, CONFIG.getReaderFeatures());
        }
    }
}

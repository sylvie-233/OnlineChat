package com.sylvie233.common.util;

/**
 * 雪花算法 — 分布式唯一 ID 生成器（轻量版）
 * <p>
 * 针对 100 人以下的小规模部署精简位宽，保证生成的 ID 始终在
 * JavaScript 安全整数范围内（&lt; 2^53），前端不必转换 String。
 * </p>
 *
 * <pre>
 * 位布局（46 bits total）:
 *   [1 unused] [37 bits 毫秒时间戳] [8 bits 序列号]
 *   时间戳范围: 2^37 ms ≈ 4.3 年（epoch 2025-01-01，可用至 ~2029）
 *   序列号范围: 0~255 / ms（对 100 用户绰绰有余）
 *   最大 ID  ≈ 2^45 ≈ 3.5×10^13（14 位十进制，远小于 2^53）
 * </pre>
 */
public class SnowflakeIdWorker {

    /** 起始时间戳 (2025-01-01 00:00:00 UTC) */
    private static final long START_TIMESTAMP = 1735689600000L;

    private static final long SEQUENCE_BITS = 8L;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 255

    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS; // 8

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdWorker() {
        // 单实例部署，不需要 workerId / datacenterId
    }

    /** 保留双参构造兼容旧调用方，参数直接忽略 */
    @Deprecated
    public SnowflakeIdWorker(long workerId, long datacenterId) {
        this();
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成 ID");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT) | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}

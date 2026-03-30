package com.bilibili.im.common.id.impl;

import com.bilibili.im.common.id.MessageIdGenerator;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeMessageIdGenerator implements MessageIdGenerator {

    private static final long EPOCH = 1735689600000L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private static final long WORKER_ID = 0L;
    private static final long DATACENTER_ID = 0L;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeMessageIdGenerator() {
        if (WORKER_ID > MAX_WORKER_ID || DATACENTER_ID > MAX_DATACENTER_ID) {
            throw new IllegalStateException("snowflake worker or datacenter id is invalid");
        }
    }

    @Override
    public synchronized long nextId() {
        long timestamp = currentTimestamp();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("clock moved backwards");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (DATACENTER_ID << DATACENTER_ID_SHIFT)
                | (WORKER_ID << WORKER_ID_SHIFT)
                | sequence;
    }

    private static long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = currentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimestamp();
        }
        return timestamp;
    }

    private static long currentTimestamp() {
        return System.currentTimeMillis();
    }
}

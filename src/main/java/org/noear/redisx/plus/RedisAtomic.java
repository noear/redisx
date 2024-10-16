package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;

/**
 * Redis 原子数字
 *
 * @author noear
 * @since 1.0
 */
public class RedisAtomic {
    private final RedisClient client;
    private final String atomicName;
    private final int inSeconds;//永久:-1

    public RedisAtomic(RedisClient client, String atomicName) {
        this(client, atomicName, -1);
    }

    public RedisAtomic(RedisClient client, String atomicName, int inSeconds) {
        this.client = client;
        this.atomicName = atomicName;
        this.inSeconds = inSeconds;
    }

    /**
     * 获取值
     */
    public long get() {
        return client.openAndGet(s -> s.key(atomicName).getAsLong());
    }

    /**
     * 原子增量（不设置时间，即为永久）
     */
    public long increment() {
        return client.openAndGet(s -> s.key(atomicName).expire(inSeconds).incr());
    }

    /**
     * 原子增量（不设置时间，即为永久）
     */
    public long incrementBy(long num) {
        return client.openAndGet(s -> s.key(atomicName).expire(inSeconds).incr(num));
    }

    /**
     * 原子减量（不设置时间，即为永久）
     */
    public long decrement() {
        return client.openAndGet(s -> s.key(atomicName).expire(inSeconds).decr());
    }

    /**
     * 原子减量（不设置时间，即为永久）
     */
    public long decrementBy(long num) {
        return client.openAndGet(s -> s.key(atomicName).expire(inSeconds).incr(-num));
    }
}

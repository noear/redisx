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

    public RedisAtomic(RedisClient client, String atomicName) {
        this.client = client;
        this.atomicName = atomicName;
    }

    /**
     * 获取值
     * */
    public long get(){
        return client.openAndGet(session -> session.key(atomicName).getAsLong());
    }

    /**
     * 原子增量
     * */
    public long increment() {
        return client.openAndGet(session -> session.key(atomicName).expire(-2).incr());
    }

    /**
     * 原子增量
     * */
    public long incrementBy(long num) {
        return client.openAndGet(session -> session.key(atomicName).expire(-2).incr(num));
    }

    /**
     * 原子减量
     * */
    public long decrement() {
        return client.openAndGet(session -> session.key(atomicName).expire(-2).decr());
    }

    /**
     * 原子减量
     * */
    public long decrementBy(long num) {
        return client.openAndGet(session -> session.key(atomicName).expire(-2).incr(-num));
    }
}

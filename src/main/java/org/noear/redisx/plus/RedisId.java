package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;

/**
 * Redis Id生成器
 *
 * @author noear
 * @since 1.0
 * */
public class RedisId {
    private final RedisClient client;
    private final String idName;

    public RedisId(RedisClient client, String idName) {
        this.client = client;
        this.idName = idName;
    }

    /**
     * 生成（不设置时间，即为永久）
     */
    public long generate() {
        return client.openAndGet((s) -> s.key(idName).incr());
    }

    /**
     * 生成
     *
     * @param inSeconds 有效秒数
     */
    public long generate(int inSeconds) {
        return client.openAndGet((s) -> s.key(idName).expire(inSeconds).incr());
    }
}
package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;

/**
 * @author noear
 * @since 1.5
 */
public class RedisCache {
    private final RedisClient client;

    public RedisCache(RedisClient client) {
        this.client = client;
    }


    public void store(String key, String val, int inSeconds) {
        client.open(session -> session.key(key).expire(inSeconds).set(val));
    }

    public String get(String key) {
        return client.openAndGet(session -> session.key(key).get());
    }

    public void remove(String key) {
        client.open(session -> session.key(key).delete());
    }
}

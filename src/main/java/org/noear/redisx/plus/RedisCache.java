package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;
import org.noear.redisx.utils.AssertUtil;
import org.noear.redisx.utils.SerializationUtil;

import java.util.Base64;

/**
 * Redis cache
 *
 * @author noear
 * @since 1.0
 */
public class RedisCache {
    private final RedisClient client;

    public RedisCache(RedisClient client) {
        this.client = client;
    }


    /**
     * 存储
     * */
    public void store(String key, String val, int inSeconds) {
        client.open(session -> session.key(key).expire(inSeconds).set(val));
    }

    /**
     * 存储并序列化
     * */
    public void storeAndSerialize(String key, Object obj, int inSeconds) {
        AssertUtil.notNull(obj,"redis value cannot be null");

        byte[] bytes = SerializationUtil.serialize(obj);
        String val = Base64.getEncoder().encodeToString(bytes);

        client.open(session -> session.key(key).expire(inSeconds).set(val));
    }

    /**
     * 获取
     * */
    public String get(String key) {
        return client.openAndGet(session -> session.key(key).get());
    }

    /**
     * 获取并反序列化
     * */
    public <T> T getAndDeserialize(String key) {
        String val = client.openAndGet(session -> session.key(key).get());

        if (val == null) {
            return null;
        } else {
            byte[] bytes = Base64.getDecoder().decode(val);
            return (T) SerializationUtil.deserialize(bytes);
        }
    }

    /**
     * 移除
     * */
    public void remove(String key) {
        client.open(session -> session.key(key).delete());
    }
}

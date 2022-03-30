package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;
import org.noear.redisx.utils.AssertUtil;
import org.noear.redisx.utils.SerializationUtil;

import java.util.Base64;
import java.util.function.Supplier;

/**
 * Redis bucket
 *
 * @author noear
 * @since 1.3
 */
public class RedisBucket {
    private final RedisClient client;

    public RedisBucket(RedisClient client) {
        this.client = client;
    }


    /**
     * 存储
     */
    public void store(String key, String val, int inSeconds) {
        client.open(s -> s.key(key).expire(inSeconds).set(val));
    }

    /**
     * 永久存储
     */
    public void store(String key, String val) {
        client.open(s -> s.key(key).persist().set(val));
    }

    /**
     * 存储并序列化
     */
    public void storeAndSerialize(String key, Object obj, int inSeconds) {
        AssertUtil.notNull(obj, "redis value cannot be null");

        byte[] bytes = SerializationUtil.serialize(obj);
        String val = Base64.getEncoder().encodeToString(bytes);

        client.open(s -> s.key(key).expire(inSeconds).set(val));
    }

    /**
     * 永久存储并序列化
     */
    public void storeAndSerialize(String key, Object obj) {
        AssertUtil.notNull(obj, "redis value cannot be null");

        byte[] bytes = SerializationUtil.serialize(obj);
        String val = Base64.getEncoder().encodeToString(bytes);

        client.open(s -> s.key(key).persist().set(val));
    }

    /**
     * 获取
     */
    public String get(String key) {
        return client.openAndGet(s -> s.key(key).get());
    }

    /**
     * 获取并反序列化
     */
    public <T> T getAndDeserialize(String key) {
        String val = client.openAndGet(s -> s.key(key).get());

        if (val == null) {
            return null;
        } else {
            byte[] bytes = Base64.getDecoder().decode(val);
            return (T) SerializationUtil.deserialize(bytes);
        }
    }

    public String getOrStore(String key, int inSeconds, Supplier<String> supplier) {
        String val = get(key);
        if (val == null) {
            val = supplier.get();
            store(key, val, inSeconds);
        }

        return val;
    }

    public <T> T getOrStoreAndSerialize(String key, int inSeconds, Supplier<T> supplier) {
        T val = getAndDeserialize(key);

        if (val == null) {
            val = supplier.get();
            storeAndSerialize(key, val, inSeconds);
        }

        return val;
    }

    /**
     * 移除
     */
    public void remove(String key) {
        client.open(s -> s.key(key).delete());
    }

    /**
     * 延期
     */
    public void delay(String key, int inSeconds) {
        client.open(s -> s.key(key).expire(inSeconds).delay());
    }

    /**
     * 获取剩余过期时间
     */
    public long ttl(String key) {
        return client.openAndGet(s -> s.key(key).ttl());
    }
}

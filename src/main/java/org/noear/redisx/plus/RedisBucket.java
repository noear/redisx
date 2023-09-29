package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;
import org.noear.redisx.utils.AssertUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
     * 存储（不设置时间，即为永久）
     */
    public void store(String key, String val) {
        client.open(s -> s.key(key).set(val));
    }

    /**
     * 存储并序列化
     */
    public void storeAndSerialize(String key, Object obj, int inSeconds) {
        AssertUtil.notNull(obj, "redis value cannot be null");

        String val = client.serializer().encode(obj);

        client.open(s -> s.key(key).expire(inSeconds).set(val));
    }

    /**
     * 存储并序列化（不设置时间，即为永久）
     */
    public void storeAndSerialize(String key, Object obj) {
        AssertUtil.notNull(obj, "redis value cannot be null");

        String val = client.serializer().encode(obj);

        client.open(s -> s.key(key).set(val));
    }

    /**
     * 获取
     */
    public String get(String key) {
        return client.openAndGet(s -> s.key(key).get());
    }

    /**
     * @deprecated 1.5
     * */
    @Deprecated
    public List<String> getByKeys(String... keys) {
        return getMore(keys);
    }

    /**
     * 获取更多
     * */
    public List<String> getMore(String... keys) {
        return client.openAndGet(s -> s.getMore(keys));
    }


    /**
     * @deprecated 1.5
     * */
    @Deprecated
    public <T> T getAndDeserialize(String key) {
        return (T)getAndDeserialize( key, Object.class);
    }

    /**
     * 获取并反序列化
     */
    public <T> T getAndDeserialize(String key, Class<T> clz) {
        String val = get(key);

        if (val == null) {
            return null;
        } else {
            return (T) client.serializer().decode(val, clz);
        }
    }

    /**
     * @deprecated 1.5
     * */
    @Deprecated
    public <T> List<T> getAndDeserializeByKeys(String... keys) {
        return (List<T>) getAndDeserializeByKeys(Object.class, keys);
    }

    /**
     * @deprecated 1.5
     * */
    @Deprecated
    public <T> List<T> getAndDeserializeByKeys(Class<T> clz, String... keys) {
        return getMoreAndDeserialize(clz, keys);
    }

    /**
     * 获取更多并反序列化
     * */
    public <T> List<T> getMoreAndDeserialize(Class<T> clz, String... keys) {
        List<String> vals = getMore(keys);

        if (vals == null) {
            return null;
        } else {
            List<T> list = new ArrayList<>();
            for (String val : vals) {
                list.add((T) client.serializer().decode(val, clz));
            }
            return list;
        }
    }

    /**
     * 获取或存储
     * */
    public String getOrStore(String key, int inSeconds, Supplier<String> supplier) {
        String val = get(key);
        if (val == null) {
            val = supplier.get();
            store(key, val, inSeconds);
        }

        return val;
    }

    /**
     * @deprecated 1.5
     * */
    @Deprecated
    public <T> T getOrStoreAndSerialize(String key, int inSeconds, Supplier<T> supplier) {
        T val = (T)getAndDeserialize(key, Object.class);

        if (val == null) {
            val = supplier.get();
            storeAndSerialize(key, val, inSeconds);
        }

        return val;
    }

    /**
     * 获取或存储并序列化
     * */
    public <T> T getOrStoreAndSerialize(String key, int inSeconds, Class<T> clz, Supplier<T> supplier) {
        T val = getAndDeserialize(key, clz);

        if (val == null) {
            val = supplier.get();
            storeAndSerialize(key, val, inSeconds);
        }

        return val;
    }

    /**
     * 检查是否存在
     * */
    public Boolean exists(String key){
        return client.openAndGet(s -> s.key(key).exists());
    }

    /**
     * 检查是多个主键是否存在
     * @deprecated
     * */
    @Deprecated
    public Long existsByKeys(Collection<String> keys){
        return exists(keys);
    }

    /**
     * 检查是多个主键是否存在
     * */
    public Long exists(Collection<String> keys){
        return client.openAndGet(s -> s.existsKeys(keys));
    }

    /**
     * 检查一批匹配模式的主键是否存在
     * */
    public Long existsByPattern(String pattern) {
        return client.openAndGet(s -> {
            Set<String> keys = s.keys(pattern);
            return s.existsKeys(keys);
        });
    }

    /**
     * 移除
     */
    public Boolean remove(String key) {
        return client.openAndGet(s -> s.key(key).delete());
    }

    /**
     * 移除多个主键
     *
     * @deprecated 1.5
     * */
    @Deprecated
    public Long removeByKeys(Collection<String> keys) {
        return client.openAndGet(s -> s.deleteKeys(keys));
    }

    /**
     * 移除多个主键
     * */
    public Long remove(Collection<String> keys) {
        return client.openAndGet(s -> s.deleteKeys(keys));
    }

    /**
     * 移除一批匹配模式的主键
     * */
    public Long removeByPattern(String pattern) {
        return client.openAndGet(s -> {
            Set<String> keys = s.keys(pattern);
            return s.deleteKeys(keys);
        });
    }

    /**
     * 延期
     */
    public void delay(String key, int inSeconds) {
        client.open(s -> s.key(key).delay(inSeconds));
    }

    /**
     * 获取剩余时间
     */
    public long ttl(String key) {
        return client.openAndGet(s -> s.key(key).ttl());
    }

    /**
     * 获取主键
     */
    public Set<String> keys(String pattern) {
        return client.openAndGet(s -> s.keys(pattern));
    }

}

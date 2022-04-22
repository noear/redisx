package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;
import org.noear.redisx.utils.AssertUtil;
import org.noear.redisx.utils.SerializationUtil;
import org.noear.redisx.utils.TextUtil;

import java.util.*;

/**
 * @author noear
 * @since 1.2
 */
public class RedisHash implements Map<String,String> {
    private final RedisClient client;
    private final String hashName;
    private final int inSeconds;//永久:-1

    public RedisHash(RedisClient client, String hashName) {
        this(client, hashName, -1);
    }

    public RedisHash(RedisClient client, String hashName, int inSeconds) {
        this.client = client;
        this.hashName = hashName;
        this.inSeconds = inSeconds;
    }

    @Override
    public int size() {
        return client.openAndGet(s -> s.key(hashName).hashLen()).intValue();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object field) {
        return client.openAndGet(s -> s.key(hashName).hashHas(field.toString()));
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取并反序列化
     */
    public <T> T getAndDeserialize(String field) {
        String val = client.openAndGet(s -> s.key(hashName).hashGet(field));

        if (val == null) {
            return null;
        } else {
            byte[] bytes = Base64.getDecoder().decode(val);
            return (T) SerializationUtil.deserialize(bytes);
        }
    }

    @Override
    public String get(Object field) {
        return client.openAndGet(s -> s.key(hashName).hashGet(field.toString()));
    }

    public int getAsInt(String field) {
        String tmp = this.get(field);
        return TextUtil.isEmpty(tmp) ? 0 : Integer.parseInt(tmp);
    }

    public long getAsLong(String field) {
        String tmp = this.get(field);
        return TextUtil.isEmpty(tmp) ? 0L : Long.parseLong(tmp);
    }

    public float getAsFloat(String field) {
        String tmp = this.get(field);
        return TextUtil.isEmpty(tmp) ? 0 : Float.parseFloat(tmp);
    }

    public double getAsDouble(String field) {
        String tmp = this.get(field);
        return TextUtil.isEmpty(tmp) ? 0 : Double.parseDouble(tmp);
    }


    public void putAndSerialize(String field, Object obj) {
        AssertUtil.notNull(obj, "redis hash value cannot be null");

        byte[] bytes = SerializationUtil.serialize(obj);
        String value = Base64.getEncoder().encodeToString(bytes);

        client.open(s -> s.key(hashName).expire(inSeconds).hashSet(field, value));
    }

    @Override
    public String put(String field, String value) {
        client.open(s -> s.key(hashName).expire(inSeconds).hashSet(field, value));

        return value;
    }

    public void put(String field, int value) {
        put(field, String.valueOf(value));
    }

    public void put(String field, long value) {
        put(field, String.valueOf(value));
    }

    public void put(String field, float value) {
        put(field, String.valueOf(value));
    }

    public void put(String field, double value) {
        put(field, String.valueOf(value));
    }


    @Override
    public String remove(Object field) {
        return client.openAndGet(s -> {
            String val = s.key(hashName).hashGet(field.toString());
            if (val != null) {
                s.key(hashName).hashDel(field.toString());
            }

            return val;
        });
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        client.open(s -> s.key(hashName).hashSetAll(m));
    }

    @Override
    public void clear() {
        client.open(s -> s.key(hashName).delete());
    }

    @Override
    public Set<String> keySet() {
        return client.openAndGet(s -> s.key(hashName).hashGetAllKeys());
    }

    @Override
    public Collection<String> values() {
        return client.openAndGet(s -> s.key(hashName).hashGetAllValues());
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return client.openAndGet(s -> s.key(hashName).hashGetAll()).entrySet();
    }

    /**
     * 延时
     * */
    public void delay(int seconds) {
        client.open(s -> s.key(hashName).delay(seconds));
    }
}

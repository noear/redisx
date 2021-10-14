package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;
import org.noear.redisx.utils.TextUtil;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author noear
 * @since 1.2
 */
public class RedisHash implements Map<String,String> {
    private final RedisClient client;
    private final String hashName;

    public RedisHash(RedisClient client, String hashName) {
        this.client = client;
        this.hashName = hashName;
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


    @Override
    public String put(String field, String value) {
        client.open(s -> s.key(hashName).persist().hashSet(field, value));

        return value;
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
}

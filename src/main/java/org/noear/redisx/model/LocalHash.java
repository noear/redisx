package org.noear.redisx.model;

import org.noear.redisx.utils.TextUtil;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 本地哈希
 *
 * @author noear
 * @since 1.1
 */

public class LocalHash implements Map<String, String> {
    private Map<String, String> _map;

    public LocalHash(Map<String, String> map) {
        this._map = map;
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
    public int size() {
        return this._map.size();
    }
    @Override
    public boolean isEmpty() {
        return this._map.isEmpty();
    }
    @Override
    public boolean containsKey(Object field) {
        return this._map.containsKey(field);
    }
    @Override
    public boolean containsValue(Object value) {
        return this._map.containsValue(value);
    }
    @Override
    public String get(Object field) {
        return this._map.get(field);
    }
    @Override
    public String put(String field, String value) {
        return this._map.put(field, value);
    }
    @Override
    public String remove(Object field) {
        return this._map.remove(field);
    }
    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        this._map.putAll(m);
    }
    @Override
    public void clear() {
        this._map.clear();
    }
    @Override
    public Set<String> keySet() {
        return this._map.keySet();
    }
    @Override
    public Collection<String> values() {
        return this._map.values();
    }
    @Override
    public Set<Entry<String, String>> entrySet() {
        return this._map.entrySet();
    }
}

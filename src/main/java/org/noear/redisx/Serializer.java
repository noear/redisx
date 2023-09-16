package org.noear.redisx;

/**
 * 序列化接口
 *
 * @author noear
 * @since 1.3
 */
public interface Serializer {
    String encode(Object obj);
    Object decode(String str, Class<?> clz);
}

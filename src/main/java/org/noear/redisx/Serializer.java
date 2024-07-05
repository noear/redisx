package org.noear.redisx;

import java.lang.reflect.Type;

/**
 * 序列化接口
 *
 * @author noear
 * @since 1.3
 */
public interface Serializer {
    /**
     * 编码
     */
    String encode(Object obj);

    /**
     * 解码
     *
     * @since 1.6
     */
    Object decode(String str, Type clz);

    /**
     * 解码
     *
     * @deprecated 1.6
     */
    @Deprecated
    default Object decode(String str, Class<?> clz) {
        return decode(str, (Type) clz);
    }
}

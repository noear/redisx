package org.noear.redisx;

import org.noear.redisx.utils.SerializationUtil;

import java.util.Base64;

/**
 * 序列化接口 默认实现
 *
 * @author noear
 * @since 1.3
 */
public class SerializerDefault implements Serializer{
    @Override
    public String encode(Object obj) {
        byte[] bytes = SerializationUtil.serialize(obj);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Override
    public Object decode(String str) {
        byte[] bytes = Base64.getDecoder().decode(str);
        return SerializationUtil.deserialize(bytes);
    }
}

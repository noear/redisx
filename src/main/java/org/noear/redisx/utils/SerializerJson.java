package org.noear.redisx.utils;

import org.noear.redisx.Serializer;
import org.noear.snack.ONode;

/**
 * 序列化接口 Json 实现
 *
 * @author noear
 * @since 1.4
 */
public class SerializerJson implements Serializer {
    @Override
    public String encode(Object obj) {
        return ONode.serialize(obj);
    }

    @Override
    public Object decode(String str) {
        return ONode.deserialize(str);
    }
}

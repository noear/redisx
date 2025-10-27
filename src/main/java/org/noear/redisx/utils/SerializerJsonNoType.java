package org.noear.redisx.utils;

import org.noear.redisx.Serializer;
import org.noear.snack4.ONode;

import java.lang.reflect.Type;

/**
 * 序列化接口 Json 实现，不带类型（json 不能还原所有的类型，有一定局限制）
 *
 * @author noear
 * @since 1.4
 */
public class SerializerJsonNoType implements Serializer {
    @Override
    public String encode(Object obj) {
        return ONode.serialize(obj);
    }

    @Override
    public Object decode(String str, Type type) {
        return ONode.deserialize(str, type);
    }
}

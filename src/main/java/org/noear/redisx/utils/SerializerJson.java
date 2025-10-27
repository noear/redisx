package org.noear.redisx.utils;

import org.noear.redisx.Serializer;
import org.noear.snack4.Feature;
import org.noear.snack4.ONode;
import org.noear.snack4.Options;

import java.lang.reflect.Type;

/**
 * 序列化接口 Json 实现（json 不能还原所有的类型，有一定局限制）
 *
 * @author noear
 * @since 1.4
 */
public class SerializerJson implements Serializer {
    private final Options options = Options.of(Feature.Read_AutoType, Feature.Write_ClassName);

    @Override
    public String encode(Object obj) {
        return ONode.serialize(obj, options);
    }

    @Override
    public Object decode(String str, Type type) {
        return ONode.deserialize(str, type, options);
    }
}

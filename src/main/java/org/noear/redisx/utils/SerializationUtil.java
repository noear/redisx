package org.noear.redisx.utils;

import java.io.*;

/**
 * 序列化工具
 *
 * @author noear
 * @since 1.0
 */
public class SerializationUtil {
    /**
     * 序列化
     */
    public static byte[] serialize(Object object) {
        if (object == null) {
            return null;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);

            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(object);
                oos.flush();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to serialize object of type: " + object.getClass(), e);
            }

            return baos.toByteArray();
        }
    }

    /**
     * 反序列化
     */
    public static Object deserialize(byte[] bytes) {
        if (bytes == null) {
            return null;
        } else {
            try {
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                return ois.readObject();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to deserialize object", e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to deserialize object type", e);
            }
        }
    }
}

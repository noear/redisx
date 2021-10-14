package org.noear.redisx.utils;

/**
 * @author noear
 * @since 1.2
 */
public class AssertUtil {
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmpty(String str, String message) {
        if (TextUtil.isEmpty(str)) {
            throw new IllegalArgumentException(message);
        }
    }
}

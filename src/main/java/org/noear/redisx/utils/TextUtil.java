package org.noear.redisx.utils;

/**
 * @author noear
 * @since 1.0
 */
public class TextUtil {
    /**
     * 检查字符串是否为空
     *
     * @param s 字符串
     */
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }
}

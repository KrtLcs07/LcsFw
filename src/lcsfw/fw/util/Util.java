package lcsfw.fw.util;

import java.lang.reflect.Parameter;

public class Util {

    public static boolean isSpringParameter(Parameter parametre){
        return  parametre.getClass().getTypeName().equals("org.springframework.web.context.WebApplicationContext");
    }

    public static Object convertString(String string, Class<?> targetType) {
        if (targetType == String.class) {
            return string;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(string);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(string);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(string);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(string);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(string);
        } else if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(string);
        } else if (targetType == byte.class || targetType == Byte.class) {
            return Byte.parseByte(string);
        } else if (targetType == char.class || targetType == Character.class) {
            if (string.length() != 1) {
                throw new IllegalArgumentException("Cannot convert string to char: " + string);
            }
            return string.charAt(0);
        }
        throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
    }
}

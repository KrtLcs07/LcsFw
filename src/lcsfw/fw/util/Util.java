package lcsfw.fw.util;

import java.lang.reflect.Method;

public class Util {
    public static boolean haveParameter(Method methode, Class<?> param) {
        Class<?>[] parameterTypes = methode.getParameterTypes();
        for (Class<?> type : parameterTypes) {
            if (type.equals(param)) {
                return true;
            }
        }
        return false;
    }
}

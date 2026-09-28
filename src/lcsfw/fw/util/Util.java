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

    public static String toJSON(Object obj) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        Method[] methods = obj.getClass().getDeclaredMethods();
        boolean first = true;
        for (Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                try {
                    Object value = method.invoke(obj);
                    if (!first) {
                        json.append(",");
                    }
                    json.append("\"").append(method.getName().substring(3)).append("\":");
                    if (value instanceof String) {
                        json.append("\"").append(value).append("\"");
                    } else {
                        json.append(value);
                    }
                    first = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        json.append("}");
        return json.toString();
    }
}

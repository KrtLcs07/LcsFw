package lcsfw.fw.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

public class Util {

    public static String toJSON(Object obj) {
        StringBuilder json = new StringBuilder();
        appendJSON(obj, json, new IdentityHashMap<>());
        return json.toString();
    }

    private static void appendJSON(Object value, StringBuilder json, IdentityHashMap<Object, Boolean> ancestors) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof CharSequence || value instanceof Character || value instanceof Enum<?>) {
            appendString(value instanceof Enum<?> ? ((Enum<?>) value).name() : value.toString(), json);
        } else if (value instanceof Boolean || value instanceof Number) {
            if (value instanceof Double && !Double.isFinite((Double) value)
                    || value instanceof Float && !Float.isFinite((Float) value)) {
                throw new IllegalArgumentException("NaN et Infinity ne sont pas des valeurs JSON valides");
            }
            json.append(value);
        } else {
            if (ancestors.put(value, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("Référence cyclique impossible à sérialiser en JSON");
            }
            try {
                if (value.getClass().isArray()) {
                    appendArray(value, json, ancestors);
                } else if (value instanceof Iterable<?>) {
                    appendIterable((Iterable<?>) value, json, ancestors);
                } else if (value instanceof Map<?, ?>) {
                    appendMap((Map<?, ?>) value, json, ancestors);
                } else {
                    appendBean(value, json, ancestors);
                }
            } finally {
                ancestors.remove(value);
            }
        }
    }

    private static void appendArray(Object array, StringBuilder json, IdentityHashMap<Object, Boolean> ancestors) {
        json.append('[');
        for (int index = 0; index < Array.getLength(array); index++) {
            if (index > 0) {
                json.append(',');
            }
            appendJSON(Array.get(array, index), json, ancestors);
        }
        json.append(']');
    }

    private static void appendIterable(Iterable<?> values, StringBuilder json,
            IdentityHashMap<Object, Boolean> ancestors) {
        json.append('[');
        boolean first = true;
        for (Object value : values) {
            if (!first) {
                json.append(',');
            }
            appendJSON(value, json, ancestors);
            first = false;
        }
        json.append(']');
    }

    private static void appendMap(Map<?, ?> values, StringBuilder json, IdentityHashMap<Object, Boolean> ancestors) {
        json.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!first) {
                json.append(',');
            }
            appendString(String.valueOf(entry.getKey()), json);
            json.append(':');
            appendJSON(entry.getValue(), json, ancestors);
            first = false;
        }
        json.append('}');
    }

    private static void appendBean(Object bean, StringBuilder json, IdentityHashMap<Object, Boolean> ancestors) {
        try {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(bean.getClass(), Object.class)
                    .getPropertyDescriptors();
            json.append('{');
            boolean first = true;
            for (PropertyDescriptor property : properties) {
                Method getter = property.getReadMethod();
                if (getter == null) {
                    continue;
                }
                if (!first) {
                    json.append(',');
                }
                String getterName = getter.getName();
                String propertyName = getterName.startsWith("is") ? getterName.substring(2)
                        : getterName.substring(3);
                appendString(propertyName, json);
                json.append(':');
                appendJSON(getter.invoke(bean), json, ancestors);
                first = false;
            }
            json.append('}');
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Impossible de sérialiser " + bean.getClass().getName(), e);
        }
    }

    private static void appendString(String value, StringBuilder json) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"':
                    json.append("\\\"");
                    break;
                case '\\':
                    json.append("\\\\");
                    break;
                case '\b':
                    json.append("\\b");
                    break;
                case '\f':
                    json.append("\\f");
                    break;
                case '\n':
                    json.append("\\n");
                    break;
                case '\r':
                    json.append("\\r");
                    break;
                case '\t':
                    json.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
            }
        }
        json.append('"');
    }
}

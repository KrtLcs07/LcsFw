package lcsfw.fw.conf;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

public class AppProperties {
    private static final Properties properties = new Properties();

    private AppProperties() {
    }

    public static void load(String ressource) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(ressource)) {
            if (in == null) {
                throw new FileNotFoundException(
                        "LCSFW : fichier de configuration introuvable : "
                                + ressource);
            }
            properties.clear();
            properties.load(in);

        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "LCSFW : la propriété '" + key
                            + "' doit être un nombre entier. Valeur reçue : "
                            + value,
                    e);
        }
    }

}

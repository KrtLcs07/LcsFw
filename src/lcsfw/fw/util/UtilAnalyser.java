package lcsfw.fw.util;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class UtilAnalyser {

    public static List<Class<?>> getClasses(String packageName) throws URISyntaxException, ClassNotFoundException {
        List<Class<?>> retour = new ArrayList<>();
        String path = packageName.replace('.', '/');
        File directory = new File(Thread.currentThread().getContextClassLoader().getResource(path).toURI());
        // System.out.println(Thread.currentThread().getContextClassLoader().getResource(packageName).getFile());
        // System.out.println(Thread.currentThread().getContextClassLoader().getResource(packageName).toURI());
        if (directory.exists()) {
            for (String file : directory.list()) {
                if (file.endsWith(".class")) {
                    String className = packageName + '.' + file.replace(".class", "");
                    if (className.startsWith(".")) {
                        className = className.substring(1);
                    }
                    Class<?> clazz = Class.forName(className);
                    retour.add(clazz);
                    // System.out.println(clazz.getName());
                    continue;
                }
                if (!file.contains(".")) {
                    try {
                        retour.addAll(getClasses(packageName + "." + file));

                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        } else {
            System.out.println("directory tsy miexiste");
        }
        return retour;
    }
}
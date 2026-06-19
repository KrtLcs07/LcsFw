package lcsfw.fw.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ScanAnnotation {

    public static List<Method> getMethodesWithAnnotation(Class<? extends Annotation> annotationClass, Class<?> tagetClass ){
        List<Method> methods = new ArrayList<>();
        Method[] lsMethods = tagetClass.getMethods();

        for (Method method : lsMethods) {
            if (method.isAnnotationPresent(annotationClass)) {
                methods.add(method);
            }
        }
        return methods;
         
    }

    public static List<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotationClass, String packagee)
            throws ClassNotFoundException, URISyntaxException {

        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> class1 : UtilAnalyser.getClasses(packagee)) {
            if (class1.isAnnotationPresent(annotationClass)) {
                classes.add(class1);
            }
        }
        return classes;
    }

    public static List<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotationClass)
            throws ClassNotFoundException, URISyntaxException {
        return getClassesWithAnnotation(annotationClass, "");
    }
}

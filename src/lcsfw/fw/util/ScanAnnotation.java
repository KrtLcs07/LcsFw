package lcsfw.fw.util;

import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ScanAnnotation {

    List<Class<?>> getClassesWithAnnoation(Class<? extends Annotation> annotationClass, String packagee)
            throws ClassNotFoundException, URISyntaxException {

        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> class1 : UtilAnalyser.getClasses(packagee)) {
            if (class1.isAnnotationPresent(annotationClass)) {
                classes.add(annotationClass);
            }
        }
        return classes;
    }

    List<Class<?>> getClassesWithAnnoation(Class<? extends Annotation> annotationClass)
            throws ClassNotFoundException, URISyntaxException {
                return getClassesWithAnnoation(annotationClass, "");
    }
}

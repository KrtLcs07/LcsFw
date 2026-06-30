package lcsfw.fw.controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import lcsfw.fw.annotation.Controller;
import lcsfw.fw.annotation.UrlMapping;
import lcsfw.fw.mapping.Mapping;
import lcsfw.fw.mapping.UrlMethode;
import lcsfw.fw.util.ScanAnnotation;

public class FrontControllerListner implements ServletContextListener {

    private static final Class<? extends Annotation> CONTROLLER_ANNOTATION = Controller.class;
    private static final Class<? extends Annotation> URLMAPPING_ANNOTATION = UrlMapping.class;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        String packageController = context.getInitParameter("controller-package");
        if (packageController == null || packageController.isEmpty()) {
            packageController = "";
        }

        HashMap<UrlMethode, Mapping> mapping = new HashMap<>();
        
        try {
            mapping = initMapping(packageController);
            context.setAttribute("mapping", mapping);
        } catch (Exception e) {
            System.out.println("Erreur (LcsFw):");
            e.printStackTrace();
        }
    }

    protected HashMap<UrlMethode, Mapping> initMapping(String packageController) throws Exception {
        HashMap<UrlMethode, Mapping> mapping = new HashMap<>();
        List<Class<?>> classesController;

        try {
            classesController = ScanAnnotation.getClassesWithAnnotation(CONTROLLER_ANNOTATION, packageController);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Erreur (LcsFw): " + e);
        }
        if (classesController.isEmpty()) {
            return mapping;
        }

        for (Class<?> class1 : classesController) {
            List<Method> methods = ScanAnnotation.getMethodesWithAnnotation(URLMAPPING_ANNOTATION, class1);
            for (Method method : methods) {
                UrlMapping urlAnnotation = (UrlMapping) method.getAnnotation(URLMAPPING_ANNOTATION);

                UrlMethode urlMethode = new UrlMethode(urlAnnotation.url(), urlAnnotation.method());
                if (mapping.containsKey(urlMethode)) {
                    throw new Exception("Il y a DUPLICATION d'url avec le même methode");
                }
                mapping.put(urlMethode, new Mapping(class1, method));
            }
        }
        return mapping;
    }
}

package lcsfw.fw.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lcsfw.fw.annotation.Controller;
import lcsfw.fw.annotation.UrlMapping;
import lcsfw.fw.http.HttpMethode;
import lcsfw.fw.mapping.Mapping;
import lcsfw.fw.mapping.UrlMethode;
import lcsfw.fw.util.ScanAnnotation;

public class FrontController extends HttpServlet {

    private static final Class<? extends Annotation> CONTROLLER_ANNOTATION = Controller.class;
    private static final Class<? extends Annotation> URLMAPPING_ANNOTATION = UrlMapping.class;

    List<Class<?>> classesController;
    HashMap<UrlMethode, Mapping> mapping;

    protected void initMapping() throws Exception {
        if (classesController.isEmpty()) {
            return;
        }
        mapping = new HashMap<>();

        for (Class<?> class1 : classesController) {
            List<Method> methods = ScanAnnotation.getMethodesWithAnnotation(URLMAPPING_ANNOTATION, class1);
            for (Method method : methods) {
                UrlMapping urlAnnotation = (UrlMapping) method.getAnnotation(URLMAPPING_ANNOTATION);

                UrlMethode urlMethode = new UrlMethode(urlAnnotation.url(), urlAnnotation.method());
                if (mapping.get(urlMethode) != null) {
                    throw new Exception("Il y a 2 url avec le même methode");
                }
                mapping.put(urlMethode, new Mapping(class1, method));
            }
        }
    }

    @Override
    public void init() throws ServletException {
        try {
            classesController = ScanAnnotation.getClassesWithAnnotation(CONTROLLER_ANNOTATION, "");
            initMapping();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Erreur (LcsFw): " + e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.println("Framework de Lucas (LCSFW)");

        String askUrl = req.getRequestURI();
        String contextPath = req.getContextPath();

        askUrl = askUrl.substring(contextPath.length());
        out.println(askUrl);
        HttpMethode methode = HttpMethode.valueOf(req.getMethod());
        UrlMethode urlMethode = new UrlMethode(askUrl, methode);
        Mapping map = mapping.get(urlMethode);
        if (map != null) {
            Class<?> class1 = map.getControllerClass();
            Method method = map.getMethod();
            out.println("Url existe :");
            out.println(askUrl + " ("+ method+") --> " + map.getClass().getSimpleName() + " | " + method.getName());
            out.println("Execution de la methode demandé.... ");

            try {
                Object obj = class1.getDeclaredConstructor().newInstance();
                method.invoke(obj);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                throw new ServletException("Erreur (LcsFw) :" + e);
            }

        }

        else {
            out.println("Url Introuvable, voici ceux qui existe :");
            for (UrlMethode url : mapping.keySet()) {
                Mapping nMap = mapping.get(url);

                out.println(
                        url.getUrl() + " --> " + nMap.getClass().getSimpleName() + " | " + nMap.getMethod().getName());

            }
        }

    }
}

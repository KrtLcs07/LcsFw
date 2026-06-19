package lcsfw.fw.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
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
import lcsfw.fw.mapping.Mapping;
import lcsfw.fw.util.ScanAnnotation;

public class FrontController extends HttpServlet {

    private static final Class<? extends Annotation> CONTROLLER_ANNOTATION = Controller.class;
    private static final Class<? extends Annotation> URLMAPPING_ANNOTATION = UrlMapping.class;

    List<Class<?>> classesController;
    HashMap<String, Mapping> mapping;

    protected void initMapping() {
        if (classesController.isEmpty()) {
            return;
        }
        mapping = new HashMap<>();

        for (Class<?> class1 : classesController) {
            List<Method> methods = ScanAnnotation.getMethodesWithAnnotation(URLMAPPING_ANNOTATION, class1);
            for (Method method : methods) {
                UrlMapping url = (UrlMapping) method.getAnnotation(URLMAPPING_ANNOTATION);

                mapping.put(url.value(), new Mapping(class1, method));
            }
        }
    }

    @Override
    public void init() throws ServletException {
        try {
            classesController = ScanAnnotation.getClassesWithAnnotation(CONTROLLER_ANNOTATION, "");
            initMapping();
        } catch (ClassNotFoundException | URISyntaxException e) {
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

        String askUrl = req.getServletPath();
        Mapping map = mapping.get(askUrl);
        if (map != null) {
            out.println("Url existe :");
            out.println(askUrl + " --> " + map.getClass().getSimpleName() + " | " + map.getMethod().getName());
        }

        else {
            out.println("Url Introuvable, voici ceux qui existe :");
            for (String url : mapping.keySet()) {
                Mapping nMap = mapping.get(url);

                out.println( url + " --> " + nMap.getClass().getSimpleName() + " | " + nMap.getMethod().getName());

            }
        }

    }
}

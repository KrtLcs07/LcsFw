package lcsfw.fw.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
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


    List<Class<?>> classes;
    HashMap<String, Mapping> mapping;

    protected void initMapping(){
        if (classes.isEmpty()) {
            return;
        }

        for (Class<?> class1 : classes) {
            ScanAnnotation.getMethodesWithAnnotation(, class1)
        }
    }

    @Override
    public void init() throws ServletException {
        try {
            classes = ScanAnnotation.getClassesWithAnnotation(CONTROLLER_ANNOTATION, "");
            initMapping();
        } catch (ClassNotFoundException | URISyntaxException e) {
            e.printStackTrace();
            throw new ServletException( "Erreur (LcsFw): " + e);
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
        out.println("Framweork de Lucas (LCSFW) fonctionnel");
        out.println("Les classes annotées sont :");

        for (Class<?> class1 : classes) {
            out.println("- " + class1.getName());
        }

    }
}

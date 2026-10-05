package lcsfw.fw.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lcsfw.fw.annotation.ApiREST;
import lcsfw.fw.http.HttpMethode;
import lcsfw.fw.mapping.Mapping;
import lcsfw.fw.mapping.UrlMethode;
import lcsfw.fw.util.JsonUtil;
import lcsfw.fw.util.Util;
import lcsfw.fw.view.ModelAndView;

public class FrontController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletContext context = req.getServletContext();

        String prefix = context.getInitParameter("view-prefix");
        String sufix = context.getInitParameter("view-suffix");
        Object springContext = context.getAttribute("springContext");

        @SuppressWarnings("unchecked")
        HashMap<UrlMethode, Mapping> mapping = (HashMap<UrlMethode, Mapping>) context.getAttribute("mapping");
        // out.println(mapping);
        if (mapping == null) {
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().println("Mapping introuvable");
            return;
        }
        String askUrl = req.getRequestURI();
        String contextPath = req.getContextPath();

        askUrl = askUrl.substring(contextPath.length());
        // out.println(askUrl);
        HttpMethode methode = HttpMethode.valueOf(req.getMethod());
        UrlMethode urlMethode = new UrlMethode(askUrl, methode);

        Mapping map = mapping.get(urlMethode);
        if (map != null) {
            Class<?> class1 = map.getControllerClass();
            Method method = map.getMethod();
            // out.println("Url existe :");
            // out.println(askUrl + " (" + method + ") --> " +
            // map.getClass().getSimpleName() + " | " + method.getName());
            // out.println("Execution de la methode demandé.... ");
            Parameter[] parametres = method.getParameters();

            Class<?> returnType = method.getReturnType();
            if (returnType != ModelAndView.class && !method.isAnnotationPresent(ApiREST.class)) {
                throw new ServletException("La methode " + method + " n'as pas de type de retour valide");
            }
            try {
                Object obj = class1.getDeclaredConstructor().newInstance();

                Object resultRetour = null;
                if (parametres.length == 0) {
                    resultRetour = method.invoke(obj);
                } else {
                    Object[] args = new Object[parametres.length];
                    for (int i = 0; i < parametres.length; i++) {
                        Parameter parameter = parametres[i];
                        String paramName = parameter.getName();
                        String paramValue = req.getParameter(paramName);

                        if (paramValue == null && Util.isSpringParameter(parameter)) {
                            throw new ServletException("Le parametre " + paramName + " est manquant");
                        }
                        Class<?> paramType = parameter.getType();
                        if (paramType.getName().equals("org.springframework.web.context.WebApplicationContext")) {
                            if (springContext == null) {
                                throw new ServletException("Le contexte spring n'as pas été trouvé");
                            }
                            args[i] = springContext;
                        } else {
                            Object convertedValue = Util.convertString(paramValue, paramType);
                            args[i] = convertedValue;
                        }
                    }
                    resultRetour = method.invoke(obj, args);
                }

                if (resultRetour != null) {
                    // out.println(resultRetour.toString());
                    if (returnType == ModelAndView.class && !method.isAnnotationPresent(ApiREST.class)) {

                        ModelAndView retour = (ModelAndView) resultRetour;
                        addArgToRequest(req, retour.getData());
                        String path = "/" + prefix + "/" + retour.getView() + "." + sufix;
                        RequestDispatcher dispat = req.getRequestDispatcher(path);
                        dispat.forward(req, resp);
                    } else {
                        resp.setContentType("application/json;charset=UTF-8");
                        if (resultRetour instanceof String) {
                            String retour = (String) resultRetour;
                            resp.getWriter().write(retour);
                        } else {
                            String json = JsonUtil.toJSON(resultRetour);
                            resp.getWriter().write(json);
                        }
                    }

                } else {
                    throw new ServletException("Le retour envoyé est null");

                }

            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                throw new ServletException("Erreur (LcsFw) :" + e);
            }

        }

        else

        {
            resp.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.println("Framework de Lucas (LCSFW)");

            out.println("Recherche :");
            out.println(urlMethode.getUrl());
            out.println(urlMethode.getMethode());
            out.println(urlMethode.hashCode());
            out.println("Url Introuvable, voici ceux qui existe :");
            for (UrlMethode url : mapping.keySet()) {
                Mapping nMap = mapping.get(url);
                out.println(
                        url.getUrl() + " --> " + nMap.getClass().getSimpleName() + " | " + nMap.getMethod().getName());

            }
        }

    }

    private void addArgToRequest(HttpServletRequest req, Map<String, Object> data) {
        for (String argument : data.keySet()) {
            Object value = data.get(argument);
            req.setAttribute(argument, value);
        }
    }

    

}

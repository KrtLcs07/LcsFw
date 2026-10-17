package lcsfw.fw.controller.helper;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lcsfw.fw.http.HttpMethode;
import lcsfw.fw.mapping.Mapping;
import lcsfw.fw.mapping.UrlMethode;

public class RouteHelper {

    @SuppressWarnings("unchecked")
    public static Map<UrlMethode, Mapping> getMapping(ServletContext context) {
        Object attribute = context.getAttribute("mapping");

        if (!(attribute instanceof Map<?, ?>)) {
            return null;
        }

        return (Map<UrlMethode, Mapping>) attribute;
    }

    public static String getRequestedUrl(HttpServletRequest req) {
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();

        return requestURI.substring(contextPath.length());
    }

    public static HttpMethode getHttpMethode(HttpServletRequest req)
            throws ServletException {

        try {
            return HttpMethode.valueOf(req.getMethod());
        } catch (IllegalArgumentException e) {
            throw new ServletException(
                    "LCSFW : méthode HTTP non prise en charge : "
                            + req.getMethod(),
                    e);
        }
    }

    public static void handleNotFound(HttpServletRequest req, HttpServletResponse resp,
            Map<UrlMethode, Mapping> mapping, UrlMethode cle) throws IOException {

        ServletContext context = req.getServletContext();

        context.log(
                "LCSFW : aucune route pour "
                        + cle.getMethode() + " " + cle.getUrl());

        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("text/plain;charset=UTF-8");

        var out = resp.getWriter();
        out.println("LCSFW - Erreur 404 : route introuvable.");
        out.println("URL demandée : " + cle.getUrl());
        out.println("Méthode HTTP : " + cle.getMethode());
        out.println();
        out.println("Routes enregistrées :");

        for (Map.Entry<UrlMethode, Mapping> entry : mapping.entrySet()) {
            UrlMethode url = entry.getKey();
            Mapping route = entry.getValue();

            out.println(
                    url.getMethode() + " " + url.getUrl()
                            + " -> "
                            + route.getControllerClass().getSimpleName()
                            + "." + route.getMethod().getName());
        }
    }

}

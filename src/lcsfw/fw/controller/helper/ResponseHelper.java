package lcsfw.fw.controller.helper;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lcsfw.fw.util.json.JsonUtil;
import lcsfw.fw.view.ModelAndView;

public class ResponseHelper {
    public static void dispatchView(HttpServletRequest req, HttpServletResponse resp, ModelAndView result,
            String prefix,
            String suffix) throws ServletException, IOException {

        if (result.getView() == null || result.getView().isBlank()) {
            throw new ServletException(
                    "LCSFW : ModelAndView ne contient aucun nom de vue.");
        }

        if (prefix == null || suffix == null) {
            throw new ServletException(
                    "LCSFW : les paramètres de configuration "
                            + "'view-prefix' et 'view-suffix' sont obligatoires "
                            + "pour afficher une vue.");
        }

        addArgToRequest(req, result.getData());

        String path = "/" + prefix + "/" + result.getView() + "." + suffix;

        RequestDispatcher dispatcher = req.getRequestDispatcher(path);

        if (dispatcher == null) {
            throw new ServletException(
                    "LCSFW : aucun RequestDispatcher disponible pour la vue "
                            + path);
        }

        dispatcher.forward(req, resp);
    }

    public static void writeJsonResponse(HttpServletResponse resp, Object result) throws IOException, ServletException {

        resp.setContentType("application/json;charset=UTF-8");

        if (result instanceof String) {
            // Contrat actuel : une String retournée par @ApiREST
            // est déjà du JSON et ne doit pas être sérialisée une seconde fois.
            resp.getWriter().write((String) result);
        } else {
            try {
                String json = JsonUtil.toJSON(result);
                resp.getWriter().write(json);
            } catch (RuntimeException e) {
                throw new ServletException(
                        "LCSFW : impossible de convertir le résultat de l'API "
                                + "en JSON. Type : " + result.getClass().getName(),
                        e);
            }
        }
    }

    private static void addArgToRequest(HttpServletRequest req, Map<String, Object> data) {
        if (data == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    public static void handleUnauthorized(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        handleAccessError(
                req,
                resp,
                HttpServletResponse.SC_UNAUTHORIZED,
                "unauthorized",
                "Vous devez vous authentifier pour accéder à cette ressource.");
    }

    public static void handleForbidden(
            HttpServletRequest req,
            HttpServletResponse resp) throws IOException {

        handleAccessError(
                req,
                resp,
                HttpServletResponse.SC_FORBIDDEN,
                "forbidden",
                "Vous n'avez pas le rôle nécessaire pour accéder à cette ressource.");
    }

    private static void handleAccessError(HttpServletRequest req, HttpServletResponse resp, int status, String error,
            String message) throws IOException {

        if (resp.isCommitted()) {
            return;
        }

        resp.reset();
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");

        String accept = req.getHeader("Accept");

        if (accept != null && accept.contains("application/json")) {
            resp.setContentType("application/json");

            resp.getWriter().write(
                    "{\"error\":\"" + error + "\","
                            + "\"status\":" + status + ","
                            + "\"message\":\"" + message + "\"}");

            return;
        }

        resp.setContentType("text/html");

        resp.getWriter().write(
                "<!DOCTYPE html>"
                        + "<html lang=\"fr\">"
                        + "<head><meta charset=\"UTF-8\">"
                        + "<title>" + status + " - Accès refusé</title></head>"
                        + "<body>"
                        + "<h1>" + status + " - "
                        + (status == HttpServletResponse.SC_UNAUTHORIZED
                                ? "Authentification requise"
                                : "Accès interdit")
                        + "</h1>"
                        + "<p>" + message + "</p>"
                        + "</body></html>");
    }
}

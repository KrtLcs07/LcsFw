package lcsfw.fw.controller.helper;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lcsfw.fw.util.JsonUtil;
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

}

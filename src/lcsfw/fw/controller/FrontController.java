package lcsfw.fw.controller;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lcsfw.fw.annotation.ApiREST;
import lcsfw.fw.annotation.param.ObjectParam;
import lcsfw.fw.annotation.param.RequestParam;
import lcsfw.fw.http.HttpMethode;
import lcsfw.fw.mapping.Mapping;
import lcsfw.fw.mapping.UrlMethode;
import lcsfw.fw.util.JsonUtil;
import lcsfw.fw.util.Util;
import lcsfw.fw.view.ModelAndView;

public class FrontController extends HttpServlet {

    private static final Class<? extends Annotation> REQUESTPARAM_ANNOTATION = RequestParam.class;

    private static final Class<? extends Annotation> OBJECTPARAM_ANNOTATION = ObjectParam.class;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    // =========================================================
    // 1. Traitement principal de la requête
    // =========================================================

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        ServletContext context = req.getServletContext();

        Map<UrlMethode, Mapping> mapping = getMapping(context);

        if (mapping == null) {
            throw new ServletException(
                    "LCSFW : le mapping des URL est introuvable dans le ServletContext. "
                            + "Vérifiez l'initialisation du FrontControllerListener.");
        }

        String url = getRequestedUrl(req);
        HttpMethode httpMethode = getHttpMethode(req);

        UrlMethode cle = new UrlMethode(url, httpMethode);
        Mapping route = mapping.get(cle);

        if (route == null) {
            handleNotFound(req, resp, mapping, cle);
            return;
        }

        String prefix = context.getInitParameter("view-prefix");
        String suffix = context.getInitParameter("view-suffix");
        Object springContext = context.getAttribute("springContext");

        executeMappedMethod(req, resp, route, prefix, suffix, springContext);
    }

    // =========================================================
    // 2. Récupération et identification de la route
    // =========================================================

    @SuppressWarnings("unchecked")
    private Map<UrlMethode, Mapping> getMapping(ServletContext context) {
        Object attribute = context.getAttribute("mapping");

        if (!(attribute instanceof Map<?, ?>)) {
            return null;
        }

        return (Map<UrlMethode, Mapping>) attribute;
    }

    private String getRequestedUrl(HttpServletRequest req) {
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();

        return requestURI.substring(contextPath.length());
    }

    private HttpMethode getHttpMethode(HttpServletRequest req)
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

    private void handleNotFound(HttpServletRequest req, HttpServletResponse resp,
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

    // =========================================================
    // 3. Exécution de la méthode du contrôleur
    // =========================================================

    private void executeMappedMethod(
            HttpServletRequest req,
            HttpServletResponse resp,
            Mapping route,
            String prefix,
            String suffix,
            Object springContext) throws ServletException, IOException {

        Class<?> controllerClass = route.getControllerClass();
        Method method = route.getMethod();

        validateReturnType(method);

        try {
            Object controller = controllerClass.getDeclaredConstructor().newInstance();

            Object[] args = resolveArguments(method, req, springContext);

            Object result = method.invoke(controller, args);

            if (result == null) {
                throw new ServletException(
                        "LCSFW : la méthode " + controllerClass.getSimpleName() + "." + method.getName()
                                + "() a retourné null.");
            }

            if (method.isAnnotationPresent(ApiREST.class)) {
                writeJsonResponse(resp, result);
            } else {
                dispatchView(req, resp, (ModelAndView) result,
                        prefix, suffix);
            }

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;

            req.getServletContext().log(
                    "LCSFW : exception pendant l'exécution de "
                            + controllerClass.getName() + "."
                            + method.getName() + "()",
                    cause);

            throw new ServletException(
                    "LCSFW : erreur dans la méthode du contrôleur "
                            + controllerClass.getSimpleName() + "."
                            + method.getName() + "() : "
                            + cause.getMessage(),
                    cause);

        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            req.getServletContext().log(
                    "LCSFW : impossible d'instancier le contrôleur "
                            + controllerClass.getName()
                            + " ou d'invoquer sa méthode " + method.getName(),
                    e);

            throw new ServletException(
                    "LCSFW : échec de l'exécution de "
                            + controllerClass.getSimpleName() + "."
                            + method.getName() + "(). Vérifiez le constructeur "
                            + "du contrôleur, ses paramètres et son accessibilité.",
                    e);
        }
    }

    private void validateReturnType(Method method)
            throws ServletException {

        boolean isApi = method.isAnnotationPresent(ApiREST.class);
        Class<?> returnType = method.getReturnType();

        if (isApi && returnType == void.class) {
            throw new ServletException(
                    "LCSFW : la méthode API "
                            + method.getName()
                            + "() ne peut pas retourner void.");
        }

        if (!isApi && returnType != ModelAndView.class) {
            throw new ServletException(
                    "LCSFW : la méthode "
                            + method.getDeclaringClass().getSimpleName() + "."
                            + method.getName()
                            + "() doit retourner ModelAndView ou être annotée "
                            + "avec @ApiREST.");
        }
    }

    // =========================================================
    // 4. Résolution des paramètres de la méthode
    // =========================================================

    private Object[] resolveArguments(Method method, HttpServletRequest req, Object springContext)
            throws ServletException {

        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            args[i] = resolveParameter(parameters[i], req, springContext);
        }
        return args;
    }

    private Object resolveParameter(Parameter parameter, HttpServletRequest req, Object springContext)
            throws ServletException {

        boolean hasRequestParam = parameter.isAnnotationPresent(REQUESTPARAM_ANNOTATION);

        boolean hasObjectParam = parameter.isAnnotationPresent(OBJECTPARAM_ANNOTATION);

        boolean isSpringParam = Util.isSpringParameter(parameter);

        if (hasRequestParam && hasObjectParam) {
            throw new ServletException(
                    "LCSFW : le paramètre " + parameter.getName() + " ne peut pas porter simultanément "
                            + "@RequestParam et @ObjectParam.");
        }

        if (isSpringParam) {
            if (hasRequestParam || hasObjectParam) {
                throw new ServletException(
                        "LCSFW : le paramètre Spring " + parameter.getName() + " ne doit pas porter @RequestParam "
                                + "ou @ObjectParam.");
            }

            if (springContext == null) {
                throw new ServletException(
                        "LCSFW : le contexte Spring est absent. " + "Vérifiez sa configuration et l'attribut "
                                + "'springContext' du ServletContext.");
            }

            if (!parameter.getType().isInstance(springContext)) {
                throw new ServletException(
                        "LCSFW : le contexte Spring est de type " + springContext.getClass().getName()
                                + ", incompatible avec le paramètre " + parameter.getName() + " de type "
                                + parameter.getType().getName() + ".");
            }

            return springContext;
        }

        if (!hasRequestParam && !hasObjectParam) {
            throw new ServletException(
                    "LCSFW : le paramètre " + parameter.getName() + " de type " + parameter.getType().getSimpleName()
                            + " doit porter @RequestParam ou @ObjectParam.");
        }

        if (hasRequestParam) {
            return resolveRequestParam(parameter, req);
        }

        return resolveObjectParam(parameter, req);
    }

    private Object resolveRequestParam(Parameter parameter, HttpServletRequest req) throws ServletException {

        Class<?> paramType = parameter.getType();

        if (!Util.isStandartType(paramType)) {
            throw new ServletException(
                    "LCSFW : @RequestParam ne peut être utilisé ici "
                            + "qu'avec un type simple. Paramètre : "
                            + parameter.getName() + " (" + paramType.getName() + ").");
        }

        RequestParam annotation = parameter.getAnnotation(RequestParam.class);

        String paramName = annotation.name();

        if (paramName == null || paramName.isEmpty()) {
            paramName = parameter.getName();
        }

        String paramValue = req.getParameter(paramName);

        if (paramValue == null && annotation.required()) {
            throw new ServletException(
                    "LCSFW : paramètre de requête obligatoire manquant : "
                            + paramName);
        }

        try {
            return Util.convertOrDefault(paramValue, paramType);
        } catch (RuntimeException e) {
            throw new ServletException(
                    "LCSFW : impossible de convertir le paramètre '" + paramName + "' en " + paramType.getSimpleName()
                            + ". Valeur reçue : " + paramValue,
                    e);
        }
    }

    private Object resolveObjectParam(Parameter parameter, HttpServletRequest req) throws ServletException {

        ObjectParam annotation = parameter.getAnnotation(ObjectParam.class);

        String name = annotation.name();
        String link = annotation.link();

        if (name == null || name.isEmpty()) {
            throw new ServletException(
                    "LCSFW : l'attribut 'name' de @ObjectParam " + "est obligatoire pour le paramètre "
                            + parameter.getName() + ".");
        }

        if (link == null || link.isEmpty()) {
            throw new ServletException(
                    "LCSFW : l'attribut 'link' de @ObjectParam " + "est obligatoire pour le paramètre "
                            + parameter.getName() + ".");
        }

        try {
            Class<?> type = parameter.getType();
            Object object = type.getDeclaredConstructor().newInstance();

            fillObjectAttribute(object, req, link, name);

            return object;

        } catch (ReflectiveOperationException e) {
            throw new ServletException(
                    "LCSFW : impossible de créer l'objet du paramètre " + parameter.getName() + " de type "
                            + parameter.getType().getName() + ". Vérifiez qu'il possède un constructeur sans argument "
                            + "accessible.",
                    e);
        } catch (Exception e) {
            throw new ServletException("LCSFW : impossible de remplir l'objet du paramètre " + parameter.getName()
                    + " : " + e.getMessage(), e);
        }
    }

    // =========================================================
    // 5. Réponse MVC : ModelAndView et dispatch JSP
    // =========================================================

    private void dispatchView(HttpServletRequest req, HttpServletResponse resp, ModelAndView result, String prefix,
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

    private void addArgToRequest(
            HttpServletRequest req,
            Map<String, Object> data) {
        if (data == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    // =========================================================
    // 6. Réponse API : JSON
    // =========================================================

    private void writeJsonResponse(HttpServletResponse resp, Object result) throws IOException, ServletException {

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

    // =========================================================
    // 7. Remplissage récursif des objets annotés @ObjectParam
    // =========================================================

    private void fillObjectAttribute(
            Object obj, HttpServletRequest req, String link, String debut) throws Exception {

        Field[] fields = obj.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);

            if (!Util.isStandartType(field.getType())) {
                Object fieldValue = field.getType().getDeclaredConstructor().newInstance();

                fillObjectAttribute(fieldValue, req, link, debut + link + field.getName());

                field.set(obj, fieldValue);

            } else {
                String paramName = debut + link + field.getName();
                String value = req.getParameter(paramName);

                if (value != null) {
                    Object convertedValue = Util.convertString(value, field.getType());

                    field.set(obj, convertedValue);
                }
            }
        }
    }
}
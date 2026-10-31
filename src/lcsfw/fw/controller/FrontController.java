package lcsfw.fw.controller;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import lcsfw.fw.annotation.*;
import lcsfw.fw.controller.helper.*;
import lcsfw.fw.http.methode.HttpMethode;
import lcsfw.fw.http.session.LcsSession;
import lcsfw.fw.mapping.*;
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

        Map<UrlMethode, Mapping> mapping = RouteHelper.getMapping(context);

        if (mapping == null) {
            throw new ServletException(
                    "LCSFW : le mapping des URL est introuvable dans le ServletContext. "
                            + "Vérifiez l'initialisation du FrontControllerListener.");
        }

        String url = RouteHelper.getRequestedUrl(req);
        HttpMethode httpMethode = RouteHelper.getHttpMethode(req);

        UrlMethode cle = new UrlMethode(url, httpMethode);
        Mapping route = mapping.get(cle);

        if (route == null) {
            RouteHelper.handleNotFound(req, resp, mapping, cle);
            return;
        }

        if (!checkAuthorization(req, resp, route, context)) {
            return;
        }

        String prefix = context.getInitParameter("view-prefix");
        String suffix = context.getInitParameter("view-suffix");
        Object springContext = context.getAttribute("springContext");

        executeMappedMethod(req, resp, route, prefix, suffix, springContext);
    }

    private void executeMappedMethod(
            HttpServletRequest req,
            HttpServletResponse resp,
            Mapping route,
            String prefix,
            String suffix,
            Object springContext) throws ServletException, IOException {

        Class<?> controllerClass = route.getControllerClass();
        Method method = route.getMethod();

        ExecuteHelper.validateReturnType(method);

        try {
            Object controller = getControllerInstance(controllerClass, req.getServletContext());

            Object[] args = ExecuteHelper.resolveArguments(method, req, springContext);

            Object result = method.invoke(controller, args);

            HttpSession httpSession = req.getSession(false);

            if (httpSession != null) {
                for (Object object : args) {
                    if (object instanceof LcsSession lcsSession) {
                        lcsSession.flush(httpSession);
                    }
                }
            }

            if (result == null) {
                throw new ServletException(
                        "LCSFW : la méthode " + controllerClass.getSimpleName() + "." + method.getName()
                                + "() a retourné null.");
            }

            if (method.isAnnotationPresent(ApiREST.class)) {
                ResponseHelper.writeJsonResponse(resp, result);
            } else {
                ResponseHelper.dispatchView(req, resp, (ModelAndView) result,
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

    private boolean checkAuthorization(
            HttpServletRequest req,
            HttpServletResponse resp,
            Mapping route,
            ServletContext context) throws ServletException, IOException {

        Class<?> controllerClass = route.getControllerClass();
        Method method = route.getMethod();

        Auth auth = method.getAnnotation(Auth.class);

        if (auth == null) {
            auth = controllerClass.getAnnotation(Auth.class);
        }

        if (auth == null) {
            return true;
        }

        String sessionUserName = (String) context.getAttribute("lcsfw-session-user-name");

        if (sessionUserName == null || sessionUserName.isBlank()) {
            throw new ServletException(
                    "LCSFW : le nom de l'attribut utilisateur de session "
                            + "n'est pas configuré dans le web.xml "
                            + "(session-user-name).");
        }

        HttpSession session = req.getSession(false);

        if (session == null) {
            ResponseHelper.handleUnauthorized(req, resp);
            return false;
        }

        Object user = session.getAttribute(sessionUserName);

        if (user == null) {
            ResponseHelper.handleUnauthorized(req, resp);
            return false;
        }

        String[] requiredRoles = auth.roles();

        if (requiredRoles.length == 0) {
            return true;
        }

        String sessionRoleName = (String) context.getAttribute("lcsfw-session-role-name");

        if (sessionRoleName == null || sessionRoleName.isBlank()) {
            throw new ServletException(
                    "LCSFW : le nom de l'attribut de rôle de session "
                            + "n'est pas configuré dans le web.xml "
                            + "(session-role-name).");
        }

        Object roleAttribute = session.getAttribute(sessionRoleName);

        if (roleAttribute == null) {
            ResponseHelper.handleForbidden(req, resp);
            return false;
        }

        List<String> requiredRoleList = Arrays.asList(requiredRoles);

        if (roleAttribute instanceof String role) {
            if (requiredRoleList.contains(role)) {
                return true;
            }

        } else if (roleAttribute instanceof Collection<?> roles) {
            for (Object role : roles) {
                if (role instanceof String
                        && requiredRoleList.contains(role)) {
                    return true;
                }
            }

        } else if (roleAttribute instanceof String[] roles) {
            for (String role : roles) {
                if (requiredRoleList.contains(role)) {
                    return true;
                }
            }

        } else {
            throw new ServletException(
                    "LCSFW : l'attribut de session '"
                            + sessionRoleName
                            + "' doit être une String, une Collection "
                            + "ou un tableau de String.");
        }

        ResponseHelper.handleForbidden(req, resp);
        return false;
    }

    @SuppressWarnings("unchecked")
    private Object getControllerInstance(Class<?> controllerClass, ServletContext context)
            throws ReflectiveOperationException {

        Map<Class<?>, Object> controllersInstance = (Map<Class<?>, Object>) context.getAttribute("controllersInstance");

        if (controllersInstance == null) {
            throw new IllegalStateException(
                    "LCSFW : la map 'controllersInstance' n'est pas initialisée.");
        }

        Object controller = controllersInstance.get(controllerClass);

        if (controller == null) {
            controller = controllerClass.getDeclaredConstructor().newInstance();
            controllersInstance.put(controllerClass, controller);
        }

        return controller;
    }
}
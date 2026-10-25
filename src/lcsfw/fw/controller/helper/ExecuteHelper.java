package lcsfw.fw.controller.helper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lcsfw.fw.annotation.ApiREST;
import lcsfw.fw.annotation.param.ObjectParam;
import lcsfw.fw.annotation.param.RequestParam;
import lcsfw.fw.file.Mapfile;
import lcsfw.fw.http.session.LcsSession;
import lcsfw.fw.util.Util;
import lcsfw.fw.view.ModelAndView;

import java.io.IOException;
import java.lang.annotation.Annotation;

public class ExecuteHelper {

    private static final Class<? extends Annotation> REQUESTPARAM_ANNOTATION = RequestParam.class;

    private static final Class<? extends Annotation> OBJECTPARAM_ANNOTATION = ObjectParam.class;

    public static void validateReturnType(Method method)
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

    public static Object[] resolveArguments(Method method, HttpServletRequest req, Object springContext)
            throws ServletException {

        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            args[i] = resolveParameter(parameters[i], req, springContext);
        }
        return args;
    }

    private static Object resolveParameter(Parameter parameter, HttpServletRequest req, Object springContext)
            throws ServletException {

        boolean hasRequestParam = parameter.isAnnotationPresent(REQUESTPARAM_ANNOTATION);

        boolean hasObjectParam = parameter.isAnnotationPresent(OBJECTPARAM_ANNOTATION);

        boolean isSpringParam = Util.isSpringParameter(parameter);

        boolean isSessionParam = parameter.getType().equals(LcsSession.class);

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


            if (!parameter.getType().isInstance(springContext)) {
                throw new ServletException(
                        "LCSFW : le contexte Spring est de type " + springContext.getClass().getName()
                                + ", incompatible avec le paramètre " + parameter.getName() + " de type "
                                + parameter.getType().getName() + ".");
            }

            return springContext;
        }

        if (isSessionParam) {
            if (hasRequestParam || hasObjectParam) {
                throw new ServletException(
                        "LCSFW : le paramètre Session " + parameter.getName() + " ne doit pas porter @RequestParam "
                                + "ou @ObjectParam.");
            }

            return LcsSession.fromHttpSession(req.getSession(true));
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

    private static Object resolveRequestParam(Parameter parameter, HttpServletRequest req) throws ServletException {

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

    private static Object resolveObjectParam(Parameter parameter, HttpServletRequest req) throws ServletException {

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
            Object object = null;
            if (Mapfile.class.isAssignableFrom(type)) {
                object = getUploadedFile(req, name);
            } else {
                object = type.getDeclaredConstructor().newInstance();
                fillObjectAttribute(object, req, link, name);

            }
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

    private static void fillObjectAttribute(
            Object obj, HttpServletRequest req, String link, String debut) throws Exception {

        Field[] fields = obj.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            String paramName = debut + link + field.getName();
            if (Mapfile.class.isAssignableFrom(field.getType())) {
                Mapfile mapfile = getUploadedFile(req, paramName);
                field.set(obj, mapfile);
                continue;
            }
            if (!Util.isStandartType(field.getType())) {
                Object fieldValue = field.getType().getDeclaredConstructor().newInstance();

                fillObjectAttribute(fieldValue, req, link, paramName);

                field.set(obj, fieldValue);

            } else {
                String value = req.getParameter(paramName);

                if (value != null) {
                    Object convertedValue = Util.convertString(value, field.getType());

                    field.set(obj, convertedValue);
                }
            }
        }
    }

    private static Mapfile getUploadedFile(
            HttpServletRequest req,
            String paramName) throws IOException, ServletException {

        String contentType = req.getContentType();

        if (contentType == null
                || !contentType.toLowerCase(java.util.Locale.ROOT)
                        .startsWith("multipart/form-data")) {

            throw new ServletException(
                    "LCSFW : impossible de récupérer le fichier '"
                            + paramName + "' : la requête n'est pas multipart/form-data. "
                            + "Vérifiez que le formulaire HTML utilise "
                            + "enctype=\"multipart/form-data\".");
        }

        try {
            Part part = req.getPart(paramName);

            if (part == null) {
                return null;
            }

            String filename = part.getSubmittedFileName();

            if (filename == null || filename.isBlank()) {
                return null;
            }

            return Mapfile.fromPart(part);

        } catch (IllegalStateException e) {
            throw new ServletException(
                    "LCSFW : impossible de traiter le fichier '"
                            + paramName + "'. Le servlet n'est peut-être pas configuré "
                            + "pour le multipart, ou une limite de taille a été dépassée. "
                            + "Vérifiez la configuration <multipart-config> du servlet "
                            + "et les limites max-file-size et max-request-size.",
                    e);

        } catch (ServletException e) {
            throw new ServletException(
                    "LCSFW : erreur lors de la lecture du fichier '"
                            + paramName + "'. Vérifiez que le servlet possède une "
                            + "configuration multipart, que la requête est valide "
                            + "et que les limites de taille sont respectées.",
                    e);
        }
    }

}

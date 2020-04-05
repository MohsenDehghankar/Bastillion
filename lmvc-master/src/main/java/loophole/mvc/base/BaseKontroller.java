/**
 *    Copyright (C) 2018 Loophole, LLC
 *
 *    This program is free software: you can redistribute it and/or  modify
 *    it under the terms of the GNU Affero General Public License, version 3,
 *    as published by the Free Software Foundation.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.
 *
 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    As a special exception, the copyright holders give permission to link the
 *    code of portions of this program with the OpenSSL library under certain
 *    conditions as described in each individual source file and distribute
 *    linked combinations including the program with the OpenSSL library. You
 *    must comply with the GNU Affero General Public License in all respects for
 *    all of the code used other than as permitted herein. If you modify file(s)
 *    with this exception, you may extend this exception to your version of the
 *    file(s), but you are not obligated to do so. If you do not wish to do so,
 *    delete this exception statement from your version. If you delete this
 *    exception statement from all source files in the program, then also delete
 *    it in the license file.
 */
package loophole.mvc.base;

import loophole.mvc.annotation.Kontrol;
import loophole.mvc.annotation.MethodType;
import loophole.mvc.annotation.Model;
import loophole.mvc.annotation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

/**
 * Base controller class that initializes all controllers and is called through
 * the dispatcher servlet
 */
public class BaseKontroller {

    private static final String PACKAGES = System.getProperty("MVC_CONTROLLER_PKGS") != null
            ? System.getProperty("MVC_CONTROLLER_PKGS") : "";
    private static Logger log = LoggerFactory.getLogger(BaseKontroller.class);
    private static List<Class<?>> ktrlList = new ArrayList<>();

    // Load scan packages for controllers that have the Kontrol method annotation
    static {
        for (String packageNm : PACKAGES.split(",")) {
            ClassLoader classLoader = BaseKontroller.class.getClassLoader();
            String path = packageNm.replace('.', '/');
            try {
                Enumeration<URL> resources = classLoader.getResources(path);
                List<File> dirs = new ArrayList<>();
                while (resources.hasMoreElements()) {
                    URL resource = resources.nextElement();
                    dirs.add(new File(resource.getFile()));
                }
                for (File directory : dirs) {
                    loadKontrollers(directory, packageNm);
                }
            } catch (Exception ex) {
                log.error(ex.toString(), ex);
            }
        }
    }

    private List<String> errors = new ArrayList<>();
    private Map<String, String> fieldErrors = new LinkedHashMap<>();
    private HttpServletRequest request;
    private HttpServletResponse response;

    public BaseKontroller(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    private static void loadKontrollers(File d, String packageNm) throws ClassNotFoundException {
        File[] files = d.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    loadKontrollers(file, packageNm + "." + file.getName());
                } else if (file.getName().endsWith(".class")) {
                    String fileNm = file.getName().replaceAll("\\.class", "");
                    if (packageNm != null && packageNm.length() > 0) {
                        fileNm = packageNm.replaceAll("^\\.", "") + '.' + fileNm;
                    }
                    Class<?> clazz = Class
                            .forName(fileNm);
                    if (BaseKontroller.class.equals(clazz.getSuperclass())) {
                        ktrlList.add(clazz);
                    }
                }
            }
        }
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    /**
     * Find and execute controller based on annotated path
     *
     * @return page to forward / redirect
     */
    public String execute() {
        String forward = null;

        for (Class<?> clazz : ktrlList) {

            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(Kontrol.class)) {
                    Kontrol c = method.getAnnotation(Kontrol.class);

                    if (request.getRequestURI().contains(c.path() + DispatcherServlet.CTR_EXT)
                            && MethodType.valueOf(request.getMethod()).equals(c.method())) {

                        try {
                            Constructor constructor = clazz.getDeclaredConstructor(HttpServletRequest.class, HttpServletResponse.class);
                            BaseKontroller ctrl = (BaseKontroller) constructor.newInstance(new Object[]{request, response});

                            // set attributes
                            for (Field field : clazz.getDeclaredFields()) {
                                if (field.isAnnotationPresent(Model.class)) {
                                    Model m = field.getAnnotation(Model.class);
                                    if (m.name().length() > 0) {
                                        field.setAccessible(true);
                                        if (request.getAttribute(m.name()) != null) {
                                            field.set(ctrl, request.getAttribute(m.name()));
                                        }
                                    }
                                }
                            }
                            // set parameters
                            Enumeration<String> parameterNames = request.getParameterNames();
                            while (parameterNames.hasMoreElements()) {
                                String param = parameterNames.nextElement();
                                setFieldFromParams(ctrl, param, request);
                            }

                            // invoke validate method
                            for (Method validateMethod : clazz.getMethods()) {
                                if (validateMethod.isAnnotationPresent(Validate.class)) {
                                    Validate v = validateMethod.getAnnotation(Validate.class);
                                    if (validateMethod.getName().equalsIgnoreCase("validate" + method.getName())) {
                                        forward = v.input();
                                        Method m = clazz.getDeclaredMethod(validateMethod.getName());
                                        m.invoke(ctrl);
                                    }
                                }
                            }

                            //invoke execute method
                            if (!ctrl.hasErrors()) {
                                Method m = clazz.getDeclaredMethod(method.getName());
                                forward = (String) m.invoke(ctrl);
                            }

                            // set set attributes from controller to forward to view
                            for (Field field : clazz.getDeclaredFields()) {
                                if (field.isAnnotationPresent(Model.class)) {
                                    Model v = field.getAnnotation(Model.class);
                                    if (v.name().length() > 0) {
                                        field.setAccessible(true);
                                        if (field.get(ctrl) != null) {
                                            request.setAttribute(v.name(), field.get(ctrl));
                                        } else {
                                            request.setAttribute(v.name(), null);
                                            try {
                                                request.setAttribute(v.name(), field.getType().getDeclaredConstructor().newInstance());
                                            } catch (NoSuchMethodException ex){
                                               //ignore exception
                                            }

                                        }
                                    }
                                }
                            }
                            //set errors to request
                            request.setAttribute("errors", ctrl.getErrors());
                            request.setAttribute("fieldErrors", ctrl.getFieldErrors());

                        } catch (Exception ex) {
                            log.error(ex.toString(), ex);
                        }
                    }
                }
            }

        }
        return forward;

    }

    private void setFieldFromParams(Object ctrl, String param, HttpServletRequest request)
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        if (param != null) {

            Map<String, String> requestMap = new HashMap<>();
            for (String name : param.split("\\.")) {
                for (Field field : getAllFields(ctrl.getClass())) {
                    Model v = field.getAnnotation(Model.class);

                    String key = null;
                    if (name.contains("[")) {
                        key = name.replaceAll(".*\\[", "").replaceAll("\\'", "").replaceAll("\\].*", "");
                        name = name.substring(0, name.indexOf("["));
                        requestMap.put(key, request.getParameter(param));
                    }
                    if ((v == null && field.getName().equals(name)) || (v != null && name.equals(v.name()))) {
                        field.setAccessible(true);
                        if (!requestMap.isEmpty() && field.getType().getName().equals("java.util.Map")) {
                            Type type = field.getGenericType();
                            if (type instanceof ParameterizedType) {
                                Type keyType = ((ParameterizedType) type).getActualTypeArguments()[0];
                                Type valueType = ((ParameterizedType) type).getActualTypeArguments()[1];
                                Map map = Map.class.cast(field.get(ctrl));
                                for (String k : requestMap.keySet()) {
                                    Object keyOb = null;
                                    Object valOb = null;
                                    try {
                                        Class<?> theClass = Class.forName(keyType.getTypeName());
                                        Constructor<?> cons = theClass.getConstructor(String.class);
                                        keyOb = cons.newInstance(k);

                                        theClass = Class.forName(valueType.getTypeName());
                                        cons = theClass.getConstructor(String.class);
                                        valOb = cons.newInstance(requestMap.get(k));
                                        log.debug("Setting " + param + " : " + keyOb + " -  " + valOb);
                                    } catch (ClassNotFoundException ex) {
                                        log.error(ex.toString(), ex);
                                    }
                                    map.put(keyOb, valOb);
                                }
                                field.set(ctrl, map);
                            }
                        } else if (field.getType().getName().equals("java.util.List") && request.getParameter(param) != null) {

                            Map<String, String[]> parameterMap = request.getParameterMap();
                            Type type = field.getGenericType();
                            if (type instanceof ParameterizedType) {
                                Type valueType = ((ParameterizedType) type).getActualTypeArguments()[0];
                                List list = List.class.cast(field.get(ctrl));
                                for (String p : parameterMap.get(param)) {
                                    Object valOb = null;
                                    try {
                                        Class<?> theClass = Class.forName(valueType.getTypeName());
                                        Constructor<?> cons = theClass.getConstructor(String.class);
                                        valOb = cons.newInstance(p);
                                        log.debug("Setting " + param + " : " + valOb);
                                    } catch (ClassNotFoundException ex) {
                                        log.error(ex.toString(), ex);
                                    }
                                    list.add(valOb);
                                }
                                field.set(ctrl, list);
                            }

                        } else {
                            log.debug("Setting " + param + " : " + request.getParameter(param));
                            if (field.getType().getName().equals("java.lang.String")) {
                                field.set(ctrl, request.getParameter(param));
                            } else if (field.getType().getName().equals("java.lang.Boolean")) {
                                if (!StringUtils.isEmpty(request.getParameter(param))) {
                                    field.set(ctrl, Boolean.parseBoolean(request.getParameter(param)));
                                }
                            } else if (field.getType().getName().equals("java.lang.Byte")) {
                                if (!StringUtils.isEmpty(request.getParameter(param))) {
                                    field.set(ctrl, Byte.parseByte(request.getParameter(param)));
                                }
                            } else if (field.getType().getName().equals("java.lang.Character")) {
                                if (!StringUtils.isEmpty(request.getParameter(param))) {
                                    field.set(ctrl, request.getParameter(param).charAt(0));
                                }
                            } else if (field.getType().getName().equals("java.lang.Double")) {
                                if (!StringUtils.isEmpty(request.getParameter(param))) {
                                    field.set(ctrl, Double.parseDouble(request.getParameter(param)));
                                }
                            } else if (field.getType().getName().equals("java.lang.Float")) {
                                if (!StringUtils.isEmpty(request.getParameter(param))) {
                                    field.set(ctrl, Float.parseFloat(request.getParameter(param)));
                                }
                            } else if (field.getType().getName().equals("java.lang.Integer")) {
                                if (!StringUtils.isEmpty(request.getParameter(param))) {
                                    field.set(ctrl, Integer.parseInt(request.getParameter(param)));
                                }
                            } else if (field.getType().getName().equals("java.lang.Long")) {
                                if (!StringUtils.isEmpty(request.getParameter(param))) {
                                    field.set(ctrl, Long.parseLong(request.getParameter(param)));
                                }
                            } else if (field.getType().getName().equals("java.lang.Short")) {
                                if (!StringUtils.isEmpty(request.getParameter(param))) {
                                    field.set(ctrl, Short.parseShort(request.getParameter(param)));
                                }
                            } else if (field.get(ctrl) == null) {
                                field.set(ctrl, null);
                                try {
                                    field.set(ctrl, field.getType().getDeclaredConstructor().newInstance());
                                } catch (NoSuchMethodException ex){
                                    //ignore exception
                                }
                            }

                        }
                        ctrl = field.get(ctrl);
                    }
                }
            }
        }

    }

    /**
     * true if errors are set in the request
     *
     * @return true if errors
     */
    public boolean hasErrors() {
        return !(fieldErrors.isEmpty() && errors.isEmpty());
    }

    /**
     * add error to map set in request
     *
     * @param error   error name
     * @param message error message
     */
    public void addFieldError(String error, String message) {
        this.fieldErrors.put(error, message);
    }

    /**
     * add error to list set in request
     *
     * @param message error message
     */
    public void addError(String message) {
        this.errors.add(message);
    }

    /**
     * get error to set in request
     *
     * @return list of errors
     */
    public List<String> getErrors() {
        return this.errors;
    }

    /**
     * get field errors to set in request
     *
     * @return map of field errors
     */
    public Map<String, String> getFieldErrors() {
        return this.fieldErrors;
    }

    /**
     * get http servlet request
     *
     * @return http servlet request
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * set http servlet request
     *
     * @param request http servlet reqeust
     */
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * get http servlet response
     *
     * @return http servlet response
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * set http servlet response
     *
     * @param response http servlet response
     */
    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

}

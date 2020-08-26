/**
 * Copyright (C) 2013 Loophole, LLC
 * <p>
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * As a special exception, the copyright holders give permission to link the
 * code of portions of this program with the OpenSSL library under certain
 * conditions as described in each individual source file and distribute
 * linked combinations including the program with the OpenSSL library. You
 * must comply with the GNU Affero General Public License in all respects for
 * all of the code used other than as permitted herein. If you modify file(s)
 * with this exception, you may extend this exception to your version of the
 * file(s), but you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version. If you delete this
 * exception statement from all source files in the program, then also delete
 * it in the license file.
 */
package io.bastillion.common.filter;

import io.bastillion.common.util.AuthUtil;
import io.bastillion.manage.db.AuthDB;
import io.bastillion.manage.model.Auth;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.bastillion.manage.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter determines if admin user is authenticated
 */
public class AuthFilter implements Filter {

    private static Logger log = LoggerFactory.getLogger(AuthFilter.class);

    public void init(FilterConfig config) throws ServletException {

    }

    public void destroy() {
    }

    /**
     * doFilter determines if user is an administrator or redirect to login page
     *
     * @param req   task request
     * @param resp  task response
     * @param chain filter chain
     * @throws ServletException
     * @throws IOException
     */
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException, IOException {

        HttpServletRequest servletRequest = (HttpServletRequest) req;
        HttpServletResponse servletResponse = (HttpServletResponse) resp;
        boolean isAdmin = false;

//        System.out.println("session: ");
//        System.out.println(servletRequest.getSession().getId());
//
//        System.out.println("doFilter");
//        System.out.println("current csrf: " + servletRequest.getParameter("_csrf"));
//        System.out.println("current session csrf: " + servletRequest.getSession().getAttribute("_csrf"));


        //read auth token
        String authToken = AuthUtil.getAuthToken(servletRequest.getSession());
        String authFromCookie = null;
        Long userId = -1L;

        //System.out.println("auth token : " + authToken);
        try {
            for (Cookie cookie : servletRequest.getCookies()) {
                if (cookie.getName().equals("user")) {
                    //System.out.println("auth decrypted: " + EncryptionUtil.decrypt(cookie.getValue()));
                    authFromCookie = EncryptionUtil.decrypt(cookie.getValue());
                }
                if (cookie.getName().equals("user_id")) {
                    userId = Long.parseLong(cookie.getValue());
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (authToken == null || authToken.trim().equals("")) {
            if (authFromCookie != null && !authFromCookie.trim().equals("")) {
                authToken = authFromCookie;
            }
        }

//        System.out.println("token from cookie: " + authFromCookie);

        //check if exists
        if (authToken != null && !authToken.trim().equals("")) {
            //check if valid admin auth token
            Long userId2 = AuthUtil.getUserId(servletRequest.getSession());
            userId2 = (userId2 == null && userId != -1L) ? userId : userId2;
            String userType = AuthDB.isAuthorized(userId2, authToken);
            if (userType != null) {
                String uri = servletRequest.getRequestURI();
                if (Auth.MANAGER.equals(userType)) {
                    isAdmin = true;
                } else if (!uri.contains("/manage/") && Auth.ADMINISTRATOR.equals(userType)) {
                    isAdmin = true;
                }
                AuthUtil.setUserType(servletRequest.getSession(), userType);

                //check to see if user has timed out
                String timeStr = AuthUtil.getTimeout(servletRequest.getSession());
                /*try {
                 *//*if (timeStr != null && !timeStr.trim().equals("")) {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyyHHmmss");
                        Date sessionTimeout = sdf.parse(timeStr);
                        Date currentTime = new Date();

                        //if current time > timeout then redirect to login page
//                        if (sessionTimeout == null || currentTime.after(sessionTimeout)) {
//                            isAdmin = false;
//                        } else {
//                            AuthUtil.setTimeout(servletRequest.getSession());
//                        }
                        isAdmin = true;
                    } else {
                        isAdmin = false;
                    }*//*
                    isAdmin = true;

                } catch (Exception ex) {
                    log.error(ex.toString(), ex);
                    isAdmin = false;
                }*/
            }

        }
        //if not admin redirect to login page
        if (!isAdmin) {
//            System.out.println("csrf: " + servletRequest.getParameter("_csrf"));
            AuthUtil.deleteAllSession(servletRequest.getSession());
            servletResponse.sendRedirect(servletRequest.getContextPath() + "/logout.ktrl?_csrf="
                    + servletRequest.getParameter("_csrf"));
        } else {
            chain.doFilter(req, resp);
        }
    }


}

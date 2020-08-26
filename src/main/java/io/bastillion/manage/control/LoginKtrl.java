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
package io.bastillion.manage.control;


import io.bastillion.common.util.AppConfig;
import io.bastillion.common.util.AuthUtil;
import io.bastillion.manage.db.AuthDB;
import io.bastillion.manage.db.UserDB;
import io.bastillion.manage.model.Auth;
import io.bastillion.manage.model.User;
import io.bastillion.manage.util.EncryptionUtil;
import io.bastillion.manage.util.OTPUtil;
import loophole.mvc.annotation.Kontrol;
import loophole.mvc.annotation.MethodType;
import loophole.mvc.annotation.Model;
import loophole.mvc.annotation.Validate;
import loophole.mvc.base.BaseKontroller;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginKtrl extends BaseKontroller {

    //check if otp is enabled
    @Model(name = "otpEnabled")
    static final Boolean otpEnabled = ("required".equals(AppConfig.getProperty("oneTimePassword")) || "optional".equals(AppConfig.getProperty("oneTimePassword")));
    //    private static Logger loginAuditLogger = LoggerFactory.getLogger("io.bastillion.manage.control.LoginAudit");
    private static Logger syslogger = LoggerFactory.getLogger("login-sysLogger");
    private final String AUTH_ERROR = "Authentication Failed : Login credentials are invalid";
    private final String AUTH_ERROR_NO_PROFILE = "Authentication Failed : There are no profiles assigned to this account";
    private final String AUTH_ERROR_EXPIRED_ACCOUNT = "Authentication Failed : Account has expired";
    @Model(name = "auth")
    Auth auth;


    public LoginKtrl(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Kontrol(path = "/login", method = MethodType.GET)
    public String login() {

        String retVal = "redirect:/admin/menu.html";

        String token = null;
        Long user_id = -1L;
        // Get cookies
        for (Cookie cookie : getRequest().getCookies()) {
            if (cookie.getName().equals("user")) {
                token = EncryptionUtil.decrypt(cookie.getValue());
            }
            if (cookie.getName().equals("user_id")) {
                user_id = Long.parseLong(cookie.getValue());
            }
        }
        if (token == null || user_id == -1L) {
            return "/login.html";
        }

        // Create auth based on cookies
        auth = new Auth();
        auth.setId(user_id);
        User user = UserDB.getUser(user_id);
        auth.setUsername(user.getUsername());
        auth.setAuthToken(token);
        auth.setUserType(user.getUserType());
        //
        // Update login
        AuthUtil.setAuthToken(getRequest().getSession(), token);
        AuthUtil.setUserId(getRequest().getSession(), user.getId());
        AuthUtil.setAuthType(getRequest().getSession(), user.getAuthType());
        AuthUtil.setTimeout(getRequest().getSession());
        AuthUtil.setUsername(getRequest().getSession(), user.getUsername());

        AuthDB.updateLastLogin(user);
        //

        return retVal;
//        return "/login.html";
    }

    @Kontrol(path = "/loginSubmit", method = MethodType.POST)
    public String loginSubmit() {
        String retVal = "redirect:/admin/menu.html";

        String authToken = null;
        try {
            authToken = AuthDB.login(auth);

            // set cookie
            Cookie ck = new Cookie("user", EncryptionUtil.encrypt(authToken));

            getResponse().addCookie(ck);

        } catch (Exception e) {
            System.out.println("Problem in login");
            e.printStackTrace();
            addError("Problem in login");
            return "/login.html";
        }

        //get client IP
        String clientIP = AuthUtil.getClientIPAddress(getRequest());
        if (authToken != null) {

            User user = AuthDB.getUserByAuthToken(authToken);

            // cookie
            Cookie ck2 = new Cookie("user_id", String.valueOf(user.getId()));
            getResponse().addCookie(ck2);
            //

            if (user != null) {
                String sharedSecret = null;
                if (otpEnabled) {
                    sharedSecret = AuthDB.getSharedSecret(user.getId());
                    if (StringUtils.isNotEmpty(sharedSecret) && (auth.getOtpToken() == null || !OTPUtil.verifyToken(sharedSecret, auth.getOtpToken()))) {
                        // logging
                        syslogger.info(auth.getUsername() + " (" + clientIP + ") - " + AUTH_ERROR);
                        //loginAuditLogger.info(auth.getUsername() + " (" + clientIP + ") - " + AUTH_ERROR);
                        addError(AUTH_ERROR);
                        return "/login.html";
                    }
                }
                //check to see if admin has any assigned profiles
                /*if (!User.MANAGER.equals(user.getUserType()) && (user.getProfileList() == null || user.getProfileList().size() <= 0)) {
                    // logging
                    syslogger.info(auth.getUsername() + " (" + clientIP + ") - " + AUTH_ERROR_NO_PROFILE);
                    //loginAuditLogger.info(auth.getUsername() + " (" + clientIP + ") - " + AUTH_ERROR_NO_PROFILE);
                    addError(AUTH_ERROR_NO_PROFILE);
                    return "/login.html";
                }*/

                //check to see if account has expired
                if (user.isExpired()) {
                    // logging
                    syslogger.info(auth.getUsername() + " (" + clientIP + ") - " + AUTH_ERROR_EXPIRED_ACCOUNT);
                    //loginAuditLogger.info(auth.getUsername() + " (" + clientIP + ") - " + AUTH_ERROR_EXPIRED_ACCOUNT);
                    addError(AUTH_ERROR_EXPIRED_ACCOUNT);
                    return "/login.html";
                }

                AuthUtil.setAuthToken(getRequest().getSession(), authToken);
                AuthUtil.setUserId(getRequest().getSession(), user.getId());
                AuthUtil.setAuthType(getRequest().getSession(), user.getAuthType());
                AuthUtil.setTimeout(getRequest().getSession());
                AuthUtil.setUsername(getRequest().getSession(), user.getUsername());

                AuthDB.updateLastLogin(user);

                //for first time login redirect to set OTP
                if (otpEnabled && StringUtils.isEmpty(sharedSecret)) {
                    retVal = "redirect:/admin/viewOTP.ktrl";
                } else if ("changeme".equals(auth.getPassword()) && Auth.AUTH_BASIC.equals(user.getAuthType())) {
                    retVal = "redirect:/admin/userSettings.ktrl";
                }
                // logging
                syslogger.info(auth.getUsername() + " (" + clientIP + ") - Authentication Success");
                //loginAuditLogger.info(auth.getUsername() + " (" + clientIP + ") - Authentication Success");
            }

        } else {
            syslogger.info(auth.getUsername() + " (" + clientIP + ") - " + AUTH_ERROR);
            //loginAuditLogger.info(auth.getUsername() + " (" + clientIP + ") - " + AUTH_ERROR);
            addError(AUTH_ERROR);
            retVal = "/login.html";
        }


        return retVal;
    }


    @Kontrol(path = "/logout", method = MethodType.GET)
    public String logout() {

        // Remove cookies
        Cookie ck = new Cookie("user", "");
        ck.setMaxAge(0);
        getResponse().addCookie(ck);
        Cookie ck2 = new Cookie("user_id", "");
        ck2.setMaxAge(0);
        getResponse().addCookie(ck2);
        //


        AuthUtil.deleteAllSession(getRequest().getSession());

        return "redirect:/";
    }


    /**
     * Validates fields for auth submit
     */
    @Validate(input = "/login.html")
    public void validateLoginSubmit() {
        if (auth.getUsername() == null ||
                auth.getUsername().trim().equals("")) {
            addFieldError("auth.username", "Required");
        }
        if (auth.getPassword() == null ||
                auth.getPassword().trim().equals("")) {
            addFieldError("auth.password", "Required");
        }

    }
}

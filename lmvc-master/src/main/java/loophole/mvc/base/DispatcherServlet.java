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

import loophole.mvc.filter.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("*" + DispatcherServlet.CTR_EXT)
public class DispatcherServlet extends HttpServlet {


    private static Logger log = LoggerFactory.getLogger(DispatcherServlet.class);
    public static final String CTR_EXT = ".ktrl";
    private static final long serialVersionUID = 412L;

    public DispatcherServlet() {
        super();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        execute(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        execute(request, response);
    }

    /**
     * Execute through base controller
     *
     * @param request  HTTP servlet request
     * @param response HTTP servlet response
     */
    private void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BaseKontroller bc = new BaseKontroller(request, response);

        String forward = bc.execute();

        if (forward != null) {
            if (forward.contains("redirect:")) {
                //add csrf to redirect
                forward = forward.contains("?") ? forward + "&" : forward + "?";
                forward = forward + SecurityFilter._CSRF + "=" + request.getSession().getAttribute(SecurityFilter._CSRF);
                forward = request.getContextPath() + forward.replaceAll("redirect:", "");
                log.debug("redirect : " + forward);
                response.sendRedirect(forward);

            } else {
                forward = forward.replaceAll("forward:", "");
                log.debug("forward: " + forward);
                request.getRequestDispatcher(forward)
                        .forward(request, response);
            }
        }

    }
}
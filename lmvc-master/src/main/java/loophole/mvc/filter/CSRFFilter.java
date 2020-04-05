/**
 * Copyright (C) 2018 Loophole, LLC
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
package loophole.mvc.filter;

import loophole.mvc.base.DispatcherServlet;
import loophole.mvc.base.TemplateServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Filter to check for a CSRF token and protect application from cross-site scripting attacks.
 */
@WebFilter(urlPatterns = {"/", "*" + DispatcherServlet.CTR_EXT, "*" + TemplateServlet.VIEW_EXT})
public class CSRFFilter implements Filter {

	private static Logger log = LoggerFactory.getLogger(CSRFFilter.class);

	// csrf parameter and session name
	public static final String _CSRF = "_csrf";
	private static final SecureRandom random = new SecureRandom();


	public void init(FilterConfig filterConfig) {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;

		// csrf check
		log.debug("CSRF parameter token is " + request.getParameter(_CSRF));
		log.debug("CSRF sesson token is " + httpServletRequest.getSession().getAttribute(_CSRF));
		String _csrf = (String) httpServletRequest.getSession().getAttribute(_CSRF);
		if (_csrf == null || _csrf.equals(request.getParameter(_CSRF))) {
			log.debug("CSRF token is valid for " + httpServletRequest.getRequestURL());
			if (_csrf == null || httpServletRequest.getMethod().equalsIgnoreCase("POST")) {
				_csrf = (new BigInteger(165, random)).toString(36).toUpperCase();
				httpServletRequest.getSession().setAttribute(_CSRF, _csrf);
			}
			filterChain.doFilter(request, response);
			return;
		}
		log.debug("CSRF token is invalid for " + httpServletRequest.getRequestURL());
		httpServletRequest.getSession().invalidate();
		log.debug("Session invalidated");
		httpServletResponse.sendRedirect(httpServletRequest.getContextPath());
	}

	public void destroy() {
	}
}

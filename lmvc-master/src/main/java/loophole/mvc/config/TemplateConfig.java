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
package loophole.mvc.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import loophole.mvc.base.TemplateServlet;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

@WebListener
public class TemplateConfig implements ServletContextListener {

	private static final String TEMPLATE_ENGINE = "com.thymeleafexamples.thymeleaf3.TemplateEngineInstance";
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		TemplateEngine engine = templateEngine(sce.getServletContext());
		sce.getServletContext().setAttribute(TEMPLATE_ENGINE, engine);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

	private TemplateEngine templateEngine(ServletContext servletContext) {
		TemplateEngine engine = new TemplateEngine();
		engine.setTemplateResolver(templateResolver(servletContext));
		return engine;
	}

	private ITemplateResolver templateResolver(ServletContext servletContext) {
		ServletContextTemplateResolver resolver = new ServletContextTemplateResolver(servletContext);
		resolver.setPrefix("/");
		resolver.setSuffix(TemplateServlet.VIEW_EXT);
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCacheable(true);
		resolver.setCacheTTLMs(60000L);
		resolver.setCharacterEncoding("UTF-8");
		return resolver;
	}
	
	public static TemplateEngine getTemplateEngine(ServletContext context) {
		return (TemplateEngine) context.getAttribute(TEMPLATE_ENGINE);
	}
}
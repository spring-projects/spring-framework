/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet;

import java.io.IOException;
import java.util.Locale;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * @author Juergen Hoeller
 * @since 21.05.2003
 */
public class SimpleWebApplicationContext extends StaticWebApplicationContext {

	@Override
	public void refresh() throws BeansException {
		registerSingleton("/locale.do", LocaleChecker.class);

		addMessage("test", Locale.ENGLISH, "test message");
		addMessage("test", Locale.CANADA, "Canadian & test message");
		addMessage("testArgs", Locale.ENGLISH, "test {0} message {1}");
		addMessage("testArgsFormat", Locale.ENGLISH, "test {0} message {1,number,#.##} X");

		registerSingleton("handlerMapping", BeanNameUrlHandlerMapping.class);
		registerSingleton("viewResolver", InternalResourceViewResolver.class);

		super.refresh();
	}


	public static class LocaleChecker implements Controller {

		@Override
		public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

			if (!(RequestContextUtils.findWebApplicationContext(request) instanceof SimpleWebApplicationContext)) {
				throw new ServletException("Incorrect WebApplicationContext");
			}
			if (!(RequestContextUtils.getLocaleResolver(request) instanceof AcceptHeaderLocaleResolver)) {
				throw new ServletException("Incorrect LocaleResolver");
			}
			if (!Locale.CANADA.equals(RequestContextUtils.getLocale(request))) {
				throw new ServletException("Incorrect Locale");
			}
			return null;
		}
	}

}

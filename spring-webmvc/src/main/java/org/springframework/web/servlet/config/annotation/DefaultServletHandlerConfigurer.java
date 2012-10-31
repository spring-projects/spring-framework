/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config.annotation;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

/**
 * Configures a request handler for serving static resources by forwarding the request to the Servlet container's 
 * "default" Servlet. This is intended to be used when the Spring MVC {@link DispatcherServlet} is mapped to "/"
 * thus overriding the Servlet container's default handling of static resources. Since this handler is configured 
 * at the lowest precedence, effectively it allows all other handler mappings to handle the request, and if none
 * of them do, this handler can forward it to the "default" Servlet.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 *
 * @see DefaultServletHttpRequestHandler
 */
public class DefaultServletHandlerConfigurer {

	private final ServletContext servletContext;

	private DefaultServletHttpRequestHandler handler;

	/**
	 * Create a {@link DefaultServletHandlerConfigurer} instance.
	 * @param servletContext the ServletContext to use to configure the underlying DefaultServletHttpRequestHandler.
	 */
	public DefaultServletHandlerConfigurer(ServletContext servletContext) {
		Assert.notNull(servletContext, "A ServletContext is required to configure default servlet handling");
		this.servletContext = servletContext;
	}

	/**
	 * Enable forwarding to the "default" Servlet. When this method is used the {@link DefaultServletHttpRequestHandler}
	 * will try to auto-detect the "default" Servlet name. Alternatively, you can specify the name of the default 
	 * Servlet via {@link #enable(String)}.
	 * @see DefaultServletHttpRequestHandler
	 */
	public void enable() {
		enable(null);
	}

	/**
	 * Enable forwarding to the "default" Servlet identified by the given name.
	 * This is useful when the default Servlet cannot be auto-detected, for example when it has been manually configured.
	 * @see DefaultServletHttpRequestHandler
	 */
	public void enable(String defaultServletName) {
		handler = new DefaultServletHttpRequestHandler();
		handler.setDefaultServletName(defaultServletName);
		handler.setServletContext(servletContext);
	}

	/**
	 * Return a handler mapping instance ordered at {@link Integer#MAX_VALUE} containing the
	 * {@link DefaultServletHttpRequestHandler} instance mapped to {@code "/**"}; or {@code null} if 
	 * default servlet handling was not been enabled.
	 */
	protected AbstractHandlerMapping getHandlerMapping() {
		if (handler == null) {
			return null;
		}
		
		Map<String, HttpRequestHandler> urlMap = new HashMap<String, HttpRequestHandler>();
		urlMap.put("/**", handler);

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(Integer.MAX_VALUE);
		handlerMapping.setUrlMap(urlMap);
		return handlerMapping;
	}

}
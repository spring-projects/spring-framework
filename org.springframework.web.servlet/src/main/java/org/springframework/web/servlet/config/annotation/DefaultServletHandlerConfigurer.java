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

import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

/**
 * Helps with configuring a handler for serving static resources by forwarding to the Servlet container's default
 * Servlet. This is commonly used when the {@link DispatcherServlet} is mapped to "/", which results in cleaner
 * URLs (without a servlet prefix) but may need to still allow some requests (e.g. static resources) to be handled
 * by the Servlet container's default servlet.
 *
 * <p>It is important the configured handler remains last in the order of all {@link HandlerMapping} instances in
 * the Spring MVC web application context. That is is the case if relying on @{@link EnableMvcConfiguration}.
 * However, if you register your own HandlerMapping instance sure to set its "order" property to a value lower
 * than that of the {@link DefaultServletHttpRequestHandler}, which is {@link Integer#MAX_VALUE}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 *
 * @see ResourceConfigurer
 */
public class DefaultServletHandlerConfigurer {

	private DefaultServletHttpRequestHandler requestHandler;

	private final ServletContext servletContext;

	public DefaultServletHandlerConfigurer(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * Enable forwarding to the Servlet container default servlet. The {@link DefaultServletHttpRequestHandler}
	 * will try to auto-detect the default Servlet at startup using a list of known names. Alternatively, you can
	 * specify the name of the default Servlet, see {@link #enable(String)}.
	 */
	public void enable() {
		enable(null);
	}

	/**
	 * Enable forwarding to the Servlet container default servlet specifying explicitly the name of the default
	 * Servlet to forward static resource requests to. This is useful when the default Servlet cannot be detected
	 * (e.g. when using an unknown container or when it has been manually configured).
	 */
	public void enable(String defaultServletName) {
		requestHandler = new DefaultServletHttpRequestHandler();
		requestHandler.setDefaultServletName(defaultServletName);
		requestHandler.setServletContext(servletContext);
	}

	/**
	 * Return a {@link SimpleUrlHandlerMapping} instance ordered at {@link Integer#MAX_VALUE} containing a
	 * {@link DefaultServletHttpRequestHandler} mapped to {@code /**}.
	 */
	protected SimpleUrlHandlerMapping getHandlerMapping() {
		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(Integer.MAX_VALUE);
		handlerMapping.setUrlMap(getUrlMap());
		return handlerMapping;
	}

	private Map<String, HttpRequestHandler> getUrlMap() {
		Map<String, HttpRequestHandler> urlMap = new HashMap<String, HttpRequestHandler>();
		if (requestHandler != null) {
			urlMap.put("/**", requestHandler);
		}
		return urlMap ;
	}

}
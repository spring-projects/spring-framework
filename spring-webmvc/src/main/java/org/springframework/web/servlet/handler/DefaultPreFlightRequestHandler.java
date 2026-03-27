/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.PreFlightRequestHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.util.ServletRequestPathUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Default implementation of {@link PreFlightRequestHandler} that discovers all
 * {@link HandlerMapping} beans in the {@link ApplicationContext} and uses them to
 * find a handler for a pre-flight CORS request, then delegates to the
 * {@link PreFlightRequestHandler} returned by that mapping.
 *
 * <p>Handler mappings are detected and sorted in the same way as in
 * {@link DispatcherServlet}, with fallback to the default mappings configured in
 * {@code DispatcherServlet.properties} if no mappings are found.
 *
 * @since 7.1
 * @see PreFlightRequestHandler
 * @see HandlerMapping
 * @see DispatcherServlet
 */
public class DefaultPreFlightRequestHandler
		implements PreFlightRequestHandler, ApplicationContextAware, InitializingBean {

	private @Nullable ApplicationContext applicationContext;

	private @Nullable List<HandlerMapping> handlerMappings;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.handlerMappings == null) {
			Assert.notNull(this.applicationContext, "No ApplicationContext");
			this.handlerMappings = initHandlerMappings(this.applicationContext);
		}
	}

	private static List<HandlerMapping> initHandlerMappings(ApplicationContext context) {
		Map<String, HandlerMapping> beans =
				BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);

		if (!beans.isEmpty()) {
			List<HandlerMapping> mappings = new ArrayList<>(beans.values());
			AnnotationAwareOrderComparator.sort(mappings);
			return Collections.unmodifiableList(mappings);
		}

		return Collections.unmodifiableList(initFallback(context));
	}

	private static List<HandlerMapping> initFallback(ApplicationContext applicationContext) {
		Properties properties;
		try {
			Resource resource = new ClassPathResource("DispatcherServlet.properties", DispatcherServlet.class);
			properties = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load DispatcherServlet.properties: " + ex.getMessage());
		}

		String value = properties.getProperty(HandlerMapping.class.getName());
		String[] names = StringUtils.commaDelimitedListToStringArray(value);
		List<HandlerMapping> result = new ArrayList<>(names.length);
		for (String name : names) {
			try {
				Class<?> clazz = ClassUtils.forName(name, DispatcherServlet.class.getClassLoader());
				Object mapping = applicationContext.getAutowireCapableBeanFactory().createBean(clazz);
				result.add((HandlerMapping) mapping);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Could not find default HandlerMapping [" + name + "]");
			}
		}
		return result;
	}

	/**
	 * Find the matching {@link HandlerMapping} for the request, and invoke the
	 * handler it returns as a {@link PreFlightRequestHandler}.
	 * @throws NoHandlerFoundException if no handler matches the request
	 * @since 7.1
	 */
	@Override
	public void handlePreFlight(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Assert.state(this.handlerMappings != null, "Not yet initialized via afterPropertiesSet.");
		Assert.state(CorsUtils.isPreFlightRequest(request), "Not a pre-flight request.");
		RequestPath previousPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
		try {
			ServletRequestPathUtils.parseAndCache(request);
			for (HandlerMapping mapping : this.handlerMappings) {
				HandlerExecutionChain chain = mapping.getHandler(request);
				if (chain != null) {
					Object handler = chain.getHandler();
					if (handler instanceof PreFlightRequestHandler preFlightHandler) {
						preFlightHandler.handlePreFlight(request, response);
						return;
					}
					throw new IllegalStateException("Expected PreFlightRequestHandler: " + handler.getClass());
				}
			}
			throw new NoHandlerFoundException(
					request.getMethod(), request.getRequestURI(), new ServletServerHttpRequest(request).getHeaders());
		}
		finally {
			ServletRequestPathUtils.setParsedRequestPath(previousPath, request);
		}
	}
}

/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Helper class to get information from the {@code HandlerMapping} that would
 * serve a specific request.
 *
 * <p>Provides the following methods:
 * <ul>
 * <li>{@link #getMatchableHandlerMapping} &mdash; obtain a {@code HandlerMapping}
 * to check request-matching criteria against.
 * <li>{@link #getCorsConfiguration} &mdash; obtain the CORS configuration for the
 * request.
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
public class HandlerMappingIntrospector implements CorsConfigurationSource {

	private final List<HandlerMapping> handlerMappings;


	/**
	 * Constructor that detects the configured {@code HandlerMapping}s in the
	 * given {@code ApplicationContext} or falls back on
	 * "DispatcherServlet.properties" like the {@code DispatcherServlet}.
	 */
	public HandlerMappingIntrospector(ApplicationContext context) {
		this.handlerMappings = initHandlerMappings(context);
	}


	private static List<HandlerMapping> initHandlerMappings(ApplicationContext context) {
		Map<String, HandlerMapping> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerMapping.class, true, false);
		if (!beans.isEmpty()) {
			List<HandlerMapping> mappings = new ArrayList<>(beans.values());
			AnnotationAwareOrderComparator.sort(mappings);
			return mappings;
		}
		return initDefaultHandlerMappings(context);
	}

	private static List<HandlerMapping> initDefaultHandlerMappings(ApplicationContext context) {
		Properties props;
		String path = "DispatcherServlet.properties";
		try {
			Resource resource = new ClassPathResource(path, DispatcherServlet.class);
			props = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load '" + path + "': " + ex.getMessage());
		}

		String value = props.getProperty(HandlerMapping.class.getName());
		String[] names = StringUtils.commaDelimitedListToStringArray(value);
		List<HandlerMapping> result = new ArrayList<>(names.length);
		for (String name : names) {
			try {
				Class<?> clazz = ClassUtils.forName(name, DispatcherServlet.class.getClassLoader());
				Object mapping = context.getAutowireCapableBeanFactory().createBean(clazz);
				result.add((HandlerMapping) mapping);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Could not find default HandlerMapping [" + name + "]");
			}
		}
		return result;
	}


	/**
	 * Return the configured HandlerMapping's.
	 */
	public List<HandlerMapping> getHandlerMappings() {
		return this.handlerMappings;
	}

	/**
	 * Find the {@link HandlerMapping} that would handle the given request and
	 * return it as a {@link MatchableHandlerMapping} that can be used to test
	 * request-matching criteria.
	 * <p>If the matching HandlerMapping is not an instance of
	 * {@link MatchableHandlerMapping}, an IllegalStateException is raised.
	 * @param request the current request
	 * @return the resolved matcher, or {@code null}
	 * @throws Exception if any of the HandlerMapping's raise an exception
	 */
	public MatchableHandlerMapping getMatchableHandlerMapping(HttpServletRequest request) throws Exception {
		HttpServletRequest wrapper = new RequestAttributeChangeIgnoringWrapper(request);
		for (HandlerMapping handlerMapping : this.handlerMappings) {
			Object handler = handlerMapping.getHandler(wrapper);
			if (handler == null) {
				continue;
			}
			if (handlerMapping instanceof MatchableHandlerMapping) {
				return ((MatchableHandlerMapping) handlerMapping);
			}
			throw new IllegalStateException("HandlerMapping is not a MatchableHandlerMapping");
		}
		return null;
	}

	@Override
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		HttpServletRequest wrapper = new RequestAttributeChangeIgnoringWrapper(request);
		for (HandlerMapping handlerMapping : this.handlerMappings) {
			HandlerExecutionChain handler = null;
			try {
				handler = handlerMapping.getHandler(wrapper);
			}
			catch (Exception ex) {
				// Ignore
			}
			if (handler == null) {
				continue;
			}
			if (handler.getInterceptors() != null) {
				for (HandlerInterceptor interceptor : handler.getInterceptors()) {
					if (interceptor instanceof CorsConfigurationSource) {
						return ((CorsConfigurationSource) interceptor).getCorsConfiguration(wrapper);
					}
				}
			}
			if (handler.getHandler() instanceof CorsConfigurationSource) {
				return ((CorsConfigurationSource) handler.getHandler()).getCorsConfiguration(wrapper);
			}
		}
		return null;
	}


	/**
	 * Request wrapper that ignores request attribute changes.
	 */
	private static class RequestAttributeChangeIgnoringWrapper extends HttpServletRequestWrapper {

		public RequestAttributeChangeIgnoringWrapper(HttpServletRequest request) {
			super(request);
		}

		@Override
		public void setAttribute(String name, Object value) {
			// Ignore attribute change
		}
	}

}

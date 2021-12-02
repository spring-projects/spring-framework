/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

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
 * <p><strong>Note:</strong> This is primarily an SPI to allow Spring Security
 * to align its pattern matching with the same pattern matching that would be
 * used in Spring MVC for a given request, in order to avoid security issues.
 * Use of this introspector should be avoided for other purposes because it
 * incurs the overhead of resolving the handler for a request.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
public class HandlerMappingIntrospector
		implements CorsConfigurationSource, ApplicationContextAware, InitializingBean {

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private List<HandlerMapping> handlerMappings;

	private Map<HandlerMapping, PathPatternMatchableHandlerMapping> pathPatternHandlerMappings = Collections.emptyMap();


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.handlerMappings == null) {
			Assert.notNull(this.applicationContext, "No ApplicationContext");
			this.handlerMappings = initHandlerMappings(this.applicationContext);
			this.pathPatternHandlerMappings = initPathPatternMatchableHandlerMappings(this.handlerMappings);
		}
	}

	/**
	 * Return the configured or detected {@code HandlerMapping}s.
	 */
	public List<HandlerMapping> getHandlerMappings() {
		return (this.handlerMappings != null ? this.handlerMappings : Collections.emptyList());
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
	@Nullable
	public MatchableHandlerMapping getMatchableHandlerMapping(HttpServletRequest request) throws Exception {
		HttpServletRequest wrappedRequest = new AttributesPreservingRequest(request);
		return doWithMatchingMapping(wrappedRequest, false, (matchedMapping, executionChain) -> {
			if (matchedMapping instanceof MatchableHandlerMapping) {
				PathPatternMatchableHandlerMapping mapping = this.pathPatternHandlerMappings.get(matchedMapping);
				if (mapping != null) {
					RequestPath requestPath = ServletRequestPathUtils.getParsedRequestPath(wrappedRequest);
					return new PathSettingHandlerMapping(mapping, requestPath);
				}
				else {
					String lookupPath = (String) wrappedRequest.getAttribute(UrlPathHelper.PATH_ATTRIBUTE);
					return new PathSettingHandlerMapping((MatchableHandlerMapping) matchedMapping, lookupPath);
				}
			}
			throw new IllegalStateException("HandlerMapping is not a MatchableHandlerMapping");
		});
	}

	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		AttributesPreservingRequest wrappedRequest = new AttributesPreservingRequest(request);
		return doWithMatchingMappingIgnoringException(wrappedRequest, (handlerMapping, executionChain) -> {
			for (HandlerInterceptor interceptor : executionChain.getInterceptorList()) {
				if (interceptor instanceof CorsConfigurationSource) {
					return ((CorsConfigurationSource) interceptor).getCorsConfiguration(wrappedRequest);
				}
			}
			if (executionChain.getHandler() instanceof CorsConfigurationSource) {
				return ((CorsConfigurationSource) executionChain.getHandler()).getCorsConfiguration(wrappedRequest);
			}
			return null;
		});
	}

	@Nullable
	private <T> T doWithMatchingMapping(
			HttpServletRequest request, boolean ignoreException,
			BiFunction<HandlerMapping, HandlerExecutionChain, T> matchHandler) throws Exception {

		Assert.notNull(this.handlerMappings, "Handler mappings not initialized");

		boolean parseRequestPath = !this.pathPatternHandlerMappings.isEmpty();
		RequestPath previousPath = null;
		if (parseRequestPath) {
			previousPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
			ServletRequestPathUtils.parseAndCache(request);
		}
		try {
			for (HandlerMapping handlerMapping : this.handlerMappings) {
				HandlerExecutionChain chain = null;
				try {
					chain = handlerMapping.getHandler(request);
				}
				catch (Exception ex) {
					if (!ignoreException) {
						throw ex;
					}
				}
				if (chain == null) {
					continue;
				}
				return matchHandler.apply(handlerMapping, chain);
			}
		}
		finally {
			if (parseRequestPath) {
				ServletRequestPathUtils.setParsedRequestPath(previousPath, request);
			}
		}
		return null;
	}

	@Nullable
	private <T> T doWithMatchingMappingIgnoringException(
			HttpServletRequest request, BiFunction<HandlerMapping, HandlerExecutionChain, T> matchHandler) {

		try {
			return doWithMatchingMapping(request, true, matchHandler);
		}
		catch (Exception ex) {
			throw new IllegalStateException("HandlerMapping exception not suppressed", ex);
		}
	}


	private static List<HandlerMapping> initHandlerMappings(ApplicationContext applicationContext) {
		Map<String, HandlerMapping> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				applicationContext, HandlerMapping.class, true, false);
		if (!beans.isEmpty()) {
			List<HandlerMapping> mappings = new ArrayList<>(beans.values());
			AnnotationAwareOrderComparator.sort(mappings);
			return Collections.unmodifiableList(mappings);
		}
		return Collections.unmodifiableList(initFallback(applicationContext));
	}

	private static List<HandlerMapping> initFallback(ApplicationContext applicationContext) {
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
				Object mapping = applicationContext.getAutowireCapableBeanFactory().createBean(clazz);
				result.add((HandlerMapping) mapping);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Could not find default HandlerMapping [" + name + "]");
			}
		}
		return result;
	}

	private static Map<HandlerMapping, PathPatternMatchableHandlerMapping> initPathPatternMatchableHandlerMappings(
			List<HandlerMapping> mappings) {

		return mappings.stream()
				.filter(mapping -> mapping instanceof MatchableHandlerMapping)
				.map(mapping -> (MatchableHandlerMapping) mapping)
				.filter(mapping -> mapping.getPatternParser() != null)
				.collect(Collectors.toMap(mapping -> mapping, PathPatternMatchableHandlerMapping::new));
	}


	/**
	 * Request wrapper that buffers request attributes in order protect the
	 * underlying request from attribute changes.
	 */
	private static class AttributesPreservingRequest extends HttpServletRequestWrapper {

		private final Map<String, Object> attributes;

		AttributesPreservingRequest(HttpServletRequest request) {
			super(request);
			this.attributes = initAttributes(request);
		}

		private Map<String, Object> initAttributes(HttpServletRequest request) {
			Map<String, Object> map = new HashMap<>();
			Enumeration<String> names = request.getAttributeNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				map.put(name, request.getAttribute(name));
			}
			return map;
		}

		@Override
		public void setAttribute(String name, Object value) {
			this.attributes.put(name, value);
		}

		@Override
		public Object getAttribute(String name) {
			return this.attributes.get(name);
		}

		@Override
		public Enumeration<String> getAttributeNames() {
			return Collections.enumeration(this.attributes.keySet());
		}

		@Override
		public void removeAttribute(String name) {
			this.attributes.remove(name);
		}
	}


	private static class PathSettingHandlerMapping implements MatchableHandlerMapping {

		private final MatchableHandlerMapping delegate;

		private final Object path;

		private final String pathAttributeName;

		PathSettingHandlerMapping(MatchableHandlerMapping delegate, Object path) {
			this.delegate = delegate;
			this.path = path;
			this.pathAttributeName = (path instanceof RequestPath ?
					ServletRequestPathUtils.PATH_ATTRIBUTE : UrlPathHelper.PATH_ATTRIBUTE);
		}

		@Nullable
		@Override
		public RequestMatchResult match(HttpServletRequest request, String pattern) {
			Object previousPath = request.getAttribute(this.pathAttributeName);
			request.setAttribute(this.pathAttributeName, this.path);
			try {
				return this.delegate.match(request, pattern);
			}
			finally {
				request.setAttribute(this.pathAttributeName, previousPath);
			}
		}

		@Nullable
		@Override
		public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
			return this.delegate.getHandler(request);
		}
	}

}

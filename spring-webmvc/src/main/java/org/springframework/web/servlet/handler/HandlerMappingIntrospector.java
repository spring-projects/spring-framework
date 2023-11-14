/*
 * Copyright 2002-2023 the original author or authors.
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

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
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
import org.springframework.web.util.pattern.PathPatternParser;

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
 * <p>Note that this is primarily an SPI to allow Spring Security
 * to align its pattern matching with the same pattern matching that would be
 * used in Spring MVC for a given request, in order to avoid security issues.
 * Use of this introspector should be avoided for other purposes because it
 * incurs the overhead of resolving the handler for a request.
 *
 * <p>Alternative security filter solutions that also rely on
 * {@link HandlerMappingIntrospector} should consider adding an additional
 * {@link jakarta.servlet.Filter} that invokes
 * {@link #setCache(HttpServletRequest)} and {@link #resetCache(ServletRequest, CachedResult)}
 * before and after delegating to the rest of the chain. Such a Filter should
 * process all dispatcher types and should be ordered ahead of security filters.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
public class HandlerMappingIntrospector
		implements CorsConfigurationSource, ApplicationContextAware, InitializingBean {

	private static final String CACHED_RESULT_ATTRIBUTE =
			HandlerMappingIntrospector.class.getName() + ".CachedResult";


	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private List<HandlerMapping> handlerMappings;

	private Map<HandlerMapping, PathPatternMatchableHandlerMapping> pathPatternMappings = Collections.emptyMap();


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.handlerMappings == null) {
			Assert.notNull(this.applicationContext, "No ApplicationContext");
			this.handlerMappings = initHandlerMappings(this.applicationContext);

			this.pathPatternMappings = this.handlerMappings.stream()
					.filter(m -> m instanceof MatchableHandlerMapping hm && hm.getPatternParser() != null)
					.map(mapping -> (MatchableHandlerMapping) mapping)
					.collect(Collectors.toMap(mapping -> mapping, PathPatternMatchableHandlerMapping::new));
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
	 * Return the configured or detected {@code HandlerMapping}s.
	 */
	public List<HandlerMapping> getHandlerMappings() {
		return (this.handlerMappings != null ? this.handlerMappings : Collections.emptyList());
	}


	/**
	 * Perform a lookup and save the {@link CachedResult} as a request attribute.
	 * This method can be invoked from a filter before subsequent calls to
	 * {@link #getMatchableHandlerMapping(HttpServletRequest)} and
	 * {@link #getCorsConfiguration(HttpServletRequest)} to avoid repeated lookups.
	 * @param request the current request
	 * @return the previous {@link CachedResult}, if there is one from a parent dispatch
	 * @throws ServletException thrown the lookup fails for any reason
	 * @since 6.0.14
	 */
	@Nullable
	public CachedResult setCache(HttpServletRequest request) throws ServletException {
		CachedResult previous = getAttribute(request);
		if (previous == null || !previous.matches(request)) {
			try {
				HttpServletRequest wrapped = new AttributesPreservingRequest(request);
				CachedResult cachedResult = doWithHandlerMapping(wrapped, false, (mapping, executionChain) -> {
					MatchableHandlerMapping matchableMapping = createMatchableHandlerMapping(mapping, wrapped);
					CorsConfiguration corsConfig = getCorsConfiguration(wrapped, executionChain);
					return new CachedResult(request, matchableMapping, corsConfig);
				});
				request.setAttribute(CACHED_RESULT_ATTRIBUTE,
						cachedResult != null ? cachedResult : new CachedResult(request, null, null));
			}
			catch (Throwable ex) {
				throw new ServletException("HandlerMapping introspection failed", ex);
			}
		}
		return previous;
	}

	/**
	 * Restore a previous {@link CachedResult}. This method can be invoked from
	 * a filter after delegating to the rest of the chain.
	 * @since 6.0.14
	 */
	public void resetCache(ServletRequest request, @Nullable CachedResult cachedResult) {
		request.setAttribute(CACHED_RESULT_ATTRIBUTE, cachedResult);
	}

	/**
	 * Find the {@link HandlerMapping} that would handle the given request and
	 * return a {@link MatchableHandlerMapping} to use for path matching.
	 * @param request the current request
	 * @return the resolved {@code MatchableHandlerMapping}, or {@code null}
	 * @throws IllegalStateException if the matching HandlerMapping is not an
	 * instance of {@link MatchableHandlerMapping}
	 * @throws Exception if any of the HandlerMapping's raise an exception
	 */
	@Nullable
	public MatchableHandlerMapping getMatchableHandlerMapping(HttpServletRequest request) throws Exception {
		CachedResult cachedResult = getCachedResultFor(request);
		if (cachedResult != null) {
			return cachedResult.getHandlerMapping();
		}
		HttpServletRequest requestToUse = new AttributesPreservingRequest(request);
		return doWithHandlerMapping(requestToUse, false,
				(mapping, executionChain) -> createMatchableHandlerMapping(mapping, requestToUse));
	}

	private MatchableHandlerMapping createMatchableHandlerMapping(HandlerMapping mapping, HttpServletRequest request) {
		if (mapping instanceof MatchableHandlerMapping) {
			PathPatternMatchableHandlerMapping pathPatternMapping = this.pathPatternMappings.get(mapping);
			if (pathPatternMapping != null) {
				RequestPath requestPath = ServletRequestPathUtils.getParsedRequestPath(request);
				return new LookupPathMatchableHandlerMapping(pathPatternMapping, requestPath);
			}
			else {
				String lookupPath = (String) request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE);
				return new LookupPathMatchableHandlerMapping((MatchableHandlerMapping) mapping, lookupPath);
			}
		}
		throw new IllegalStateException("HandlerMapping is not a MatchableHandlerMapping");
	}

	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		CachedResult cachedResult = getCachedResultFor(request);
		if (cachedResult != null) {
			return cachedResult.getCorsConfig();
		}
		try {
			boolean ignoreException = true;
			AttributesPreservingRequest requestToUse = new AttributesPreservingRequest(request);
			return doWithHandlerMapping(requestToUse, ignoreException,
					(handlerMapping, executionChain) -> getCorsConfiguration(requestToUse, executionChain));
		}
		catch (Exception ex) {
			// HandlerMapping exceptions have been ignored. Some more basic error perhaps like request parsing
			throw new IllegalStateException(ex);
		}
	}

	@Nullable
	private static CorsConfiguration getCorsConfiguration(HttpServletRequest request, HandlerExecutionChain chain) {
		for (HandlerInterceptor interceptor : chain.getInterceptorList()) {
			if (interceptor instanceof CorsConfigurationSource source) {
				return source.getCorsConfiguration(request);
			}
		}
		if (chain.getHandler() instanceof CorsConfigurationSource source) {
			return source.getCorsConfiguration(request);
		}
		return null;
	}

	@Nullable
	private <T> T doWithHandlerMapping(
			HttpServletRequest request, boolean ignoreException,
			BiFunction<HandlerMapping, HandlerExecutionChain, T> extractor) throws Exception {

		Assert.state(this.handlerMappings != null, "HandlerMapping's not initialized");

		boolean parsePath = !this.pathPatternMappings.isEmpty();
		RequestPath previousPath = null;
		if (parsePath) {
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
				return extractor.apply(handlerMapping, chain);
			}
		}
		finally {
			if (parsePath) {
				ServletRequestPathUtils.setParsedRequestPath(previousPath, request);
			}
		}
		return null;
	}

	/**
	 * Return a {@link CachedResult} that matches the given request.
	 */
	@Nullable
	private CachedResult getCachedResultFor(HttpServletRequest request) {
		CachedResult result = getAttribute(request);
		return (result != null && result.matches(request) ? result : null);
	}

	@Nullable
	private static CachedResult getAttribute(HttpServletRequest request) {
		return (CachedResult) request.getAttribute(CACHED_RESULT_ATTRIBUTE);
	}


	/**
	 * Container for a {@link MatchableHandlerMapping} and {@link CorsConfiguration}
	 * for a given request identified by dispatcher type and requestURI.
	 * @since 6.0.14
	 */
	@SuppressWarnings("serial")
	public static final class CachedResult {

		private final DispatcherType dispatcherType;

		private final String requestURI;

		@Nullable
		private final MatchableHandlerMapping handlerMapping;

		@Nullable
		private final CorsConfiguration corsConfig;

		private CachedResult(HttpServletRequest request,
				@Nullable MatchableHandlerMapping mapping, @Nullable CorsConfiguration config) {

			this.dispatcherType = request.getDispatcherType();
			this.requestURI = request.getRequestURI();
			this.handlerMapping = mapping;
			this.corsConfig = config;
		}

		public boolean matches(HttpServletRequest request) {
			return (this.dispatcherType.equals(request.getDispatcherType()) &&
					this.requestURI.matches(request.getRequestURI()));
		}

		@Nullable
		public MatchableHandlerMapping getHandlerMapping() {
			return this.handlerMapping;
		}

		@Nullable
		public CorsConfiguration getCorsConfig() {
			return this.corsConfig;
		}

		@Override
		public String toString() {
			return "CacheValue " + this.dispatcherType + " '" + this.requestURI + "'";
		}
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
			this.attributes.put(AbstractHandlerMapping.SUPPRESS_LOGGING_ATTRIBUTE, Boolean.TRUE);
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


	private static class LookupPathMatchableHandlerMapping implements MatchableHandlerMapping {

		private final MatchableHandlerMapping delegate;

		private final Object lookupPath;

		private final String pathAttributeName;

		LookupPathMatchableHandlerMapping(MatchableHandlerMapping delegate, Object lookupPath) {
			this.delegate = delegate;
			this.lookupPath = lookupPath;
			this.pathAttributeName = (lookupPath instanceof RequestPath ?
					ServletRequestPathUtils.PATH_ATTRIBUTE : UrlPathHelper.PATH_ATTRIBUTE);
		}

		@Override
		public PathPatternParser getPatternParser() {
			return this.delegate.getPatternParser();
		}

		@Nullable
		@Override
		public RequestMatchResult match(HttpServletRequest request, String pattern) {
			pattern = initFullPathPattern(pattern);
			Object previousPath = request.getAttribute(this.pathAttributeName);
			request.setAttribute(this.pathAttributeName, this.lookupPath);
			try {
				return this.delegate.match(request, pattern);
			}
			finally {
				request.setAttribute(this.pathAttributeName, previousPath);
			}
		}

		private String initFullPathPattern(String pattern) {
			PathPatternParser parser = (getPatternParser() != null ? getPatternParser() : PathPatternParser.defaultInstance);
			return parser.initFullPathPattern(pattern);
		}

		@Nullable
		@Override
		public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
			return this.delegate.getHandler(request);
		}
	}

}

/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.PreFlightRequestHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.NoHandlerFoundException;
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
 *
 * <p>Use of this component incurs the performance overhead of mapping the
 * request, and should not be repeated multiple times per request.
 * {@link #createCacheFilter()} exposes a Filter to cache the results.
 * Applications that rely on Spring Security don't need to deploy this Filter
 * since Spring Security doe that. However, other custom security layers, used
 * in place of Spring Security that use this component should deploy the cache
 * Filter with requirements described in the Javadoc for the method.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
public class HandlerMappingIntrospector
		implements CorsConfigurationSource, PreFlightRequestHandler, ApplicationContextAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(HandlerMappingIntrospector.class.getName());

	private static final String CACHED_RESULT_ATTRIBUTE =
			HandlerMappingIntrospector.class.getName() + ".CachedResult";


	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private List<HandlerMapping> handlerMappings;

	private Map<HandlerMapping, PathPatternMatchableHandlerMapping> pathPatternMappings = Collections.emptyMap();

	private final CacheResultLogHelper cacheLogHelper = new CacheResultLogHelper();


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
	 * Return {@code true} if all {@link HandlerMapping} beans
	 * {@link HandlerMapping#usesPathPatterns() use parsed PathPatterns},
	 * and {@code false} if any don't.
	 * @since 6.2
	 */
	public boolean allHandlerMappingsUsePathPatternParser() {
		Assert.state(this.handlerMappings != null, "Not yet initialized via afterPropertiesSet.");
		return getHandlerMappings().stream().allMatch(HandlerMapping::usesPathPatterns);
	}


	/**
	 * Find the matching {@link HandlerMapping} for the request, and invoke the
	 * handler it returns as a {@link PreFlightRequestHandler}.
	 * @throws NoHandlerFoundException if no handler matches the request
	 * @since 6.2
	 */
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


	/**
	 * {@link Filter} that looks up the {@code MatchableHandlerMapping} and
	 * {@link CorsConfiguration} for the request proactively before delegating
	 * to the rest of the chain, caching the result in a request attribute, and
	 * restoring it after the chain returns.
	 * <p><strong>Note:</strong> Applications that rely on Spring Security do
	 * not use this component directly and should not deploy the filter instead
	 * allowing Spring Security to do it. Other custom security layers used in
	 * place of Spring Security that also rely on {@code HandlerMappingIntrospector}
	 * should deploy this filter ahead of other filters where lookups are
	 * performed, and should also make sure the filter is configured to handle
	 * all dispatcher types.
	 * @return the Filter instance to use
	 * @since 6.0.14
	 */
	public Filter createCacheFilter() {
		return (request, response, chain) -> {
			CachedResult previous = setCache((HttpServletRequest) request);
			try {
				chain.doFilter(request, response);
			}
			finally {
				resetCache(request, previous);
			}
		};
	}

	/**
	 * Perform a lookup and save the {@link CachedResult} as a request attribute.
	 * This method can be invoked from a filter before subsequent calls to
	 * {@link #getMatchableHandlerMapping(HttpServletRequest)} and
	 * {@link #getCorsConfiguration(HttpServletRequest)} to avoid repeated lookups.
	 * @param request the current request
	 * @return the previous {@link CachedResult}, if there is one from a parent dispatch
	 * @since 6.0.14
	 */
	@Nullable
	public CachedResult setCache(HttpServletRequest request) {
		CachedResult previous = (CachedResult) request.getAttribute(CACHED_RESULT_ATTRIBUTE);
		if (previous == null || !previous.matches(request)) {
			HttpServletRequest wrapped = new AttributesPreservingRequest(request);
			CachedResult result;
			try {
				// Try to get both in one lookup (with ignoringException=false)
				result = doWithHandlerMapping(wrapped, false, (mapping, executionChain) -> {
					MatchableHandlerMapping matchableMapping = createMatchableHandlerMapping(mapping, wrapped);
					CorsConfiguration corsConfig = getCorsConfiguration(executionChain, wrapped);
					return new CachedResult(request, matchableMapping, corsConfig, null, null);
				});
			}
			catch (Exception ex) {
				try {
					// Try CorsConfiguration at least with ignoreException=true
					AttributesPreservingRequest requestToUse = new AttributesPreservingRequest(request);
					result = doWithHandlerMapping(requestToUse, true, (mapping, executionChain) -> {
						CorsConfiguration corsConfig = getCorsConfiguration(executionChain, wrapped);
						return new CachedResult(request, null, corsConfig, ex, null);
					});
				}
				catch (Exception ex2) {
					result = new CachedResult(request, null, null, ex, new IllegalStateException(ex2));
				}
			}
			if (result == null) {
				result = new CachedResult(request, null, null, null, null);
			}
			request.setAttribute(CACHED_RESULT_ATTRIBUTE, result);
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
		CachedResult result = CachedResult.getResultFor(request);
		if (result != null) {
			return result.getHandlerMapping();
		}
		this.cacheLogHelper.logHandlerMappingCacheMiss(request);
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
		CachedResult result = CachedResult.getResultFor(request);
		if (result != null) {
			return result.getCorsConfig();
		}
		this.cacheLogHelper.logCorsConfigCacheMiss(request);
		try {
			boolean ignoreException = true;
			AttributesPreservingRequest requestToUse = new AttributesPreservingRequest(request);
			return doWithHandlerMapping(requestToUse, ignoreException,
					(handlerMapping, executionChain) -> getCorsConfiguration(executionChain, requestToUse));
		}
		catch (Exception ex) {
			// HandlerMapping exceptions are ignored. More basic error like parsing the request path.
			throw new IllegalStateException(ex);
		}
	}

	@Nullable
	private static CorsConfiguration getCorsConfiguration(HandlerExecutionChain chain, HttpServletRequest request) {
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
	 * Container for a {@link MatchableHandlerMapping} and {@link CorsConfiguration}
	 * for a given request matched by dispatcher type and requestURI.
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

		@Nullable
		private final Exception failure;

		@Nullable
		private final IllegalStateException corsConfigFailure;

		private CachedResult(HttpServletRequest request,
				@Nullable MatchableHandlerMapping mapping, @Nullable CorsConfiguration config,
				@Nullable Exception failure, @Nullable IllegalStateException corsConfigFailure) {

			this.dispatcherType = request.getDispatcherType();
			this.requestURI = request.getRequestURI();
			this.handlerMapping = mapping;
			this.corsConfig = config;
			this.failure = failure;
			this.corsConfigFailure = corsConfigFailure;
		}

		public boolean matches(HttpServletRequest request) {
			return (this.dispatcherType.equals(request.getDispatcherType()) &&
					this.requestURI.equals(request.getRequestURI()));
		}

		@Nullable
		public MatchableHandlerMapping getHandlerMapping() throws Exception {
			if (this.failure != null) {
				throw this.failure;
			}
			return this.handlerMapping;
		}

		@Nullable
		public CorsConfiguration getCorsConfig() {
			if (this.corsConfigFailure != null) {
				throw this.corsConfigFailure;
			}
			return this.corsConfig;
		}

		@Override
		public String toString() {
			return "CachedResult for " + this.dispatcherType + " dispatch to '" + this.requestURI + "'";
		}


		/**
		 * Return a {@link CachedResult} that matches the given request.
		 */
		@Nullable
		public static CachedResult getResultFor(HttpServletRequest request) {
			CachedResult result = (CachedResult) request.getAttribute(CACHED_RESULT_ATTRIBUTE);
			return (result != null && result.matches(request) ? result : null);
		}
	}


	private static class CacheResultLogHelper {

		private final Map<String, AtomicInteger> counters =
				Map.of("MatchableHandlerMapping", new AtomicInteger(), "CorsConfiguration", new AtomicInteger());

		public void logHandlerMappingCacheMiss(HttpServletRequest request) {
			logCacheMiss("MatchableHandlerMapping", request);
		}

		public void logCorsConfigCacheMiss(HttpServletRequest request) {
			logCacheMiss("CorsConfiguration", request);
		}

		private void logCacheMiss(String label, HttpServletRequest request) {
			AtomicInteger counter = this.counters.get(label);
			Assert.notNull(counter, "Expected '" + label + "' counter.");

			String message = getLogMessage(label, request);

			if (logger.isWarnEnabled() && counter.getAndIncrement() == 0) {
				logger.warn(message + " This is logged once only at WARN level, and every time at TRACE.");
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("No CachedResult, performing " + label + " lookup instead.");
			}
		}

		private static String getLogMessage(String label, HttpServletRequest request) {
			return "Cache miss for " + request.getDispatcherType() + " dispatch to '" + request.getRequestURI() + "' " +
					"(previous " + request.getAttribute(CACHED_RESULT_ATTRIBUTE) + "). " +
					"Performing " + label + " lookup.";
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
		@Nullable
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
		@Nullable
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

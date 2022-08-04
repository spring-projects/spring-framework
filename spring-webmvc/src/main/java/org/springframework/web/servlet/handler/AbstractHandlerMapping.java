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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Abstract base class for {@link org.springframework.web.servlet.HandlerMapping}
 * implementations. Supports ordering, a default handler, handler interceptors,
 * including handler interceptors mapped by path patterns.
 *
 * <p>Note: This base class does <i>not</i> support exposure of the
 * {@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}. Support for this attribute
 * is up to concrete subclasses, typically based on request URL mappings.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 07.04.2003
 * @see #getHandlerInternal
 * @see #setDefaultHandler
 * @see #setInterceptors
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport
		implements HandlerMapping, Ordered, BeanNameAware {

	/** Dedicated "hidden" logger for request mappings. */
	protected final Log mappingsLogger =
			LogDelegateFactory.getHiddenLog(HandlerMapping.class.getName() + ".Mappings");


	@Nullable
	private Object defaultHandler;

	@Nullable
	private PathPatternParser patternParser;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final List<Object> interceptors = new ArrayList<>();

	private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<>();

	@Nullable
	private CorsConfigurationSource corsConfigurationSource;

	private CorsProcessor corsProcessor = new DefaultCorsProcessor();

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	@Nullable
	private String beanName;


	/**
	 * Set the default handler for this handler mapping.
	 * This handler will be returned if no specific mapping was found.
	 * <p>Default is {@code null}, indicating no default handler.
	 */
	public void setDefaultHandler(@Nullable Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	/**
	 * Return the default handler for this handler mapping,
	 * or {@code null} if none.
	 */
	@Nullable
	public Object getDefaultHandler() {
		return this.defaultHandler;
	}

	/**
	 * Enable use of pre-parsed {@link PathPattern}s as an alternative to
	 * String pattern matching with {@link AntPathMatcher}. The syntax is
	 * largely the same but the {@code PathPattern} syntax is more tailored for
	 * web applications, and its implementation is more efficient.
	 * <p>This property is mutually exclusive with the following others which
	 * are effectively ignored when this is set:
	 * <ul>
	 * <li>{@link #setAlwaysUseFullPath} -- {@code PathPatterns} always use the
	 * full path and ignore the servletPath/pathInfo which are decoded and
	 * partially normalized and therefore not comparable against the
	 * {@link HttpServletRequest#getRequestURI() requestURI}.
	 * <li>{@link #setRemoveSemicolonContent} -- {@code PathPatterns} always
	 * ignore semicolon content for path matching purposes, but path parameters
	 * remain available for use in controllers via {@code @MatrixVariable}.
	 * <li>{@link #setUrlDecode} -- {@code PathPatterns} match one decoded path
	 * segment at a time and never need the full decoded path which can cause
	 * issues due to decoded reserved characters.
	 * <li>{@link #setUrlPathHelper} -- the request path is pre-parsed globally
	 * by the {@link org.springframework.web.servlet.DispatcherServlet
	 * DispatcherServlet} or by
	 * {@link org.springframework.web.filter.ServletRequestPathFilter
	 * ServletRequestPathFilter} using {@link ServletRequestPathUtils} and saved
	 * in a request attribute for re-use.
	 * <li>{@link #setPathMatcher} -- patterns are parsed to {@code PathPatterns}
	 * and used instead of String matching with {@code PathMatcher}.
	 * </ul>
	 * <p>By default this is not set.
	 * @param patternParser the parser to use
	 * @since 5.3
	 */
	public void setPatternParser(PathPatternParser patternParser) {
		this.patternParser = patternParser;
	}

	/**
	 * Return the {@link #setPatternParser(PathPatternParser) configured}
	 * {@code PathPatternParser}, or {@code null}.
	 * @since 5.3
	 */
	@Nullable
	public PathPatternParser getPatternParser() {
		return this.patternParser;
	}

	/**
	 * Shortcut to same property on the configured {@code UrlPathHelper}.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setAlwaysUseFullPath(alwaysUseFullPath);
		}
	}

	/**
	 * Shortcut to same property on the underlying {@code UrlPathHelper}.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlDecode(urlDecode);
		}
	}

	/**
	 * Shortcut to same property on the underlying {@code UrlPathHelper}.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 * @see org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setRemoveSemicolonContent(removeSemicolonContent);
		}
	}

	/**
	 * Configure the UrlPathHelper to use for resolution of lookup paths.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlPathHelper(urlPathHelper);
		}
	}

	/**
	 * Return the {@link #setUrlPathHelper configured} {@code UrlPathHelper}.
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * Configure the PathMatcher to use.
	 * <p><strong>Note:</strong> This property is mutually exclusive with and
	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
	 * <p>By default this is {@link AntPathMatcher}.
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setPathMatcher(pathMatcher);
		}
	}

	/**
	 * Return the {@link #setPathMatcher configured} {@code PathMatcher}.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * Set the interceptors to apply for all handlers mapped by this handler mapping.
	 * <p>Supported interceptor types are {@link HandlerInterceptor},
	 * {@link WebRequestInterceptor}, and {@link MappedInterceptor}.
	 * Mapped interceptors apply only to request URLs that match its path patterns.
	 * Mapped interceptor beans are also detected by type during initialization.
	 * @param interceptors array of handler interceptors
	 * @see #adaptInterceptor
	 * @see org.springframework.web.servlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 * @see MappedInterceptor
	 */
	public void setInterceptors(Object... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	/**
	 * Set "global" CORS configuration mappings. The first matching URL pattern
	 * determines the {@code CorsConfiguration} to use which is then further
	 * {@link CorsConfiguration#combine(CorsConfiguration) combined} with the
	 * {@code CorsConfiguration} for the selected handler.
	 * <p>This is mutually exclusive with
	 * {@link #setCorsConfigurationSource(CorsConfigurationSource)}.
	 * @since 4.2
	 * @see #setCorsProcessor(CorsProcessor)
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		if (CollectionUtils.isEmpty(corsConfigurations)) {
			this.corsConfigurationSource = null;
			return;
		}
		UrlBasedCorsConfigurationSource source;
		if (getPatternParser() != null) {
			source = new UrlBasedCorsConfigurationSource(getPatternParser());
			source.setCorsConfigurations(corsConfigurations);
		}
		else {
			source = new UrlBasedCorsConfigurationSource();
			source.setCorsConfigurations(corsConfigurations);
			source.setPathMatcher(this.pathMatcher);
			source.setUrlPathHelper(this.urlPathHelper);
		}
		setCorsConfigurationSource(source);
	}

	/**
	 * Set a {@code CorsConfigurationSource} for "global" CORS config. The
	 * {@code CorsConfiguration} determined by the source is
	 * {@link CorsConfiguration#combine(CorsConfiguration) combined} with the
	 * {@code CorsConfiguration} for the selected handler.
	 * <p>This is mutually exclusive with {@link #setCorsConfigurations(Map)}.
	 * @since 5.1
	 * @see #setCorsProcessor(CorsProcessor)
	 */
	public void setCorsConfigurationSource(CorsConfigurationSource source) {
		Assert.notNull(source, "CorsConfigurationSource must not be null");
		this.corsConfigurationSource = source;
		if (source instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) source).setAllowInitLookupPath(false);
		}
	}

	/**
	 * Return the {@link #setCorsConfigurationSource(CorsConfigurationSource)
	 * configured} {@code CorsConfigurationSource}, if any.
	 * @since 5.3
	 */
	@Nullable
	public CorsConfigurationSource getCorsConfigurationSource() {
		return this.corsConfigurationSource;
	}

	/**
	 * Configure a custom {@link CorsProcessor} to use to apply the matched
	 * {@link CorsConfiguration} for a request.
	 * <p>By default {@link DefaultCorsProcessor} is used.
	 * @since 4.2
	 */
	public void setCorsProcessor(CorsProcessor corsProcessor) {
		Assert.notNull(corsProcessor, "CorsProcessor must not be null");
		this.corsProcessor = corsProcessor;
	}

	/**
	 * Return the configured {@link CorsProcessor}.
	 */
	public CorsProcessor getCorsProcessor() {
		return this.corsProcessor;
	}

	/**
	 * Specify the order value for this HandlerMapping bean.
	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	protected String formatMappingName() {
		return this.beanName != null ? "'" + this.beanName + "'" : getClass().getName();
	}


	/**
	 * Initializes the interceptors.
	 * @see #extendInterceptors(java.util.List)
	 * @see #initInterceptors()
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		extendInterceptors(this.interceptors);
		detectMappedInterceptors(this.adaptedInterceptors);
		initInterceptors();
	}

	/**
	 * Extension hook that subclasses can override to register additional interceptors,
	 * given the configured interceptors (see {@link #setInterceptors}).
	 * <p>Will be invoked before {@link #initInterceptors()} adapts the specified
	 * interceptors into {@link HandlerInterceptor} instances.
	 * <p>The default implementation is empty.
	 * @param interceptors the configured interceptor List (never {@code null}), allowing
	 * to add further interceptors before as well as after the existing interceptors
	 */
	protected void extendInterceptors(List<Object> interceptors) {
	}

	/**
	 * Detect beans of type {@link MappedInterceptor} and add them to the list
	 * of mapped interceptors.
	 * <p>This is called in addition to any {@link MappedInterceptor}s that may
	 * have been provided via {@link #setInterceptors}, by default adding all
	 * beans of type {@link MappedInterceptor} from the current context and its
	 * ancestors. Subclasses can override and refine this policy.
	 * @param mappedInterceptors an empty list to add to
	 */
	protected void detectMappedInterceptors(List<HandlerInterceptor> mappedInterceptors) {
		mappedInterceptors.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(
				obtainApplicationContext(), MappedInterceptor.class, true, false).values());
	}

	/**
	 * Initialize the specified interceptors adapting
	 * {@link WebRequestInterceptor}s to {@link HandlerInterceptor}.
	 * @see #setInterceptors
	 * @see #adaptInterceptor
	 */
	protected void initInterceptors() {
		if (!this.interceptors.isEmpty()) {
			for (int i = 0; i < this.interceptors.size(); i++) {
				Object interceptor = this.interceptors.get(i);
				if (interceptor == null) {
					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
				}
				this.adaptedInterceptors.add(adaptInterceptor(interceptor));
			}
		}
	}

	/**
	 * Adapt the given interceptor object to {@link HandlerInterceptor}.
	 * <p>By default, the supported interceptor types are
	 * {@link HandlerInterceptor} and {@link WebRequestInterceptor}. Each given
	 * {@link WebRequestInterceptor} is wrapped with
	 * {@link WebRequestHandlerInterceptorAdapter}.
	 * @param interceptor the interceptor
	 * @return the interceptor downcast or adapted to HandlerInterceptor
	 * @see org.springframework.web.servlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 * @see WebRequestHandlerInterceptorAdapter
	 */
	protected HandlerInterceptor adaptInterceptor(Object interceptor) {
		if (interceptor instanceof HandlerInterceptor) {
			return (HandlerInterceptor) interceptor;
		}
		else if (interceptor instanceof WebRequestInterceptor) {
			return new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) interceptor);
		}
		else {
			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
		}
	}

	/**
	 * Return the adapted interceptors as {@link HandlerInterceptor} array.
	 * @return the array of {@link HandlerInterceptor HandlerInterceptor}s,
	 * or {@code null} if none
	 */
	@Nullable
	protected final HandlerInterceptor[] getAdaptedInterceptors() {
		return (!this.adaptedInterceptors.isEmpty() ?
				this.adaptedInterceptors.toArray(new HandlerInterceptor[0]) : null);
	}

	/**
	 * Return all configured {@link MappedInterceptor}s as an array.
	 * @return the array of {@link MappedInterceptor}s, or {@code null} if none
	 */
	@Nullable
	protected final MappedInterceptor[] getMappedInterceptors() {
		List<MappedInterceptor> mappedInterceptors = new ArrayList<>(this.adaptedInterceptors.size());
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				mappedInterceptors.add((MappedInterceptor) interceptor);
			}
		}
		return (!mappedInterceptors.isEmpty() ? mappedInterceptors.toArray(new MappedInterceptor[0]) : null);
	}


	/**
	 * Return "true" if this {@code HandlerMapping} has been
	 * {@link #setPatternParser enabled} to use parsed {@code PathPattern}s.
	 */
	@Override
	public boolean usesPathPatterns() {
		return getPatternParser() != null;
	}

	/**
	 * Look up a handler for the given request, falling back to the default
	 * handler if no specific one is found.
	 * @param request current HTTP request
	 * @return the corresponding handler instance, or the default handler
	 * @see #getHandlerInternal
	 */
	@Override
	@Nullable
	public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		Object handler = getHandlerInternal(request);
		if (handler == null) {
			handler = getDefaultHandler();
		}
		if (handler == null) {
			return null;
		}
		// Bean name or resolved handler?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = obtainApplicationContext().getBean(handlerName);
		}

		// Ensure presence of cached lookupPath for interceptors and others
		if (!ServletRequestPathUtils.hasCachedPath(request)) {
			initLookupPath(request);
		}

		HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);

		if (logger.isTraceEnabled()) {
			logger.trace("Mapped to " + handler);
		}
		else if (logger.isDebugEnabled() && !DispatcherType.ASYNC.equals(request.getDispatcherType())) {
			logger.debug("Mapped to " + executionChain.getHandler());
		}

		if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
			CorsConfiguration config = getCorsConfiguration(handler, request);
			if (getCorsConfigurationSource() != null) {
				CorsConfiguration globalConfig = getCorsConfigurationSource().getCorsConfiguration(request);
				config = (globalConfig != null ? globalConfig.combine(config) : config);
			}
			if (config != null) {
				config.validateAllowCredentials();
			}
			executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
		}

		return executionChain;
	}

	/**
	 * Look up a handler for the given request, returning {@code null} if no
	 * specific one is found. This method is called by {@link #getHandler};
	 * a {@code null} return value will lead to the default handler, if one is set.
	 * <p>On CORS pre-flight requests this method should return a match not for
	 * the pre-flight request but for the expected actual request based on the URL
	 * path, the HTTP methods from the "Access-Control-Request-Method" header, and
	 * the headers from the "Access-Control-Request-Headers" header thus allowing
	 * the CORS configuration to be obtained via {@link #getCorsConfiguration(Object, HttpServletRequest)},
	 * <p>Note: This method may also return a pre-built {@link HandlerExecutionChain},
	 * combining a handler object with dynamically determined interceptors.
	 * Statically specified interceptors will get merged into such an existing chain.
	 * @param request current HTTP request
	 * @return the corresponding handler instance, or {@code null} if none found
	 * @throws Exception if there is an internal error
	 */
	@Nullable
	protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;

	/**
	 * Initialize the path to use for request mapping.
	 * <p>When parsed patterns are {@link #usesPathPatterns() enabled} a parsed
	 * {@code RequestPath} is expected to have been
	 * {@link ServletRequestPathUtils#parseAndCache(HttpServletRequest) parsed}
	 * externally by the {@link org.springframework.web.servlet.DispatcherServlet}
	 * or {@link org.springframework.web.filter.ServletRequestPathFilter}.
	 * <p>Otherwise for String pattern matching via {@code PathMatcher} the
	 * path is {@link UrlPathHelper#resolveAndCacheLookupPath resolved} by this
	 * method.
	 * @since 5.3
	 */
	protected String initLookupPath(HttpServletRequest request) {
		if (usesPathPatterns()) {
			request.removeAttribute(UrlPathHelper.PATH_ATTRIBUTE);
			RequestPath requestPath = ServletRequestPathUtils.getParsedRequestPath(request);
			String lookupPath = requestPath.pathWithinApplication().value();
			return UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);
		}
		else {
			return getUrlPathHelper().resolveAndCacheLookupPath(request);
		}
	}

	/**
	 * Build a {@link HandlerExecutionChain} for the given handler, including
	 * applicable interceptors.
	 * <p>The default implementation builds a standard {@link HandlerExecutionChain}
	 * with the given handler, the common interceptors of the handler mapping, and any
	 * {@link MappedInterceptor MappedInterceptors} matching to the current request URL. Interceptors
	 * are added in the order they were registered. Subclasses may override this
	 * in order to extend/rearrange the list of interceptors.
	 * <p><b>NOTE:</b> The passed-in handler object may be a raw handler or a
	 * pre-built {@link HandlerExecutionChain}. This method should handle those
	 * two cases explicitly, either building a new {@link HandlerExecutionChain}
	 * or extending the existing chain.
	 * <p>For simply adding an interceptor in a custom subclass, consider calling
	 * {@code super.getHandlerExecutionChain(handler, request)} and invoking
	 * {@link HandlerExecutionChain#addInterceptor} on the returned chain object.
	 * @param handler the resolved handler instance (never {@code null})
	 * @param request current HTTP request
	 * @return the HandlerExecutionChain (never {@code null})
	 * @see #getAdaptedInterceptors()
	 */
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
		HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
				(HandlerExecutionChain) handler : new HandlerExecutionChain(handler));

		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
				if (mappedInterceptor.matches(request)) {
					chain.addInterceptor(mappedInterceptor.getInterceptor());
				}
			}
			else {
				chain.addInterceptor(interceptor);
			}
		}
		return chain;
	}

	/**
	 * Return {@code true} if there is a {@link CorsConfigurationSource} for this handler.
	 * @since 5.2
	 */
	protected boolean hasCorsConfigurationSource(Object handler) {
		if (handler instanceof HandlerExecutionChain) {
			handler = ((HandlerExecutionChain) handler).getHandler();
		}
		return (handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null);
	}

	/**
	 * Retrieve the CORS configuration for the given handler.
	 * @param handler the handler to check (never {@code null}).
	 * @param request the current request.
	 * @return the CORS configuration for the handler, or {@code null} if none
	 * @since 4.2
	 */
	@Nullable
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		Object resolvedHandler = handler;
		if (handler instanceof HandlerExecutionChain) {
			resolvedHandler = ((HandlerExecutionChain) handler).getHandler();
		}
		if (resolvedHandler instanceof CorsConfigurationSource) {
			return ((CorsConfigurationSource) resolvedHandler).getCorsConfiguration(request);
		}
		return null;
	}

	/**
	 * Update the HandlerExecutionChain for CORS-related handling.
	 * <p>For pre-flight requests, the default implementation replaces the selected
	 * handler with a simple HttpRequestHandler that invokes the configured
	 * {@link #setCorsProcessor}.
	 * <p>For actual requests, the default implementation inserts a
	 * HandlerInterceptor that makes CORS-related checks and adds CORS headers.
	 * @param request the current request
	 * @param chain the handler chain
	 * @param config the applicable CORS configuration (possibly {@code null})
	 * @since 4.2
	 */
	protected HandlerExecutionChain getCorsHandlerExecutionChain(HttpServletRequest request,
			HandlerExecutionChain chain, @Nullable CorsConfiguration config) {

		if (CorsUtils.isPreFlightRequest(request)) {
			HandlerInterceptor[] interceptors = chain.getInterceptors();
			return new HandlerExecutionChain(new PreFlightHandler(config), interceptors);
		}
		else {
			chain.addInterceptor(0, new CorsInterceptor(config));
			return chain;
		}
	}


	private class PreFlightHandler implements HttpRequestHandler, CorsConfigurationSource {

		@Nullable
		private final CorsConfiguration config;

		public PreFlightHandler(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
			corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}


	private class CorsInterceptor implements HandlerInterceptor, CorsConfigurationSource {

		@Nullable
		private final CorsConfiguration config;

		public CorsInterceptor(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
				throws Exception {

			// Consistent with CorsFilter, ignore ASYNC dispatches
			WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
			if (asyncManager.hasConcurrentResult()) {
				return true;
			}

			return corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}

}

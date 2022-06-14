/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.function.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.SpringProperties;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * {@code HandlerMapping} implementation that supports {@link RouterFunction RouterFunctions}.
 *
 * <p>If no {@link RouterFunction} is provided at
 * {@linkplain #RouterFunctionMapping(RouterFunction) construction time}, this mapping
 * will detect all router functions in the application context, and consult them in
 * {@linkplain org.springframework.core.annotation.Order order}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.2
 */
public class RouterFunctionMapping extends AbstractHandlerMapping implements InitializingBean {

	/**
	 * Boolean flag controlled by a {@code spring.xml.ignore} system property that instructs Spring to
	 * ignore XML, i.e. to not initialize the XML-related infrastructure.
	 * <p>The default is "false".
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");


	@Nullable
	private RouterFunction<?> routerFunction;

	private List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

	private boolean detectHandlerFunctionsInAncestorContexts = false;


	/**
	 * Create an empty {@code RouterFunctionMapping}.
	 * <p>If this constructor is used, this mapping will detect all
	 * {@link RouterFunction} instances available in the application context.
	 */
	public RouterFunctionMapping() {
	}

	/**
	 * Create a {@code RouterFunctionMapping} with the given {@link RouterFunction}.
	 * <p>If this constructor is used, no application context detection will occur.
	 * @param routerFunction the router function to use for mapping
	 */
	public RouterFunctionMapping(RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}


	/**
	 * Set the router function to map to.
	 * <p>If this property is used, no application context detection will occur.
	 */
	public void setRouterFunction(@Nullable RouterFunction<?> routerFunction) {
		this.routerFunction = routerFunction;
	}

	/**
	 * Return the configured {@link RouterFunction}.
	 * <p><strong>Note:</strong> When router functions are detected from the
	 * ApplicationContext, this method may return {@code null} if invoked
	 * prior to {@link #afterPropertiesSet()}.
	 * @return the router function or {@code null}
	 */
	@Nullable
	public RouterFunction<?> getRouterFunction() {
		return this.routerFunction;
	}

	/**
	 * Set the message body converters to use.
	 * <p>These converters are used to convert from and to HTTP requests and responses.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * Set whether to detect handler functions in ancestor ApplicationContexts.
	 * <p>Default is "false": Only handler functions in the current ApplicationContext
	 * will be detected, i.e. only in the context that this HandlerMapping itself
	 * is defined in (typically the current DispatcherServlet's context).
	 * <p>Switch this flag on to detect handler beans in ancestor contexts
	 * (typically the Spring root WebApplicationContext) as well.
	 */
	public void setDetectHandlerFunctionsInAncestorContexts(boolean detectHandlerFunctionsInAncestorContexts) {
		this.detectHandlerFunctionsInAncestorContexts = detectHandlerFunctionsInAncestorContexts;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.routerFunction == null) {
			initRouterFunctions();
		}
		if (CollectionUtils.isEmpty(this.messageConverters)) {
			initMessageConverters();
		}
		if (this.routerFunction != null) {
			PathPatternParser patternParser = getPatternParser();
			if (patternParser == null) {
				patternParser = new PathPatternParser();
				setPatternParser(patternParser);
			}
			RouterFunctions.changeParser(this.routerFunction, patternParser);
		}
	}

	/**
	 * Detect a all {@linkplain RouterFunction router functions} in the
	 * current application context.
	 */
	private void initRouterFunctions() {
		List<RouterFunction<?>> routerFunctions = obtainApplicationContext()
				.getBeanProvider(RouterFunction.class)
				.orderedStream()
				.map(router -> (RouterFunction<?>) router)
				.collect(Collectors.toList());

		ApplicationContext parentContext = obtainApplicationContext().getParent();
		if (parentContext != null && !this.detectHandlerFunctionsInAncestorContexts) {
			parentContext.getBeanProvider(RouterFunction.class).stream().forEach(routerFunctions::remove);
		}

		this.routerFunction = routerFunctions.stream().reduce(RouterFunction::andOther).orElse(null);
		logRouterFunctions(routerFunctions);
	}

	private void logRouterFunctions(List<RouterFunction<?>> routerFunctions) {
		if (mappingsLogger.isDebugEnabled()) {
			routerFunctions.forEach(function -> mappingsLogger.debug("Mapped " + function));
		}
		else if (logger.isDebugEnabled()) {
			int total = routerFunctions.size();
			String message = total + " RouterFunction(s) in " + formatMappingName();
			if (logger.isTraceEnabled()) {
				if (total > 0) {
					routerFunctions.forEach(function -> logger.trace("Mapped " + function));
				}
				else {
					logger.trace(message);
				}
			}
			else if (total > 0) {
				logger.debug(message);
			}
		}
	}

	/**
	 * Initializes a default set of {@linkplain HttpMessageConverter message converters}.
	 */
	private void initMessageConverters() {
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>(4);
		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(new StringHttpMessageConverter());

		if (!shouldIgnoreXml) {
			try {
				messageConverters.add(new SourceHttpMessageConverter<>());
			}
			catch (Error err) {
				// Ignore when no TransformerFactory implementation is available
			}
		}
		messageConverters.add(new AllEncompassingFormHttpMessageConverter());

		this.messageConverters = messageConverters;
	}


	@Override
	@Nullable
	protected Object getHandlerInternal(HttpServletRequest servletRequest) throws Exception {
		if (this.routerFunction != null) {
			ServerRequest request = ServerRequest.create(servletRequest, this.messageConverters);
			HandlerFunction<?> handlerFunction = this.routerFunction.route(request).orElse(null);
			setAttributes(servletRequest, request, handlerFunction);
			return handlerFunction;
		}
		else {
			return null;
		}
	}

	private void setAttributes(HttpServletRequest servletRequest, ServerRequest request,
			@Nullable HandlerFunction<?> handlerFunction) {

		PathPattern matchingPattern =
				(PathPattern) servletRequest.getAttribute(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE);
		if (matchingPattern != null) {
			servletRequest.removeAttribute(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE);
			servletRequest.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, matchingPattern.getPatternString());
		}
		servletRequest.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, handlerFunction);
		servletRequest.setAttribute(RouterFunctions.REQUEST_ATTRIBUTE, request);
	}

}

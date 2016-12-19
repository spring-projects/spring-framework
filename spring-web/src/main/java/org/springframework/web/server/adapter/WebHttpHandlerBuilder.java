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
package org.springframework.web.server.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import org.springframework.web.server.handler.FilteringWebHandler;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Builder for an {@link HttpHandler} that adapts to a target {@link WebHandler}
 * along with a chain of {@link WebFilter}s and a set of
 * {@link WebExceptionHandler}s.
 *
 * <p>Example usage:
 * <pre>
 * WebFilter filter = ... ;
 * WebHandler webHandler = ... ;
 * WebExceptionHandler exceptionHandler = ...;
 *
 * HttpHandler httpHandler = WebHttpHandlerBuilder.webHandler(webHandler)
 *         .filters(filter)
 *         .exceptionHandlers(exceptionHandler)
 *         .build();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebHttpHandlerBuilder {

	/** Well-known name for the target WebHandler in the bean factory. */
	public static final String WEB_HANDLER_BEAN_NAME = "webHandler";

	/** Well-known name for the WebSessionManager in the bean factory. */
	public static final String WEB_SESSION_MANAGER_BEAN_NAME = "webSessionManager";


	private final WebHandler targetHandler;

	private final List<WebFilter> filters = new ArrayList<>();

	private final List<WebExceptionHandler> exceptionHandlers = new ArrayList<>();

	private WebSessionManager sessionManager;


	/**
	 * Private constructor.
	 * See factory method {@link #webHandler(WebHandler)}.
	 */
	private WebHttpHandlerBuilder(WebHandler targetHandler) {
		Assert.notNull(targetHandler, "WebHandler must not be null");
		this.targetHandler = targetHandler;
	}


	/**
	 * Factory method to create a new builder instance.
	 * @param webHandler the target handler for the request
	 * @return the prepared builder
	 */
	public static WebHttpHandlerBuilder webHandler(WebHandler webHandler) {
		return new WebHttpHandlerBuilder(webHandler);
	}

	/**
	 * Factory method to create a new builder instance by detecting beans in an
	 * {@link ApplicationContext}. The following are detected:
	 * <ul>
	 *	<li>{@link WebHandler} [1] -- looked up by the name
	 *	{@link #WEB_HANDLER_BEAN_NAME}.
	 *	<li>{@link WebFilter} [0..N] -- detected by type and ordered,
	 *	see {@link AnnotationAwareOrderComparator}.
	 *	<li>{@link WebExceptionHandler} [0..N] -- detected by type and
	 *	ordered.
	 *	<li>{@link WebSessionManager} [0..1] -- looked up by the name
	 *	{@link #WEB_SESSION_MANAGER_BEAN_NAME}.
	 * </ul>
	 * @param context the application context to use for the lookup
	 * @return the prepared builder
	 */
	public static WebHttpHandlerBuilder applicationContext(ApplicationContext context) {

		// Target WebHandler

		WebHttpHandlerBuilder builder = new WebHttpHandlerBuilder(
				context.getBean(WEB_HANDLER_BEAN_NAME, WebHandler.class));

		// WebFilter...

		Collection<WebFilter> filters = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, WebFilter.class, true, false).values();

		WebFilter[] sortedFilters = filters.toArray(new WebFilter[filters.size()]);
		AnnotationAwareOrderComparator.sort(sortedFilters);
		builder.filters(sortedFilters);

		// WebExceptionHandler...

		Collection<WebExceptionHandler> handlers = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, WebExceptionHandler.class, true, false).values();

		WebExceptionHandler[] sortedHandlers = handlers.toArray(new WebExceptionHandler[handlers.size()]);
		AnnotationAwareOrderComparator.sort(sortedHandlers);
		builder.exceptionHandlers(sortedHandlers);

		// WebSessionManager

		try {
			builder.sessionManager(
					context.getBean(WEB_SESSION_MANAGER_BEAN_NAME, WebSessionManager.class));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Fall back on default
		}

		return builder;
	}


	/**
	 * Add the given filters to use for processing requests.
	 * @param filters the filters to add
	 */
	public WebHttpHandlerBuilder filters(WebFilter... filters) {
		if (!ObjectUtils.isEmpty(filters)) {
			this.filters.addAll(Arrays.asList(filters));
		}
		return this;
	}

	/**
	 * Add the given exception handler to apply at the end of request processing.
	 * @param exceptionHandlers the exception handlers
	 */
	public WebHttpHandlerBuilder exceptionHandlers(WebExceptionHandler... exceptionHandlers) {
		if (!ObjectUtils.isEmpty(exceptionHandlers)) {
			this.exceptionHandlers.addAll(Arrays.asList(exceptionHandlers));
		}
		return this;
	}

	/**
	 * Configure the {@link WebSessionManager} to set on the
	 * {@link ServerWebExchange WebServerExchange}.
	 * <p>By default {@link DefaultWebSessionManager} is used.
	 * @param sessionManager the session manager
	 * @see HttpWebHandlerAdapter#setSessionManager(WebSessionManager)
	 */
	public WebHttpHandlerBuilder sessionManager(WebSessionManager sessionManager) {
		this.sessionManager = sessionManager;
		return this;
	}

	/**
	 * Build the {@link HttpHandler}.
	 */
	public HttpHandler build() {
		WebHandler webHandler = this.targetHandler;
		if (!this.filters.isEmpty()) {
			WebFilter[] array = new WebFilter[this.filters.size()];
			webHandler = new FilteringWebHandler(webHandler, this.filters.toArray(array));
		}
		WebExceptionHandler[] array = new WebExceptionHandler[this.exceptionHandlers.size()];
		webHandler = new ExceptionHandlingWebHandler(webHandler,  this.exceptionHandlers.toArray(array));
		HttpWebHandlerAdapter httpHandler = new HttpWebHandlerAdapter(webHandler);
		if (this.sessionManager != null) {
			httpHandler.setSessionManager(this.sessionManager);
		}
		return httpHandler;
	}

}

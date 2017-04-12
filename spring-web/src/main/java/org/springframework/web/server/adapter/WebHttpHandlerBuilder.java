/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.List;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
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
 * This builder has two purposes.
 *
 * <p>One is to assemble a processing chain that consists of a target
 * {@link WebHandler}, then decorated with a set of {@link WebFilter}'s, then
 * further decorated with a set of {@link WebExceptionHandler}'s.
 *
 * <p>The second purpose is to adapt the resulting processing chain to an
 * {@link HttpHandler} -- the lowest level reactive HTTP handling abstraction,
 * which can then be used with any of the supported runtimes. The adaptation
 * is done with the help of {@link HttpWebHandlerAdapter}.
 *
 * <p>The processing chain can be assembled manually via builder methods, or
 * detected from Spring configuration via
 * {@link #applicationContext(ApplicationContext)}, or a mix of both.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see HttpWebHandlerAdapter
 */
public class WebHttpHandlerBuilder {

	/** Well-known name for the target WebHandler in the bean factory. */
	public static final String WEB_HANDLER_BEAN_NAME = "webHandler";

	/** Well-known name for the WebSessionManager in the bean factory. */
	public static final String WEB_SESSION_MANAGER_BEAN_NAME = "webSessionManager";


	private final WebHandler webHandler;

	private final List<WebFilter> filters = new ArrayList<>();

	private final List<WebExceptionHandler> exceptionHandlers = new ArrayList<>();

	private WebSessionManager sessionManager;


	/**
	 * Private constructor.
	 */
	private WebHttpHandlerBuilder(WebHandler webHandler) {
		Assert.notNull(webHandler, "WebHandler must not be null");
		this.webHandler = webHandler;
	}


	/**
	 * Static factory method to create a new builder instance.
	 * @param webHandler the target handler for the request
	 * @return the prepared builder
	 */
	public static WebHttpHandlerBuilder webHandler(WebHandler webHandler) {
		return new WebHttpHandlerBuilder(webHandler);
	}

	/**
	 * Static factory method to create a new builder instance by detecting beans
	 * in an {@link ApplicationContext}. The following are detected:
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

		WebHttpHandlerBuilder builder = new WebHttpHandlerBuilder(
				context.getBean(WEB_HANDLER_BEAN_NAME, WebHandler.class));

		// Autowire lists for @Bean + @Order

		SortedBeanContainer container = new SortedBeanContainer();
		context.getAutowireCapableBeanFactory().autowireBean(container);
		builder.filters(container.getFilters());
		builder.exceptionHandlers(container.getExceptionHandlers());

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
	 * Add the given filters.
	 * @param filters the filters to add
	 */
	public WebHttpHandlerBuilder filters(List<? extends WebFilter> filters) {
		if (!ObjectUtils.isEmpty(filters)) {
			this.filters.addAll(filters);
		}
		return this;
	}

	/**
	 * Insert the given filter before other configured filters.
	 * @param filter the filters to insert
	 */
	public WebHttpHandlerBuilder prependFilter(WebFilter filter) {
		Assert.notNull(filter, "WebFilter is required");
		this.filters.add(0, filter);
		return this;
	}

	/**
	 * Add the given exception handlers.
	 * @param handlers the exception handlers
	 */
	public WebHttpHandlerBuilder exceptionHandlers(List<WebExceptionHandler> handlers) {
		if (!ObjectUtils.isEmpty(handlers)) {
			this.exceptionHandlers.addAll(handlers);
		}
		return this;
	}

	/**
	 * Insert the given exception handler before other configured handlers.
	 * @param handler the exception handler to insert
	 */
	public WebHttpHandlerBuilder prependExceptionHandler(WebExceptionHandler handler) {
		Assert.notNull(handler, "WebExceptionHandler is required");
		this.exceptionHandlers.add(0, handler);
		return this;
	}

	/**
	 * Configure the {@link WebSessionManager} to set on the
	 * {@link ServerWebExchange WebServerExchange}.
	 * <p>By default {@link DefaultWebSessionManager} is used.
	 * @param manager the session manager
	 * @see HttpWebHandlerAdapter#setSessionManager(WebSessionManager)
	 */
	public WebHttpHandlerBuilder sessionManager(WebSessionManager manager) {
		this.sessionManager = manager;
		return this;
	}


	/**
	 * Build the {@link HttpHandler}.
	 */
	public HttpHandler build() {

		WebHandler decorated;

		decorated = new FilteringWebHandler(this.webHandler, this.filters);
		decorated = new ExceptionHandlingWebHandler(decorated,  this.exceptionHandlers);

		HttpWebHandlerAdapter adapted = new HttpWebHandlerAdapter(decorated);
		if (this.sessionManager != null) {
			adapted.setSessionManager(this.sessionManager);
		}

		return adapted;
	}


	private static class SortedBeanContainer {

		private List<WebFilter> filters;

		private List<WebExceptionHandler> exceptionHandlers;


		@Autowired(required = false)
		public void setFilters(List<WebFilter> filters) {
			this.filters = filters;
		}

		public List<WebFilter> getFilters() {
			return this.filters;
		}

		@Autowired(required = false)
		public void setExceptionHandlers(List<WebExceptionHandler> exceptionHandlers) {
			this.exceptionHandlers = exceptionHandlers;
		}

		public List<WebExceptionHandler> getExceptionHandlers() {
			return this.exceptionHandlers;
		}
	}

}

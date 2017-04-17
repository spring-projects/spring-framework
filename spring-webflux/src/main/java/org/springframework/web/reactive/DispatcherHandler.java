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

package org.springframework.web.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Central dispatcher for HTTP request handlers/controllers. Dispatches to registered
 * handlers for processing a web request, providing convenient mapping facilities.
 *
 * <p>It can use any {@link HandlerMapping} implementation to control the routing of
 * requests to handler objects. HandlerMapping objects can be defined as beans in
 * the application context.
 *
 * <p>It can use any {@link HandlerAdapter}; this allows for using any handler interface.
 * HandlerAdapter objects can be added as beans in the application context.
 *
 * <p>It can use any {@link HandlerResultHandler}; this allows to process the result of
 * the request handling. HandlerResultHandler objects can be added as beans in the
 * application context.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 5.0
 */
public class DispatcherHandler implements WebHandler, ApplicationContextAware {

	@SuppressWarnings("ThrowableInstanceNeverThrown")
	private static final Exception HANDLER_NOT_FOUND_EXCEPTION =
			new ResponseStatusException(HttpStatus.NOT_FOUND, "No matching handler");


	private static final Log logger = LogFactory.getLog(DispatcherHandler.class);

	private List<HandlerMapping> handlerMappings;

	private List<HandlerAdapter> handlerAdapters;

	private List<HandlerResultHandler> resultHandlers;


	/**
	 * Create a new {@code DispatcherHandler} which needs to be configured with
	 * an {@link ApplicationContext} through {@link #setApplicationContext}.
	 */
	public DispatcherHandler() {
	}

	/**
	 * Create a new {@code DispatcherHandler} for the given {@link ApplicationContext}.
	 * @param applicationContext the application context to find the handler beans in
	 */
	public DispatcherHandler(ApplicationContext applicationContext) {
		initStrategies(applicationContext);
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		initStrategies(applicationContext);
	}

	protected void initStrategies(ApplicationContext context) {
		Map<String, HandlerMapping> mappingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerMapping.class, true, false);

		this.handlerMappings = new ArrayList<>(mappingBeans.values());
		AnnotationAwareOrderComparator.sort(this.handlerMappings);

		Map<String, HandlerAdapter> adapterBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerAdapter.class, true, false);

		this.handlerAdapters = new ArrayList<>(adapterBeans.values());
		AnnotationAwareOrderComparator.sort(this.handlerAdapters);

		Map<String, HandlerResultHandler> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerResultHandler.class, true, false);

		this.resultHandlers = new ArrayList<>(beans.values());
		AnnotationAwareOrderComparator.sort(this.resultHandlers);
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		if (logger.isDebugEnabled()) {
			ServerHttpRequest request = exchange.getRequest();
			logger.debug("Processing " + request.getMethod() + " request for [" + request.getURI() + "]");
		}
		return Flux.fromIterable(this.handlerMappings)
				.concatMap(mapping -> mapping.getHandler(exchange))
				.next()
				.switchIfEmpty(Mono.error(HANDLER_NOT_FOUND_EXCEPTION))
				.flatMap(handler -> invokeHandler(exchange, handler))
				.flatMap(result -> handleResult(exchange, result));
	}

	private Mono<HandlerResult> invokeHandler(ServerWebExchange exchange, Object handler) {
		for (HandlerAdapter handlerAdapter : this.handlerAdapters) {
			if (handlerAdapter.supports(handler)) {
				return handlerAdapter.handle(exchange, handler);
			}
		}
		return Mono.error(new IllegalStateException("No HandlerAdapter: " + handler));
	}

	private Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		return getResultHandler(result).handleResult(exchange, result)
				.onErrorResume(ex -> result.applyExceptionHandler(ex).flatMap(exceptionResult ->
						getResultHandler(exceptionResult).handleResult(exchange, exceptionResult)));
	}

	private HandlerResultHandler getResultHandler(HandlerResult handlerResult) {
		for (HandlerResultHandler resultHandler : this.resultHandlers) {
			if (resultHandler.supports(handlerResult)) {
				return resultHandler;
			}
		}
		throw new IllegalStateException("No HandlerResultHandler for " + handlerResult.getReturnValue());
	}


	/**
	 * Expose a dispatcher-based {@link WebHandler} for the given application context,
	 * typically for further configuration with filters and exception handlers through
	 * a {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder}.
	 * @param applicationContext the application context to find the handler beans in
	 * @see #DispatcherHandler(ApplicationContext)
	 * @see org.springframework.web.server.adapter.WebHttpHandlerBuilder#webHandler
	 */
	public static WebHandler toWebHandler(ApplicationContext applicationContext) {
		return new DispatcherHandler(applicationContext);
	}

	/**
	 * Expose a dispatcher-based {@link HttpHandler} for the given application context,
	 * typically for direct registration with an engine adapter such as
	 * {@link org.springframework.http.server.reactive.ServletHttpHandlerAdapter}.
	 *
	 * <p>Delegates to {@link WebHttpHandlerBuilder#applicationContext} that
	 * detects the target {@link DispatcherHandler} along with
	 * {@link org.springframework.web.server.WebFilter}s, and
	 * {@link org.springframework.web.server.WebExceptionHandler}s in the given
	 * ApplicationContext.
	 *
	 * @param context the application context to find the handler beans in
	 * @see #DispatcherHandler(ApplicationContext)
	 * @see HttpWebHandlerAdapter
	 * @see org.springframework.http.server.reactive.ServletHttpHandlerAdapter
	 * @see org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
	 * @see org.springframework.http.server.reactive.UndertowHttpHandlerAdapter
	 */
	public static HttpHandler toHttpHandler(ApplicationContext context) {
		return WebHttpHandlerBuilder.applicationContext(context).build();
	}

}

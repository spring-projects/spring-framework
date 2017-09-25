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

package org.springframework.web.reactive;

import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;

/**
 * Central dispatcher for HTTP request handlers/controllers. Dispatches to
 * registered handlers for processing a request, providing convenient mapping
 * facilities.
 *
 * <p>{@code DispatcherHandler} discovers the delegate components it needs from
 * Spring configuration. It detects the following in the application context:
 * <ul>
 * <li>{@link HandlerMapping} -- map requests to handler objects
 * <li>{@link HandlerAdapter} -- for using any handler interface
 * <li>{@link HandlerResultHandler} -- process handler return values
 * </ul>
 *
 * <p>{@code DispatcherHandler} s also designed to be a Spring bean itself and
 * implements {@link ApplicationContextAware} for access to the context it runs
 * in. If {@code DispatcherHandler} is declared with the bean name "webHandler"
 * it is discovered by {@link WebHttpHandlerBuilder#applicationContext} which
 * creates a processing chain together with {@code WebFilter},
 * {@code WebExceptionHandler} and others.
 *
 * <p>A {@code DispatcherHandler} bean declaration is included in
 * {@link org.springframework.web.reactive.config.EnableWebFlux @EnableWebFlux}
 * configuration.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 5.0
 * @see WebHttpHandlerBuilder#applicationContext(ApplicationContext)
 */
public class DispatcherHandler implements WebHandler, ApplicationContextAware {

	@SuppressWarnings("ThrowableInstanceNeverThrown")
	private static final Exception HANDLER_NOT_FOUND_EXCEPTION =
			new ResponseStatusException(HttpStatus.NOT_FOUND, "No matching handler");


	private static final Log logger = LogFactory.getLog(DispatcherHandler.class);

	@Nullable
	private List<HandlerMapping> handlerMappings;

	@Nullable
	private List<HandlerAdapter> handlerAdapters;

	@Nullable
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


	/**
	 * Return all {@link HandlerMapping} beans detected by type in the
	 * {@link #setApplicationContext injected context} and also
	 * {@link AnnotationAwareOrderComparator#sort(List) sorted}.
	 * <p><strong>Note:</strong> This method may return {@code null} if invoked
	 * prior to {@link #setApplicationContext(ApplicationContext)}.
	 * @return immutable list with the configured mappings or {@code null}
	 */
	@Nullable
	public final List<HandlerMapping> getHandlerMappings() {
		return this.handlerMappings;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		initStrategies(applicationContext);
	}


	protected void initStrategies(ApplicationContext context) {
		Map<String, HandlerMapping> mappingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerMapping.class, true, false);

		ArrayList<HandlerMapping> mappings = new ArrayList<>(mappingBeans.values());
		AnnotationAwareOrderComparator.sort(mappings);
		this.handlerMappings = Collections.unmodifiableList(mappings);

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
			logger.debug("Processing " + request.getMethodValue() + " request for [" + request.getURI() + "]");
		}
		if (this.handlerMappings == null) {
			return Mono.error(HANDLER_NOT_FOUND_EXCEPTION);
		}
		return Flux.fromIterable(this.handlerMappings)
				.concatMap(mapping -> mapping.getHandler(exchange))
				.next()
				.switchIfEmpty(Mono.error(HANDLER_NOT_FOUND_EXCEPTION))
				.flatMap(handler -> invokeHandler(exchange, handler))
				.flatMap(result -> handleResult(exchange, result));
	}

	private Mono<HandlerResult> invokeHandler(ServerWebExchange exchange, Object handler) {
		if (this.handlerAdapters != null) {
			for (HandlerAdapter handlerAdapter : this.handlerAdapters) {
				if (handlerAdapter.supports(handler)) {
					return handlerAdapter.handle(exchange, handler);
				}
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
		if (this.resultHandlers != null) {
			for (HandlerResultHandler resultHandler : this.resultHandlers) {
				if (resultHandler.supports(handlerResult)) {
					return resultHandler;
				}
			}
		}
		throw new IllegalStateException("No HandlerResultHandler for " + handlerResult.getReturnValue());
	}

}

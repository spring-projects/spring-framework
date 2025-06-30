/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.cors.reactive.PreFlightRequestHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

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
 * <p>{@code DispatcherHandler} is also designed to be a Spring bean itself and
 * implements {@link ApplicationContextAware} for access to the context it runs
 * in. If {@code DispatcherHandler} is declared as a bean with the name
 * "webHandler", it is discovered by
 * {@link WebHttpHandlerBuilder#applicationContext(ApplicationContext)} which
 * puts together a processing chain together with {@code WebFilter},
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
public class DispatcherHandler implements WebHandler, PreFlightRequestHandler, ApplicationContextAware {

	private @Nullable List<HandlerMapping> handlerMappings;

	private @Nullable List<HandlerAdapter> handlerAdapters;

	private @Nullable List<HandlerResultHandler> resultHandlers;


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
	public final @Nullable List<HandlerMapping> getHandlerMappings() {
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
		if (this.handlerMappings == null) {
			return createNotFoundError();
		}
		if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
			return handlePreFlight(exchange);
		}
		return Flux.fromIterable(this.handlerMappings)
				.concatMap(mapping -> mapping.getHandler(exchange))
				.next()
				.switchIfEmpty(createNotFoundError())
				.onErrorResume(ex -> handleResultMono(exchange, Mono.error(ex)))
				.flatMap(handler -> handleRequestWith(exchange, handler));
	}

	private <R> Mono<R> createNotFoundError() {
		return Mono.defer(() -> {
			Exception ex = new ResponseStatusException(HttpStatus.NOT_FOUND);
			return Mono.error(ex);
		});
	}

	private Mono<Void> handleResultMono(ServerWebExchange exchange, Mono<HandlerResult> resultMono) {
		if (this.handlerAdapters != null) {
			for (HandlerAdapter adapter : this.handlerAdapters) {
				if (adapter instanceof DispatchExceptionHandler exceptionHandler) {
					resultMono = resultMono.onErrorResume(ex2 -> exceptionHandler.handleError(exchange, ex2));
				}
			}
		}
		return resultMono.flatMap(result -> {
			Mono<Void> voidMono = handleResult(exchange, result, "Handler " + result.getHandler());
			DispatchExceptionHandler exceptionHandler = result.getExceptionHandler();
			if (exceptionHandler != null) {
				voidMono = voidMono.onErrorResume(ex ->
						exceptionHandler.handleError(exchange, ex).flatMap(result2 ->
								handleResult(exchange, result2, "Exception handler " +
										result2.getHandler() + ", error=\"" + ex.getMessage() + "\"")));
			}
			return voidMono;
		});
	}

	private Mono<Void> handleResult(
			ServerWebExchange exchange, HandlerResult handlerResult, String description) {

		if (this.resultHandlers != null) {
			for (HandlerResultHandler resultHandler : this.resultHandlers) {
				if (resultHandler.supports(handlerResult)) {
					description += " [DispatcherHandler]";
					return resultHandler.handleResult(exchange, handlerResult).checkpoint(description);
				}
			}
		}
		return Mono.error(new IllegalStateException(
				"No HandlerResultHandler for " + handlerResult.getReturnValue()));
	}

	private Mono<Void> handleRequestWith(ServerWebExchange exchange, Object handler) {
		if (ObjectUtils.nullSafeEquals(exchange.getResponse().getStatusCode(), HttpStatus.FORBIDDEN)) {
			return Mono.empty();  // CORS rejection
		}
		if (this.handlerAdapters != null) {
			for (HandlerAdapter adapter : this.handlerAdapters) {
				if (adapter.supports(handler)) {
					Mono<HandlerResult> resultMono = adapter.handle(exchange, handler);
					return handleResultMono(exchange, resultMono);
				}
			}
		}
		return Mono.error(new IllegalStateException("No HandlerAdapter: " + handler));
	}

	@Override
	public Mono<Void> handlePreFlight(ServerWebExchange exchange) {
		return Flux.fromIterable(this.handlerMappings != null ? this.handlerMappings : Collections.emptyList())
				.concatMap(mapping -> mapping.getHandler(exchange))
				.switchIfEmpty(Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN)))
				.next()
				.then();
	}

}

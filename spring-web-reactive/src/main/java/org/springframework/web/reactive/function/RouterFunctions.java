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

package org.springframework.web.reactive.function;

import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;

/**
 * <strong>Central entry point to Spring's functional web framework.</strong>
 * Exposes routing functionality, such as to
 * {@linkplain #route(RequestPredicate, HandlerFunction) create} a {@code RouterFunction} given a
 * {@code RequestPredicate} and {@code HandlerFunction}, and to do further
 * {@linkplain #subroute(RequestPredicate, RouterFunction) subrouting} on an existing routing
 * function.
 *
 * <p>Additionally, this class can {@linkplain #toHttpHandler(RouterFunction) transform} a
 * {@code RouterFunction} into an {@code HttpHandler}, which can be run in Servlet 3.1+,
 * Reactor, RxNetty, or Undertow.
 * And it can {@linkplain #toHandlerMapping(RouterFunction, HandlerStrategies) transform} a
 * {@code RouterFunction} into an {@code HandlerMapping}, which can be run in a
 * {@code DispatcherHandler}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class RouterFunctions {

	/**
	 * Name of the {@link ServerWebExchange} attribute that contains the {@link ServerRequest}.
	 */
	public static final String REQUEST_ATTRIBUTE = RouterFunctions.class.getName() + ".request";

	/**
	 * Name of the {@link ServerWebExchange} attribute that contains the URI
	 * templates map, mapping variable names to values.
	 */
	public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE =
			RouterFunctions.class.getName() + ".uriTemplateVariables";

	private static final HandlerFunction<Void> NOT_FOUND_HANDLER = request -> ServerResponse.notFound().build();


	/**
	 * Route to the given handler function if the given request predicate applies.
	 * @param predicate the predicate to test
	 * @param handlerFunction the handler function to route to
	 * @return a routing function that routes to {@code handlerFunction} if
	 * {@code predicate} evaluates to {@code true}
	 * @see RequestPredicates
	 */
	public static <T> RouterFunction route(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
		Assert.notNull(predicate, "'predicate' must not be null");
		Assert.notNull(handlerFunction, "'handlerFunction' must not be null");

		return request -> predicate.test(request) ? Optional.of(handlerFunction) : Optional.empty();
	}

	/**
	 * Route to the given routing function if the given request predicate applies.
	 * @param predicate the predicate to test
	 * @param router the routing function to route to
	 * @return a routing function that routes to {@code routerFunction} if
	 * {@code predicate} evaluates to {@code true}
	 * @see RequestPredicates
	 */
	public static RouterFunction subroute(RequestPredicate predicate, RouterFunction router) {
		Assert.notNull(predicate, "'predicate' must not be null");
		Assert.notNull(router, "'routerFunction' must not be null");

		return request -> {
			if (predicate.test(request)) {
				ServerRequest subRequest = predicate.subRequest(request);
				return router.route(subRequest);
			}
			else {
				return Optional.empty();
			}
		};
	}

	/**
	 * Convert the given {@linkplain RouterFunction routing function} into a {@link HttpHandler}.
	 * This conversion uses {@linkplain HandlerStrategies#builder() default strategies}.
	 * <p>The returned {@code HttpHandler} can be adapted to run in
	 * <ul>
	 * <li>Servlet 3.1+ using the
	 * {@link org.springframework.http.server.reactive.ServletHttpHandlerAdapter},</li>
	 * <li>Reactor using the
	 * {@link org.springframework.http.server.reactive.ReactorHttpHandlerAdapter},</li>
	 * <li>RxNetty using the
	 * {@link org.springframework.http.server.reactive.RxNettyHttpHandlerAdapter}, or </li>
	 * <li>Undertow using the
	 * {@link org.springframework.http.server.reactive.UndertowHttpHandlerAdapter}.</li>
	 * </ul>
	 * @param routerFunction the routing function to convert
	 * @return an http handler that handles HTTP request using the given routing function
	 */
	public static HttpHandler toHttpHandler(RouterFunction routerFunction) {
		return toHttpHandler(routerFunction, HandlerStrategies.withDefaults());
	}

	/**
	 * Convert the given {@linkplain RouterFunction routing function} into a {@link HttpHandler},
	 * using the given strategies.
	 * <p>The returned {@code HttpHandler} can be adapted to run in
	 * <ul>
	 * <li>Servlet 3.1+ using the
	 * {@link org.springframework.http.server.reactive.ServletHttpHandlerAdapter},</li>
	 * <li>Reactor using the
	 * {@link org.springframework.http.server.reactive.ReactorHttpHandlerAdapter},</li>
	 * <li>RxNetty using the
	 * {@link org.springframework.http.server.reactive.RxNettyHttpHandlerAdapter}, or </li>
	 * <li>Undertow using the
	 * {@link org.springframework.http.server.reactive.UndertowHttpHandlerAdapter}.</li>
	 * </ul>
	 * @param routerFunction the routing function to convert
	 * @param strategies the strategies to use
	 * @return an http handler that handles HTTP request using the given routing function
	 */
	public static HttpHandler toHttpHandler(RouterFunction routerFunction, HandlerStrategies strategies) {
		Assert.notNull(routerFunction, "RouterFunction must not be null");
		Assert.notNull(strategies, "HandlerStrategies must not be null");

		return new HttpWebHandlerAdapter(exchange -> {
			ServerRequest request = new DefaultServerRequest(exchange, strategies);
			addAttributes(exchange, request);
			HandlerFunction<?> handlerFunction = routerFunction.route(request).orElse(notFound());
			ServerResponse<?> response = handlerFunction.handle(request);
			return response.writeTo(exchange, strategies);
		});
	}

	/**
	 * Convert the given {@code RouterFunction} into a {@code HandlerMapping}.
	 * This conversion uses {@linkplain HandlerStrategies#builder() default strategies}.
	 * <p>The returned {@code HandlerMapping} can be run in a
	 * {@link org.springframework.web.reactive.DispatcherHandler}.
	 * @param routerFunction the routing function to convert
	 * @return an handler mapping that maps HTTP request to a handler using the given routing function
	 * @see org.springframework.web.reactive.function.support.HandlerFunctionAdapter
	 * @see org.springframework.web.reactive.function.support.ServerResponseResultHandler
	 */
	public static HandlerMapping toHandlerMapping(RouterFunction routerFunction) {
		return toHandlerMapping(routerFunction, HandlerStrategies.withDefaults());
	}

	/**
	 * Convert the given {@linkplain RouterFunction routing function} into a {@link HandlerMapping},
	 * using the given strategies.
	 * <p>The returned {@code HandlerMapping} can be run in a
	 * {@link org.springframework.web.reactive.DispatcherHandler}.
	 * @param routerFunction the routing function to convert
	 * @param strategies the strategies to use
	 * @return an handler mapping that maps HTTP request to a handler using the given routing function
	 * @see org.springframework.web.reactive.function.support.HandlerFunctionAdapter
	 * @see org.springframework.web.reactive.function.support.ServerResponseResultHandler
	 */
	public static HandlerMapping toHandlerMapping(RouterFunction routerFunction, HandlerStrategies strategies) {
		Assert.notNull(routerFunction, "RouterFunction must not be null");
		Assert.notNull(strategies, "HandlerStrategies must not be null");

		return exchange -> {
			ServerRequest request = new DefaultServerRequest(exchange, strategies);
			addAttributes(exchange, request);
			Optional<? extends HandlerFunction<?>> route = routerFunction.route(request);
			return Mono.justOrEmpty(route);
		};
	}


	private static void addAttributes(ServerWebExchange exchange, ServerRequest request) {
		Map<String, Object> attributes = exchange.getAttributes();
		attributes.put(REQUEST_ATTRIBUTE, request);
	}

	@SuppressWarnings("unchecked")
	private static <T> HandlerFunction<T> notFound() {
		return (HandlerFunction<T>) NOT_FOUND_HANDLER;
	}

}

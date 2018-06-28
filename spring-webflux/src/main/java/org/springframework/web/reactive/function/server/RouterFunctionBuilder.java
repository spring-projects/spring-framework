/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link RouterFunctions.Builder}.
 * @author Arjen Poutsma
 * @since 5.1
 */
class RouterFunctionBuilder implements RouterFunctions.Builder {

	private List<RouterFunction<ServerResponse>> routerFunctions = new ArrayList<>();

	private List<HandlerFilterFunction<ServerResponse, ServerResponse>> filterFunctions = new ArrayList<>();


	@Override
	public RouterFunctions.Builder route(RequestPredicate predicate,
			HandlerFunction<ServerResponse> handlerFunction) {
		this.routerFunctions.add(RouterFunctions.route(predicate, handlerFunction));
		return this;
	}

	@Override
	public RouterFunctions.Builder routeGet(HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.method(HttpMethod.GET), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routeGet(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.GET(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routeHead(HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.method(HttpMethod.HEAD), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routeHead(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.HEAD(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routePost(HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.method(HttpMethod.POST), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routePost(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.POST(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routePut(HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.method(HttpMethod.PUT), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routePut(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.PUT(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routePatch(HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.method(HttpMethod.PATCH), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routePatch(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.PATCH(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routeDelete(HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.method(HttpMethod.DELETE), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routeDelete(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.DELETE(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routeOptions(HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.method(HttpMethod.OPTIONS), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder routeOptions(String pattern, HandlerFunction<ServerResponse> handlerFunction) {
		return route(RequestPredicates.OPTIONS(pattern), handlerFunction);
	}

	@Override
	public RouterFunctions.Builder nest(RequestPredicate predicate,
			Consumer<RouterFunctions.Builder> builderConsumer) {

		Assert.notNull(builderConsumer, "'builderConsumer' must not be null");

		RouterFunctionBuilder nestedBuilder = new RouterFunctionBuilder();
		builderConsumer.accept(nestedBuilder);
		RouterFunction<ServerResponse> nestedRoute = nestedBuilder.build();

		this.routerFunctions.add(RouterFunctions.nest(predicate, nestedRoute));
		return this;
	}

	@Override
	public RouterFunctions.Builder nest(RequestPredicate predicate,
			Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier) {

		Assert.notNull(routerFunctionSupplier, "'routerFunctionSupplier' must not be null");

		RouterFunction<ServerResponse> nestedRoute = routerFunctionSupplier.get();

		this.routerFunctions.add(RouterFunctions.nest(predicate, nestedRoute));
		return this;
	}

	@Override
	public RouterFunctions.Builder nestPath(String pattern,
			Consumer<RouterFunctions.Builder> builderConsumer) {
		return nest(RequestPredicates.path(pattern), builderConsumer);
	}

	@Override
	public RouterFunctions.Builder nestPath(String pattern,
			Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier) {
		return nest(RequestPredicates.path(pattern), routerFunctionSupplier);
	}

	@Override
	public RouterFunctions.Builder filter(HandlerFilterFunction<ServerResponse, ServerResponse> filterFunction) {
		Assert.notNull(filterFunction, "'filterFunction' must not be null");

		this.filterFunctions.add(filterFunction);
		return this;
	}

	@Override
	public RouterFunctions.Builder before(
			Function<ServerRequest, Mono<ServerRequest>> requestProcessor) {

		Assert.notNull(requestProcessor, "Function must not be null");
		return filter((request, next) -> requestProcessor.apply(request).flatMap(next::handle));
	}

	@Override
	public RouterFunctions.Builder after(
			BiFunction<ServerRequest, ServerResponse, Mono<ServerResponse>> responseProcessor) {
		return filter((request, next) -> next.handle(request)
				.flatMap(serverResponse -> responseProcessor.apply(request, serverResponse)));
	}

	@Override
	public <T extends Throwable> RouterFunctions.Builder exception(
			Class<T> exceptionType,
			BiFunction<T, ServerRequest, Mono<ServerResponse>> fallback) {
		Assert.notNull(exceptionType, "'exceptionType' must not be null");
		Assert.notNull(fallback, "'fallback' must not be null");

		return filter((request, next) -> next.handle(request)
				.onErrorResume(exceptionType, t -> fallback.apply(t, request)));
	}

	@Override
	public RouterFunction<ServerResponse> build() {

		RouterFunction<ServerResponse> result = this.routerFunctions.stream()
				.reduce(RouterFunction::and)
				.orElseThrow(IllegalStateException::new);

		if (this.filterFunctions.isEmpty()) {
			return result;
		}
		else {
			HandlerFilterFunction<ServerResponse, ServerResponse> filter =
					this.filterFunctions.stream()
							.reduce(HandlerFilterFunction::andThen)
							.orElseThrow(IllegalStateException::new);

			return result.filter(filter);
		}
	}

}

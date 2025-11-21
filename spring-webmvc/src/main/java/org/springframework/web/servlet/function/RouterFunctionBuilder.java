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

package org.springframework.web.servlet.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link RouterFunctions.Builder}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.2
 */
class RouterFunctionBuilder implements RouterFunctions.Builder {

	private final List<RouterFunction<ServerResponse>> routerFunctions = new ArrayList<>();

	private final List<HandlerFilterFunction<ServerResponse, ServerResponse>> filterFunctions = new ArrayList<>();

	private final List<HandlerFilterFunction<ServerResponse, ServerResponse>> errorHandlers = new ArrayList<>();


	@SuppressWarnings("unchecked")
	@Override
	public <T extends ServerResponse> RouterFunctions.Builder add(RouterFunction<T> routerFunction) {
		Assert.notNull(routerFunction, "RouterFunction must not be null");
		this.routerFunctions.add((RouterFunction<ServerResponse>) routerFunction);
		return this;
	}

	private <T extends ServerResponse> RouterFunctions.Builder add(
			RequestPredicate predicate, HandlerFunction<T> handlerFunction) {

		return add(RouterFunctions.route(predicate, handlerFunction));
	}

	// GET

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder GET(HandlerFunction<T> function) {
		return add(RequestPredicates.method(HttpMethod.GET), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder GET(
			RequestPredicate predicate, HandlerFunction<T> handlerFunction) {

		return add(RequestPredicates.method(HttpMethod.GET).and(predicate), handlerFunction);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder GET(String pattern, HandlerFunction<T> function) {
		return add(RequestPredicates.GET(pattern), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder GET(String pattern, RequestPredicate predicate,
			HandlerFunction<T> handlerFunction) {

		return add(RequestPredicates.GET(pattern).and(predicate), handlerFunction);
	}

	// HEAD

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder HEAD(HandlerFunction<T> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.HEAD), handlerFunction);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder HEAD(
			RequestPredicate predicate, HandlerFunction<T> function) {

		return add(RequestPredicates.method(HttpMethod.HEAD).and(predicate), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder HEAD(String pattern, HandlerFunction<T> function) {
		return add(RequestPredicates.HEAD(pattern), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder HEAD(
			String pattern, RequestPredicate predicate, HandlerFunction<T> handlerFunction) {

		return add(RequestPredicates.HEAD(pattern).and(predicate), handlerFunction);
	}

	// POST

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder POST(HandlerFunction<T> handlerFunction) {
		return add(RequestPredicates.method(HttpMethod.POST), handlerFunction);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder POST(
			RequestPredicate predicate, HandlerFunction<T> function) {

		return add(RequestPredicates.method(HttpMethod.POST).and(predicate), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder POST(String pattern, HandlerFunction<T> function) {
		return add(RequestPredicates.POST(pattern), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder POST(
			String pattern, RequestPredicate predicate, HandlerFunction<T> handlerFunction) {

		return add(RequestPredicates.POST(pattern).and(predicate), handlerFunction);
	}

	// PUT

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder PUT(HandlerFunction<T> function) {
		return add(RequestPredicates.method(HttpMethod.PUT), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder PUT(
			RequestPredicate predicate, HandlerFunction<T> function) {

		return add(RequestPredicates.method(HttpMethod.PUT).and(predicate), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder PUT(String pattern, HandlerFunction<T> function) {
		return add(RequestPredicates.PUT(pattern), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder PUT(
			String pattern, RequestPredicate predicate, HandlerFunction<T> function) {

		return add(RequestPredicates.PUT(pattern).and(predicate), function);
	}

	// PATCH

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder PATCH(HandlerFunction<T> function) {
		return add(RequestPredicates.method(HttpMethod.PATCH), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder PATCH(
			RequestPredicate predicate, HandlerFunction<T> function) {

		return add(RequestPredicates.method(HttpMethod.PATCH).and(predicate), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder PATCH(String pattern, HandlerFunction<T> function) {
		return add(RequestPredicates.PATCH(pattern), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder PATCH(
			String pattern, RequestPredicate predicate, HandlerFunction<T> function) {

		return add(RequestPredicates.PATCH(pattern).and(predicate), function);
	}

	// DELETE

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder DELETE(HandlerFunction<T> function) {
		return add(RequestPredicates.method(HttpMethod.DELETE), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder DELETE(
			RequestPredicate predicate, HandlerFunction<T> function) {

		return add(RequestPredicates.method(HttpMethod.DELETE).and(predicate), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder DELETE(String pattern, HandlerFunction<T> function) {
		return add(RequestPredicates.DELETE(pattern), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder DELETE(
			String pattern, RequestPredicate predicate, HandlerFunction<T> function) {

		return add(RequestPredicates.DELETE(pattern).and(predicate), function);
	}

	// OPTIONS

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder OPTIONS(HandlerFunction<T> function) {
		return add(RequestPredicates.method(HttpMethod.OPTIONS), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder OPTIONS(
			RequestPredicate predicate, HandlerFunction<T> function) {

		return add(RequestPredicates.method(HttpMethod.OPTIONS).and(predicate), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder OPTIONS(String pattern, HandlerFunction<T> function) {
		return add(RequestPredicates.OPTIONS(pattern), function);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder OPTIONS(
			String pattern, RequestPredicate predicate, HandlerFunction<T> function) {

		return add(RequestPredicates.OPTIONS(pattern).and(predicate), function);
	}

	// other

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder route(
			RequestPredicate predicate, HandlerFunction<T> function) {

		return add(RouterFunctions.route(predicate, function));
	}

	@Override
	public RouterFunctions.Builder resource(RequestPredicate predicate, Resource resource) {
		return add(RouterFunctions.resource(predicate, resource));
	}

	@Override
	public RouterFunctions.Builder resource(
			RequestPredicate predicate, Resource resource, BiConsumer<Resource, HttpHeaders> headersConsumer) {

		return add(RouterFunctions.resource(predicate, resource, headersConsumer));
	}

	@Override
	public RouterFunctions.Builder resources(String pattern, Resource location) {
		return add(RouterFunctions.resources(pattern, location));
	}

	@Override
	public RouterFunctions.Builder resources(String pattern, Resource location,
			BiConsumer<Resource, HttpHeaders> headersConsumer) {

		return add(RouterFunctions.resources(pattern, location, headersConsumer));
	}

	@Override
	public RouterFunctions.Builder resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
		return add(RouterFunctions.resources(lookupFunction));
	}

	@Override
	public RouterFunctions.Builder resources(
			Function<ServerRequest, Optional<Resource>> lookupFunction,
			BiConsumer<Resource, HttpHeaders> headersConsumer) {

		return add(RouterFunctions.resources(lookupFunction, headersConsumer));
	}

	@Override
	public RouterFunctions.Builder nest(
			RequestPredicate predicate, Consumer<RouterFunctions.Builder> builderConsumer) {

		Assert.notNull(builderConsumer, "Consumer must not be null");

		RouterFunctionBuilder nestedBuilder = new RouterFunctionBuilder();
		builderConsumer.accept(nestedBuilder);
		RouterFunction<ServerResponse> nestedRoute = nestedBuilder.build();
		this.routerFunctions.add(RouterFunctions.nest(predicate, nestedRoute));
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ServerResponse> RouterFunctions.Builder nest(
			RequestPredicate predicate, Supplier<RouterFunction<T>> routerFunctionSupplier) {

		Assert.notNull(routerFunctionSupplier, "RouterFunction Supplier must not be null");

		RouterFunction<ServerResponse> route = (RouterFunction<ServerResponse>) routerFunctionSupplier.get();
		this.routerFunctions.add(RouterFunctions.nest(predicate, route));
		return this;
	}

	@Override
	public RouterFunctions.Builder path(String pattern, Consumer<RouterFunctions.Builder> builderConsumer) {
		return nest(RequestPredicates.path(pattern), builderConsumer);
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder path(
			String pattern, Supplier<RouterFunction<T>> routerFunctionSupplier) {

		return nest(RequestPredicates.path(pattern), routerFunctionSupplier);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ServerResponse, R extends ServerResponse> RouterFunctions.Builder filter(
			HandlerFilterFunction<T, R> filterFunction) {

		Assert.notNull(filterFunction, "HandlerFilterFunction must not be null");
		this.filterFunctions.add((HandlerFilterFunction<ServerResponse, ServerResponse>) filterFunction);
		return this;
	}

	@Override
	public RouterFunctions.Builder before(Function<ServerRequest, ServerRequest> requestProcessor) {
		Assert.notNull(requestProcessor, "RequestProcessor must not be null");
		return filter(HandlerFilterFunction.ofRequestProcessor(requestProcessor));
	}

	@Override
	public <T extends ServerResponse, R extends ServerResponse> RouterFunctions.Builder after(
			BiFunction<ServerRequest, T, R> responseProcessor) {

		Assert.notNull(responseProcessor, "ResponseProcessor must not be null");
		return filter(HandlerFilterFunction.ofResponseProcessor(responseProcessor));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ServerResponse> RouterFunctions.Builder onError(
			Predicate<Throwable> predicate, BiFunction<Throwable, ServerRequest, T> responseProvider) {

		Assert.notNull(predicate, "Predicate must not be null");
		Assert.notNull(responseProvider, "ResponseProvider must not be null");

		HandlerFilterFunction<T, T> filter = HandlerFilterFunction.ofErrorHandler(predicate, responseProvider);
		this.errorHandlers.add(0, (HandlerFilterFunction<ServerResponse, ServerResponse>) filter);
		return this;
	}

	@Override
	public <T extends ServerResponse> RouterFunctions.Builder onError(
			Class<? extends Throwable> exceptionType, BiFunction<Throwable, ServerRequest, T> responseProvider) {

		Assert.notNull(exceptionType, "ExceptionType must not be null");
		Assert.notNull(responseProvider, "ResponseProvider must not be null");

		return onError(exceptionType::isInstance, responseProvider);
	}

	@Override
	public RouterFunctions.Builder withAttribute(String name, Object value) {
		Assert.hasLength(name, "Name must not be empty");
		Assert.notNull(value, "Value must not be null");
		Assert.state(!this.routerFunctions.isEmpty(),
				"attributes can only be called after any other method (GET, path, etc.)");
		int lastIdx = this.routerFunctions.size() - 1;
		RouterFunction<ServerResponse> attributed = this.routerFunctions.get(lastIdx).withAttribute(name, value);
		this.routerFunctions.set(lastIdx, attributed);
		return this;
	}

	@Override
	public RouterFunctions.Builder withAttributes(Consumer<Map<String, Object>> consumer) {
		Assert.notNull(consumer, "AttributesConsumer must not be null");
		Assert.state(!this.routerFunctions.isEmpty(),
				"attributes can only be called after any other method (GET, path, etc.)");
		int lastIdx = this.routerFunctions.size() - 1;
		RouterFunction<ServerResponse> attributed = this.routerFunctions.get(lastIdx).withAttributes(consumer);
		this.routerFunctions.set(lastIdx, attributed);
		return this;
	}

	@Override
	public RouterFunction<ServerResponse> build() {
		Assert.state(!this.routerFunctions.isEmpty(),
				"No routes registered. Register a route with GET(), POST(), etc.");

		RouterFunction<ServerResponse> result = new BuiltRouterFunction(this.routerFunctions);
		if (this.filterFunctions.isEmpty() && this.errorHandlers.isEmpty()) {
			return result;
		}

		HandlerFilterFunction<ServerResponse, ServerResponse> filter =
				Stream.concat(this.filterFunctions.stream(), this.errorHandlers.stream())
						.reduce(HandlerFilterFunction::andThen)
						.orElseThrow(IllegalStateException::new);

		return result.filter(filter);
	}


	/**
	 * Router function returned by {@link #build()} that simply iterates over the registered routes.
	 */
	private static class BuiltRouterFunction extends RouterFunctions.AbstractRouterFunction<ServerResponse> {

		private final List<RouterFunction<ServerResponse>> routerFunctions;

		public BuiltRouterFunction(List<RouterFunction<ServerResponse>> routerFunctions) {
			Assert.notEmpty(routerFunctions, "RouterFunctions must not be empty");
			this.routerFunctions = new ArrayList<>(routerFunctions);
		}

		@Override
		public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
			for (RouterFunction<ServerResponse> routerFunction : this.routerFunctions) {
				Optional<HandlerFunction<ServerResponse>> result = routerFunction.route(request);
				if (result.isPresent()) {
					return result;
				}
			}
			return Optional.empty();
		}

		@Override
		public void accept(RouterFunctions.Visitor visitor) {
			this.routerFunctions.forEach(routerFunction -> routerFunction.accept(visitor));
		}
	}


}

/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.web.servlet.handler.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.method;
import static org.springframework.web.servlet.function.RequestPredicates.path;

/**
 * @author Arjen Poutsma
 */
class RouterFunctionTests {

	private final ServerRequest request = new DefaultServerRequest(
			PathPatternsTestUtils.initRequest("GET", "", true), Collections.emptyList());


	@Test
	void and() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction1 = request -> Optional.empty();
		RouterFunction<ServerResponse> routerFunction2 = request -> Optional.of(handlerFunction);

		RouterFunction<ServerResponse> result = routerFunction1.and(routerFunction2);
		assertThat(result).isNotNull();

		Optional<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		assertThat(resultHandlerFunction).isPresent();
		assertThat(resultHandlerFunction).contains(handlerFunction);
	}


	@Test
	void andOther() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().body("42");
		RouterFunction<?> routerFunction1 = request -> Optional.empty();
		RouterFunction<ServerResponse> routerFunction2 = request -> Optional.of(handlerFunction);

		RouterFunction<?> result = routerFunction1.andOther(routerFunction2);
		assertThat(result).isNotNull();

		Optional<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);
		assertThat(resultHandlerFunction).isPresent();
		assertThat(resultHandlerFunction.get()).isEqualTo(handlerFunction);
	}


	@Test
	void andRoute() {
		RouterFunction<ServerResponse> routerFunction1 = request -> Optional.empty();
		RequestPredicate requestPredicate = request -> true;

		RouterFunction<ServerResponse> result = routerFunction1.andRoute(requestPredicate, this::handlerMethod);
		assertThat(result).isNotNull();

		Optional<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);
		assertThat(resultHandlerFunction).isPresent();
	}


	@Test
	void filter() {
		String string = "42";
		HandlerFunction<EntityResponse<String>> handlerFunction =
				request -> EntityResponse.fromObject(string).build();
		RouterFunction<EntityResponse<String>> routerFunction =
				request -> Optional.of(handlerFunction);

		HandlerFilterFunction<EntityResponse<String>, EntityResponse<Integer>> filterFunction =
				(request, next) -> {
					String stringResponse = next.handle(request).entity();
					Integer intResponse = Integer.parseInt(stringResponse);
					return EntityResponse.fromObject(intResponse).build();
				};

		RouterFunction<EntityResponse<Integer>> result = routerFunction.filter(filterFunction);
		assertThat(result).isNotNull();

		Optional<EntityResponse<Integer>> resultHandlerFunction = result.route(request)
				.map(hf -> {
					try {
						return hf.handle(request);
					}
					catch (Exception ex) {
						throw new AssertionError(ex.getMessage(), ex);
					}
				});
		assertThat(resultHandlerFunction).isPresent();
		assertThat((int) resultHandlerFunction.get().entity()).isEqualTo(42);
	}


	@Test
	void attributes() {
		RouterFunction<ServerResponse> route = RouterFunctions.route(
				GET("/atts/1"), request -> ServerResponse.ok().build())
				.withAttribute("foo", "bar")
				.withAttribute("baz", "qux")
				.and(RouterFunctions.route(GET("/atts/2"), request -> ServerResponse.ok().build())
				.withAttributes(atts -> {
					atts.put("foo", "bar");
					atts.put("baz", "qux");
				}))
				.and(RouterFunctions.nest(path("/atts"),
						RouterFunctions.route(GET("/3"), request -> ServerResponse.ok().build())
						.withAttribute("foo", "bar")
						.and(RouterFunctions.route(GET("/4"), request -> ServerResponse.ok().build())
						.withAttribute("baz", "qux"))
						.and(RouterFunctions.nest(path("/5"),
								RouterFunctions.route(method(GET), request -> ServerResponse.ok().build())
								.withAttribute("foo", "n3"))
						.withAttribute("foo", "n2")))
				.withAttribute("foo", "n1"));

		AttributesTestVisitor visitor = new AttributesTestVisitor();
		route.accept(visitor);
		assertThat(visitor.routerFunctionsAttributes()).containsExactly(
				List.of(Map.of("foo", "bar", "baz", "qux")),
				List.of(Map.of("foo", "bar", "baz", "qux")),
				List.of(Map.of("foo", "bar"), Map.of("foo", "n1")),
				List.of(Map.of("baz", "qux"), Map.of("foo", "n1")),
				List.of(Map.of("foo", "n3"), Map.of("foo", "n2"), Map.of("foo", "n1"))
		);
		assertThat(visitor.visitCount()).isEqualTo(7);
	}



	private ServerResponse handlerMethod(ServerRequest request) {
		return ServerResponse.ok().body("42");
	}



}

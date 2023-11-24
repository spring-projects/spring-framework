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

package org.springframework.web.reactive.function.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * @author Arjen Poutsma
 */
public class RouterFunctionTests {

	@Test
	public void and() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction1 = request -> Mono.empty();
		RouterFunction<ServerResponse> routerFunction2 = request -> Mono.just(handlerFunction);

		RouterFunction<ServerResponse> result = routerFunction1.and(routerFunction2);
		assertThat(result).isNotNull();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);

		StepVerifier.create(resultHandlerFunction)
				.expectNext(handlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	public void andOther() {
		HandlerFunction<ServerResponse> handlerFunction =
				request -> ServerResponse.ok().bodyValue("42");
		RouterFunction<?> routerFunction1 = request -> Mono.empty();
		RouterFunction<ServerResponse> routerFunction2 =
				request -> Mono.just(handlerFunction);

		RouterFunction<?> result = routerFunction1.andOther(routerFunction2);
		assertThat(result).isNotNull();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		Mono<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);

		StepVerifier.create(resultHandlerFunction)
				.expectNextMatches(o -> o.equals(handlerFunction))
				.expectComplete()
				.verify();
	}

	@Test
	public void andRoute() {
		RouterFunction<ServerResponse> routerFunction1 = request -> Mono.empty();
		RequestPredicate requestPredicate = request -> true;

		RouterFunction<ServerResponse> result = routerFunction1.andRoute(requestPredicate, this::handlerMethod);
		assertThat(result).isNotNull();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		Mono<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);

		StepVerifier.create(resultHandlerFunction)
				.expectNextCount(1)
				.expectComplete()
				.verify();
	}

	@Test
	public void filter() {
		Mono<String> stringMono = Mono.just("42");
		HandlerFunction<EntityResponse<Mono<String>>> handlerFunction =
				request -> EntityResponse.fromPublisher(stringMono, String.class).build();
		RouterFunction<EntityResponse<Mono<String>>> routerFunction =
				request -> Mono.just(handlerFunction);

		HandlerFilterFunction<EntityResponse<Mono<String>>, EntityResponse<Mono<Integer>>> filterFunction =
				(request, next) -> next.handle(request).flatMap(
						response -> {
							Mono<Integer> intMono = response.entity()
									.map(Integer::parseInt);
							return EntityResponse.fromPublisher(intMono, Integer.class).build();
						});

		RouterFunction<EntityResponse<Mono<Integer>>> result = routerFunction.filter(filterFunction);
		assertThat(result).isNotNull();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		Mono<EntityResponse<Mono<Integer>>> responseMono =
				result.route(request).flatMap(hf -> hf.handle(request));

		StepVerifier.create(responseMono)
				.consumeNextWith(
						serverResponse ->
							StepVerifier.create(serverResponse.entity())
									.expectNext(42)
									.expectComplete()
									.verify()
						)
				.expectComplete()
				.verify();
	}

	@Test
	public void attributes() {
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



	private Mono<ServerResponse> handlerMethod(ServerRequest request) {
		return ServerResponse.ok().bodyValue("42");
	}



}

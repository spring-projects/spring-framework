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

package org.springframework.web.reactive.function.server;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.Assert.assertNotNull;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

/**
 * @author Arjen Poutsma
 */
@SuppressWarnings("unchecked")
public class RouterFunctionTests {

	@Test
	public void andSame() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction1 = request -> Mono.empty();
		RouterFunction<ServerResponse> routerFunction2 = request -> Mono.just(handlerFunction);

		RouterFunction<ServerResponse> result = routerFunction1.andSame(routerFunction2);
		assertNotNull(result);

		MockServerRequest request = MockServerRequest.builder().build();
		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);

		StepVerifier.create(resultHandlerFunction)
				.expectNext(handlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	public void and() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction =
				request -> ServerResponse.ok().body(fromObject("42"));
		RouterFunction<?> routerFunction1 = request -> Mono.empty();
		RouterFunction<ServerResponse> routerFunction2 =
				request -> Mono.just(handlerFunction);

		RouterFunction<?> result = routerFunction1.and(routerFunction2);
		assertNotNull(result);

		MockServerRequest request = MockServerRequest.builder().build();
		Mono<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);

		StepVerifier.create(resultHandlerFunction)
				.expectNextMatches(o -> o.equals(handlerFunction))
				.expectComplete()
				.verify();
	}

	@Test
	public void andRoute() throws Exception {
		RouterFunction<?> routerFunction1 = request -> Mono.empty();
		RequestPredicate requestPredicate = request -> true;

		RouterFunction<?> result = routerFunction1.andRoute(requestPredicate, this::handlerMethod);
		assertNotNull(result);

		MockServerRequest request = MockServerRequest.builder().build();
		Mono<? extends HandlerFunction<?>> resultHandlerFunction = result.route(request);

		StepVerifier.create(resultHandlerFunction)
				.expectNextCount(1)
				.expectComplete()
				.verify();
	}

	private Mono<ServerResponse> handlerMethod(ServerRequest request) {
		return ServerResponse.ok().body(fromObject("42"));
	}

	/*
	TODO: enable when ServerEntityResponse is reintroduced

	@Test
	public void filter() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().body(fromObject("42"));
		RouterFunction<ServerResponse> routerFunction = request -> Mono.just(handlerFunction);

		HandlerFilterFunction<String, Integer> filterFunction =
				(request, next) -> next.handle(request).then(
						response -> {
							Flux<Integer> body = Flux.from(response.body())
									.map(Integer::parseInt);
							return ServerResponse.ok().body(body, Integer.class);
						});
		RouterFunction<Integer> result = routerFunction.filter(filterFunction);
		assertNotNull(result);

		MockServerRequest request = MockServerRequest.builder().build();
		Mono<? extends ServerResponse<Integer>> responseMono =
				result.route(request).then(hf -> hf.handle(request));

		StepVerifier.create(responseMono)
				.consumeNextWith(
						serverResponse -> {
							StepVerifier.create(serverResponse.body())
									.expectNext(42)
									.expectComplete()
									.verify();
						})
				.expectComplete()
				.verify();
	}
	 */

}
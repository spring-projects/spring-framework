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

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class RouterFunctionBuilderTests {

	@Test
	public void route() {
		RouterFunction<ServerResponse> route = RouterFunctions.builder()
				.routeGet("/foo", request -> ServerResponse.ok().build())
				.routePost(request -> ServerResponse.noContent().build())
				.build();

		MockServerRequest fooRequest = MockServerRequest.builder().
				method(HttpMethod.GET).
				uri(URI.create("http://localhost/foo"))
				.build();

		Mono<Integer> responseMono = route.route(fooRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(fooRequest))
				.map(ServerResponse::statusCode)
				.map(HttpStatus::value);

		StepVerifier.create(responseMono)
				.expectNext(200)
				.verifyComplete();

		MockServerRequest barRequest = MockServerRequest.builder().
				method(HttpMethod.POST).
				uri(URI.create("http://localhost"))
				.build();

		responseMono = route.route(barRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(barRequest))
				.map(ServerResponse::statusCode)
				.map(HttpStatus::value);

		StepVerifier.create(responseMono)
				.expectNext(204)
				.verifyComplete();

	}

	@Test
	public void nest() {
		RouterFunction<?> route = RouterFunctions.builder()
				.nestPath("/foo", builder ->
						builder.nestPath("/bar",
								() -> RouterFunctions.builder()
										.routeGet("/baz", request -> ServerResponse.ok().build())
										.build()))
				.build();

		MockServerRequest fooRequest = MockServerRequest.builder().
				method(HttpMethod.GET).
				uri(URI.create("http://localhost/foo/bar/baz"))
				.build();

		Mono<Integer> responseMono = route.route(fooRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(fooRequest))
				.map(ServerResponse::statusCode)
				.map(HttpStatus::value);

		StepVerifier.create(responseMono)
				.expectNext(200)
				.verifyComplete();
	}

	@Test
	public void filters() {
		AtomicInteger filterCount = new AtomicInteger();

		RouterFunction<?> route = RouterFunctions.builder()
				.routeGet("/foo", request -> ServerResponse.ok().build())
				.routeGet("/bar", request -> Mono.error(new IllegalStateException()))
				.before(request -> {
					int count = filterCount.getAndIncrement();
					assertEquals(0, count);
					return Mono.just(request);
				})
				.after((request, response) -> {
					int count = filterCount.getAndIncrement();
					assertEquals(3, count);
					return Mono.just(response);
				})
				.filter((request, next) -> {
					int count = filterCount.getAndIncrement();
					assertEquals(1, count);
					Mono<ServerResponse> responseMono = next.handle(request);
					count = filterCount.getAndIncrement();
					assertEquals(2, count);
					return responseMono;
				})
				.exception(IllegalStateException.class, (e, request) -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
				.build();

		MockServerRequest fooRequest = MockServerRequest.builder().
				method(HttpMethod.GET).
				uri(URI.create("http://localhost/foo"))
				.build();

		Mono<ServerResponse> fooResponseMono = route.route(fooRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(fooRequest));


		StepVerifier.create(fooResponseMono)
				.consumeNextWith(serverResponse -> {
					assertEquals(4, filterCount.get());
				})
				.verifyComplete();

		filterCount.set(0);

		MockServerRequest barRequest = MockServerRequest.builder().
				method(HttpMethod.GET).
				uri(URI.create("http://localhost/bar"))
				.build();


		Mono<Integer> barResponseMono = route.route(barRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(barRequest))
				.map(ServerResponse::statusCode)
				.map(HttpStatus::value);

		StepVerifier.create(barResponseMono)
				.expectNext(500)
				.verifyComplete();

	}

}
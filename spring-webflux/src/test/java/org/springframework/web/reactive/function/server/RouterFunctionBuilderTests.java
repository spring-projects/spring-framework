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

package org.springframework.web.reactive.function.server;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RequestPredicates.HEAD;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * @author Arjen Poutsma
 */
class RouterFunctionBuilderTests {

	@Test
	void route() {
		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.GET("/foo", request -> ServerResponse.ok().build())
				.POST("/", RequestPredicates.contentType(MediaType.TEXT_PLAIN), request -> ServerResponse.noContent().build())
				.route(HEAD("/foo"), request -> ServerResponse.accepted().build())
				.build();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com/foo").build();
		ServerRequest getRequest = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<HttpStatusCode> responseMono = route.route(getRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(getRequest))
				.map(ServerResponse::statusCode);

		StepVerifier.create(responseMono)
				.expectNext(HttpStatus.OK)
				.verifyComplete();

		mockRequest = MockServerHttpRequest.head("https://example.com/foo").build();
		ServerRequest headRequest =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		responseMono = route.route(headRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(headRequest))
				.map(ServerResponse::statusCode);

		StepVerifier.create(responseMono)
				.expectNext(HttpStatus.ACCEPTED)
				.verifyComplete();

		mockRequest = MockServerHttpRequest.post("https://example.com/").
				contentType(MediaType.TEXT_PLAIN).build();
		ServerRequest barRequest =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		responseMono = route.route(barRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(barRequest))
				.map(ServerResponse::statusCode);

		StepVerifier.create(responseMono)
				.expectNext(HttpStatus.NO_CONTENT)
				.verifyComplete();

		mockRequest = MockServerHttpRequest.post("https://example.com/").build();
		ServerRequest invalidRequest =
				new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		responseMono = route.route(invalidRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(invalidRequest))
				.map(ServerResponse::statusCode);

		StepVerifier.create(responseMono)
				.verifyComplete();

	}

	@Test
	void resource() {
		Resource resource = new ClassPathResource("/org/springframework/web/reactive/function/server/response.txt");
		assertThat(resource.exists()).isTrue();

		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.resource(path("/test"), resource)
				.build();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://localhost/test").build();
		ServerRequest resourceRequest = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<HttpStatusCode> responseMono = route.route(resourceRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(resourceRequest))
				.map(ServerResponse::statusCode);

		StepVerifier.create(responseMono)
				.expectNext(HttpStatus.OK)
				.verifyComplete();
	}

	@Test
	void resources() {
		Resource resource = new ClassPathResource("/org/springframework/web/reactive/function/server/");
		assertThat(resource.exists()).isTrue();

		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.resources("/resources/**", resource)
				.build();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://localhost/resources/response.txt").build();
		ServerRequest resourceRequest = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<HttpStatusCode> responseMono = route.route(resourceRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(resourceRequest))
				.map(ServerResponse::statusCode);

		StepVerifier.create(responseMono)
				.expectNext(HttpStatus.OK)
				.verifyComplete();

		mockRequest = MockServerHttpRequest.post("https://localhost/resources/foo.txt").build();
		ServerRequest invalidRequest = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		responseMono = route.route(invalidRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(invalidRequest))
				.map(ServerResponse::statusCode);

		StepVerifier.create(responseMono)
				.verifyComplete();
	}

	@Test
	void resourcesCaching() {
		Resource resource = new ClassPathResource("/org/springframework/web/reactive/function/server/");
		assertThat(resource.exists()).isTrue();

		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.resources("/resources/**", resource, (r, headers) -> headers.setCacheControl(CacheControl.maxAge(Duration.ofSeconds(60))))
				.build();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://localhost/resources/response.txt").build();
		ServerRequest resourceRequest = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<String> responseMono = route.route(resourceRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(resourceRequest))
				.map(ServerResponse::headers)
				.mapNotNull(HttpHeaders::getCacheControl);

		StepVerifier.create(responseMono)
				.expectNext("max-age=60")
				.verifyComplete();
	}

	@Test
	void nest() {
		RouterFunction<?> route = RouterFunctions.route()
				.path("/foo", builder ->
						builder.path("/bar",
								() -> RouterFunctions.route()
										.GET("/baz", request -> ServerResponse.ok().build())
										.build()))
				.build();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://localhost/foo/bar/baz").build();
		ServerRequest fooRequest = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<HttpStatusCode> responseMono = route.route(fooRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(fooRequest))
				.map(ServerResponse::statusCode);

		StepVerifier.create(responseMono)
				.expectNext(HttpStatus.OK)
				.verifyComplete();
	}

	@Test
	void filters() {
		AtomicInteger filterCount = new AtomicInteger();

		RouterFunction<?> route = RouterFunctions.route()
				.GET("/foo", request -> ServerResponse.ok().build())
				.GET("/bar", request -> Mono.error(new IllegalStateException()))
				.before(request -> {
					int count = filterCount.getAndIncrement();
					assertThat(count).isEqualTo(0);
					return request;
				})
				.after((request, response) -> {
					int count = filterCount.getAndIncrement();
					assertThat(count).isEqualTo(3);
					return response;
				})
				.filter((request, next) -> {
					int count = filterCount.getAndIncrement();
					assertThat(count).isEqualTo(1);
					Mono<ServerResponse> responseMono = next.handle(request);
					count = filterCount.getAndIncrement();
					assertThat(count).isEqualTo(2);
					return responseMono;
				})
				.onError(IllegalStateException.class, (e, request) -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
				.build();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://localhost/foo").build();
		ServerRequest fooRequest = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<ServerResponse> fooResponseMono = route.route(fooRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(fooRequest));


		StepVerifier.create(fooResponseMono)
				.consumeNextWith(serverResponse -> assertThat(filterCount.get()).isEqualTo(4)
				)
				.verifyComplete();

		filterCount.set(0);

		mockRequest = MockServerHttpRequest.get("https://localhost/bar").build();
		ServerRequest barRequest = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<HttpStatusCode> barResponseMono = route.route(barRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(barRequest))
				.map(ServerResponse::statusCode);

		StepVerifier.create(barResponseMono)
				.expectNext(HttpStatus.INTERNAL_SERVER_ERROR)
				.verifyComplete();
	}

	@Test
	void multipleOnErrors() {
		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.GET("/error", request -> Mono.error(new IOException()))
				.onError(IOException.class, (t, r) -> ServerResponse.status(200).build())
				.onError(Exception.class, (t, r) -> ServerResponse.status(201).build())
				.build();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com/error").build();
		ServerRequest serverRequest = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<HttpStatusCode> responseStatus = route.route(serverRequest)
				.flatMap(handlerFunction -> handlerFunction.handle(serverRequest))
				.map(ServerResponse::statusCode);

		StepVerifier.create(responseStatus)
				.assertNext(status -> assertThat(status).isEqualTo(HttpStatus.OK))
				.verifyComplete();

	}

	@Test
	void attributes() {
		RouterFunction<ServerResponse> route = RouterFunctions.route()
				.GET("/atts/1", request -> ServerResponse.ok().build())
				.withAttribute("foo", "bar")
				.withAttribute("baz", "qux")
				.GET("/atts/2", request -> ServerResponse.ok().build())
				.withAttributes(atts -> {
					atts.put("foo", "bar");
					atts.put("baz", "qux");
				})
				.path("/atts", b1 -> b1
					.GET("/3", request -> ServerResponse.ok().build())
					.withAttribute("foo", "bar")
					.GET("/4", request -> ServerResponse.ok().build())
					.withAttribute("baz", "qux")
					.path("/5", b2 -> b2
						.GET(request -> ServerResponse.ok().build())
						.withAttribute("foo", "n3"))
					.withAttribute("foo", "n2")
				)
				.withAttribute("foo", "n1")
				.build();

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
}

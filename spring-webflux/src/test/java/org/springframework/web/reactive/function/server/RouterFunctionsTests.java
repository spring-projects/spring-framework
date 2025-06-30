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

package org.springframework.web.reactive.function.server;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 * @since 5.0
 */
class RouterFunctionsTests {

	@Test
	void routeMatch() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		RequestPredicate requestPredicate = mock();
		given(requestPredicate.test(request)).willReturn(true);

		RouterFunction<ServerResponse>
				result = RouterFunctions.route(requestPredicate, handlerFunction);
		assertThat(result).isNotNull();

		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);

		StepVerifier.create(resultHandlerFunction)
				.expectNext(handlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	void routeNoMatch() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		RequestPredicate requestPredicate = mock();
		given(requestPredicate.test(request)).willReturn(false);

		RouterFunction<ServerResponse> result = RouterFunctions.route(requestPredicate, handlerFunction);
		assertThat(result).isNotNull();

		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		StepVerifier.create(resultHandlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	void nestMatch() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = request -> Mono.just(handlerFunction);

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		RequestPredicate requestPredicate = mock();
		given(requestPredicate.nest(request)).willReturn(Optional.of(request));

		RouterFunction<ServerResponse> result = RouterFunctions.nest(requestPredicate, routerFunction);
		assertThat(result).isNotNull();

		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		StepVerifier.create(resultHandlerFunction)
				.expectNext(handlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	void nestNoMatch() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = request -> Mono.just(handlerFunction);

		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		RequestPredicate requestPredicate = mock();
		given(requestPredicate.nest(request)).willReturn(Optional.empty());

		RouterFunction<ServerResponse> result = RouterFunctions.nest(requestPredicate, routerFunction);
		assertThat(result).isNotNull();

		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		StepVerifier.create(resultHandlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	void toHttpHandlerNormal() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.accepted().build();
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertThat(result).isNotNull();

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("https://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
	}

	@Test
	void toHttpHandlerHandlerThrowsException() {
		HandlerFunction<ServerResponse> handlerFunction =
				request -> {
					throw new IllegalStateException();
				};
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertThat(result).isNotNull();

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("https://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void toHttpHandlerHandlerReturnsException() {
		HandlerFunction<ServerResponse> handlerFunction =
				request -> Mono.error(new IllegalStateException());
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertThat(result).isNotNull();

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("https://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void toHttpHandlerHandlerResponseStatusException() {
		HandlerFunction<ServerResponse> handlerFunction =
				request -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertThat(result).isNotNull();

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("https://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void toHttpHandlerRouteNotFoundReturnsResponseStatusException() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.accepted().build();
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.GET("/path"), handlerFunction);

		HandlerStrategies handlerStrategies = HandlerStrategies.empty().exceptionHandler((exchange, ex) -> {
			exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
			DataBuffer buffer = exchange.getResponse().bufferFactory().wrap("Custom response".getBytes(StandardCharsets.UTF_8));
			return exchange.getResponse().writeWith(Flux.just(buffer));
		}).build();
		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction, handlerStrategies);
		assertThat(result).isNotNull();

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("https://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(httpResponse.getBodyAsString().block()).isEqualTo("Custom response");
	}

	@Test
	void toHttpHandlerHandlerReturnResponseStatusExceptionInResponseWriteTo() {
		HandlerFunction<ServerResponse> handlerFunction =
				// Mono.<ServerResponse> is required for compilation in Eclipse
				request -> Mono.just(new ServerResponse() {
					@Override
					public HttpStatus statusCode() {
						return HttpStatus.OK;
					}
					@Override
					public HttpHeaders headers() {
						return new HttpHeaders();
					}
					@Override
					public MultiValueMap<String, ResponseCookie> cookies() {
						return new LinkedMultiValueMap<>();
					}
					@Override
					public Mono<Void> writeTo(ServerWebExchange exchange, Context context) {
						return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
					}
				});
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertThat(result).isNotNull();

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("https://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void toHttpHandlerHandlerThrowResponseStatusExceptionInResponseWriteTo() {
		HandlerFunction<ServerResponse> handlerFunction =
				// Mono.<ServerResponse> is required for compilation in Eclipse
				request -> Mono.just(new ServerResponse() {
					@Override
					public HttpStatus statusCode() {
						return HttpStatus.OK;
					}
					@Override
					public HttpHeaders headers() {
						return new HttpHeaders();
					}
					@Override
					public MultiValueMap<String, ResponseCookie> cookies() {
						return new LinkedMultiValueMap<>();
					}
					@Override
					public Mono<Void> writeTo(ServerWebExchange exchange, Context context) {
						throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
					}
				});
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertThat(result).isNotNull();

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("https://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void toHttpHandlerWebFilter() {
		AtomicBoolean filterInvoked = new AtomicBoolean();

		WebFilter webFilter = (exchange, chain) -> {
			filterInvoked.set(true);
			return chain.filter(exchange);
		};

		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.accepted().build();
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HandlerStrategies handlerStrategies = HandlerStrategies.builder()
				.webFilter(webFilter).build();

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction, handlerStrategies);
		assertThat(result).isNotNull();

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("https://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertThat(httpResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

		assertThat(filterInvoked.get()).isTrue();
	}

}

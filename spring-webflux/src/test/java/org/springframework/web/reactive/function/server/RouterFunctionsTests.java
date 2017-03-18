/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Arjen Poutsma
 * @since 5.0
 */
@SuppressWarnings("unchecked")
public class RouterFunctionsTests {

	@Test
	public void routeMatch() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

		MockServerRequest request = MockServerRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(true);

		RouterFunction<ServerResponse>
				result = RouterFunctions.route(requestPredicate, handlerFunction);
		assertNotNull(result);

		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);

		StepVerifier.create(resultHandlerFunction)
				.expectNext(handlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	public void routeNoMatch() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();

		MockServerRequest request = MockServerRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(false);

		RouterFunction<ServerResponse> result = RouterFunctions.route(requestPredicate, handlerFunction);
		assertNotNull(result);

		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		StepVerifier.create(resultHandlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	public void nestMatch() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = request -> Mono.just(handlerFunction);

		MockServerRequest request = MockServerRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(true);

		RouterFunction<ServerResponse> result = RouterFunctions.nest(requestPredicate, routerFunction);
		assertNotNull(result);

		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		StepVerifier.create(resultHandlerFunction)
				.expectNext(handlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	public void nestNoMatch() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = request -> Mono.just(handlerFunction);

		MockServerRequest request = MockServerRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(false);

		RouterFunction<ServerResponse> result = RouterFunctions.nest(requestPredicate, routerFunction);
		assertNotNull(result);

		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		StepVerifier.create(resultHandlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	public void toHttpHandlerNormal() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.accepted().build();
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertNotNull(result);

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("http://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertEquals(HttpStatus.ACCEPTED, httpResponse.getStatusCode());
	}

	@Test
	public void toHttpHandlerHandlerThrowsException() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction =
				request -> {
					throw new IllegalStateException();
				};
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertNotNull(result);

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("http://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpResponse.getStatusCode());
	}

	@Test
	public void toHttpHandlerHandlerReturnsException() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction =
				request -> Mono.error(new IllegalStateException());
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertNotNull(result);

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("http://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpResponse.getStatusCode());
	}

	@Test
	public void toHttpHandlerHandlerResponseStatusException() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction =
				request -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertNotNull(result);

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("http://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertEquals(HttpStatus.NOT_FOUND, httpResponse.getStatusCode());
	}

	@Test
	public void toHttpHandlerHandlerReturnResponseStatusExceptionInResponseWriteTo() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction =
				// Mono.<ServerResponse> is required for compilation in Eclipse
				request -> Mono.<ServerResponse> just(new ServerResponse() {
					@Override
					public HttpStatus statusCode() {
						return HttpStatus.OK;
					}

					@Override
					public HttpHeaders headers() {
						return new HttpHeaders();
					}

					@Override
					public Mono<Void> writeTo(ServerWebExchange exchange,
							HandlerStrategies strategies) {
						return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
					}
				});
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertNotNull(result);

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("http://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertEquals(HttpStatus.NOT_FOUND, httpResponse.getStatusCode());
	}

	@Test
	public void toHttpHandlerHandlerThrowResponseStatusExceptionInResponseWriteTo() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction =
				// Mono.<ServerResponse> is required for compilation in Eclipse
				request -> Mono.<ServerResponse> just(new ServerResponse() {
					@Override
					public HttpStatus statusCode() {
						return HttpStatus.OK;
					}

					@Override
					public HttpHeaders headers() {
						return new HttpHeaders();
					}

					@Override
					public Mono<Void> writeTo(ServerWebExchange exchange,
							HandlerStrategies strategies) {
						throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
					}
				});
		RouterFunction<ServerResponse> routerFunction =
				RouterFunctions.route(RequestPredicates.all(), handlerFunction);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction);
		assertNotNull(result);

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("http://localhost").build();
		MockServerHttpResponse httpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, httpResponse).block();
		assertEquals(HttpStatus.NOT_FOUND, httpResponse.getStatusCode());
	}

}

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

import java.util.stream.Stream;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Arjen Poutsma
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
	public void subrouteMatch() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = request -> Mono.just(handlerFunction);

		MockServerRequest request = MockServerRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(true);

		RouterFunction<ServerResponse> result = RouterFunctions.subroute(requestPredicate, routerFunction);
		assertNotNull(result);

		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		StepVerifier.create(resultHandlerFunction)
				.expectNext(handlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	public void subrouteNoMatch() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = request -> Mono.just(handlerFunction);

		MockServerRequest request = MockServerRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(false);

		RouterFunction<ServerResponse> result = RouterFunctions.subroute(requestPredicate, routerFunction);
		assertNotNull(result);

		Mono<HandlerFunction<ServerResponse>> resultHandlerFunction = result.route(request);
		StepVerifier.create(resultHandlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	public void toHttpHandler() throws Exception {
		HandlerStrategies strategies = mock(HandlerStrategies.class);
		when(strategies.messageReaders()).thenReturn(
				Stream::<HttpMessageReader<?>>empty);
		when(strategies.messageWriters()).thenReturn(
				Stream::<HttpMessageWriter<?>>empty);
		when(strategies.viewResolvers()).thenReturn(
				Stream::<ViewResolver>empty);

		ServerRequest request = mock(ServerRequest.class);
		ServerResponse response = mock(ServerResponse.class);
		when(response.writeTo(any(ServerWebExchange.class), eq(strategies))).thenReturn(Mono.empty());

		HandlerFunction<ServerResponse> handlerFunction = mock(HandlerFunction.class);
		when(handlerFunction.handle(any(ServerRequest.class))).thenReturn(Mono.just(response));

		RouterFunction<ServerResponse> routerFunction = mock(RouterFunction.class);
		when(routerFunction.route(any(ServerRequest.class))).thenReturn(Mono.just(handlerFunction));

		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(false);

		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction, strategies);
		assertNotNull(result);

		MockServerHttpRequest httpRequest = MockServerHttpRequest.get("http://localhost").build();
		MockServerHttpResponse serverHttpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, serverHttpResponse);
	}

}

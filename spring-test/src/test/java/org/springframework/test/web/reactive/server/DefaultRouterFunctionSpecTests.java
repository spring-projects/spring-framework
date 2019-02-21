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
package org.springframework.test.web.reactive.server;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Unit tests for {@link DefaultRouterFunctionSpec}.
 * @author Rossen Stoyanchev
 */
public class DefaultRouterFunctionSpecTests {

	@Test
	public void webFilter() {

		RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
				.GET("/", request -> ServerResponse.ok().build())
				.build();

		new DefaultRouterFunctionSpec(routerFunction)
				.handlerStrategies(HandlerStrategies.builder()
						.webFilter((exchange, chain) -> {
							exchange.getResponse().getHeaders().set("foo", "123");
							return chain.filter(exchange);
						})
						.build())
				.build()
				.get()
				.uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("foo", "123");
	}

	@Test
	public void exceptionHandler() {

		RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
				.GET("/error", request -> Mono.error(new IllegalStateException("boo")))
				.build();

		new DefaultRouterFunctionSpec(routerFunction)
				.handlerStrategies(HandlerStrategies.builder()
						.exceptionHandler((exchange, ex) -> {
							exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
							return Mono.empty();
						})
						.build())
				.build()
				.get()
				.uri("/error")
				.exchange()
				.expectStatus().isBadRequest();
	}
}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Tests for {@link DefaultRouterFunctionSpec}.
 *
 * @author Rossen Stoyanchev
 */
class DefaultRouterFunctionSpecTests {

	@Test
	void webFilter() {

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
	void exceptionHandler() {

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

/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.function.server.support;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

/**
 * @author Arjen Poutsma
 */
public class RouterFunctionMappingTests {

	private final ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://example.com/match"));

	private final ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();


	@Test
	public void normal() {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = request -> Mono.just(handlerFunction);

		RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
		mapping.setMessageReaders(this.codecConfigurer.getReaders());

		Mono<Object> result = mapping.getHandler(this.exchange);

		StepVerifier.create(result)
				.expectNext(handlerFunction)
				.expectComplete()
				.verify();
	}

	@Test
	public void noMatch() {
		RouterFunction<ServerResponse> routerFunction = request -> Mono.empty();
		RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
		mapping.setMessageReaders(this.codecConfigurer.getReaders());

		Mono<Object> result = mapping.getHandler(this.exchange);

		StepVerifier.create(result)
				.expectComplete()
				.verify();
	}

	@Test
	public void changeParser() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
				.GET("/foo", handlerFunction)
				.POST("/bar", handlerFunction)
				.build();

		RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
		mapping.setMessageReaders(this.codecConfigurer.getReaders());
		mapping.setUseCaseSensitiveMatch(false);
		mapping.afterPropertiesSet();

		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://example.com/FOO"));
		Mono<Object> result = mapping.getHandler(exchange);

		StepVerifier.create(result)
				.expectNext(handlerFunction)
				.verifyComplete();

	}

}

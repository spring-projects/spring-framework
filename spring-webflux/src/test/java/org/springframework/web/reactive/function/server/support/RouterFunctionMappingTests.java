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

package org.springframework.web.reactive.function.server.support;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author Arjen Poutsma
 */
public class RouterFunctionMappingTests {


	private List<HttpMessageReader<?>> messageReaders;

	private ServerWebExchange exchange;

	private ServerCodecConfigurer codecConfigurer;

	@Before
	public void setUp() {
		this.messageReaders = Collections.singletonList(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		this.exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://example.com/match"));
		codecConfigurer = ServerCodecConfigurer.create();

	}

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

	@Configuration
	@EnableWebFlux
	private static class TestConfig {

		public RouterFunction<ServerResponse> match() {
			HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
			return RouterFunctions.route(RequestPredicates.GET("/match"), handlerFunction);
		}

		public RouterFunction<ServerResponse> noMatch() {
			HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
			RouterFunction<ServerResponse> routerFunction = request -> Mono.empty();
			return RouterFunctions.route(RequestPredicates.GET("/no-match"), handlerFunction);
		}

	}
}
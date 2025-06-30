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

package org.springframework.web.reactive.function.server.support;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.reactive.config.ApiVersionConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RequestPredicates.version;

/**
 * {@link RouterFunctionMapping} integration tests for API versioning.
 * @author Rossen Stoyanchev
 */
public class RouterFunctionMappingVersionTests {

	private RouterFunctionMapping mapping;


	@BeforeEach
	void setUp() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();

		this.mapping = wac.getBean(RouterFunctionMapping.class);
	}


	@Test
	void mapVersion() {
		testGetHandler("1.0", "none");
		testGetHandler("1.1", "none");
		testGetHandler("1.2", "1.2");
		testGetHandler("1.3", "1.2");
		testGetHandler("1.5", "1.5");
	}

	private void testGetHandler(String version, String expectedBody) {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/").header("X-API-Version", version));

		Mono<?> result = this.mapping.getHandler(exchange);

		StepVerifier.create(result)
				.consumeNextWith(handler -> assertThat(((TestHandler) handler).body()).isEqualTo(expectedBody))
				.verifyComplete();
	}

	@Test
	void deprecation() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/").header("X-API-Version", "1"));

		Mono<?> result = this.mapping.getHandler(exchange);

		StepVerifier.create(result)
				.consumeNextWith(handler -> {
					assertThat(((TestHandler) handler).body()).isEqualTo("none");
					assertThat(exchange.getResponse().getHeaders().getFirst("Link"))
							.isEqualTo("<https://example.org/deprecation>; rel=\"deprecation\"; type=\"text/html\"");
				})
				.verifyComplete();
	}


	@EnableWebFlux
	private static class WebConfig implements WebFluxConfigurer {

		@Override
		public void configureApiVersioning(ApiVersionConfigurer configurer) {

			StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
			handler.configureVersion("1").setDeprecationLink(URI.create("https://example.org/deprecation"));

			configurer.useRequestHeader("X-API-Version")
					.addSupportedVersions("1", "1.1", "1.3")
					.setDeprecationHandler(handler);
		}

		@Bean
		RouterFunction<?> routerFunction() {
			return RouterFunctions.route()
					.path("/", builder -> builder
							.GET(version("1.5"), new TestHandler("1.5"))
							.GET(version("1.2+"), new TestHandler("1.2"))
							.GET(new TestHandler("none")))
					.build();
		}
	}


	private record TestHandler(String body) implements HandlerFunction<ServerResponse> {

		@Override
		public Mono<ServerResponse> handle(ServerRequest request) {
			return ServerResponse.ok().bodyValue(body);
		}
	}

}

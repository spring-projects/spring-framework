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

package org.springframework.test.web.reactive.server.samples.bind;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * Sample tests demonstrating "mock" server tests binding to server infrastructure
 * declared in a Spring ApplicationContext.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
@SpringJUnitConfig
class ApplicationContextTests {

	private static final String SSL_SESSION_ID = "sslSessionId";


	@Autowired
	ApplicationContext context;


	@Test
	void buildWithDefaults() {
		var client = WebTestClient.bindToApplicationContext(context).build();

		client.get().uri("/test")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}

	@Test  // gh-35042
	void buildWithSslInfo() {
		var client = WebTestClient.bindToApplicationContext(context)
				.sslInfo(SslInfo.from("test123"))
				.webFilter(new SslSessionIdFilter())
				.build();

		client.get().uri("/sslInfo")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Session ID: test123");
	}


	@Configuration
	@EnableWebFlux
	@Import(TestController.class)
	static class WebFluxConfig {
	}

	@RestController
	static class TestController {

		@GetMapping("/test")
		String test() {
			return "It works!";
		}

		@GetMapping("/sslInfo")
		String sslInfo(ServerHttpRequest request) {
			return "Session ID: " + request.getAttributes().get(SSL_SESSION_ID);
		}
	}

	private static class SslSessionIdFilter implements WebFilter {

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
			var request = exchange.getRequest();
			request.getAttributes().put(SSL_SESSION_ID, request.getSslInfo().getSessionId());
			return chain.filter(exchange);
		}
	}

}

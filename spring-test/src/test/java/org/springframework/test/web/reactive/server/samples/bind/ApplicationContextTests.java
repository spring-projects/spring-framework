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

package org.springframework.test.web.reactive.server.samples.bind;

import java.security.Principal;
import java.util.function.UnaryOperator;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;

import static org.junit.Assert.assertEquals;

/**
 * Binding to server infrastructure declared in a Spring ApplicationContext.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ApplicationContextTests {

	private WebTestClient client;


	@Before
	public void setUp() throws Exception {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(WebConfig.class);
		context.refresh();

		this.client = WebTestClient.bindToApplicationContext(context)
				.exchangeMutator(principal("Pablo"))
				.webFilter(prefixFilter("Mr."))
				.build();
	}


	@Test
	public void bodyContent() throws Exception {
		this.client.get().uri("/principal")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Hello Mr. Pablo!");
	}

	@Test
	public void bodyContentWithConsumer() throws Exception {
		this.client.get().uri("/principal")
				.exchange()
				.expectStatus().isOk()
				.expectBody().consumeAsStringWith(body -> assertEquals("Hello Mr. Pablo!", body));
	}

	@Test
	public void perRequestExchangeMutator() throws Exception {
		this.client.exchangeMutator(principal("Giovanni"))
				.get().uri("/principal")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Hello Mr. Giovanni!");
	}

	@Test
	public void perRequestMultipleExchangeMutators() throws Exception {
		this.client
				.exchangeMutator(attribute("attr1", "foo"))
				.exchangeMutator(attribute("attr2", "bar"))
				.get().uri("/attributes")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("foo+bar");
	}


	private UnaryOperator<ServerWebExchange> principal(String userName) {
		return exchange -> exchange.mutate().principal(Mono.just(new TestUser(userName))).build();
	}

	private WebFilter prefixFilter(String prefix) {
		return (exchange, chain) -> {
			Mono<Principal> user = exchange.getPrincipal().map(p -> new TestUser(prefix + " " + p.getName()));
			return chain.filter(exchange.mutate().principal(user).build());
		};
	}

	private UnaryOperator<ServerWebExchange> attribute(String attrName, String attrValue) {
		return exchange -> {
			exchange.getAttributes().put(attrName, attrValue);
			return exchange;
		};
	}


	@Configuration
	@EnableWebFlux
	static class WebConfig {

		@Bean
		public TestController controller() {
			return new TestController();
		}

	}

	@RestController
	static class TestController {

		@GetMapping("/principal")
		public String handle(Principal principal) {
			return "Hello " + principal.getName() + "!";
		}

		@GetMapping("/attributes")
		public String handle(@RequestAttribute String attr1, @RequestAttribute String attr2) {
			return attr1 + "+" + attr2;
		}
	}

	private static class TestUser implements Principal {

		private final String name;

		TestUser(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}
	}

}

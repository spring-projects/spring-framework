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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
				.build();
	}

	private UnaryOperator<ServerWebExchange> principal(String userName) {
		return exchange -> {
			Principal user = mock(Principal.class);
			when(user.getName()).thenReturn(userName);
			return exchange.mutate().principal(Mono.just(user)).build();
		};
	}


	@Test
	public void basic() throws Exception {
		this.client.get().uri("/principal")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value().isEqualTo("Hello Pablo!");
	}

	@Test
	public void perRequestExchangeMutator() throws Exception {
		this.client.exchangeMutator(principal("Giovanni"))
				.get().uri("/principal")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value().isEqualTo("Hello Giovanni!");
	}

	@Test
	public void perRequestMultipleExchangeMutators() throws Exception {
		this.client
				.exchangeMutator(attribute("attr1", "foo"))
				.exchangeMutator(attribute("attr2", "bar"))
				.get().uri("/attributes")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).value().isEqualTo("foo+bar");
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

}

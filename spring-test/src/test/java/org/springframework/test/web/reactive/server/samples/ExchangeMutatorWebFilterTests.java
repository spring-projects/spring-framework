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
package org.springframework.test.web.reactive.server.samples;

import java.security.Principal;
import java.util.function.UnaryOperator;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.test.web.reactive.server.ExchangeMutatorWebFilter;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/**
 * Samples tests that demonstrate applying ServerWebExchange initialization.
 * @author Rossen Stoyanchev
 */
public class ExchangeMutatorWebFilterTests {

	private ExchangeMutatorWebFilter exchangeMutator;

	private WebTestClient webTestClient;


	@Before
	public void setUp() throws Exception {

		this.exchangeMutator = new ExchangeMutatorWebFilter(userIdentity("Pablo"));

		this.webTestClient = WebTestClient.bindToController(new TestController())
				.webFilter(this.exchangeMutator)
				.build();
	}

	@Test
	public void globalMutator() throws Exception {
		this.webTestClient.get().uri("/userIdentity")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Hello Pablo!");
	}

	@Test
	public void perRequestMutators() throws Exception {
		this.webTestClient
				.filter(this.exchangeMutator.perClient(userIdentity("Giovanni")))
				.get().uri("/userIdentity")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Hello Giovanni!");
	}


	private UnaryOperator<ServerWebExchange> userIdentity(String userName) {
		return exchange -> exchange.mutate().principal(Mono.just(new TestUser(userName))).build();
	}


	@RestController
	static class TestController {

		@GetMapping("/userIdentity")
		public String handle(Principal principal) {
			return "Hello " + principal.getName() + "!";
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

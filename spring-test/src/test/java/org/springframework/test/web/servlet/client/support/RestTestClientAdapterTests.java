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

package org.springframework.test.web.servlet.client.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RestTestClientAdapter}.
 *
 * @author Devendra Reddy Pennabadi
 */
class RestTestClientAdapterTests {

	private TestService service;


	@BeforeEach
	void setUp() {
		RestTestClient client = RestTestClient.bindToController(new TestController()).build();
		RestTestClientAdapter adapter = RestTestClientAdapter.create(client);
		this.service = HttpServiceProxyFactory.builderFor(adapter).build().createClient(TestService.class);
	}


	@Test
	void getAsString() {
		String result = this.service.getGreeting();
		assertThat(result).isEqualTo("Hello Spring!");
	}

	@Test
	void getAsResponseEntity() {
		ResponseEntity<String> entity = this.service.getGreetingEntity();
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello Spring!");
	}

	@Test
	void getWithPathVariable() {
		String result = this.service.getGreetingById("42");
		assertThat(result).isEqualTo("Hello 42");
	}

	@Test
	void postBody() {
		String result = this.service.echo(new Greeting("Spring"));
		assertThat(result).isEqualTo("echo: Spring");
	}

	@Test
	void requestHeader() {
		String result = this.service.getWithHeader("custom-value");
		assertThat(result).isEqualTo("header: custom-value");
	}


	private interface TestService {

		@GetExchange("/greeting")
		String getGreeting();

		@GetExchange("/greeting")
		ResponseEntity<String> getGreetingEntity();

		@GetExchange("/greeting/{id}")
		String getGreetingById(@PathVariable String id);

		@PostExchange("/echo")
		String echo(@RequestBody Greeting body);

		@GetExchange("/with-header")
		String getWithHeader(@RequestHeader("X-Custom") String value);
	}


	@RestController
	static class TestController {

		@GetMapping("/greeting")
		String greeting() {
			return "Hello Spring!";
		}

		@GetMapping("/greeting/{id}")
		String greetingById(@PathVariable String id) {
			return "Hello " + id;
		}

		@PostMapping("/echo")
		String echo(@RequestBody Greeting body) {
			return "echo: " + body.name();
		}

		@GetMapping("/with-header")
		String withHeader(@RequestHeader("X-Custom") String value) {
			return "header: " + value;
		}
	}


	record Greeting(String name) {
	}

}

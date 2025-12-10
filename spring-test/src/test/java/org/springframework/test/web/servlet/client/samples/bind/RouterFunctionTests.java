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

package org.springframework.test.web.servlet.client.samples.bind;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Sample tests demonstrating "mock" server tests binding to a RouterFunction.
 *
 * @author Rob Worsnop
 */
class RouterFunctionTests {

	private RestTestClient testClient;


	@BeforeEach
	void setUp() {
		RouterFunction<?> route = route(GET("/test"), request -> ServerResponse.ok().body("It works!"));
		this.testClient = RestTestClient.bindToRouterFunction(route).build();
	}

	@Test
	void test() {
		this.testClient.get().uri("/test")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}

	@Test
	void testEntityExchangeResult() {
		EntityExchangeResult<byte[]> result = this.testClient.get().uri("/test")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.returnResult();

		assertThat(result.getResponseBody()).isEqualTo("It works!".getBytes(StandardCharsets.UTF_8));
	}

}

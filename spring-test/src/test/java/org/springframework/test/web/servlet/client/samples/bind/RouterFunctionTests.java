/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void entityExchangeResult() {
		EntityExchangeResult<byte[]> result = this.testClient.get().uri("/test")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.returnResult();

		assertThat(result.getResponseBody()).isEqualTo("It works!".getBytes(StandardCharsets.UTF_8));
	}

}

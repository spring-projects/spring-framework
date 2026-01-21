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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.ReactorHttpServer;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Sample tests demonstrating live server integration tests.
 *
 * @author Rob Worsnop
 */
class HttpServerTests {

	private ReactorHttpServer server;

	private RestTestClient client;


	@BeforeEach
	void start() throws Exception {

		HttpHandler httpHandler = RouterFunctions.toHttpHandler(
				route(GET("/test"), request -> ServerResponse.ok().bodyValue("It works!")));

		this.server = new ReactorHttpServer();
		this.server.setHandler(httpHandler);
		this.server.afterPropertiesSet();
		this.server.start();

		this.client = RestTestClient.bindToServer()
				.baseUrl("http://localhost:" + this.server.getPort())
				.build();
	}

	@AfterEach
	void stop() {
		this.server.stop();
	}


	@Test
	void test() {
		this.client.get().uri("/test")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}

}

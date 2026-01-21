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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sample tests demonstrating "mock" server tests binding to an annotated
 * controller.
 *
 * @author Rob Worsnop
 */
class ControllerTests {

	private RestTestClient client;


	@BeforeEach
	void setUp() {
		this.client = RestTestClient.bindToController(new TestController()).build();
	}


	@Test
	void test() {
		this.client.get().uri("/test")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}


	@RestController
	static class TestController {

		@GetMapping("/test")
		public String handle() {
			return "It works!";
		}
	}

}

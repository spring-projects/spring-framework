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

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests with custom headers.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unused")
public class HeaderTests {

	private WebTestClient client;


	@Before
	public void setUp() throws Exception {
		this.client = WebTestClient
				.bindToController(new TestController())
				.configureClient().baseUrl("/header")
				.build();
	}


	@Test
	public void requestResponseHeaderPair() throws Exception {
		this.client.get().uri("/request-response-pair").header("h1", "in")
				.exchange()
				.expectNoBody()
				.assertThat()
				.status().isOk()
				.header().valueEquals("h1", "in-out");
	}

	@Test
	public void headerMultivalue() throws Exception {
		this.client.get().uri("/multivalue")
				.exchange()
				.expectNoBody()
				.assertThat()
				.status().isOk()
				.header().valueEquals("h1", "v1", "v2", "v3");
	}


	@RestController
	@RequestMapping("header")
	static class TestController {

		@GetMapping("request-response-pair")
		ResponseEntity<Void> handleHeader(@RequestHeader("h1") String myHeader) {
			String value = myHeader + "-out";
			return ResponseEntity.ok().header("h1", value).build();
		}

		@GetMapping("multivalue")
		ResponseEntity<Void> multivalue() {
			return ResponseEntity.ok().header("h1", "v1", "v2", "v3").build();
		}
	}

}

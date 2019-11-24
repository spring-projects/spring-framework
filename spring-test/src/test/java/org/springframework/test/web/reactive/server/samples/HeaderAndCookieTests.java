/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.web.reactive.server.samples;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests with headers and cookies.
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HeaderAndCookieTests {

	private final WebTestClient client = WebTestClient.bindToController(new TestController()).build();


	@Test
	public void requestResponseHeaderPair() throws Exception {
		this.client.get().uri("/header-echo").header("h1", "in")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("h1", "in-out");
	}

	@Test
	public void headerMultipleValues() throws Exception {
		this.client.get().uri("/header-multi-value")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("h1", "v1", "v2", "v3");
	}

	@Test
	public void setCookies() {
		this.client.get().uri("/cookie-echo")
				.cookies(cookies -> cookies.add("k1", "v1"))
				.exchange()
				.expectHeader().valueMatches("Set-Cookie", "k1=v1");
	}


	@RestController
	static class TestController {

		@GetMapping("header-echo")
		ResponseEntity<Void> handleHeader(@RequestHeader("h1") String myHeader) {
			String value = myHeader + "-out";
			return ResponseEntity.ok().header("h1", value).build();
		}

		@GetMapping("header-multi-value")
		ResponseEntity<Void> multiValue() {
			return ResponseEntity.ok().header("h1", "v1", "v2", "v3").build();
		}

		@GetMapping("cookie-echo")
		ResponseEntity<Void> handleCookie(@CookieValue("k1") String cookieValue) {
			HttpHeaders headers = new HttpHeaders();
			headers.set("Set-Cookie", "k1=" + cookieValue);
			return new ResponseEntity<>(headers, HttpStatus.OK);
		}
	}

}

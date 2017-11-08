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

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests with error status codes or error conditions.
 *
 * @author Anton Bondarenko
 * @since 5.0
 */
public class CookieTests {

	private final WebTestClient client = WebTestClient
			.bindToController(new TestController())
			.configureClient().baseUrl("/cookie")
			.build();

	@Test
	public void setCookies() {
		this.client.get().uri("/send-cookies")
				.cookies(cookies -> cookies.add("k1", "v1"))
				.exchange()
				.expectHeader().valueMatches("Set-Cookie", "k1=v1");
	}

	@RestController
	@RequestMapping("cookie")
	static class TestController {

		@GetMapping("send-cookies")
		ResponseEntity<Void> handleCookie(@CookieValue("k1") String cookieValue) {
			HttpHeaders headers = new HttpHeaders();
			headers.set("Set-Cookie", "k1=" + cookieValue);
			return new ResponseEntity<>(headers, HttpStatus.OK);
		}

	}

}

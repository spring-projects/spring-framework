/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class HeaderAndCookieTests {

	private final WebTestClient client = WebTestClient.bindToController(new TestController()).build();


	@Test
	void requestResponseHeaderPair() {
		this.client.get().uri("/header-echo").header("h1", "in")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("h1", "in-out");
	}

	@Test
	void headerMultipleValues() {
		this.client.get().uri("/header-multi-value")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("h1", "v1", "v2", "v3");
	}

	@Test
	void setCookies() {
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

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

package org.springframework.test.web.servlet.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests using the {@link RestTestClient} API.
 */
class RestTestClientTests {

	private RestTestClient client;


	@BeforeEach
	void setUp() {
		this.client = RestTestClient.bindToController(new TestController()).build();
	}


	@Nested
	class HttpMethods {

		@ParameterizedTest
		@ValueSource(strings = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD"})
		void testMethod(String method) {
			RestTestClientTests.this.client.method(HttpMethod.valueOf(method)).uri("/test")
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.method").isEqualTo(method);
		}

		@Test
		void testGet() {
			RestTestClientTests.this.client.get().uri("/test")
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.method").isEqualTo("GET");
		}

		@Test
		void testPost() {
			RestTestClientTests.this.client.post().uri("/test")
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.method").isEqualTo("POST");
		}

		@Test
		void testPut() {
			RestTestClientTests.this.client.put().uri("/test")
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.method").isEqualTo("PUT");
		}

		@Test
		void testDelete() {
			RestTestClientTests.this.client.delete().uri("/test")
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.method").isEqualTo("DELETE");
		}

		@Test
		void testPatch() {
			RestTestClientTests.this.client.patch().uri("/test")
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.method").isEqualTo("PATCH");
		}

		@Test
		void testHead() {
			RestTestClientTests.this.client.head().uri("/test")
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.method").isEqualTo("HEAD");
		}

		@Test
		void testOptions() {
			RestTestClientTests.this.client.options().uri("/test")
					.exchange()
					.expectStatus().isOk()
					.expectHeader().valueEquals("Allow", "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS")
					.expectBody().isEmpty();
		}

	}


	@Nested
	class Mutation {

		@Test
		void test() {
			RestTestClientTests.this.client.mutate()
					.defaultHeader("foo", "bar")
					.uriBuilderFactory(new DefaultUriBuilderFactory("/test"))
					.defaultCookie("foo", "bar")
					.defaultCookies(cookies -> cookies.add("a", "b"))
					.defaultHeaders(headers -> headers.set("a", "b"))
					.build().get()
					.exchange()
					.expectStatus().isOk()
					.expectBody()
					.jsonPath("$.uri").isEqualTo("/test")
					.jsonPath("$.headers.Cookie").isEqualTo("foo=bar; a=b")
					.jsonPath("$.headers.foo").isEqualTo("bar")
					.jsonPath("$.headers.a").isEqualTo("b");
		}
	}


	@Nested
	class Uris {

		@Test
		void test() {
			RestTestClientTests.this.client.get().uri("/test")
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.uri").isEqualTo("/test");
		}

		@Test
		void testWithPathVariables() {
			RestTestClientTests.this.client.get().uri("/test/{id}", 1)
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.uri").isEqualTo("/test/1");
		}

		@Test
		void testWithParameterMap() {
			RestTestClientTests.this.client.get().uri("/test/{id}", Map.of("id", 1))
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.uri").isEqualTo("/test/1");
		}

		@Test
		void testWithUrlBuilder() {
			RestTestClientTests.this.client.get().uri(builder -> builder.path("/test/{id}").build(1))
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.uri").isEqualTo("/test/1");
		}

		@Test
		void testURI() {
			RestTestClientTests.this.client.get().uri(URI.create("/test"))
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.uri").isEqualTo("/test");
		}
	}


	@Nested
	class Cookies {
		@Test
		void testCookie() {
			RestTestClientTests.this.client.get().uri("/test")
					.cookie("foo", "bar")
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.headers.Cookie").isEqualTo("foo=bar");
		}

		@Test
		void testCookies() {
			RestTestClientTests.this.client.get().uri("/test")
					.cookies(cookies -> cookies.add("foo", "bar"))
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.headers.Cookie").isEqualTo("foo=bar");
		}
	}


	@Nested
	class Headers {
		@Test
		void testHeader() {
			RestTestClientTests.this.client.get().uri("/test")
					.header("foo", "bar")
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.headers.foo").isEqualTo("bar");
		}

		@Test
		void testHeaders() {
			RestTestClientTests.this.client.get().uri("/test")
					.headers(headers -> headers.set("foo", "bar"))
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.headers.foo").isEqualTo("bar");
		}

		@Test
		void testContentType() {
			RestTestClientTests.this.client.post().uri("/test")
					.contentType(MediaType.APPLICATION_JSON)
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.headers.Content-Type").isEqualTo("application/json");
		}

		@Test
		void testAcceptCharset() {
			RestTestClientTests.this.client.get().uri("/test")
					.acceptCharset(StandardCharsets.UTF_8)
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.headers.Accept-Charset").isEqualTo("utf-8");
		}

		@Test
		void testIfModifiedSince() {
			RestTestClientTests.this.client.get().uri("/test")
					.ifModifiedSince(ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT")))
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.headers.If-Modified-Since").isEqualTo("Thu, 01 Jan 1970 00:00:00 GMT");
		}

		@Test
		void testIfNoneMatch() {
			RestTestClientTests.this.client.get().uri("/test")
					.ifNoneMatch("foo")
					.exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$.headers.If-None-Match").isEqualTo("foo");
		}
	}


	@Nested
	class Expectations {
		@Test
		void testExpectCookie() {
			RestTestClientTests.this.client.get().uri("/test")
					.exchange()
					.expectCookie().value("session", Matchers.equalTo("abc"));
		}
	}


	@Nested
	class ReturnResults {
		@Test
		void testBodyReturnResult() {
			var result = RestTestClientTests.this.client.get().uri("/test")
					.exchange()
					.expectStatus().isOk()
					.expectBody(Map.class).returnResult();
			assertThat(result.getResponseBody().get("uri")).isEqualTo("/test");
		}

		@Test
		void testReturnResultClass() {
			var result = RestTestClientTests.this.client.get().uri("/test")
					.exchange()
					.expectStatus().isOk()
					.returnResult(Map.class);
			assertThat(result.getResponseBody().get("uri")).isEqualTo("/test");
		}

		@Test
		void testReturnResultParameterizedTypeReference() {
			var result = RestTestClientTests.this.client.get().uri("/test")
					.exchange()
					.expectStatus().isOk()
					.returnResult(new ParameterizedTypeReference<Map<String, Object>>() {
					});
			assertThat(result.getResponseBody().get("uri")).isEqualTo("/test");
		}
	}


	@RestController
	static class TestController {

		@RequestMapping(path = {"/test", "/test/*"}, produces = "application/json")
		public Map<String, Object> handle(
				@RequestHeader HttpHeaders headers,
				HttpServletRequest request, HttpServletResponse response) {
			response.addCookie(new Cookie("session", "abc"));
			return Map.of(
					"method", request.getMethod(),
					"uri", request.getRequestURI(),
					"headers", headers.toSingleValueMap()
			);
		}
	}
}

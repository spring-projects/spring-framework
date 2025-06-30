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

package org.springframework.mock.http.server.reactive;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.internal.util.MockUtil;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Named.named;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MockServerHttpRequest}.
 *
 * @author Rossen Stoyanchev
 */
class MockServerHttpRequestTests {

	@Test
	void cookieHeaderSet() {
		HttpCookie foo11 = new HttpCookie("foo1", "bar1");
		HttpCookie foo12 = new HttpCookie("foo1", "bar2");
		HttpCookie foo21 = new HttpCookie("foo2", "baz1");
		HttpCookie foo22 = new HttpCookie("foo2", "baz2");

		MockServerHttpRequest request = MockServerHttpRequest.get("/")
				.cookie(foo11, foo12, foo21, foo22).build();

		assertThat(request.getCookies().get("foo1")).isEqualTo(Arrays.asList(foo11, foo12));
		assertThat(request.getCookies().get("foo2")).isEqualTo(Arrays.asList(foo21, foo22));
		assertThat(request.getHeaders().get(HttpHeaders.COOKIE)).isEqualTo(Arrays.asList("foo1=bar1", "foo1=bar2", "foo2=baz1", "foo2=baz2"));
	}

	@Test
	void queryParams() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/foo bar?a=b")
				.queryParam("name A", "value A1", "value A2")
				.queryParam("name B", "value B1")
				.build();

		assertThat(request.getURI().toString()).isEqualTo("/foo%20bar?a=b&name%20A=value%20A1&name%20A=value%20A2&name%20B=value%20B1");
	}

	/**
	 * Ensure that {@code sslInfo()} can be used with the fluent builder pattern.
	 */
	@Test  // gh-35075
	void sslInfo() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/test")
				.sslInfo(mock())
				.build();

		assertThat(request.getSslInfo()).as("is mock").satisfies(sslInfo -> MockUtil.isMock(sslInfo));
		assertThat(request.getURI().toString()).isEqualTo("/test");
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource
	void httpMethodNotNullOrEmpty(ThrowingCallable callable) {
		assertThatIllegalArgumentException()
			.isThrownBy(callable)
			.withMessageContaining("HTTP method is required.");
	}

	static Stream<Named<ThrowingCallable>> httpMethodNotNullOrEmpty() {
		String uriTemplate = "/foo bar?a=b";
		URI uri = UriComponentsBuilder.fromUriString(uriTemplate).build().toUri();
		return Stream.of(
				named("null HttpMethod, URI", () -> MockServerHttpRequest.method(null, uri).build()),
				named("null HttpMethod, uriTemplate", () -> MockServerHttpRequest.method(null, uriTemplate).build())
		);
	}

}

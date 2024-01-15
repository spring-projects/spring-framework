/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockServerHttpResponse}.
 *
 * @author Rossen Stoyanchev
 */
class MockServerHttpResponseTests {

	@Test
	void cookieHeaderSet() {

		ResponseCookie foo11 = ResponseCookie.from("foo1", "bar1").build();
		ResponseCookie foo12 = ResponseCookie.from("foo1", "bar2").build();
		ResponseCookie foo21 = ResponseCookie.from("foo2", "baz1").build();
		ResponseCookie foo22 = ResponseCookie.from("foo2", "baz2").build();

		MockServerHttpResponse response = new MockServerHttpResponse();
		response.addCookie(foo11);
		response.addCookie(foo12);
		response.addCookie(foo21);
		response.addCookie(foo22);

		response.applyCookies();

		assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isEqualTo(Arrays.asList("foo1=bar1", "foo1=bar2", "foo2=baz1", "foo2=baz2"));
	}

}

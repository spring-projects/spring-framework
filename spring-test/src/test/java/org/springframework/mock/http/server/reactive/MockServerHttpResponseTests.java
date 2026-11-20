/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

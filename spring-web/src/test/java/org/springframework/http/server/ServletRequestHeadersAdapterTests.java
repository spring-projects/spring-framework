/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Unit tests for {@link ServletRequestHeadersAdapter}.
 */
class ServletRequestHeadersAdapterTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MultiValueMap<String, String> headersAdapter = ServletRequestHeadersAdapter.create(request);


	@Test // gh-36418
	void caseSensitiveOverride() {
		request.addHeader("foo", "value");
		headersAdapter.set("Foo", "override value");

		assertThat(headersAdapter.size()).isEqualTo(1);
		assertThat(headersAdapter.keySet()).containsExactly("Foo");
		assertThat(headersAdapter.getFirst("foo")).isEqualTo("override value");
		assertThat(headersAdapter.get("foo")).containsExactly("override value");
	}

	@Test
	void addAppendsToRequestValues() {
		request.addHeader("Accept", "text/plain");
		headersAdapter.add("Accept", "application/json");
		headersAdapter.addAll("accept", List.of("text/html"));

		assertThat(headersAdapter.get("Accept")).containsExactly("text/plain", "application/json", "text/html");
		assertThat(headersAdapter.getFirst("Accept")).isEqualTo("text/plain");
		assertThat(headersAdapter.size()).isEqualTo(1);
	}

	@Test
	void removeRequestHeader() {
		request.addHeader("X-Trace", "abc");
		request.addHeader("Accept", "text/plain");

		assertThat(headersAdapter.remove("x-trace")).containsExactly("abc");

		assertThat(headersAdapter.containsKey("X-Trace")).isFalse();
		assertThat(headersAdapter.get("X-Trace")).isNull();
		assertThat(headersAdapter.getFirst("X-Trace")).isNull();
		assertThat(headersAdapter.keySet()).containsExactly("Accept");
		assertThat(headersAdapter.size()).isEqualTo(1);
		assertThat(headersAdapter.toSingleValueMap()).containsExactly(entry("Accept", "text/plain"));
		assertThat(headersAdapter.remove("X-Trace")).isNull();
	}

	@Test
	void addAfterRemoveDoesNotRestoreRequestValues() {
		request.addHeader("X-Trace", "abc");
		headersAdapter.remove("X-Trace");
		headersAdapter.add("X-Trace", "def");

		assertThat(headersAdapter.get("X-Trace")).containsExactly("def");
		assertThat(headersAdapter.size()).isEqualTo(1);
	}

	@Test
	void putReturnsPreviousRequestValues() {
		request.addHeader("X-Trace", "abc");

		assertThat(headersAdapter.put("X-Trace", List.of("def"))).containsExactly("abc");
		assertThat(headersAdapter.get("X-Trace")).containsExactly("def");
	}

	@Test
	void toSingleValueMapWithEmptyOverride() {
		request.addHeader("Accept", "text/plain");
		headersAdapter.put("X-Empty", List.of());

		assertThat(headersAdapter.toSingleValueMap()).containsExactly(entry("Accept", "text/plain"));
	}

	@Test
	void toSingleValueMapWithNullOverride() {
		request.addHeader("Accept", "text/plain");
		headersAdapter.set("X-Test", null);

		assertThat(headersAdapter.toSingleValueMap())
				.containsExactly(entry("Accept", "text/plain"), entry("X-Test", null));
	}

	@Test
	void clear() {
		request.addHeader("X-Trace", "abc");
		request.addHeader("Accept", "text/plain");
		headersAdapter.set("X-Override", "value");

		headersAdapter.clear();

		assertThat(headersAdapter.isEmpty()).isTrue();
		assertThat(headersAdapter.size()).isZero();
		assertThat(headersAdapter.keySet()).isEmpty();
		assertThat(headersAdapter.containsKey("Accept")).isFalse();

		headersAdapter.add("Accept", "application/json");
		assertThat(headersAdapter.get("Accept")).containsExactly("application/json");
	}

	@Test
	void mutationsThroughServletServerHttpRequest() {
		request.addHeader("Accept", "text/plain");
		request.addHeader("X-Trace", "abc");
		HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();

		headers.remove("X-Trace");
		headers.add("Accept", "application/json");

		assertThat(headers.containsHeader("X-Trace")).isFalse();
		assertThat(headers.get("Accept")).containsExactly("text/plain", "application/json");
	}

}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ServletResponseHeadersAdapter}.
 *
 * @author Rossen Stoyanchev
 */
class ServletResponseHeadersAdapterTests {

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final ServletResponseHeadersAdapter headersAdapter = new ServletResponseHeadersAdapter(this.response);


	@Test
	void getNoValue() {
		assertThat(this.headersAdapter.get("My-Header")).isNull();
	}

	@Test
	void getContentType() {
		this.response.setContentType("text/plain");

		assertThat(this.headersAdapter.get(HttpHeaders.CONTENT_TYPE)).containsExactly("text/plain");
	}

	@Test
	void getSingleValue() {
		this.response.addHeader("My-Header", "a");

		assertThat(this.headersAdapter.get("My-Header")).containsExactly("a");
	}

	@Test
	void getMultipleValues() {
		this.response.addHeader("My-Header", "a");
		this.response.addHeader("My-Header", "b");

		assertThat(this.headersAdapter.get("My-Header")).containsExactly("a", "b");
	}

	@Test
	void getMultipleValuesWithSet() {
		Set<String> values = new LinkedHashSet<>(List.of("a", "b"));
		HttpServletResponse servletResponse = mock();
		when(servletResponse.getHeaders("My-Header")).thenReturn(values);

		List<String> result = new ServletResponseHeadersAdapter(servletResponse).get("My-Header");

		assertThat(result).containsExactly("a", "b");
	}

	@Test
	void remove() {
		this.response.addHeader("My-Header", "a");
		this.response.addHeader("My-Header", "b");

		assertThat(this.headersAdapter.remove("My-Header")).containsExactly("a", "b");
		assertThat(this.response.containsHeader("My-Header")).isFalse();
	}

}

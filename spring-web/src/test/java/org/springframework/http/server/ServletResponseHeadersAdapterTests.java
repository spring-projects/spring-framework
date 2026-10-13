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

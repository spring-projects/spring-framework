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

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ServletRequestHeadersAdapter}.
 */
public class ServletRequestHeadersAdapterTests {

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

	@Test // gh-36426
	void contentTypeCharsetAppendedForTextType() {
		request.setContentType("text/plain");
		request.setCharacterEncoding("UTF-8");

		assertThat(headersAdapter.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/plain;charset=UTF-8");
		assertThat(headersAdapter.get(HttpHeaders.CONTENT_TYPE)).containsExactly("text/plain;charset=UTF-8");
	}

	@Test // gh-36426
	void contentTypeCharsetNotAppendedForApplicationJson() {
		request.setContentType("application/json");
		request.setCharacterEncoding("UTF-8");

		assertThat(headersAdapter.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json");
		assertThat(headersAdapter.get(HttpHeaders.CONTENT_TYPE)).containsExactly("application/json");
	}

	@Test // gh-36426
	void contentTypeCharsetNotAppendedWhenAlreadyPresent() {
		request.setContentType("text/plain;charset=ISO-8859-1");
		request.setCharacterEncoding("UTF-8");

		assertThat(headersAdapter.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/plain;charset=ISO-8859-1");
		assertThat(headersAdapter.get(HttpHeaders.CONTENT_TYPE)).containsExactly("text/plain;charset=ISO-8859-1");
	}

	@Test // gh-36426
	void contentTypeCharsetNotAppendedWhenNoEncoding() {
		request.setContentType("text/plain");

		assertThat(headersAdapter.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/plain");
		assertThat(headersAdapter.get(HttpHeaders.CONTENT_TYPE)).containsExactly("text/plain");
	}
}

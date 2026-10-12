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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

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

	@Test // gh-36426
	void contentTypeWithCharacterEncoding() {
		request.setContentType("application/x-www-form-urlencoded");
		MultiValueMap<String, String> adapter = createAdapter("UTF-8");

		String expected = "application/x-www-form-urlencoded;charset=UTF-8";
		assertThat(adapter.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo(expected);
		assertThat(adapter.getFirst("content-type")).isEqualTo(expected);
		assertThat(adapter.get("CONTENT-TYPE")).containsExactly(expected);
		assertThat(adapter.toSingleValueMap()).containsEntry(HttpHeaders.CONTENT_TYPE, expected);
		assertThat(adapter.keySet()).containsExactly(HttpHeaders.CONTENT_TYPE);
	}

	@Test // gh-36426
	void contentTypeWithCharset() {
		request.setContentType("text/plain;charset=ISO-8859-1");
		MultiValueMap<String, String> adapter = createAdapter("UTF-8");

		assertThat(adapter.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/plain;charset=ISO-8859-1");
	}

	@Test // gh-36426
	void contentTypeWithoutCharacterEncoding() {
		request.setContentType("text/plain");
		MultiValueMap<String, String> adapter = ServletRequestHeadersAdapter.create(request);

		assertThat(adapter.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/plain");
		assertThat(adapter.get(HttpHeaders.CONTENT_TYPE)).containsExactly("text/plain");
	}

	@Test // gh-36426
	void contentTypeWithInvalidCharacterEncoding() {
		request.setContentType("text/plain");
		MultiValueMap<String, String> adapter = createAdapter("bogus");

		assertThat(adapter.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/plain");
	}

	private MultiValueMap<String, String> createAdapter(String characterEncoding) {
		HttpServletRequest wrapper = new HttpServletRequestWrapper(request) {
			@Override
			public String getCharacterEncoding() {
				return characterEncoding;
			}
		};
		return ServletRequestHeadersAdapter.create(wrapper);
	}

}

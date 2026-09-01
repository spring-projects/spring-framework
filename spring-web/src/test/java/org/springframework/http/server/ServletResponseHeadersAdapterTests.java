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

import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ServletResponseHeadersAdapter}.
 */
class ServletResponseHeadersAdapterTests {

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final ServletResponseHeadersAdapter headersAdapter = new ServletResponseHeadersAdapter(response);


	@Test // gh-37080
	void getWithSingleValue() {
		response.addHeader("MyHeader", "value");

		assertThat(headersAdapter.get("MyHeader")).containsExactly("value");
	}

	@Test
	void getWithMultipleValues() {
		response.addHeader("MyHeader", "value1");
		response.addHeader("MyHeader", "value2");

		assertThat(headersAdapter.get("MyHeader")).containsExactly("value1", "value2");
	}

	@Test
	void getWithNonExistentHeader() {
		assertThat(headersAdapter.get("NonExistent")).isNull();
	}

	@Test
	void getWithContentTypeSpecialCase() {
		response.setContentType("text/plain");

		assertThat(headersAdapter.get("Content-Type")).containsExactly("text/plain");
	}

}

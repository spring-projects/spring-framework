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

package org.springframework.test.web.servlet.assertj;


import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.json.JsonContent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResponseBodyAssert}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
class ResponseBodyAssertTests {

	@Test
	void isEqualToWithByteArray() {
		MockHttpServletResponse response = createResponse("hello");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		assertThat(fromResponse(response)).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void isEqualToWithString() {
		MockHttpServletResponse response = createResponse("hello");
		assertThat(fromResponse(response)).isEqualTo("hello");
	}

	@Test
	void jsonPathWithJsonResponseShouldPass() {
		MockHttpServletResponse response = createResponse("{\"message\": \"hello\"}");
		assertThat(fromResponse(response)).jsonPath().extractingPath("$.message").isEqualTo("hello");
	}

	@Test
	void jsonPathWithJsonCompatibleResponseShouldPass() {
		MockHttpServletResponse response = createResponse("{\"albumById\": {\"name\": \"Greatest hits\"}}");
		assertThat(fromResponse(response)).jsonPath()
				.extractingPath("$.albumById.name").isEqualTo("Greatest hits");
	}

	@Test
	void jsonCanLoadResourceRelativeToClass() {
		MockHttpServletResponse response = createResponse("{ \"name\" : \"Spring\", \"age\" : 123 }");
		// See org/springframework/test/json/example.json
		assertThat(fromResponse(response)).json(JsonContent.class).isLenientlyEqualTo("example.json");
	}

	private MockHttpServletResponse createResponse(String body) {
		try {
			MockHttpServletResponse response = new MockHttpServletResponse();
			response.getWriter().print(body);
			return response;
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private AssertProvider<ResponseBodyAssert> fromResponse(MockHttpServletResponse response) {
		return () -> new ResponseBodyAssert(response.getContentAsByteArray(), Charset.forName(response.getCharacterEncoding()), null);
	}

}

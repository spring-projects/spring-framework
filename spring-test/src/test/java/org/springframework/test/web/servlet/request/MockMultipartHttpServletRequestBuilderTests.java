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

package org.springframework.test.web.servlet.request;

import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.mock.web.MockPart;
import org.springframework.mock.web.MockServletContext;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockMultipartHttpServletRequestBuilder}.
 * @author Rossen Stoyanchev
 */
public class MockMultipartHttpServletRequestBuilderTests {

	@Test // gh-26166
	void addFileAndParts() throws Exception {
		MockMultipartHttpServletRequest mockRequest =
				(MockMultipartHttpServletRequest) createBuilder("/upload")
						.file(new MockMultipartFile("file", "test.txt", "text/plain", "Test".getBytes(UTF_8)))
						.part(new MockPart("name", "value".getBytes(UTF_8)))
						.buildRequest(new MockServletContext());

		assertThat(mockRequest.getFileMap()).containsOnlyKeys("file");
		assertThat(mockRequest.getParameterMap()).containsOnlyKeys("name");
		assertThat(mockRequest.getParts()).extracting(Part::getName).containsExactly("name");
	}

	@Test // gh-26261, gh-26400
	void addFileWithoutFilename() throws Exception {
		MockPart jsonPart = new MockPart("data", "{\"node\":\"node\"}".getBytes(UTF_8));
		jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		MockMultipartHttpServletRequest mockRequest =
				(MockMultipartHttpServletRequest) createBuilder("/upload")
						.file(new MockMultipartFile("file", "Test".getBytes(UTF_8)))
						.part(jsonPart)
						.buildRequest(new MockServletContext());

		assertThat(mockRequest.getFileMap()).containsOnlyKeys("file");
		assertThat(mockRequest.getParameterMap()).hasSize(1);
		assertThat(mockRequest.getParameter("data")).isEqualTo("{\"node\":\"node\"}");
		assertThat(mockRequest.getParts()).extracting(Part::getName).containsExactly("data");
	}

	@Test
	void mergeAndBuild() {
		MockHttpServletRequestBuilder parent = new MockHttpServletRequestBuilder(HttpMethod.GET).uri("/");
		parent.characterEncoding("UTF-8");
		Object result = createBuilder("/fileUpload").merge(parent);

		assertThat(result).isNotNull();
		assertThat(result.getClass()).isEqualTo(MockMultipartHttpServletRequestBuilder.class);

		MockMultipartHttpServletRequestBuilder builder = (MockMultipartHttpServletRequestBuilder) result;
		MockHttpServletRequest request = builder.buildRequest(new MockServletContext());
		assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	private MockMultipartHttpServletRequestBuilder createBuilder(String uri) {
		MockMultipartHttpServletRequestBuilder builder = new MockMultipartHttpServletRequestBuilder();
		builder.uri(uri);
		return builder;
	}

}

/*
 * Copyright 2002-2020 the original author or authors.
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

import java.nio.charset.StandardCharsets;

import javax.servlet.http.Part;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MockMultipartHttpServletRequestBuilder}.
 * @author Rossen Stoyanchev
 */
public class MockMultipartHttpServletRequestBuilderTests {

	@Test // gh-26166
	void addFilesAndParts() throws Exception {
		MockHttpServletRequest mockRequest = new MockMultipartHttpServletRequestBuilder("/upload")
				.file(new MockMultipartFile("file", "test.txt", "text/plain", "Test".getBytes(StandardCharsets.UTF_8)))
				.part(new MockPart("data", "{\"node\":\"node\"}".getBytes(StandardCharsets.UTF_8)))
				.buildRequest(new MockServletContext());

		StandardMultipartHttpServletRequest parsedRequest = new StandardMultipartHttpServletRequest(mockRequest);

		assertThat(parsedRequest.getParameterMap()).containsOnlyKeys("data");
		assertThat(parsedRequest.getFileMap()).containsOnlyKeys("file");
		assertThat(parsedRequest.getParts()).extracting(Part::getName).containsExactly("file", "data");
	}

	@Test
	void mergeAndBuild() {
		MockHttpServletRequestBuilder parent = new MockHttpServletRequestBuilder(HttpMethod.GET, "/");
		parent.characterEncoding("UTF-8");
		Object result = new MockMultipartHttpServletRequestBuilder("/fileUpload").merge(parent);

		assertThat(result).isNotNull();
		assertThat(result.getClass()).isEqualTo(MockMultipartHttpServletRequestBuilder.class);

		MockMultipartHttpServletRequestBuilder builder = (MockMultipartHttpServletRequestBuilder) result;
		MockHttpServletRequest request = builder.buildRequest(new MockServletContext());
		assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
	}

}

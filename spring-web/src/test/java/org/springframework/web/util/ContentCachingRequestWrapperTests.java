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

package org.springframework.web.util;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ContentCachingRequestWrapper}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
class ContentCachingRequestWrapperTests {

	protected static final String FORM_CONTENT_TYPE = MediaType.APPLICATION_FORM_URLENCODED_VALUE;


	@Test
	void cachedContentToByteArrayWithNoRead() {
		ContentCachingRequestWrapper wrapper = createGetRequest("Hello", -1);
		assertThat(wrapper.getContentAsByteArray()).isEmpty();
	}

	@Test
	void cachedContentToStringWithNoRead() {
		ContentCachingRequestWrapper wrapper = createGetRequest("Hello", -1);
		assertThat(wrapper.getContentAsString()).isEqualTo("");
	}

	@Test
	void cachedContentToByteArray() throws Exception {
		ContentCachingRequestWrapper wrapper = createGetRequest("Hello World", -1);
		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(wrapper.getContentAsByteArray()).isEqualTo(response);
	}

	@Test
	void cachedContentToString() throws Exception {
		ContentCachingRequestWrapper wrapper = createGetRequest("Hello World", -1);
		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(wrapper.getContentAsString()).isEqualTo(new String(response, UTF_8));
	}

	@Test
	void cachedContentToByteArrayWithLimit() throws Exception {
		ContentCachingRequestWrapper wrapper = createGetRequest("Hello World", 3);
		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(response).isEqualTo("Hello World".getBytes(UTF_8));
		assertThat(wrapper.getContentAsByteArray()).isEqualTo("Hel".getBytes(UTF_8));
	}

	@Test
	void cachedContentToStringWithLimit() throws Exception {
		ContentCachingRequestWrapper wrapper = createGetRequest("Hello World", 3);
		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(response).isEqualTo("Hello World".getBytes(UTF_8));
		assertThat(wrapper.getContentAsString()).isEqualTo(new String("Hel".getBytes(UTF_8), UTF_8));
	}

	@Test
	void shouldNotAllocateMoreThanCacheLimit() {
		ContentCachingRequestWrapper wrapper = createGetRequest("Hello World", 3);
		assertThat(wrapper).extracting("cachedContent.initialBlockSize").isEqualTo(3);
	}


	@Test
	void cachedContentWithOverflow() {
		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello World"), 3) {
			@Override
			protected void handleContentOverflow(int contentCacheLimit) {
				throw new IllegalStateException(String.valueOf(contentCacheLimit));
			}
		};

		assertThatIllegalStateException()
				.isThrownBy(() -> wrapper.getInputStream().readAllBytes())
				.withMessage("3");
	}

	@Test
	void requestParams() throws Exception {
		MockHttpServletRequest request = createPostRequest();
		request.setParameter("first", "value");
		request.setParameter("second", "foo", "bar");

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request, -1);
		// getting request parameters will consume the request body
		assertThat(wrapper.getParameterMap()).isNotEmpty();
		assertThat(new String(wrapper.getContentAsByteArray())).isEqualTo("first=value&second=foo&second=bar");
		// SPR-12810 : inputstream body should be consumed
		assertThat(new String(wrapper.getInputStream().readAllBytes())).isEmpty();
	}

	@Test // SPR-12810
	void inputStreamFormPostRequest() throws Exception {
		MockHttpServletRequest request = createPostRequest();
		request.setParameter("first", "value");
		request.setParameter("second", "foo", "bar");

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request, -1);

		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(wrapper.getContentAsByteArray()).isEqualTo(response);
	}

	private ContentCachingRequestWrapper createGetRequest(String content, int cacheLimit) {
		return new ContentCachingRequestWrapper(createGetRequest(content), cacheLimit);
	}


	private MockHttpServletRequest createGetRequest(String content) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(HttpMethod.GET.name());
		request.setCharacterEncoding(UTF_8);
		request.setContent(content.getBytes(UTF_8));
		return request;
	}

	private MockHttpServletRequest createPostRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(HttpMethod.POST.name());
		request.setContentType(FORM_CONTENT_TYPE);
		request.setCharacterEncoding(UTF_8.name());
		return request;
	}

}

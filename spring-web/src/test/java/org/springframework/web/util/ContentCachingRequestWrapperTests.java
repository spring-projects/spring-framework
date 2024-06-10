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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

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

	protected static final String CHARSET = StandardCharsets.UTF_8.name();

	protected static final String GET = HttpMethod.GET.name();

	protected static final String POST = HttpMethod.POST.name();

	protected static final int CONTENT_CACHE_LIMIT = 3;


	@Test
	void cachedContentToByteArrayWithNoRead() throws Exception {
		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello"));
		assertThat(wrapper.getContentAsByteArray()).isEmpty();
	}

	@Test
	void cachedContentToStringWithNoRead() throws Exception {
		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello"));
		assertThat(wrapper.getContentAsString()).isEqualTo("");
	}

	@Test
	void cachedContentToByteArray() throws Exception {
		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello World"));
		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(wrapper.getContentAsByteArray()).isEqualTo(response);
	}

	@Test
	void cachedContentToString() throws Exception {
		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello World"));
		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(wrapper.getContentAsString()).isEqualTo(new String(response, CHARSET));
	}

	@Test
	void cachedContentToByteArrayWithLimit() throws Exception {
		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello World"), CONTENT_CACHE_LIMIT);
		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(response).isEqualTo("Hello World".getBytes(CHARSET));
		assertThat(wrapper.getContentAsByteArray()).isEqualTo("Hel".getBytes(CHARSET));
	}

	@Test
	void cachedContentToStringWithLimit() throws Exception {
		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello World"), CONTENT_CACHE_LIMIT);
		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(response).isEqualTo("Hello World".getBytes(CHARSET));
		assertThat(wrapper.getContentAsString()).isEqualTo(new String("Hel".getBytes(CHARSET), CHARSET));
	}

	@Test
	void shouldNotAllocateMoreThanCacheLimit() throws Exception {
		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(createGetRequest("Hello World"), CONTENT_CACHE_LIMIT);
		Field field = ReflectionUtils.findField(ContentCachingRequestWrapper.class, "cachedContent");
		ReflectionUtils.makeAccessible(field);
		FastByteArrayOutputStream cachedContent = (FastByteArrayOutputStream) ReflectionUtils.getField(field, wrapper);
		field = ReflectionUtils.findField(FastByteArrayOutputStream.class, "initialBlockSize");
		ReflectionUtils.makeAccessible(field);
		int blockSize = (int) ReflectionUtils.getField(field, cachedContent);
		assertThat(blockSize).isEqualTo(CONTENT_CACHE_LIMIT);
	}


	@Test
	void cachedContentWithOverflow() throws Exception {
		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(
				createGetRequest("Hello World"), CONTENT_CACHE_LIMIT) {
			@Override
			protected void handleContentOverflow(int contentCacheLimit) {
				throw new IllegalStateException(String.valueOf(contentCacheLimit));
			}
		};

		assertThatIllegalStateException().isThrownBy(() ->
						wrapper.getInputStream().readAllBytes())
				.withMessage("3");
	}

	@Test
	void requestParams() throws Exception {
		MockHttpServletRequest request = createPostRequest();
		request.setParameter("first", "value");
		request.setParameter("second", "foo", "bar");

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);
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

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);

		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(wrapper.getContentAsByteArray()).isEqualTo(response);
	}

	private MockHttpServletRequest createGetRequest(String content) throws UnsupportedEncodingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(GET);
		request.setCharacterEncoding(CHARSET);
		request.setContent(content.getBytes(CHARSET));
		return request;
	}

	private MockHttpServletRequest createPostRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(POST);
		request.setContentType(FORM_CONTENT_TYPE);
		request.setCharacterEncoding(CHARSET);
		return request;
	}

}

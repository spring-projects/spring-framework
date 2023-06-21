/*
 * Copyright 2002-2023 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Brian Clozel
 */
public class ContentCachingRequestWrapperTests {

	protected static final String FORM_CONTENT_TYPE = MediaType.APPLICATION_FORM_URLENCODED_VALUE;

	protected static final Charset CHARSET = StandardCharsets.UTF_8;

	protected static final String GET = HttpMethod.GET.name();

	protected static final String POST = HttpMethod.POST.name();

	protected static final int CONTENT_CACHE_LIMIT = 3;

	private final MockHttpServletRequest request = new MockHttpServletRequest();


	@Test
	void cachedContent() throws Exception {
		this.request.setMethod(GET);
		this.request.setCharacterEncoding(CHARSET.name());
		this.request.setContent("Hello World".getBytes(CHARSET));

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request);
		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(wrapper.getContentAsByteArray()).isEqualTo(response);
		assertThat(wrapper.getContentAsString()).isEqualTo("Hello World");
	}

	@Test
	void cachedContentWithLimit() throws Exception {
		this.request.setMethod(GET);
		this.request.setCharacterEncoding(CHARSET.name());
		this.request.setContent("Hello World".getBytes(CHARSET));

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request, CONTENT_CACHE_LIMIT);
		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(response).isEqualTo("Hello World".getBytes(CHARSET));
		assertThat(wrapper.getContentAsByteArray()).isEqualTo("Hel".getBytes(CHARSET));
		assertThat(wrapper.getContentAsString()).isEqualTo("Hel");
	}

	@Test
	void cachedContentWithOverflow() throws Exception {
		this.request.setMethod(GET);
		this.request.setCharacterEncoding(CHARSET.name());
		this.request.setContent("Hello World".getBytes(CHARSET));

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request, CONTENT_CACHE_LIMIT) {
			@Override
			protected void handleContentOverflow(int contentCacheLimit) {
				throw new IllegalStateException(String.valueOf(contentCacheLimit));
			}
		};

		assertThatIllegalStateException().isThrownBy(() -> wrapper.getInputStream().readAllBytes())
			.withMessage("3");
	}

	@Test
	void requestParams() throws Exception {
		this.request.setMethod(POST);
		this.request.setContentType(FORM_CONTENT_TYPE);
		this.request.setCharacterEncoding(CHARSET.name());
		this.request.setParameter("first", "value");
		this.request.setParameter("second", "foo", "bar");

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request);
		// getting request parameters will consume the request body
		assertThat(wrapper.getParameterMap().isEmpty()).isFalse();
		assertThat(new String(wrapper.getContentAsByteArray())).isEqualTo("first=value&second=foo&second=bar");
		assertThat(wrapper.getContentAsString()).isEqualTo("first=value&second=foo&second=bar");
		// SPR-12810 : inputstream body should be consumed
		assertThat(new String(wrapper.getInputStream().readAllBytes())).isEmpty();
	}

	@Test  // SPR-12810
	void inputStreamFormPostRequest() throws Exception {
		this.request.setMethod(POST);
		this.request.setContentType(FORM_CONTENT_TYPE);
		this.request.setCharacterEncoding(CHARSET.name());
		this.request.setParameter("first", "value");
		this.request.setParameter("second", "foo", "bar");

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request);

		byte[] response = wrapper.getInputStream().readAllBytes();
		assertThat(wrapper.getContentAsByteArray()).isEqualTo(response);
		assertThat(wrapper.getContentAsString()).isEqualTo(new String(response, CHARSET));
	}

}

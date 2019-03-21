/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;

import static org.junit.Assert.*;

/**
 * @author Brian Clozel
 */
public class ContentCachingRequestWrapperTests {

	protected static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

	protected static final String CHARSET = "UTF-8";

	private final MockHttpServletRequest request = new MockHttpServletRequest();


	@Test
	public void cachedContent() throws Exception {
		this.request.setMethod("GET");
		this.request.setCharacterEncoding(CHARSET);
		this.request.setContent("Hello World".getBytes(CHARSET));

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request);
		byte[] response = FileCopyUtils.copyToByteArray(wrapper.getInputStream());
		assertArrayEquals(response, wrapper.getContentAsByteArray());
	}

	@Test
	public void cachedContentWithLimit() throws Exception {
		this.request.setMethod("GET");
		this.request.setCharacterEncoding(CHARSET);
		this.request.setContent("Hello World".getBytes(CHARSET));

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request, 3);
		byte[] response = FileCopyUtils.copyToByteArray(wrapper.getInputStream());
		assertArrayEquals("Hello World".getBytes(CHARSET), response);
		assertArrayEquals("Hel".getBytes(CHARSET), wrapper.getContentAsByteArray());
	}

	@Test
	public void cachedContentWithOverflow() throws Exception {
		this.request.setMethod("GET");
		this.request.setCharacterEncoding(CHARSET);
		this.request.setContent("Hello World".getBytes(CHARSET));

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request, 3) {
			@Override
			protected void handleContentOverflow(int contentCacheLimit) {
				throw new IllegalStateException(String.valueOf(contentCacheLimit));
			}
		};

		try {
			FileCopyUtils.copyToByteArray(wrapper.getInputStream());
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			assertEquals("3", ex.getMessage());
		}
	}

	@Test
	public void requestParams() throws Exception {
		this.request.setMethod("POST");
		this.request.setContentType(FORM_CONTENT_TYPE);
		this.request.setCharacterEncoding(CHARSET);
		this.request.setParameter("first", "value");
		this.request.setParameter("second", "foo", "bar");

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request);
		// getting request parameters will consume the request body
		assertFalse(wrapper.getParameterMap().isEmpty());
		assertEquals("first=value&second=foo&second=bar", new String(wrapper.getContentAsByteArray()));
		// SPR-12810 : inputstream body should be consumed
		assertEquals("", new String(FileCopyUtils.copyToByteArray(wrapper.getInputStream())));
	}

	@Test  // SPR-12810
	public void inputStreamFormPostRequest() throws Exception {
		this.request.setMethod("POST");
		this.request.setContentType(FORM_CONTENT_TYPE);
		this.request.setCharacterEncoding(CHARSET);
		this.request.setParameter("first", "value");
		this.request.setParameter("second", "foo", "bar");

		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(this.request);

		byte[] response = FileCopyUtils.copyToByteArray(wrapper.getInputStream());
		assertArrayEquals(response, wrapper.getContentAsByteArray());
	}

}

/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.web.reactive.server;

import java.util.Collections;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * Assertions on specific, commonly used response headers.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResponseHeadersAssertions {

	private final ExchangeActions exchangeActions;

	private final HttpHeaders headers;


	ResponseHeadersAssertions(ExchangeActions actions, HttpHeaders headers) {
		this.exchangeActions = actions;
		this.headers = headers;
	}


	public ExchangeActions cacheControl(CacheControl cacheControl) {
		String actual = this.headers.getCacheControl();
		assertEquals("Response header Cache-Control", cacheControl.getHeaderValue(), actual);
		return this.exchangeActions;
	}

	public ExchangeActions contentDisposition(ContentDisposition contentDisposition) {
		ContentDisposition actual = this.headers.getContentDisposition();
		assertEquals("Response header Content-Disposition", contentDisposition, actual);
		return this.exchangeActions;
	}

	public ExchangeActions contentLength(long contentLength) {
		long actual = this.headers.getContentLength();
		assertEquals("Response header Content-Length", contentLength, actual);
		return this.exchangeActions;
	}

	public ExchangeActions contentType(MediaType mediaType) {
		MediaType actual = this.headers.getContentType();
		assertEquals("Response header Content-Type", mediaType, actual);
		return this.exchangeActions;
	}

	public ExchangeActions expires(int expires) {
		long actual = this.headers.getExpires();
		assertEquals("Response header Expires", expires, actual);
		return this.exchangeActions;
	}

	public ExchangeActions lastModified(int lastModified) {
		long actual = this.headers.getLastModified();
		assertEquals("Response header Last-Modified", lastModified, actual);
		return this.exchangeActions;
	}

}

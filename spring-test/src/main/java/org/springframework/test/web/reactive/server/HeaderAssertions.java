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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Provides methods for HTTP header assertions.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see ResponseAssertions#header()
 */
public class HeaderAssertions<T> {

	private final ResponseAssertions<T> resultAssertions;

	private final HttpHeaders headers;


	public HeaderAssertions(HttpHeaders headers, ResponseAssertions<T> resultAssertions) {
		this.resultAssertions = resultAssertions;
		this.headers = headers;
	}


	public ResponseAssertions<T> valueEquals(String headerName, String... values) {
		List<String> actual = this.headers.get(headerName);
		assertEquals("Response header [" + headerName + "]", Arrays.asList(values), actual);
		return this.resultAssertions;
	}

	public ResponseAssertions<T> valueMatches(String headerName, String pattern) {
		List<String> values = this.headers.get(headerName);
		String value = CollectionUtils.isEmpty(values) ? "" : values.get(0);
		boolean match = Pattern.compile(pattern).matcher(value).matches();
		String message = "Response header " + headerName + "=\'" + value + "\' does not match " + pattern;
		assertTrue(message, match);
		return this.resultAssertions;
	}

	public ResponseAssertions<T> cacheControlEquals(CacheControl cacheControl) {
		String actual = this.headers.getCacheControl();
		assertEquals("Response header Cache-Control", cacheControl.getHeaderValue(), actual);
		return this.resultAssertions;
	}

	public ResponseAssertions<T> contentDispositionEquals(ContentDisposition contentDisposition) {
		ContentDisposition actual = this.headers.getContentDisposition();
		assertEquals("Response header Content-Disposition", contentDisposition, actual);
		return this.resultAssertions;
	}

	public ResponseAssertions<T> contentLengthEquals(long contentLength) {
		long actual = this.headers.getContentLength();
		assertEquals("Response header Content-Length", contentLength, actual);
		return this.resultAssertions;
	}

	public ResponseAssertions<T> contentTypeEquals(MediaType mediaType) {
		MediaType actual = this.headers.getContentType();
		assertEquals("Response header Content-Type", mediaType, actual);
		return this.resultAssertions;
	}

	public ResponseAssertions<T> expiresEquals(int expires) {
		long actual = this.headers.getExpires();
		assertEquals("Response header Expires", expires, actual);
		return this.resultAssertions;
	}

	public ResponseAssertions<T> lastModifiedEquals(int lastModified) {
		long actual = this.headers.getLastModified();
		assertEquals("Response header Last-Modified", lastModified, actual);
		return this.resultAssertions;
	}

}

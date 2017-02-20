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
 * Assertions on headers of the response.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebTestClient.ResponseSpec#expectHeader()
 */
public class HeaderAssertions {

	private final ExchangeResult exchangeResult;

	private final WebTestClient.ResponseSpec responseSpec;


	HeaderAssertions(ExchangeResult result, WebTestClient.ResponseSpec spec) {
		this.exchangeResult = result;
		this.responseSpec = spec;
	}


	/**
	 * Expect a header with the given name to match the specified values.
	 */
	public WebTestClient.ResponseSpec valueEquals(String headerName, String... values) {
		List<String> actual = getHeaders().get(headerName);
		assertEquals("Response header [" + headerName + "]", Arrays.asList(values), actual);
		return this.responseSpec;
	}

	/**
	 * Expect a header with the given name whose first value matches the
	 * provided regex pattern.
	 * @param headerName the header name
	 * @param pattern String pattern to pass to {@link Pattern#compile(String)}
	 */
	public WebTestClient.ResponseSpec valueMatches(String headerName, String pattern) {
		List<String> values = getHeaders().get(headerName);
		String value = CollectionUtils.isEmpty(values) ? "" : values.get(0);
		boolean match = Pattern.compile(pattern).matcher(value).matches();
		String message = "Response header " + headerName + "=\'" + value + "\' does not match " + pattern;
		assertTrue(message, match);
		return this.responseSpec;
	}

	/**
	 * Expect a "Cache-Control" header with the given value.
	 */
	public WebTestClient.ResponseSpec cacheControl(CacheControl cacheControl) {
		String actual = getHeaders().getCacheControl();
		assertEquals("Response header Cache-Control", cacheControl.getHeaderValue(), actual);
		return this.responseSpec;
	}

	/**
	 * Expect a "Content-Disposition" header with the given value.
	 */
	public WebTestClient.ResponseSpec contentDisposition(ContentDisposition contentDisposition) {
		ContentDisposition actual = getHeaders().getContentDisposition();
		assertEquals("Response header Content-Disposition", contentDisposition, actual);
		return this.responseSpec;
	}

	/**
	 * Expect a "Content-Length" header with the given value.
	 */
	public WebTestClient.ResponseSpec contentLength(long contentLength) {
		long actual = getHeaders().getContentLength();
		assertEquals("Response header Content-Length", contentLength, actual);
		return this.responseSpec;
	}

	/**
	 * Expect a "Content-Type" header with the given value.
	 */
	public WebTestClient.ResponseSpec contentType(MediaType mediaType) {
		MediaType actual = getHeaders().getContentType();
		assertEquals("Response header Content-Type", mediaType, actual);
		return this.responseSpec;
	}

	/**
	 * Expect an "Expires" header with the given value.
	 */
	public WebTestClient.ResponseSpec expires(int expires) {
		long actual = getHeaders().getExpires();
		assertEquals("Response header Expires", expires, actual);
		return this.responseSpec;
	}

	/**
	 * Expect a "Last-Modified" header with the given value.
	 */
	public WebTestClient.ResponseSpec lastModified(int lastModified) {
		long actual = getHeaders().getLastModified();
		assertEquals("Response header Last-Modified", lastModified, actual);
		return this.responseSpec;
	}


	// Private methods

	private HttpHeaders getHeaders() {
		return this.exchangeResult.getResponseHeaders();
	}

}

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
import java.util.regex.Pattern;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
		return assertHeader(headerName, Arrays.asList(values), getHeaders().get(headerName));
	}

	/**
	 * Expect a header with the given name whose first value matches the
	 * provided regex pattern.
	 * @param name the header name
	 * @param pattern String pattern to pass to {@link Pattern#compile(String)}
	 */
	public WebTestClient.ResponseSpec valueMatches(String name, String pattern) {
		return this.exchangeResult.assertWithDiagnosticsAndReturn(() -> {
			String value = getHeaders().getFirst(name);
			assertTrue(getMessage(name) + " not found", value != null);
			boolean match = Pattern.compile(pattern).matcher(value).matches();
			assertTrue(getMessage(name) + "=\'" + value + "\' does not match \'" + pattern + "\'", match);
			return this.responseSpec;
		});
	}

	/**
	 * Expect a "Cache-Control" header with the given value.
	 */
	public WebTestClient.ResponseSpec cacheControl(CacheControl cacheControl) {
		return assertHeader("Cache-Control", cacheControl.getHeaderValue(), getHeaders().getCacheControl());
	}

	/**
	 * Expect a "Content-Disposition" header with the given value.
	 */
	public WebTestClient.ResponseSpec contentDisposition(ContentDisposition contentDisposition) {
		return assertHeader("Content-Disposition", contentDisposition, getHeaders().getContentDisposition());
	}

	/**
	 * Expect a "Content-Length" header with the given value.
	 */
	public WebTestClient.ResponseSpec contentLength(long contentLength) {
		return assertHeader("Content-Length", contentLength, getHeaders().getContentLength());
	}

	/**
	 * Expect a "Content-Type" header with the given value.
	 */
	public WebTestClient.ResponseSpec contentType(MediaType mediaType) {
		return assertHeader("Content-Type", mediaType, getHeaders().getContentType());
	}

	/**
	 * Expect an "Expires" header with the given value.
	 */
	public WebTestClient.ResponseSpec expires(int expires) {
		return assertHeader("Expires", expires, getHeaders().getExpires());
	}

	/**
	 * Expect a "Last-Modified" header with the given value.
	 */
	public WebTestClient.ResponseSpec lastModified(int lastModified) {
		return assertHeader("Last-Modified", lastModified, getHeaders().getLastModified());
	}


	// Private methods

	private HttpHeaders getHeaders() {
		return this.exchangeResult.getResponseHeaders();
	}

	private String getMessage(String headerName) {
		return "Response header [" + headerName + "]";
	}

	private WebTestClient.ResponseSpec assertHeader(String name, Object expected, Object actual) {
		return this.exchangeResult.assertWithDiagnosticsAndReturn(() -> {
			assertEquals(getMessage(name), expected, actual);
			return this.responseSpec;
		});
	}

}

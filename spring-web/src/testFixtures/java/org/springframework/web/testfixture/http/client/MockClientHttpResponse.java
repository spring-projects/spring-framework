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

package org.springframework.web.testfixture.http.client;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.testfixture.http.MockHttpInputMessage;

/**
 * Mock implementation of {@link ClientHttpResponse}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class MockClientHttpResponse extends MockHttpInputMessage implements ClientHttpResponse {

	private final HttpStatusCode statusCode;


	/**
	 * Create a {@code MockClientHttpResponse} with an empty response body and
	 * HTTP status code {@link HttpStatus#OK OK}.
	 * @since 6.0.3
	 */
	public MockClientHttpResponse() {
		this(new byte[0], HttpStatus.OK);
	}

	/**
	 * Create a {@code MockClientHttpResponse} with response body as a byte array
	 * and the supplied HTTP status code.
	 */
	public MockClientHttpResponse(byte[] body, HttpStatusCode statusCode) {
		super(body);
		Assert.notNull(statusCode, "HttpStatusCode must not be null");
		this.statusCode = statusCode;
	}

	/**
	 * Create a {@code MockClientHttpResponse} with response body as a byte array
	 * and a custom HTTP status code.
	 * @since 5.3.17
	 */
	public MockClientHttpResponse(byte[] body, int statusCode) {
		this(body, HttpStatusCode.valueOf(statusCode));
	}

	/**
	 * Create a {@code MockClientHttpResponse} with response body as {@link InputStream}
	 * and the supplied HTTP status code.
	 */
	public MockClientHttpResponse(InputStream body, HttpStatusCode statusCode) {
		super(body);
		Assert.notNull(statusCode, "HttpStatusCode must not be null");
		this.statusCode = statusCode;
	}

	/**
	 * Create a {@code MockClientHttpResponse} with response body as {@link InputStream}
	 * and a custom HTTP status code.
	 * @since 5.3.17
	 */
	public MockClientHttpResponse(InputStream body, int statusCode) {
		this(body, HttpStatusCode.valueOf(statusCode));
	}


	@Override
	public HttpStatusCode getStatusCode() {
		return this.statusCode;
	}

	@Override
	public String getStatusText() {
		return (this.statusCode instanceof HttpStatus status ? status.getReasonPhrase() : "");
	}

	@Override
	public void close() {
		try {
			getBody().close();
		}
		catch (IOException ex) {
			// ignore
		}
	}

}

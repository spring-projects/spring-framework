/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.mock.http.client;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.util.Assert;

/**
 * Mock implementation of {@link ClientHttpResponse}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockClientHttpResponse extends MockHttpInputMessage implements ClientHttpResponse {

	private final HttpStatus status;


	/**
	 * Constructor with response body as a byte array.
	 */
	public MockClientHttpResponse(byte[] body, HttpStatus statusCode) {
		super(body);
		Assert.notNull(statusCode, "statisCode is required");
		this.status = statusCode;
	}

	/**
	 * Constructor with response body as InputStream.
	 */
	public MockClientHttpResponse(InputStream body, HttpStatus statusCode) {
		super(body);
		Assert.notNull(statusCode, "statisCode is required");
		this.status = statusCode;
	}

	public HttpStatus getStatusCode() throws IOException {
		return this.status;
	}

	public int getRawStatusCode() throws IOException {
		return this.status.value();
	}

	public String getStatusText() throws IOException {
		return this.status.getReasonPhrase();
	}

	public void close() {
	}

}

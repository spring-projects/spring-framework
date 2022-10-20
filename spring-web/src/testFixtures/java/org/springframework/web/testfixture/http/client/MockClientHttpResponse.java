/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * Mock implementation of {@link ClientHttpResponse}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class MockClientHttpResponse implements ClientHttpResponse {

	private final HttpHeaders headers = new HttpHeaders();

	private final HttpStatus status;

	private InputStream body;


	public MockClientHttpResponse() {
		this.status = HttpStatus.OK;
	}

	public MockClientHttpResponse(HttpStatus statusCode) {
		Assert.notNull(statusCode, "HttpStatus is required");
		this.status = statusCode;
	}

	@Override
	public HttpStatus getStatusCode() throws IOException {
		return this.status;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getRawStatusCode() throws IOException {
		return this.status.value();
	}

	@Override
	public String getStatusText() throws IOException {
		return this.status.getReasonPhrase();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.body;
	}

	public void setBody(byte[] body) {
		Assert.notNull(body, "body is required");
		this.body = new ByteArrayInputStream(body);
	}

	public void setBody(String body) {
		Assert.notNull(body, "body is required");
		this.body = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
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

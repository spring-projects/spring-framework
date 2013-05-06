/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.http.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.Cookies;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;

/**
 * Simple implementation of {@link ClientHttpResponse} that reads the request's body into memory,
 * thus allowing for multiple invocations of {@link #getBody()}.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
final class BufferingClientHttpResponseWrapper implements ClientHttpResponse {

	private final ClientHttpResponse response;

	private byte[] body;


	BufferingClientHttpResponseWrapper(ClientHttpResponse response) {
		this.response = response;
	}


	public HttpStatus getStatusCode() throws IOException {
		return this.response.getStatusCode();
	}

	public int getRawStatusCode() throws IOException {
		return this.response.getRawStatusCode();
	}

	public String getStatusText() throws IOException {
		return this.response.getStatusText();
	}

	public HttpHeaders getHeaders() {
		return this.response.getHeaders();
	}

	public InputStream getBody() throws IOException {
		if (this.body == null) {
			this.body = StreamUtils.copyToByteArray(this.response.getBody());
		}
		return new ByteArrayInputStream(this.body);
	}

	public Cookies getCookies() {
		return this.response.getCookies();
	}

	public void close() {
		this.response.close();
	}

}

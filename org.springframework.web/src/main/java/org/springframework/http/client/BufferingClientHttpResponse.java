/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;

/**
 * Simple implementation of {@link ClientHttpResponse} that reads the request's body into memory, thus allowing for
 * multiple invocations of {@link #getBody()}.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
class BufferingClientHttpResponse implements ClientHttpResponse {

	private final ClientHttpResponse response;

	private byte[] body;

	BufferingClientHttpResponse(ClientHttpResponse response) {
		this.response = response;
	}

	public HttpStatus getStatusCode() throws IOException {
		return response.getStatusCode();
	}

	public String getStatusText() throws IOException {
		return response.getStatusText();
	}

	public HttpHeaders getHeaders() {
		return response.getHeaders();
	}

	public InputStream getBody() throws IOException {
		if (body == null) {
			body = FileCopyUtils.copyToByteArray(response.getBody());
		}
		return new ByteArrayInputStream(body);
	}

	public void close() {
		response.close();
	}

}

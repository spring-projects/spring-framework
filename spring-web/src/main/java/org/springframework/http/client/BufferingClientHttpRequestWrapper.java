/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Simple implementation of {@link ClientHttpRequest} that wraps another request.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
final class BufferingClientHttpRequestWrapper extends AbstractBufferingClientHttpRequest {

	private final ClientHttpRequest request;


	BufferingClientHttpRequestWrapper(ClientHttpRequest request) {
		this.request = request;
	}


	@Override
	public HttpMethod getMethod() {
		return this.request.getMethod();
	}

	@Override
	public URI getURI() {
		return this.request.getURI();
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		this.request.getHeaders().putAll(headers);
		return executeWithRequest(this.request, bufferedOutput, true);
	}

}

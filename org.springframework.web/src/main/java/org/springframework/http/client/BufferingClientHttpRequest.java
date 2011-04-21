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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * Simple implementation of {@link ClientHttpRequest} that wraps another request.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
class BufferingClientHttpRequest extends AbstractBufferingClientHttpRequest {

	private final ClientHttpRequest request;

	BufferingClientHttpRequest(ClientHttpRequest request) {
		Assert.notNull(request, "'request' must not be null");
		this.request = request;
	}

	public HttpMethod getMethod() {
		return request.getMethod();
	}

	public URI getURI() {
		return request.getURI();
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		request.getHeaders().putAll(headers);
		OutputStream body = request.getBody();
		FileCopyUtils.copy(bufferedOutput, body);
		ClientHttpResponse response = request.execute();
		return new BufferingClientHttpResponse(response);
	}
}

/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Base implementation of {@link AsyncClientHttpRequest} that buffers output
 * in a byte array before sending it over the wire.
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @deprecated as of Spring 5.0, with no direct replacement
 */
@Deprecated
abstract class AbstractBufferingAsyncClientHttpRequest extends AbstractAsyncClientHttpRequest {

	private ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream(1024);


	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		return this.bufferedOutput;
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers) throws IOException {
		byte[] bytes = this.bufferedOutput.toByteArray();
		if (headers.getContentLength() < 0) {
			headers.setContentLength(bytes.length);
		}
		ListenableFuture<ClientHttpResponse> result = executeInternal(headers, bytes);
		this.bufferedOutput = new ByteArrayOutputStream(0);
		return result;
	}

	/**
	 * Abstract template method that writes the given headers and content to the HTTP request.
	 * @param headers the HTTP headers
	 * @param bufferedOutput the body content
	 * @return the response object for the executed request
	 */
	protected abstract ListenableFuture<ClientHttpResponse> executeInternal(
			HttpHeaders headers, byte[] bufferedOutput) throws IOException;

}

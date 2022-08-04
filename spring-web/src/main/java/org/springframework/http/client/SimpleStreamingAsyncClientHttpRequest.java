/*
 * Copyright 2002-2020 the original author or authors.
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link org.springframework.http.client.ClientHttpRequest} implementation
 * that uses standard Java facilities to execute streaming requests. Created
 * via the {@link org.springframework.http.client.SimpleClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see org.springframework.http.client.SimpleClientHttpRequestFactory#createRequest
 * @see org.springframework.http.client.support.AsyncHttpAccessor
 * @see org.springframework.web.client.AsyncRestTemplate
 * @deprecated as of Spring 5.0, with no direct replacement
 */
@Deprecated
final class SimpleStreamingAsyncClientHttpRequest extends AbstractAsyncClientHttpRequest {

	private final HttpURLConnection connection;

	private final int chunkSize;

	@Nullable
	private OutputStream body;

	private final boolean outputStreaming;

	private final AsyncListenableTaskExecutor taskExecutor;


	SimpleStreamingAsyncClientHttpRequest(HttpURLConnection connection, int chunkSize,
			boolean outputStreaming, AsyncListenableTaskExecutor taskExecutor) {

		this.connection = connection;
		this.chunkSize = chunkSize;
		this.outputStreaming = outputStreaming;
		this.taskExecutor = taskExecutor;
	}


	@Override
	public String getMethodValue() {
		return this.connection.getRequestMethod();
	}

	@Override
	public URI getURI() {
		try {
			return this.connection.getURL().toURI();
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(
					"Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		if (this.body == null) {
			if (this.outputStreaming) {
				long contentLength = headers.getContentLength();
				if (contentLength >= 0) {
					this.connection.setFixedLengthStreamingMode(contentLength);
				}
				else {
					this.connection.setChunkedStreamingMode(this.chunkSize);
				}
			}
			SimpleBufferingClientHttpRequest.addHeaders(this.connection, headers);
			this.connection.connect();
			this.body = this.connection.getOutputStream();
		}
		return StreamUtils.nonClosing(this.body);
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(HttpHeaders headers) throws IOException {
		return this.taskExecutor.submitListenable(() -> {
			try {
				if (this.body != null) {
					this.body.close();
				}
				else {
					SimpleBufferingClientHttpRequest.addHeaders(this.connection, headers);
					this.connection.connect();
					// Immediately trigger the request in a no-output scenario as well
					this.connection.getResponseCode();
				}
			}
			catch (IOException ex) {
				// ignore
			}
			return new SimpleClientHttpResponse(this.connection);
		});

	}

}

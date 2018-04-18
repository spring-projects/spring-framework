/*
 * Copyright 2002-2018 the original author or authors.
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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link org.springframework.http.client.ClientHttpRequest} implementation that uses
 * standard JDK facilities to execute buffered requests. Created via the
 * {@link org.springframework.http.client.SimpleClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see org.springframework.http.client.SimpleClientHttpRequestFactory#createRequest
 * @deprecated as of Spring 5.0, with no direct replacement
 */
@Deprecated
final class SimpleBufferingAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	private final HttpURLConnection connection;

	private final boolean outputStreaming;

	private final AsyncListenableTaskExecutor taskExecutor;


	SimpleBufferingAsyncClientHttpRequest(HttpURLConnection connection,
			boolean outputStreaming, AsyncListenableTaskExecutor taskExecutor) {

		this.connection = connection;
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
			throw new IllegalStateException("Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(
			final HttpHeaders headers, final byte[] bufferedOutput) throws IOException {

		return this.taskExecutor.submitListenable(new Callable<ClientHttpResponse>() {
			@Override
			public ClientHttpResponse call() throws Exception {
				SimpleBufferingClientHttpRequest.addHeaders(connection, headers);
				// JDK <1.8 doesn't support getOutputStream with HTTP DELETE
				if (getMethod() == HttpMethod.DELETE && bufferedOutput.length == 0) {
					connection.setDoOutput(false);
				}
				if (connection.getDoOutput() && outputStreaming) {
					connection.setFixedLengthStreamingMode(bufferedOutput.length);
				}
				connection.connect();
				if (connection.getDoOutput()) {
					FileCopyUtils.copy(bufferedOutput, connection.getOutputStream());
				}
				else {
					// Immediately trigger the request in a no-output scenario as well
					connection.getResponseCode();
				}
				return new SimpleClientHttpResponse(connection);
			}
		});
	}

}

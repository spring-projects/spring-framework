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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.FileCopyUtils;

/**
 * {@link org.springframework.http.client.ClientHttpRequest} implementation that uses
 * standard J2SE facilities to execute buffered requests. Created via the
 * {@link org.springframework.http.client.SimpleClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see org.springframework.http.client.SimpleClientHttpRequestFactory#createRequest(java.net.URI, org.springframework.http.HttpMethod)
 */
final class SimpleBufferingAsyncClientHttpRequest extends AbstractBufferingAsyncClientHttpRequest {

	private final HttpURLConnection connection;

	private final boolean outputStreaming;

	private final AsyncTaskExecutor taskExecutor;

	SimpleBufferingAsyncClientHttpRequest(HttpURLConnection connection,
			boolean outputStreaming, AsyncTaskExecutor taskExecutor) {
		this.connection = connection;
		this.outputStreaming = outputStreaming;
		this.taskExecutor = taskExecutor;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.connection.getRequestMethod());
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
	protected Future<ClientHttpResponse> executeInternal(final HttpHeaders headers,
			final byte[] bufferedOutput) throws IOException {
		return taskExecutor.submit(new Callable<ClientHttpResponse>() {
			@Override
			public ClientHttpResponse call() throws Exception {
				for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
					String headerName = entry.getKey();
					for (String headerValue : entry.getValue()) {
						connection.addRequestProperty(headerName, headerValue);
					}
				}

				if (connection.getDoOutput() && outputStreaming) {
					connection.setFixedLengthStreamingMode(bufferedOutput.length);
				}

				connection.connect();
				if (connection.getDoOutput()) {
					FileCopyUtils.copy(bufferedOutput, connection.getOutputStream());
				}
				return new SimpleClientHttpResponse(connection);
			}
		});
	}

}

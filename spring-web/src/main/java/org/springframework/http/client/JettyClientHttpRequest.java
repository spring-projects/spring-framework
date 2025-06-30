/*
 * Copyright 2002-present the original author or authors.
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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.InputStreamResponseListener;
import org.eclipse.jetty.client.OutputStreamRequestContent;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpRequest} implementation based on Jetty's
 * {@link org.eclipse.jetty.client.HttpClient}.
 *
 * @author Arjen Poutsma
 * @since 6.1
 * @see JettyClientHttpRequestFactory
 */
class JettyClientHttpRequest extends AbstractStreamingClientHttpRequest {

	private static final int CHUNK_SIZE = 1024;


	private final Request request;

	private final long readTimeout;


	public JettyClientHttpRequest(Request request, long readTimeout) {
		this.request = request;
		this.readTimeout = readTimeout;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.request.getMethod());
	}

	@Override
	public URI getURI() {
		return this.request.getURI();
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {
		if (!headers.isEmpty()) {
			this.request.headers(httpFields -> {
				headers.forEach((headerName, headerValues) -> {
					for (String headerValue : headerValues) {
						httpFields.add(headerName, headerValue);
					}
				});
			});
		}
		String contentType = null;
		if (headers.getContentType() != null) {
			contentType = headers.getContentType().toString();
		}
		try {
			InputStreamResponseListener responseListener = new InputStreamResponseListener();
			if (body != null) {
				OutputStreamRequestContent requestContent = new OutputStreamRequestContent(contentType);
				this.request.body(requestContent)
						.send(responseListener);
				try (OutputStream outputStream =
							new BufferedOutputStream(requestContent.getOutputStream(), CHUNK_SIZE)) {
					body.writeTo(StreamUtils.nonClosing(outputStream));
				}
			}
			else {
				this.request.send(responseListener);
			}
			Response response = responseListener.get(this.readTimeout, TimeUnit.MILLISECONDS);
			return new JettyClientHttpResponse(response, responseListener.getInputStream());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Request was interrupted: " + ex.getMessage(), ex);
		}
		catch (ExecutionException ex) {
			Throwable cause = ex.getCause();

			if (cause instanceof UncheckedIOException uioEx) {
				throw uioEx.getCause();
			}
			if (cause instanceof RuntimeException rtEx) {
				throw rtEx;
			}
			else if (cause instanceof IOException ioEx) {
				throw ioEx;
			}
			else {
				String message = (cause == null ? null : cause.getMessage());
				throw (message == null ? new IOException(cause) : new IOException(message, cause));
			}
		}
		catch (TimeoutException ex) {
			throw new IOException("Request timed out: " + ex.getMessage(), ex);
		}
	}
}

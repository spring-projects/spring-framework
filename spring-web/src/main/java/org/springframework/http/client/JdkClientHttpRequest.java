/*
 * Copyright 2023-2023 the original author or authors.
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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpRequest} implementation based the Java {@link HttpClient}.
 * Created via the {@link JdkClientHttpRequestFactory}.
 *
 * @author Marten Deinum
 * @author Arjen Poutsma
 * @since 6.1
 */
class JdkClientHttpRequest extends AbstractStreamingClientHttpRequest {

	/*
	 * The JDK HttpRequest doesn't allow all headers to be set. The named headers are taken from the default
	 * implementation for HttpRequest.
	 */
	private static final List<String> DISALLOWED_HEADERS =
			List.of("connection", "content-length", "expect", "host", "upgrade");

	private final HttpClient httpClient;

	private final HttpMethod method;

	private final URI uri;

	private final Executor executor;

	@Nullable
	private final Duration timeOut;


	public JdkClientHttpRequest(HttpClient httpClient, URI uri, HttpMethod method, Executor executor,
			@Nullable Duration readTimeout) {
		this.httpClient = httpClient;
		this.uri = uri;
		this.method = method;
		this.executor = executor;
		this.timeOut = readTimeout;
	}

	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}


	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {
		try {
			HttpRequest request = buildRequest(headers, body);
			HttpResponse<InputStream> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			return new JdkClientHttpResponse(response);
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Could not send request: " + ex.getMessage(), ex);
		}
	}


	private HttpRequest buildRequest(HttpHeaders headers, @Nullable Body body) {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(this.uri);

		if (this.timeOut != null) {
			builder.timeout(this.timeOut);
		}

		headers.forEach((headerName, headerValues) -> {
			if (!headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
				if (!DISALLOWED_HEADERS.contains(headerName.toLowerCase())) {
					for (String headerValue : headerValues) {
						builder.header(headerName, headerValue);
					}
				}
			}
		});

		builder.method(this.method.name(), bodyPublisher(headers, body));
		return builder.build();
	}

	private HttpRequest.BodyPublisher bodyPublisher(HttpHeaders headers, @Nullable Body body) {
		if (body != null) {
			Flow.Publisher<ByteBuffer> outputStreamPublisher = OutputStreamPublisher.create(
					outputStream -> body.writeTo(StreamUtils.nonClosing(outputStream)),
					this.executor);

			long contentLength = headers.getContentLength();
			if (contentLength != -1) {
				return HttpRequest.BodyPublishers.fromPublisher(outputStreamPublisher, contentLength);
			}
			else {
				return HttpRequest.BodyPublishers.fromPublisher(outputStreamPublisher);
			}
		}
		else {
			return HttpRequest.BodyPublishers.noBody();
		}
	}

}

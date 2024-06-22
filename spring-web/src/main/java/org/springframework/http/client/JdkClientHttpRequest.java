/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ClientHttpRequest} implementation based the Java {@link HttpClient}.
 * Created via the {@link JdkClientHttpRequestFactory}.
 *
 * @author Marten Deinum
 * @author Arjen Poutsma
 * @since 6.1
 */
class JdkClientHttpRequest extends AbstractStreamingClientHttpRequest {

	private static final OutputStreamPublisher.ByteMapper<ByteBuffer> BYTE_MAPPER = new ByteBufferMapper();

	private static final Set<String> DISALLOWED_HEADERS = disallowedHeaders();


	private final HttpClient httpClient;

	private final HttpMethod method;

	private final URI uri;

	private final Executor executor;

	@Nullable
	private final Duration timeout;


	public JdkClientHttpRequest(HttpClient httpClient, URI uri, HttpMethod method, Executor executor,
			@Nullable Duration readTimeout) {

		this.httpClient = httpClient;
		this.uri = uri;
		this.method = method;
		this.executor = executor;
		this.timeout = readTimeout;
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
	@SuppressWarnings("NullAway")
	protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {
		try {
			HttpRequest request = buildRequest(headers, body);
			HttpResponse<InputStream> response;
			if (this.timeout != null) {
				response = this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
						.get(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
			}
			else {
				response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			}
			return new JdkClientHttpResponse(response);
		}
		catch (UncheckedIOException ex) {
			throw ex.getCause();
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
				throw new IOException(cause.getMessage(), cause);
			}
		}
		catch (TimeoutException ex) {
			throw new IOException("Request timed out: " + ex.getMessage(), ex);
		}
	}


	private HttpRequest buildRequest(HttpHeaders headers, @Nullable Body body) {
		HttpRequest.Builder builder = HttpRequest.newBuilder().uri(this.uri);
		if (this.timeout != null) {
			builder.timeout(this.timeout);
		}

		headers.forEach((headerName, headerValues) -> {
			if (!DISALLOWED_HEADERS.contains(headerName.toLowerCase())) {
				for (String headerValue : headerValues) {
					builder.header(headerName, headerValue);
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
					BYTE_MAPPER, this.executor);

			long contentLength = headers.getContentLength();
			if (contentLength > 0) {
				return HttpRequest.BodyPublishers.fromPublisher(outputStreamPublisher, contentLength);
			}
			else if (contentLength == 0) {
				return HttpRequest.BodyPublishers.noBody();
			}
			else {
				return HttpRequest.BodyPublishers.fromPublisher(outputStreamPublisher);
			}
		}
		else {
			return HttpRequest.BodyPublishers.noBody();
		}
	}

	/**
	 * By default, {@link HttpRequest} does not allow {@code Connection},
	 * {@code Content-Length}, {@code Expect}, {@code Host}, or {@code Upgrade}
	 * headers to be set, but this can be overridden with the
	 * {@code jdk.httpclient.allowRestrictedHeaders} system property.
	 * @see jdk.internal.net.http.common.Utils#getDisallowedHeaders()
	 */
	private static Set<String> disallowedHeaders() {
		TreeSet<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		headers.addAll(Set.of("connection", "content-length", "expect", "host", "upgrade"));

		String headersToAllow = System.getProperty("jdk.httpclient.allowRestrictedHeaders");
		if (headersToAllow != null) {
			Set<String> toAllow = StringUtils.commaDelimitedListToSet(headersToAllow);
			headers.removeAll(toAllow);
		}
		return Collections.unmodifiableSet(headers);
	}


	private static final class ByteBufferMapper implements OutputStreamPublisher.ByteMapper<ByteBuffer> {

		@Override
		public ByteBuffer map(int b) {
			ByteBuffer byteBuffer = ByteBuffer.allocate(1);
			byteBuffer.put((byte) b);
			byteBuffer.flip();
			return byteBuffer;
		}

		@Override
		public ByteBuffer map(byte[] b, int off, int len) {
			ByteBuffer byteBuffer = ByteBuffer.allocate(len);
			byteBuffer.put(b, off, len);
			byteBuffer.flip();
			return byteBuffer;
		}
	}

}

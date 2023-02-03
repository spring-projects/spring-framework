/*
 * Copyright 2002-2022 the original author or authors.
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
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpRequest} implementation based on
 * Apache HttpComponents HttpClient in streaming mode.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see HttpComponentsClientHttpRequestFactory#createRequest(java.net.URI, org.springframework.http.HttpMethod)
 */
final class HttpComponentsStreamingClientHttpRequest extends AbstractClientHttpRequest
		implements StreamingHttpOutputMessage {

	private final HttpClient httpClient;

	private final ClassicHttpRequest httpRequest;

	private final HttpContext httpContext;

	@Nullable
	private Body body;


	HttpComponentsStreamingClientHttpRequest(HttpClient client, ClassicHttpRequest request, HttpContext context) {
		this.httpClient = client;
		this.httpRequest = request;
		this.httpContext = context;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.httpRequest.getMethod());
	}

	@Override
	public URI getURI() {
		try {
			return this.httpRequest.getUri();
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	@Override
	public void setBody(Body body) {
		assertNotExecuted();
		this.body = body;
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) {
		throw new UnsupportedOperationException("getBody not supported");
	}

	@SuppressWarnings("deprecation")  // execute(ClassicHttpRequest, HttpContext)
	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		HttpComponentsClientHttpRequest.addHeaders(this.httpRequest, headers);

		if (this.body != null) {
			HttpEntity requestEntity = new StreamingHttpEntity(getHeaders(), this.body);
			this.httpRequest.setEntity(requestEntity);
		}
		HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);
		Assert.isInstanceOf(ClassicHttpResponse.class, httpResponse,
				"HttpResponse not an instance of ClassicHttpResponse");
		return new HttpComponentsClientHttpResponse((ClassicHttpResponse) httpResponse);
	}


	private static class StreamingHttpEntity implements HttpEntity {

		private final HttpHeaders headers;

		private final StreamingHttpOutputMessage.Body body;

		public StreamingHttpEntity(HttpHeaders headers, StreamingHttpOutputMessage.Body body) {
			this.headers = headers;
			this.body = body;
		}

		@Override
		public boolean isRepeatable() {
			return false;
		}

		@Override
		public boolean isChunked() {
			return false;
		}

		@Override
		public long getContentLength() {
			return this.headers.getContentLength();
		}

		@Override
		@Nullable
		public String getContentType() {
			return this.headers.getFirst(HttpHeaders.CONTENT_TYPE);
		}

		@Override
		@Nullable
		public String getContentEncoding() {
			return this.headers.getFirst(HttpHeaders.CONTENT_ENCODING);
		}

		@Override
		public InputStream getContent() throws IOException, IllegalStateException {
			throw new IllegalStateException("No content available");
		}

		@Override
		public void writeTo(OutputStream outputStream) throws IOException {
			this.body.writeTo(outputStream);
		}

		@Override
		public boolean isStreaming() {
			return true;
		}

		@Override
		@Nullable
		public Supplier<List<? extends Header>> getTrailers() {
			return null;
		}

		@Override
		@Nullable
		public Set<String> getTrailerNames() {
			return null;
		}

		@Override
		public void close() throws IOException {
		}
	}

}

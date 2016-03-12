/*
 * Copyright 2002-2015 the original author or authors.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;

/**
 * {@link ClientHttpRequest} implementation that uses Apache HttpComponents
 * HttpClient to execute requests.
 *
 * <p>Created via the {@link HttpComponentsClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see HttpComponentsClientHttpRequestFactory#createRequest(java.net.URI, org.springframework.http.HttpMethod)
 */
final class HttpComponentsStreamingClientHttpRequest extends AbstractClientHttpRequest implements StreamingHttpOutputMessage {

	private final HttpClient httpClient;

	private final HttpUriRequest httpRequest;

	private final HttpContext httpContext;

	private Body body;


	HttpComponentsStreamingClientHttpRequest(HttpClient httpClient, HttpUriRequest httpRequest, HttpContext httpContext) {
		this.httpClient = httpClient;
		this.httpRequest = httpRequest;
		this.httpContext = httpContext;
	}


	@Override
	public HttpMethod getMethod() {
		return HttpMethod.resolve(this.httpRequest.getMethod());
	}

	@Override
	public URI getURI() {
		return this.httpRequest.getURI();
	}

	@Override
	public void setBody(Body body) {
		assertNotExecuted();
		this.body = body;
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		throw new UnsupportedOperationException("getBody not supported");
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		HttpComponentsClientHttpRequest.addHeaders(this.httpRequest, headers);

		if (this.httpRequest instanceof HttpEntityEnclosingRequest && body != null) {
			HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.httpRequest;
			HttpEntity requestEntity = new StreamingHttpEntity(getHeaders(), body);
			entityEnclosingRequest.setEntity(requestEntity);
		}

		HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);
		return new HttpComponentsClientHttpResponse(httpResponse);
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
		public Header getContentType() {
			MediaType contentType = this.headers.getContentType();
			return (contentType != null ? new BasicHeader("Content-Type", contentType.toString()) : null);
		}

		@Override
		public Header getContentEncoding() {
			String contentEncoding = this.headers.getFirst("Content-Encoding");
			return (contentEncoding != null ? new BasicHeader("Content-Encoding", contentEncoding) : null);

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
		@Deprecated
		public void consumeContent() throws IOException {
			throw new UnsupportedOperationException();
		}
	}

}

/*
 * Copyright 2002-2023 the original author or authors.
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
import java.net.URI;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSink;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link ClientHttpRequest} implementation based on OkHttp 3.x.
 *
 * <p>Created via the {@link OkHttp3ClientHttpRequestFactory}.
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @since 4.3
 */
@Deprecated(since = "6.1", forRemoval = true)
class OkHttp3ClientHttpRequest extends AbstractStreamingClientHttpRequest {

	private final OkHttpClient client;

	private final URI uri;

	private final HttpMethod method;


	public OkHttp3ClientHttpRequest(OkHttpClient client, URI uri, HttpMethod method) {
		this.client = client;
		this.uri = uri;
		this.method = method;
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
	@SuppressWarnings("removal")
	protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {

		RequestBody requestBody;
		if (body != null) {
			requestBody = new BodyRequestBody(headers, body);
		}
		else if (okhttp3.internal.http.HttpMethod.requiresRequestBody(getMethod().name())) {
			String header = headers.getFirst(HttpHeaders.CONTENT_TYPE);
			MediaType contentType = (header != null) ? MediaType.parse(header) : null;
			requestBody = RequestBody.create(contentType, new byte[0]);
		}
		else {
			requestBody = null;
		}
		Request.Builder builder = new Request.Builder()
				.url(this.uri.toURL());
		builder.method(this.method.name(), requestBody);
		headers.forEach((headerName, headerValues) -> {
			for (String headerValue : headerValues) {
				builder.addHeader(headerName, headerValue);
			}
		});
		Request request = builder.build();
		return new OkHttp3ClientHttpResponse(this.client.newCall(request).execute());
	}


	private static class BodyRequestBody extends RequestBody {

		private final HttpHeaders headers;

		private final Body body;


		public BodyRequestBody(HttpHeaders headers, Body body) {
			this.headers = headers;
			this.body = body;
		}

		@Override
		public long contentLength() {
			return this.headers.getContentLength();
		}

		@Nullable
		@Override
		public MediaType contentType() {
			String contentType = this.headers.getFirst(HttpHeaders.CONTENT_TYPE);
			if (StringUtils.hasText(contentType)) {
				return MediaType.parse(contentType);
			}
			else {
				return null;
			}
		}

		@Override
		public void writeTo(BufferedSink sink) throws IOException {
			this.body.writeTo(sink.outputStream());
		}

		@Override
		public boolean isOneShot() {
			return true;
		}
	}


}

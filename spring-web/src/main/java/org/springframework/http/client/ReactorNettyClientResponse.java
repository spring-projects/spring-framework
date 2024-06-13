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
import java.time.Duration;

import reactor.netty.Connection;
import reactor.netty.http.client.HttpClientResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.support.Netty4HeadersAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpResponse} implementation for the Reactor-Netty HTTP client.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 6.1
 */
final class ReactorNettyClientResponse implements ClientHttpResponse {

	private final HttpClientResponse response;

	private final Connection connection;

	private final HttpHeaders headers;

	private final Duration readTimeout;

	@Nullable
	private volatile InputStream body;


	public ReactorNettyClientResponse(HttpClientResponse response, Connection connection, Duration readTimeout) {
		this.response = response;
		this.connection = connection;
		this.readTimeout = readTimeout;
		this.headers = HttpHeaders.readOnlyHttpHeaders(new Netty4HeadersAdapter(response.responseHeaders()));
	}


	@Override
	public HttpStatusCode getStatusCode() {
		return HttpStatusCode.valueOf(this.response.status().code());
	}

	@Override
	public String getStatusText() {
		return this.response.status().reasonPhrase();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		InputStream body = this.body;
		if (body != null) {
			return body;
		}

		try {
			body = this.connection.inbound().receive().aggregate().asInputStream().block(this.readTimeout);
		}
		catch (RuntimeException ex) {
			throw ReactorNettyClientRequest.convertException(ex);
		}

		if (body == null) {
			body = InputStream.nullInputStream();
		}
		this.body = body;
		return body;
	}

	@Override
	public void close() {
		try{
			InputStream body = getBody();
			StreamUtils.drain(body);
			body.close();
		}
		catch (IOException ignored) {
		}
	}

}

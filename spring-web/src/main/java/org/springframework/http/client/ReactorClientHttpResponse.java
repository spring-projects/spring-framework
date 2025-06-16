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

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.FlowAdapters;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClientResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.support.Netty4HeadersAdapter;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpResponse} implementation for the Reactor-Netty HTTP client.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 6.1
 */
final class ReactorClientHttpResponse implements ClientHttpResponse {

	private final HttpClientResponse response;

	private final Connection connection;

	private final HttpHeaders headers;

	private volatile @Nullable InputStream body;


	/**
	 * Create a response instance.
	 * @param response the Reactor Netty response
	 * @param connection the connection for the exchange
	 * @since 6.2
	 */
	public ReactorClientHttpResponse(HttpClientResponse response, Connection connection) {
		this.response = response;
		this.connection = connection;
		this.headers = HttpHeaders.readOnlyHttpHeaders(
				new Netty4HeadersAdapter(response.responseHeaders()));
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
			SubscriberInputStream<ByteBuf> is = new SubscriberInputStream<>(
					byteBuf -> {
						byte[] bytes = new byte[byteBuf.readableBytes()];
						byteBuf.readBytes(bytes);
						byteBuf.release();
						return bytes;
					},
					ByteBuf::release, 16);
			this.connection.inbound().receive().retain().subscribe(FlowAdapters.toSubscriber(is));
			this.body = is;
			return is;
		}
		catch (RuntimeException ex) {
			throw ReactorClientHttpRequest.convertException(ex);
		}
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

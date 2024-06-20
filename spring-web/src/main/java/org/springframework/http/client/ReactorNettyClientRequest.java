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
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpRequest} implementation for the Reactor-Netty HTTP client.
 * Created via the {@link ReactorNettyClientRequestFactory}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 6.1
 */
final class ReactorNettyClientRequest extends AbstractStreamingClientHttpRequest {

	private final HttpClient httpClient;

	private final HttpMethod method;

	private final URI uri;

	private final Duration exchangeTimeout;

	private final Duration readTimeout;


	public ReactorNettyClientRequest(HttpClient httpClient, URI uri, HttpMethod method,
			Duration exchangeTimeout, Duration readTimeout) {

		this.httpClient = httpClient;
		this.method = method;
		this.uri = uri;
		this.exchangeTimeout = exchangeTimeout;
		this.readTimeout = readTimeout;
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
		HttpClient.RequestSender requestSender = this.httpClient
				.request(io.netty.handler.codec.http.HttpMethod.valueOf(this.method.name()));

		requestSender = (this.uri.isAbsolute() ? requestSender.uri(this.uri) : requestSender.uri(this.uri.toString()));

		try {
			ReactorNettyClientResponse result = requestSender.send((reactorRequest, nettyOutbound) ->
					send(headers, body, reactorRequest, nettyOutbound))
					.responseConnection((reactorResponse, connection) ->
							Mono.just(new ReactorNettyClientResponse(reactorResponse, connection, this.readTimeout)))
					.next()
					.block(this.exchangeTimeout);

			if (result == null) {
				throw new IOException("HTTP exchange resulted in no result");
			}
			else {
				return result;
			}
		}
		catch (RuntimeException ex) {
			throw convertException(ex);
		}
	}

	private Publisher<Void> send(HttpHeaders headers, @Nullable Body body,
			HttpClientRequest reactorRequest, NettyOutbound nettyOutbound) {

		headers.forEach((key, value) -> reactorRequest.requestHeaders().set(key, value));

		if (body != null) {
			AtomicReference<Executor> executor = new AtomicReference<>();

			return nettyOutbound
					.withConnection(connection -> executor.set(connection.channel().eventLoop()))
					.send(FlowAdapters.toPublisher(OutputStreamPublisher.create(
							outputStream -> body.writeTo(StreamUtils.nonClosing(outputStream)),
							new ByteBufMapper(nettyOutbound.alloc()),
							executor.getAndSet(null))));
		}
		else {
			return nettyOutbound;
		}
	}

	static IOException convertException(RuntimeException ex) {
		// Exceptions.ReactiveException is package private
		Throwable cause = ex.getCause();

		if (cause instanceof IOException ioEx) {
			return ioEx;
		}
		if (cause instanceof UncheckedIOException uioEx) {
			IOException ioEx = uioEx.getCause();
			if (ioEx != null) {
				return ioEx;
			}
		}
		return new IOException(ex.getMessage(), (cause != null ? cause : ex));
	}


	private static final class ByteBufMapper implements OutputStreamPublisher.ByteMapper<ByteBuf> {

		private final ByteBufAllocator allocator;

		public ByteBufMapper(ByteBufAllocator allocator) {
			this.allocator = allocator;
		}

		@Override
		public ByteBuf map(int b) {
			ByteBuf byteBuf = this.allocator.buffer(1);
			byteBuf.writeByte(b);
			return byteBuf;
		}

		@Override
		public ByteBuf map(byte[] b, int off, int len) {
			ByteBuf byteBuf = this.allocator.buffer(len);
			byteBuf.writeBytes(b, off, len);
			return byteBuf;
		}
	}

}

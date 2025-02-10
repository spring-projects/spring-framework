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
import org.jspecify.annotations.Nullable;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpRequest} implementation for the Reactor-Netty HTTP client.
 * Created via the {@link ReactorClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 6.1
 */
final class ReactorClientHttpRequest extends AbstractStreamingClientHttpRequest {

	private final HttpClient httpClient;

	private final HttpMethod method;

	private final URI uri;

	private final @Nullable Duration exchangeTimeout;


	/**
	 * Create an instance.
	 * @param httpClient the client to perform the request with
	 * @param method the HTTP method
	 * @param uri the URI for the request
	 * @since 6.2
	 */
	public ReactorClientHttpRequest(HttpClient httpClient, HttpMethod method, URI uri) {
		this.httpClient = httpClient;
		this.method = method;
		this.uri = uri;
		this.exchangeTimeout = null;
	}

	/**
	 * Package private constructor for use until exchangeTimeout is removed.
	 */
	ReactorClientHttpRequest(HttpClient httpClient, HttpMethod method, URI uri, @Nullable Duration exchangeTimeout) {
		this.httpClient = httpClient;
		this.method = method;
		this.uri = uri;
		this.exchangeTimeout = exchangeTimeout;
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

		HttpClient.RequestSender sender = this.httpClient
				.request(io.netty.handler.codec.http.HttpMethod.valueOf(this.method.name()));

		sender = (this.uri.isAbsolute() ? sender.uri(this.uri) : sender.uri(this.uri.toString()));

		try {
			Mono<ReactorClientHttpResponse> mono =
					sender.send((request, outbound) -> send(headers, body, request, outbound))
							.responseConnection((response, conn) -> Mono.just(new ReactorClientHttpResponse(response, conn)))
							.next();

			ReactorClientHttpResponse clientResponse =
					(this.exchangeTimeout != null ? mono.block(this.exchangeTimeout) : mono.block());

			if (clientResponse == null) {
				throw new IOException("HTTP exchange resulted in no result");
			}

			return clientResponse;
		}
		catch (RuntimeException ex) {
			throw convertException(ex);
		}
	}

	private Publisher<Void> send(
			HttpHeaders headers, @Nullable Body body, HttpClientRequest request, NettyOutbound outbound) {

		headers.forEach((key, value) -> request.requestHeaders().set(key, value));

		if (body == null) {
			// NettyOutbound#subscribe calls then() and that expects a body
			// Use empty Mono instead for a more optimal send
			return Mono.empty();
		}

		AtomicReference<Executor> executorRef = new AtomicReference<>();

		return outbound
				.withConnection(connection -> executorRef.set(connection.channel().eventLoop()))
				.send(FlowAdapters.toPublisher(new OutputStreamPublisher<>(
						os -> body.writeTo(StreamUtils.nonClosing(os)), new ByteBufMapper(outbound),
						executorRef.getAndSet(null), null)));
	}

	static IOException convertException(RuntimeException ex) {
		Throwable cause = ex.getCause(); // Exceptions.ReactiveException is private
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

		public ByteBufMapper(NettyOutbound outbound) {
			this.allocator = outbound.alloc();
		}

		@Override
		public ByteBuf map(int b) {
			ByteBuf buf = this.allocator.buffer(1);
			buf.writeByte(b);
			return buf;
		}

		@Override
		public ByteBuf map(byte[] b, int off, int len) {
			ByteBuf buf = this.allocator.buffer(len);
			buf.writeBytes(b, off, len);
			return buf;
		}
	}

}

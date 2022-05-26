/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLSession;

import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import org.xnio.channels.StreamSourceChannel;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Adapt {@link ServerHttpRequest} to the Undertow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class UndertowServerHttpRequest extends AbstractServerHttpRequest {

	private static final AtomicLong logPrefixIndex = new AtomicLong();


	private final HttpServerExchange exchange;

	private final RequestBodyPublisher body;


	public UndertowServerHttpRequest(HttpServerExchange exchange, DataBufferFactory bufferFactory)
			throws URISyntaxException {

		super(initUri(exchange), "", new UndertowHeadersAdapter(exchange.getRequestHeaders()));
		this.exchange = exchange;
		this.body = new RequestBodyPublisher(exchange, bufferFactory);
		this.body.registerListeners(exchange);
	}

	private static URI initUri(HttpServerExchange exchange) throws URISyntaxException {
		Assert.notNull(exchange, "HttpServerExchange is required");
		String requestURL = exchange.getRequestURL();
		String query = exchange.getQueryString();
		String requestUriAndQuery = (StringUtils.hasLength(query) ? requestURL + "?" + query : requestURL);
		return new URI(requestUriAndQuery);
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.exchange.getRequestMethod().toString());
	}

	@Override
	@Deprecated
	public String getMethodValue() {
		return this.exchange.getRequestMethod().toString();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
		// getRequestCookies() is deprecated in Undertow 2.2
		for (String name : this.exchange.getRequestCookies().keySet()) {
			Cookie cookie = this.exchange.getRequestCookies().get(name);
			HttpCookie httpCookie = new HttpCookie(name, cookie.getValue());
			cookies.add(name, httpCookie);
		}
		return cookies;
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		return this.exchange.getDestinationAddress();
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.exchange.getSourceAddress();
	}

	@Nullable
	@Override
	protected SslInfo initSslInfo() {
		SSLSession session = this.exchange.getConnection().getSslSession();
		if (session != null) {
			return new DefaultSslInfo(session);
		}
		return null;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return Flux.from(this.body);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeRequest() {
		return (T) this.exchange;
	}

	@Override
	protected String initId() {
		return ObjectUtils.getIdentityHexString(this.exchange.getConnection()) +
				"-" + logPrefixIndex.incrementAndGet();
	}


	private class RequestBodyPublisher extends AbstractListenerReadPublisher<DataBuffer> {

		private final StreamSourceChannel channel;

		private final DataBufferFactory bufferFactory;

		private final ByteBufferPool byteBufferPool;

		public RequestBodyPublisher(HttpServerExchange exchange, DataBufferFactory bufferFactory) {
			super(UndertowServerHttpRequest.this.getLogPrefix());
			this.channel = exchange.getRequestChannel();
			this.bufferFactory = bufferFactory;
			this.byteBufferPool = exchange.getConnection().getByteBufferPool();
		}

		private void registerListeners(HttpServerExchange exchange) {
			exchange.addExchangeCompleteListener((ex, next) -> {
				onAllDataRead();
				next.proceed();
			});
			this.channel.getReadSetter().set(c -> onDataAvailable());
			this.channel.getCloseSetter().set(c -> onAllDataRead());
			this.channel.resumeReads();
		}

		@Override
		protected void checkOnDataAvailable() {
			this.channel.resumeReads();
			// We are allowed to try, it will return null if data is not available
			onDataAvailable();
		}

		@Override
		protected void readingPaused() {
			this.channel.suspendReads();
		}

		@Override
		@Nullable
		protected DataBuffer read() throws IOException {
			PooledByteBuffer pooledByteBuffer = this.byteBufferPool.allocate();
			try (pooledByteBuffer) {
				ByteBuffer byteBuffer = pooledByteBuffer.getBuffer();
				int read = this.channel.read(byteBuffer);

				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "Read " + read + (read != -1 ? " bytes" : ""));
				}

				if (read > 0) {
					byteBuffer.flip();
					DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(read);
					dataBuffer.write(byteBuffer);
					return dataBuffer;
				}
				else if (read == -1) {
					onAllDataRead();
				}
				return null;
			}
		}

		@Override
		protected void discardData() {
			// Nothing to discard since we pass data buffers on immediately..
		}
	}

}

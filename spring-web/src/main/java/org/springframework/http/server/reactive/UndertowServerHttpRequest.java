/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.IntPredicate;
import javax.net.ssl.SSLSession;

import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HeaderValues;
import org.xnio.channels.StreamSourceChannel;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Adapt {@link ServerHttpRequest} to the Undertow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class UndertowServerHttpRequest extends AbstractServerHttpRequest {

	private final HttpServerExchange exchange;

	private final RequestBodyPublisher body;


	public UndertowServerHttpRequest(HttpServerExchange exchange, DataBufferFactory bufferFactory) {
		super(initUri(exchange), "", initHeaders(exchange));
		this.exchange = exchange;
		this.body = new RequestBodyPublisher(exchange, bufferFactory);
		this.body.registerListeners(exchange);
	}

	private static URI initUri(HttpServerExchange exchange) {
		Assert.notNull(exchange, "HttpServerExchange is required.");
		String requestURL = exchange.getRequestURL();
		String query = exchange.getQueryString();
		String requestUriAndQuery = StringUtils.isEmpty(query) ? requestURL : requestURL + "?" + query;
		return URI.create(requestUriAndQuery);
	}

	private static HttpHeaders initHeaders(HttpServerExchange exchange) {
		HttpHeaders headers = new HttpHeaders();
		for (HeaderValues values : exchange.getRequestHeaders()) {
			headers.put(values.getHeaderName().toString(), values);
		}
		return headers;
	}

	@Override
	public String getMethodValue() {
		return this.exchange.getRequestMethod().toString();
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();
		for (String name : this.exchange.getRequestCookies().keySet()) {
			Cookie cookie = this.exchange.getRequestCookies().get(name);
			HttpCookie httpCookie = new HttpCookie(name, cookie.getValue());
			cookies.add(name, httpCookie);
		}
		return cookies;
	}

	@Override
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


	private static class RequestBodyPublisher extends AbstractListenerReadPublisher<DataBuffer> {

		private final StreamSourceChannel channel;

		private final DataBufferFactory bufferFactory;

		private final ByteBufferPool byteBufferPool;

		public RequestBodyPublisher(HttpServerExchange exchange, DataBufferFactory bufferFactory) {
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
			boolean release = true;
			try {
				ByteBuffer byteBuffer = pooledByteBuffer.getBuffer();

				int read = this.channel.read(byteBuffer);
				if (logger.isTraceEnabled()) {
					logger.trace("Channel read returned " + read + (read != -1 ? " bytes" : ""));
				}

				if (read > 0) {
					byteBuffer.flip();
					DataBuffer dataBuffer = this.bufferFactory.wrap(byteBuffer);
					release = false;
					return new UndertowDataBuffer(dataBuffer, pooledByteBuffer);
				}
				else if (read == -1) {
					onAllDataRead();
				}
				return null;
			} finally {
				if (release && pooledByteBuffer.isOpen()) {
					pooledByteBuffer.close();
				}
			}
		}

	}

	private static class UndertowDataBuffer implements PooledDataBuffer {

		private final DataBuffer dataBuffer;

		private final PooledByteBuffer pooledByteBuffer;

		public UndertowDataBuffer(DataBuffer dataBuffer, PooledByteBuffer pooledByteBuffer) {
			this.dataBuffer = dataBuffer;
			this.pooledByteBuffer = pooledByteBuffer;
		}

		@Override
		public PooledDataBuffer retain() {
			return this;
		}

		@Override
		public boolean release() {
			this.pooledByteBuffer.close();
			return this.pooledByteBuffer.isOpen();
		}

		@Override
		public DataBufferFactory factory() {
			return this.dataBuffer.factory();
		}

		@Override
		public int indexOf(IntPredicate predicate, int fromIndex) {
			return this.dataBuffer.indexOf(predicate, fromIndex);
		}

		@Override
		public int lastIndexOf(IntPredicate predicate, int fromIndex) {
			return this.dataBuffer.lastIndexOf(predicate, fromIndex);
		}

		@Override
		public int readableByteCount() {
			return this.dataBuffer.readableByteCount();
		}

		@Override
		public int writableByteCount() {
			return this.dataBuffer.writableByteCount();
		}

		@Override
		public int readPosition() {
			return this.dataBuffer.readPosition();
		}

		@Override
		public DataBuffer readPosition(int readPosition) {
			return this.dataBuffer.readPosition(readPosition);
		}

		@Override
		public int writePosition() {
			return this.dataBuffer.writePosition();
		}

		@Override
		public DataBuffer writePosition(int writePosition) {
			return this.dataBuffer.writePosition(writePosition);
		}

		@Override
		public int capacity() {
			return this.dataBuffer.capacity();
		}

		@Override
		public DataBuffer capacity(int newCapacity) {
			return this.dataBuffer.capacity(newCapacity);
		}

		@Override
		public byte read() {
			return this.dataBuffer.read();
		}

		@Override
		public DataBuffer read(byte[] destination) {
			return this.dataBuffer.read(destination);
		}

		@Override
		public DataBuffer read(byte[] destination, int offset,
				int length) {
			return this.dataBuffer.read(destination, offset, length);
		}

		@Override
		public DataBuffer write(byte b) {
			return this.dataBuffer.write(b);
		}

		@Override
		public DataBuffer write(byte[] source) {
			return this.dataBuffer.write(source);
		}

		@Override
		public DataBuffer write(byte[] source, int offset,
				int length) {
			return this.dataBuffer.write(source, offset, length);
		}

		@Override
		public DataBuffer write(
				DataBuffer... buffers) {
			return this.dataBuffer.write(buffers);
		}

		@Override
		public DataBuffer write(
				ByteBuffer... byteBuffers) {
			return this.dataBuffer.write(byteBuffers);
		}

		@Override
		public DataBuffer slice(int index, int length) {
			return this.dataBuffer.slice(index, length);
		}

		@Override
		public ByteBuffer asByteBuffer() {
			return this.dataBuffer.asByteBuffer();
		}

		@Override
		public ByteBuffer asByteBuffer(int index, int length) {
			return this.dataBuffer.asByteBuffer(index, length);
		}

		@Override
		public InputStream asInputStream() {
			return this.dataBuffer.asInputStream();
		}

		@Override
		public OutputStream asOutputStream() {
			return this.dataBuffer.asOutputStream();
		}
	}
}

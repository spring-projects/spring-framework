/*
 * Copyright 2002-2016 the original author or authors.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HeaderValues;
import org.xnio.ChannelListener;
import org.xnio.channels.StreamSourceChannel;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Adapt {@link ServerHttpRequest} to the Underow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UndertowServerHttpRequest extends AbstractServerHttpRequest {

	private final HttpServerExchange exchange;

	private final RequestBodyPublisher body;

	public UndertowServerHttpRequest(HttpServerExchange exchange,
			DataBufferFactory dataBufferFactory) {
		Assert.notNull(exchange, "'exchange' is required.");
		this.exchange = exchange;
		this.body = new RequestBodyPublisher(exchange, dataBufferFactory);
		this.body.registerListener();
	}


	public HttpServerExchange getUndertowExchange() {
		return this.exchange;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.getUndertowExchange().getRequestMethod().toString());
	}

	@Override
	protected URI initUri() throws URISyntaxException {
		return new URI(this.exchange.getRequestScheme(), null,
				this.exchange.getHostName(), this.exchange.getHostPort(),
				this.exchange.getRequestURI(), this.exchange.getQueryString(), null);
	}

	@Override
	protected HttpHeaders initHeaders() {
		HttpHeaders headers = new HttpHeaders();
		for (HeaderValues values : this.getUndertowExchange().getRequestHeaders()) {
			headers.put(values.getHeaderName().toString(), values);
		}
		return headers;
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
	public Flux<DataBuffer> getBody() {
		return Flux.from(this.body);
	}

	private static class RequestBodyPublisher extends AbstractRequestBodyPublisher {

		private final ChannelListener<StreamSourceChannel> readListener =
				new ReadListener();

		private final ChannelListener<StreamSourceChannel> closeListener =
				new CloseListener();

		private final StreamSourceChannel requestChannel;

		private final DataBufferFactory dataBufferFactory;

		private final PooledByteBuffer pooledByteBuffer;

		public RequestBodyPublisher(HttpServerExchange exchange,
				DataBufferFactory dataBufferFactory) {
			this.requestChannel = exchange.getRequestChannel();
			this.pooledByteBuffer =
					exchange.getConnection().getByteBufferPool().allocate();
			this.dataBufferFactory = dataBufferFactory;
		}

		private void registerListener() {
			this.requestChannel.getReadSetter().set(this.readListener);
			this.requestChannel.getCloseSetter().set(this.closeListener);
			this.requestChannel.resumeReads();
		}

		@Override
		protected void checkOnDataAvailable() {
			onDataAvailable();
		}

		@Override
		protected DataBuffer read() throws IOException {
			ByteBuffer byteBuffer = this.pooledByteBuffer.getBuffer();
			int read = this.requestChannel.read(byteBuffer);
			if (logger.isTraceEnabled()) {
				logger.trace("read:" + read);
			}

			if (read > 0) {
				byteBuffer.flip();
				return this.dataBufferFactory.wrap(byteBuffer);
			}
			else if (read == -1) {
				onAllDataRead();
			}
			return null;
		}

		private class ReadListener implements ChannelListener<StreamSourceChannel> {

			@Override
			public void handleEvent(StreamSourceChannel channel) {
				onDataAvailable();
			}
		}

		private class CloseListener implements ChannelListener<StreamSourceChannel> {

			@Override
			public void handleEvent(StreamSourceChannel channel) {
				onAllDataRead();
			}
		}
	}
}

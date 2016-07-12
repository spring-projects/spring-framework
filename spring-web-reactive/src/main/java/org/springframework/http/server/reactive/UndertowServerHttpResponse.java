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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.HttpString;
import org.reactivestreams.Publisher;
import org.xnio.ChannelListener;
import org.xnio.channels.StreamSinkChannel;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Undertow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 */
public class UndertowServerHttpResponse extends AbstractServerHttpResponse
		implements ZeroCopyHttpOutputMessage {

	private final HttpServerExchange exchange;


	public UndertowServerHttpResponse(HttpServerExchange exchange,
			DataBufferFactory dataBufferFactory) {
		super(dataBufferFactory);
		Assert.notNull(exchange, "'exchange' is required.");
		this.exchange = exchange;
	}


	public HttpServerExchange getUndertowExchange() {
		return this.exchange;
	}


	@Override
	protected void writeStatusCode() {
		HttpStatus statusCode = this.getStatusCode();
		if (statusCode != null) {
			getUndertowExchange().setStatusCode(statusCode.value());
		}
	}


	@Override
	protected Mono<Void> writeWithInternal(Publisher<DataBuffer> publisher) {
		// lazily create Subscriber, since calling
		// {@link HttpServerExchange#getResponseChannel} as done in the
		// ResponseBodyProcessor constructor commits the response status and headers
		return Mono.from(subscriber -> {
			ResponseBodyProcessor processor = new ResponseBodyProcessor(this.exchange);
			processor.registerListener();
			publisher.subscribe(processor);
			processor.subscribe(subscriber);
		});
	}

	@Override
	public Mono<Void> writeWith(File file, long position, long count) {
		writeHeaders();
		writeCookies();

		try {
			StreamSinkChannel responseChannel =
					getUndertowExchange().getResponseChannel();
			FileChannel in = new FileInputStream(file).getChannel();
			long result = responseChannel.transferFrom(in, position, count);
			if (result < count) {
				return Mono.error(new IOException("Could only write " + result +
						" out of " + count + " bytes"));
			}
			else {
				return Mono.empty();
			}
		}
		catch (IOException ex) {
			return Mono.error(ex);
		}
	}

	@Override
	protected void writeHeaders() {
		for (Map.Entry<String, List<String>> entry : getHeaders().entrySet()) {
			HttpString headerName = HttpString.tryFromString(entry.getKey());
			this.exchange.getResponseHeaders().addAll(headerName, entry.getValue());
		}
	}

	@Override
	protected void writeCookies() {
		for (String name : getCookies().keySet()) {
			for (ResponseCookie httpCookie : getCookies().get(name)) {
				Cookie cookie = new CookieImpl(name, httpCookie.getValue());
				if (!httpCookie.getMaxAge().isNegative()) {
					cookie.setMaxAge((int) httpCookie.getMaxAge().getSeconds());
				}
				httpCookie.getDomain().ifPresent(cookie::setDomain);
				httpCookie.getPath().ifPresent(cookie::setPath);
				cookie.setSecure(httpCookie.isSecure());
				cookie.setHttpOnly(httpCookie.isHttpOnly());
				this.exchange.getResponseCookies().putIfAbsent(name, cookie);
			}
		}
	}

	private static class ResponseBodyProcessor extends AbstractResponseBodyProcessor {

		private final ChannelListener<StreamSinkChannel> listener = new WriteListener();

		private final StreamSinkChannel responseChannel;

		private volatile ByteBuffer byteBuffer;

		public ResponseBodyProcessor(HttpServerExchange exchange) {
			this.responseChannel = exchange.getResponseChannel();
		}

		public void registerListener() {
			this.responseChannel.getWriteSetter().set(this.listener);
			this.responseChannel.resumeWrites();
		}

		@Override
		protected void flush() throws IOException {
			if (logger.isTraceEnabled()) {
				logger.trace("flush");
			}
			this.responseChannel.flush();
		}

		@Override
		protected boolean write(DataBuffer dataBuffer) throws IOException {
			if (this.byteBuffer == null) {
				return false;
			}
			if (logger.isTraceEnabled()) {
				logger.trace("write: " + dataBuffer);
			}
			int total = this.byteBuffer.remaining();
			int written = writeByteBuffer(this.byteBuffer);

			if (logger.isTraceEnabled()) {
				logger.trace("written: " + written + " total: " + total);
			}
			return written == total;
		}

		private int writeByteBuffer(ByteBuffer byteBuffer) throws IOException {
			int written;
			int totalWritten = 0;
			do {
				written = this.responseChannel.write(byteBuffer);
				totalWritten += written;
			}
			while (byteBuffer.hasRemaining() && written > 0);
			return totalWritten;
		}

		@Override
		protected void receiveBuffer(DataBuffer dataBuffer) {
			super.receiveBuffer(dataBuffer);
			this.byteBuffer = dataBuffer.asByteBuffer();
		}

		@Override
		protected void releaseBuffer() {
			super.releaseBuffer();
			this.byteBuffer = null;
		}

		private class WriteListener implements ChannelListener<StreamSinkChannel> {

			@Override
			public void handleEvent(StreamSinkChannel channel) {
				onWritePossible();
			}

		}

	}
}

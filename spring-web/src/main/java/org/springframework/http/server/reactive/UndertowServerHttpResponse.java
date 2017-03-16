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
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.xnio.channels.Channels;
import org.xnio.channels.StreamSinkChannel;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
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
 * @since 5.0
 */
public class UndertowServerHttpResponse extends AbstractListenerServerHttpResponse
		implements ZeroCopyHttpOutputMessage {

	private final HttpServerExchange exchange;

	private StreamSinkChannel responseChannel;


	public UndertowServerHttpResponse(HttpServerExchange exchange, DataBufferFactory bufferFactory) {
		super(bufferFactory);
		Assert.notNull(exchange, "HttpServerExchange is required");
		this.exchange = exchange;
	}


	public HttpServerExchange getUndertowExchange() {
		return this.exchange;
	}


	@Override
	protected void applyStatusCode() {
		HttpStatus statusCode = this.getStatusCode();
		if (statusCode != null) {
			getUndertowExchange().setStatusCode(statusCode.value());
		}
	}

	@Override
	public Mono<Void> writeWith(File file, long position, long count) {
		return doCommit(() -> {
			FileChannel source = null;
			try {
				source = new FileInputStream(file).getChannel();
				StreamSinkChannel destination = getUndertowExchange().getResponseChannel();
				Channels.transferBlocking(destination, source, position, count);
				return Mono.empty();
			}
			catch (IOException ex) {
				return Mono.error(ex);
			}
			finally {
				if (source != null) {
					try {
						source.close();
					}
					catch (IOException ex) {
						// ignore
					}
				}
			}
		});
	}

	@Override
	protected void applyHeaders() {
		for (Map.Entry<String, List<String>> entry : getHeaders().entrySet()) {
			HttpString headerName = HttpString.tryFromString(entry.getKey());
			this.exchange.getResponseHeaders().addAll(headerName, entry.getValue());
		}
	}

	@Override
	protected void applyCookies() {
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

	@Override
	protected Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor() {
		return new ResponseBodyFlushProcessor();
	}

	private ResponseBodyProcessor createBodyProcessor() {
		if (this.responseChannel == null) {
			this.responseChannel = this.exchange.getResponseChannel();
		}
		ResponseBodyProcessor bodyProcessor = new ResponseBodyProcessor( this.responseChannel);
		bodyProcessor.registerListener();
		return bodyProcessor;
	}


	private static class ResponseBodyProcessor extends AbstractListenerWriteProcessor<DataBuffer> {

		private final StreamSinkChannel channel;

		private volatile ByteBuffer byteBuffer;

		public ResponseBodyProcessor(StreamSinkChannel channel) {
			Assert.notNull(channel, "StreamSinkChannel must not be null");
			this.channel = channel;
		}

		public void registerListener() {
			this.channel.getWriteSetter().set(c -> onWritePossible());
			this.channel.resumeWrites();
		}

		@Override
		protected boolean isWritePossible() {
			return false;
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
				written = this.channel.write(byteBuffer);
				totalWritten += written;
			}
			while (byteBuffer.hasRemaining() && written > 0);
			return totalWritten;
		}

		@Override
		protected void receiveData(DataBuffer dataBuffer) {
			super.receiveData(dataBuffer);
			this.byteBuffer = dataBuffer.asByteBuffer();
		}

		@Override
		protected void releaseData() {
			if (logger.isTraceEnabled()) {
				logger.trace("releaseData: " + this.currentData);
			}
			DataBufferUtils.release(this.currentData);
			this.currentData = null;

			this.byteBuffer = null;
		}

		@Override
		protected boolean isDataEmpty(DataBuffer dataBuffer) {
			return (dataBuffer.readableByteCount() == 0);
		}
	}


	private class ResponseBodyFlushProcessor extends AbstractListenerWriteFlushProcessor<DataBuffer> {

		@Override
		protected Processor<? super DataBuffer, Void> createWriteProcessor() {
			return UndertowServerHttpResponse.this.createBodyProcessor();
		}

		@Override
		protected void flush() throws IOException {
			if (UndertowServerHttpResponse.this.responseChannel != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("flush");
				}
				UndertowServerHttpResponse.this.responseChannel.flush();
			}
		}
	}

}

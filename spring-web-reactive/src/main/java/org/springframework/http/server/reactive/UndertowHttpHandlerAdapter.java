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
import java.nio.ByteBuffer;

import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.Assert;

/**
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
public class UndertowHttpHandlerAdapter implements io.undertow.server.HttpHandler {

	private static Log logger = LogFactory.getLog(UndertowHttpHandlerAdapter.class);


	private final HttpHandler delegate;

	private final DataBufferFactory dataBufferFactory;

	public UndertowHttpHandlerAdapter(HttpHandler delegate,
			DataBufferFactory dataBufferFactory) {
		Assert.notNull(delegate, "'delegate' is required");
		Assert.notNull(dataBufferFactory, "'dataBufferFactory' must not be null");
		this.delegate = delegate;
		this.dataBufferFactory = dataBufferFactory;
	}


	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {

		RequestBodyPublisher requestBody =
				new RequestBodyPublisher(exchange, this.dataBufferFactory);
		requestBody.registerListener();
		ServerHttpRequest request = new UndertowServerHttpRequest(exchange, requestBody);

		StreamSinkChannel responseChannel = exchange.getResponseChannel();
		ResponseBodySubscriber responseBody =
				new ResponseBodySubscriber(exchange, responseChannel);
		responseBody.registerListener();
		ServerHttpResponse response =
				new UndertowServerHttpResponse(exchange, responseChannel,
				publisher -> Mono.from(subscriber -> publisher.subscribe(responseBody)),
						this.dataBufferFactory);

		this.delegate.handle(request, response).subscribe(new Subscriber<Void>() {

			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(Void aVoid) {
				// no op
			}

			@Override
			public void onError(Throwable ex) {
				if (exchange.isResponseStarted() || exchange.getStatusCode() > 500) {
					logger.error("Error from request handling. Completing the request.", ex);
				}
				else {
					exchange.setStatusCode(500);
				}
				exchange.endExchange();
			}

			@Override
			public void onComplete() {
				exchange.endExchange();
			}
		});
	}

	private static class RequestBodyPublisher extends AbstractRequestBodyPublisher {

		private static final Log logger = LogFactory.getLog(RequestBodyPublisher.class);

		private final ChannelListener<StreamSourceChannel> listener =
				new RequestBodyListener();

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

		public void registerListener() {
			this.requestChannel.getReadSetter().set(this.listener);
			this.requestChannel.resumeReads();
		}

		private void close() {
			if (this.pooledByteBuffer != null) {
				IoUtils.safeClose(this.pooledByteBuffer);
			}
			if (this.requestChannel != null) {
				IoUtils.safeClose(this.requestChannel);
			}
		}

		@Override
		protected void noLongerStalled() {
			this.listener.handleEvent(this.requestChannel);
		}

		private class RequestBodyListener
				implements ChannelListener<StreamSourceChannel> {

			@Override
			public void handleEvent(StreamSourceChannel channel) {
				if (isSubscriptionCancelled()) {
					return;
				}
				logger.trace("handleEvent");
				ByteBuffer byteBuffer =
						RequestBodyPublisher.this.pooledByteBuffer.getBuffer();
				try {
					while (true) {
						if (!checkSubscriptionForDemand()) {
							break;
						}
						int read = channel.read(byteBuffer);
						logger.trace("Input read:" + read);

						if (read == -1) {
							publishOnComplete();
							close();
							break;
						}
						else if (read == 0) {
							// input not ready, wait until we are invoked again
							break;
						}
						else {
							byteBuffer.flip();
							DataBuffer dataBuffer =
									RequestBodyPublisher.this.dataBufferFactory
											.wrap(byteBuffer);
							publishOnNext(dataBuffer);
						}
					}
				}
				catch (IOException ex) {
					publishOnError(ex);
				}
			}
		}

	}

	private static class ResponseBodySubscriber extends AbstractResponseBodySubscriber {

		private final ChannelListener<StreamSinkChannel> listener =
				new ResponseBodyListener();

		private final HttpServerExchange exchange;

		private final StreamSinkChannel responseChannel;

		private volatile ByteBuffer byteBuffer;

		public ResponseBodySubscriber(HttpServerExchange exchange,
				StreamSinkChannel responseChannel) {
			this.exchange = exchange;
			this.responseChannel = responseChannel;
		}

		public void registerListener() {
			this.responseChannel.getWriteSetter().set(this.listener);
			this.responseChannel.resumeWrites();
		}

		@Override
		protected void writeError(Throwable t) {
			if (!this.exchange.isResponseStarted() &&
					this.exchange.getStatusCode() < 500) {
				this.exchange.setStatusCode(500);
			}
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

		@Override
		protected void close() {
			try {
				this.responseChannel.shutdownWrites();

				if (!this.responseChannel.flush()) {
					this.responseChannel.getWriteSetter().set(ChannelListeners
							.flushingChannelListener(
									o -> IoUtils.safeClose(this.responseChannel),
									ChannelListeners.closingChannelExceptionHandler()));
					this.responseChannel.resumeWrites();
				}
			}
			catch (IOException ignored) {
			}
		}

		private class ResponseBodyListener implements ChannelListener<StreamSinkChannel> {

			@Override
			public void handleEvent(StreamSinkChannel channel) {
				onWritePossible();
			}

		}

	}


}
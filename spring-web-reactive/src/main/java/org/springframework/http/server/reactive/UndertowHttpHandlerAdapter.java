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
import reactor.core.util.BackpressureUtils;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.util.Assert;

/**
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
public class UndertowHttpHandlerAdapter implements io.undertow.server.HttpHandler {

	private static Log logger = LogFactory.getLog(UndertowHttpHandlerAdapter.class);


	private final HttpHandler delegate;

	// TODO: use UndertowDBA when introduced
	private final DataBufferAllocator allocator;

	public UndertowHttpHandlerAdapter(HttpHandler delegate,
			DataBufferAllocator allocator) {
		Assert.notNull(delegate, "'delegate' is required");
		Assert.notNull(allocator, "'allocator' must not be null");
		this.delegate = delegate;
		this.allocator = allocator;
	}


	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {

		RequestBodyPublisher requestBody = new RequestBodyPublisher(exchange, allocator);
		requestBody.registerListener();
		ServerHttpRequest request = new UndertowServerHttpRequest(exchange, requestBody);

		ResponseBodySubscriber responseBody = new ResponseBodySubscriber(exchange);
		responseBody.registerListener();
		ServerHttpResponse response = new UndertowServerHttpResponse(exchange,
				publisher -> Mono.from(subscriber -> publisher.subscribe(responseBody)),
				allocator);

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

		private final DataBufferAllocator allocator;

		private final PooledByteBuffer pooledByteBuffer;

		public RequestBodyPublisher(HttpServerExchange exchange,
				DataBufferAllocator allocator) {
			this.requestChannel = exchange.getRequestChannel();
			this.pooledByteBuffer =
					exchange.getConnection().getByteBufferPool().allocate();
			this.allocator = allocator;
		}

		public void registerListener() {
			this.requestChannel.getReadSetter().set(listener);
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
			listener.handleEvent(requestChannel);
		}

		private class RequestBodyListener
				implements ChannelListener<StreamSourceChannel> {

			@Override
			public void handleEvent(StreamSourceChannel channel) {
				if (isSubscriptionCancelled()) {
					return;
				}
				logger.trace("handleEvent");
				ByteBuffer byteBuffer = pooledByteBuffer.getBuffer();
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
							DataBuffer dataBuffer = allocator.wrap(byteBuffer);
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

	private static class ResponseBodySubscriber implements Subscriber<DataBuffer> {

		private static final Log logger = LogFactory.getLog(ResponseBodySubscriber.class);

		private final ChannelListener<StreamSinkChannel> listener =
				new ResponseBodyListener();

		private final HttpServerExchange exchange;

		private final StreamSinkChannel responseChannel;

		private volatile ByteBuffer byteBuffer;

		private volatile boolean completed = false;

		private Subscription subscription;

		public ResponseBodySubscriber(HttpServerExchange exchange) {
			this.exchange = exchange;
			this.responseChannel = exchange.getResponseChannel();
		}

		public void registerListener() {
			this.responseChannel.getWriteSetter().set(listener);
			this.responseChannel.resumeWrites();
		}


		@Override
		public void onSubscribe(Subscription subscription) {
			logger.trace("onSubscribe. Subscription: " + subscription);
			if (BackpressureUtils.validate(this.subscription, subscription)) {
				this.subscription = subscription;
				this.subscription.request(1);
			}
		}

		@Override
		public void onNext(DataBuffer dataBuffer) {
			Assert.state(this.byteBuffer == null);
			logger.trace("onNext. buffer: " + dataBuffer);

			this.byteBuffer = dataBuffer.asByteBuffer();
		}

		@Override
		public void onError(Throwable t) {
			logger.error("onError", t);
			if (!exchange.isResponseStarted() && exchange.getStatusCode() < 500) {
				exchange.setStatusCode(500);
			}
			closeChannel(responseChannel);
		}

		@Override
		public void onComplete() {
			logger.trace("onComplete. buffer: " + this.byteBuffer);

			this.completed = true;

			if (this.byteBuffer == null) {
				closeChannel(responseChannel);
			}
		}

		private void closeChannel(StreamSinkChannel channel) {
			try {
				channel.shutdownWrites();

				if (!channel.flush()) {
					channel.getWriteSetter().set(ChannelListeners
							.flushingChannelListener(o -> IoUtils.safeClose(channel),
									ChannelListeners.closingChannelExceptionHandler()));
					channel.resumeWrites();
				}
			}
			catch (IOException ignored) {
				logger.error(ignored, ignored);

			}
		}

		private class ResponseBodyListener implements ChannelListener<StreamSinkChannel> {

			@Override
			public void handleEvent(StreamSinkChannel channel) {
				if (byteBuffer != null) {
					try {
						int total = byteBuffer.remaining();
						int written = writeByteBuffer(channel);

						logger.trace("written: " + written + " total: " + total);

						if (written == total) {
							releaseBuffer();
							if (!completed) {
								subscription.request(1);
							}
							else {
								closeChannel(channel);
							}
						}
					}
					catch (IOException ex) {
						onError(ex);
					}
				}
				else if (subscription != null) {
					subscription.request(1);
				}

			}

			private void releaseBuffer() {
				byteBuffer = null;

			}

			private int writeByteBuffer(StreamSinkChannel channel) throws IOException {
				int written;
				int totalWritten = 0;
				do {
					written = channel.write(byteBuffer);
					totalWritten += written;
				}
				while (byteBuffer.hasRemaining() && written > 0);
				return totalWritten;
			}

		}

	}

}

/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.SameThreadExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.xnio.ChannelListener;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;
import reactor.Mono;
import reactor.core.error.Exceptions;
import reactor.core.subscriber.BaseSubscriber;
import reactor.core.support.BackpressureUtils;

import org.springframework.util.Assert;

import static org.xnio.ChannelListeners.closingChannelExceptionHandler;
import static org.xnio.ChannelListeners.flushingChannelListener;
import static org.xnio.IoUtils.safeClose;


/**
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
public class UndertowHttpHandlerAdapter implements io.undertow.server.HttpHandler {

	private static Log logger = LogFactory.getLog(UndertowHttpHandlerAdapter.class);


	private final HttpHandler delegate;


	public UndertowHttpHandlerAdapter(HttpHandler delegate) {
		Assert.notNull(delegate, "'delegate' is required.");
		this.delegate = delegate;
	}


	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {

		RequestBodyPublisher requestBody = new RequestBodyPublisher(exchange);
		ServerHttpRequest request = new UndertowServerHttpRequest(exchange, requestBody);

		ResponseBodySubscriber responseBodySubscriber = new ResponseBodySubscriber(exchange);
		ServerHttpResponse response = new UndertowServerHttpResponse(exchange,
				publisher -> Mono.from(subscriber -> publisher.subscribe(responseBodySubscriber)));

		exchange.dispatch();

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


	private static class RequestBodyPublisher implements Publisher<ByteBuffer> {

		private static final AtomicLongFieldUpdater<RequestBodySubscription> DEMAND =
				AtomicLongFieldUpdater.newUpdater(RequestBodySubscription.class, "demand");


		private final HttpServerExchange exchange;

		private Subscriber<? super ByteBuffer> subscriber;


		public RequestBodyPublisher(HttpServerExchange exchange) {
			this.exchange = exchange;
		}


		@Override
		public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
			if (subscriber == null) {
				throw Exceptions.spec_2_13_exception();
			}
			if (this.subscriber != null) {
				subscriber.onError(new IllegalStateException("Only one subscriber allowed"));
			}

			this.subscriber = subscriber;
			this.subscriber.onSubscribe(new RequestBodySubscription());
		}


		private class RequestBodySubscription implements Subscription, Runnable,
				ChannelListener<StreamSourceChannel> {

			volatile long demand;

			private PooledByteBuffer pooledBuffer;

			private StreamSourceChannel channel;

			private boolean subscriptionClosed;

			private boolean draining;


			@Override
			public void request(long n) {
				BackpressureUtils.checkRequest(n, subscriber);
				if (this.subscriptionClosed) {
					return;
				}
				BackpressureUtils.getAndAdd(DEMAND, this, n);
				scheduleNextMessage();
			}

			private void scheduleNextMessage() {
				exchange.dispatch(exchange.isInIoThread() ? SameThreadExecutor.INSTANCE :
						exchange.getIoThread(), this);
			}

			@Override
			public void cancel() {
				this.subscriptionClosed = true;
				close();
			}

			private void close() {
				if (this.pooledBuffer != null) {
					safeClose(this.pooledBuffer);
					this.pooledBuffer = null;
				}
				if (this.channel != null) {
					safeClose(this.channel);
					this.channel = null;
				}
			}

			@Override
			public void run() {
				if (this.subscriptionClosed || this.draining) {
					return;
				}
				if (0 == BackpressureUtils.getAndSub(DEMAND, this, 1)) {
					return;
				}

				this.draining = true;

				if (this.channel == null) {
					this.channel = exchange.getRequestChannel();

					if (this.channel == null) {
						if (exchange.isRequestComplete()) {
							return;
						}
						else {
							throw new IllegalStateException("Failed to acquire channel!");
						}
					}
				}
				if (this.pooledBuffer == null) {
					this.pooledBuffer = exchange.getConnection().getByteBufferPool().allocate();
				}
				else {
					this.pooledBuffer.getBuffer().clear();
				}

				try {
					ByteBuffer buffer = this.pooledBuffer.getBuffer();
					int count;
					do {
						count = this.channel.read(buffer);
						if (count == 0) {
							this.channel.getReadSetter().set(this);
							this.channel.resumeReads();
						}
						else if (count == -1) {
							if (buffer.position() > 0) {
								doOnNext(buffer);
							}
							doOnComplete();
						}
						else {
							if (buffer.remaining() == 0) {
								if (this.demand == 0) {
									this.channel.suspendReads();
								}
								doOnNext(buffer);
								if (this.demand > 0) {
									scheduleNextMessage();
								}
								break;
							}
						}
					} while (count > 0);
				}
				catch (IOException e) {
					doOnError(e);
				}
			}

			private void doOnNext(ByteBuffer buffer) {
				this.draining = false;
				buffer.flip();
				subscriber.onNext(buffer);
			}

			private void doOnComplete() {
				this.subscriptionClosed = true;
				try {
					subscriber.onComplete();
				}
				finally {
					close();
				}
			}

			private void doOnError(Throwable t) {
				this.subscriptionClosed = true;
				try {
					subscriber.onError(t);
				}
				finally {
					close();
				}
			}

			@Override
			public void handleEvent(StreamSourceChannel channel) {
				if (this.subscriptionClosed) {
					return;
				}

				try {
					ByteBuffer buffer = this.pooledBuffer.getBuffer();
					int count;
					do {
						count = channel.read(buffer);
						if (count == 0) {
							return;
						}
						else if (count == -1) {
							if (buffer.position() > 0) {
								doOnNext(buffer);
							}
							doOnComplete();
						}
						else {
							if (buffer.remaining() == 0) {
								if (this.demand == 0) {
									channel.suspendReads();
								}
								doOnNext(buffer);
								if (this.demand > 0) {
									scheduleNextMessage();
								}
								break;
							}
						}
					} while (count > 0);
				}
				catch (IOException e) {
					doOnError(e);
				}
			}
		}
	}

	private static class ResponseBodySubscriber extends BaseSubscriber<ByteBuffer>
			implements ChannelListener<StreamSinkChannel> {

		private final HttpServerExchange exchange;

		private Subscription subscription;

		private final Queue<PooledByteBuffer> buffers = new ConcurrentLinkedQueue<>();

		private final AtomicInteger writing = new AtomicInteger();

		private final AtomicBoolean closing = new AtomicBoolean();

		private StreamSinkChannel responseChannel;


		public ResponseBodySubscriber(HttpServerExchange exchange) {
			this.exchange = exchange;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			super.onSubscribe(subscription);
			this.subscription = subscription;
			this.subscription.request(1);
		}

		@Override
		public void onNext(ByteBuffer buffer) {
			super.onNext(buffer);

			if (this.responseChannel == null) {
				this.responseChannel = exchange.getResponseChannel();
			}

			this.writing.incrementAndGet();
			try {
				int c;
				do {
					c = this.responseChannel.write(buffer);
				} while (buffer.hasRemaining() && c > 0);

				if (buffer.hasRemaining()) {
					this.writing.incrementAndGet();
					enqueue(buffer);
					this.responseChannel.getWriteSetter().set(this);
					this.responseChannel.resumeWrites();
				}
				else {
					this.subscription.request(1);
				}

			}
			catch (IOException ex) {
				onError(ex);
			}
			finally {
				this.writing.decrementAndGet();
				if (this.closing.get()) {
					closeIfDone();
				}
			}
		}

		private void enqueue(ByteBuffer src) {
			do {
				PooledByteBuffer buffer = exchange.getConnection().getByteBufferPool().allocate();
				ByteBuffer dst = buffer.getBuffer();
				copy(dst, src);
				dst.flip();
				this.buffers.add(buffer);
			} while (src.remaining() > 0);
		}

		private void copy(ByteBuffer dst, ByteBuffer src) {
			int n = Math.min(dst.capacity(), src.remaining());
			for (int i = 0; i < n; i++) {
				dst.put(src.get());
			}
		}

		@Override
		public void handleEvent(StreamSinkChannel channel) {
			try {
				int c;
				do {
					ByteBuffer buffer = this.buffers.peek().getBuffer();
					do {
						c = channel.write(buffer);
					} while (buffer.hasRemaining() && c > 0);

					if (!buffer.hasRemaining()) {
						safeClose(this.buffers.remove());
					}
				} while (!this.buffers.isEmpty() && c > 0);

				if (!this.buffers.isEmpty()) {
					channel.resumeWrites();
				}
				else {
					this.writing.decrementAndGet();

					if (this.closing.get()) {
						closeIfDone();
					}
					else {
						this.subscription.request(1);
					}
				}
			}
			catch (IOException ex) {
				onError(ex);
			}
		}

		@Override
		public void onError(Throwable ex) {
			super.onError(ex);
			logger.error("ResponseBodySubscriber error", ex);
			if (!exchange.isResponseStarted() && exchange.getStatusCode() < 500) {
				exchange.setStatusCode(500);
			}
		}

		@Override
		public void onComplete() {
			super.onComplete();
			if (this.responseChannel != null) {
				this.closing.set(true);
				closeIfDone();
			}
		}

		private void closeIfDone() {
			if (this.writing.get() == 0) {
				if (this.closing.compareAndSet(true, false)) {
					closeChannel();
				}
			}
		}

		private void closeChannel() {
			try {
				this.responseChannel.shutdownWrites();

				if (!this.responseChannel.flush()) {
					this.responseChannel.getWriteSetter().set(flushingChannelListener(
							o -> safeClose(this.responseChannel), closingChannelExceptionHandler()));
					this.responseChannel.resumeWrites();
				}
				this.responseChannel = null;
			}
			catch (IOException ex) {
				onError(ex);
			}
		}
	}

}

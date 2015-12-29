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
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.xnio.ChannelListener;
import org.xnio.channels.StreamSinkChannel;
import reactor.Publishers;
import reactor.core.subscriber.BaseSubscriber;

import static org.xnio.ChannelListeners.closingChannelExceptionHandler;
import static org.xnio.ChannelListeners.flushingChannelListener;
import static org.xnio.IoUtils.safeClose;

/**
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
public class UndertowServerHttpResponse implements ServerHttpResponse {

	private static final Log logger = LogFactory.getLog(UndertowServerHttpResponse.class);


	private final HttpServerExchange exchange;

	private final ResponseBodySubscriber bodySubscriber = new ResponseBodySubscriber();

	private final HttpHeaders headers = new HttpHeaders();

	private boolean headersWritten = false;


	public UndertowServerHttpResponse(HttpServerExchange exchange) {
		Assert.notNull(exchange, "'exchange' is required.");
		this.exchange = exchange;
	}


	@Override
	public void setStatusCode(HttpStatus status) {
		Assert.notNull(status);
		this.exchange.setStatusCode(status.value());
	}

	@Override
	public HttpHeaders getHeaders() {
		return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public Publisher<Void> writeHeaders() {
		applyHeaders();
		return s -> s.onSubscribe(new Subscription() {
			@Override
			public void request(long n) {
				s.onComplete();
			}

			@Override
			public void cancel() {
			}
		});
	}

	private void applyHeaders() {
		if (!this.headersWritten) {
			for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
				HttpString headerName = HttpString.tryFromString(entry.getKey());
				this.exchange.getResponseHeaders().addAll(headerName, entry.getValue());

			}
			this.headersWritten = true;
		}
	}

	@Override
	public Publisher<Void> setBody(Publisher<ByteBuffer> publisher) {
		return Publishers.lift(publisher, new WriteWithOperator<>(writePublisher -> {
			applyHeaders();
			return (subscriber -> writePublisher.subscribe(bodySubscriber));
		}));
	}


	private class ResponseBodySubscriber extends BaseSubscriber<ByteBuffer>
			implements ChannelListener<StreamSinkChannel> {

		private Subscription subscription;

		private final Queue<PooledByteBuffer> buffers = new ConcurrentLinkedQueue<>();

		private final AtomicInteger writing = new AtomicInteger();

		private final AtomicBoolean closing = new AtomicBoolean();

		private StreamSinkChannel responseChannel;


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

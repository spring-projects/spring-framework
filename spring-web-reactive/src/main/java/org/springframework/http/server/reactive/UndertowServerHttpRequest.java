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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.SameThreadExecutor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.xnio.ChannelListener;
import org.xnio.channels.StreamSourceChannel;
import reactor.core.error.SpecificationExceptions;
import reactor.core.support.BackpressureUtils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

import static org.xnio.IoUtils.safeClose;

/**
 * Adapt {@link ServerHttpRequest} to the Underow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
public class UndertowServerHttpRequest implements ServerHttpRequest {

	private final HttpServerExchange exchange;

	private URI uri;

	private HttpHeaders headers;

	private final Publisher<ByteBuffer> body = new RequestBodyPublisher();


	public UndertowServerHttpRequest(HttpServerExchange exchange) {
		Assert.notNull(exchange, "'exchange' is required.");
		this.exchange = exchange;
	}


	public HttpServerExchange getUndertowExchange() {
		return this.exchange;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.getUndertowExchange().getRequestMethod().toString());
	}

	@Override
	public URI getURI() {
		if (this.uri == null) {
			try {
				return new URI(this.getUndertowExchange().getRequestScheme(), null,
						this.getUndertowExchange().getHostName(),
						this.getUndertowExchange().getHostPort(),
						this.getUndertowExchange().getRequestURI(),
						this.getUndertowExchange().getQueryString(), null);
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
			}
		}
		return this.uri;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			for (HeaderValues values : this.getUndertowExchange().getRequestHeaders()) {
				this.headers.put(values.getHeaderName().toString(), values);
			}
		}
		return this.headers;
	}

	@Override
	public Publisher<ByteBuffer> getBody() {
		return this.body;
	}


	private static final AtomicLongFieldUpdater<RequestBodyPublisher.RequestBodySubscription> DEMAND =
			AtomicLongFieldUpdater.newUpdater(RequestBodyPublisher.RequestBodySubscription.class, "demand");

	private class RequestBodyPublisher implements Publisher<ByteBuffer> {

		private Subscriber<? super ByteBuffer> subscriber;


		@Override
		public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
			if (subscriber == null) {
				throw SpecificationExceptions.spec_2_13_exception();
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

}

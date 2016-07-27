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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBuffer;

/**
 * Abstract base class for {@code Processor} implementations that bridge between
 * event-listener APIs and Reactive Streams. Specifically, base class for the
 * Servlet 3.1 and Undertow support.
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @since 5.0
 * @see ServletServerHttpRequest
 * @see UndertowHttpHandlerAdapter
 * @see ServerHttpResponse#writeAndFlushWith(Publisher)
 */
abstract class AbstractResponseBodyFlushProcessor implements Processor<Publisher<DataBuffer>, Void> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final ResponseBodyWriteResultPublisher resultPublisher = new ResponseBodyWriteResultPublisher();

	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	private volatile boolean subscriberCompleted;

	private Subscription subscription;


	// Subscriber

	@Override
	public final void onSubscribe(Subscription subscription) {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " onSubscribe: " + subscription);
		}
		this.state.get().onSubscribe(this, subscription);
	}

	@Override
	public final void onNext(Publisher<DataBuffer> publisher) {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " onNext: " + publisher);
		}
		this.state.get().onNext(this, publisher);
	}

	@Override
	public final void onError(Throwable t) {
		if (logger.isErrorEnabled()) {
			logger.error(this.state + " onError: " + t, t);
		}
		this.state.get().onError(this, t);
	}

	@Override
	public final void onComplete() {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " onComplete");
		}
		this.state.get().onComplete(this);
	}


	// Publisher

	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		this.resultPublisher.subscribe(subscriber);
	}


	/**
	 * Creates a new processor for subscribing to a body chunk.
	 */
	protected abstract Processor<DataBuffer, Void> createBodyProcessor();

	/**
	 * Flushes the output.
	 */
	protected abstract void flush() throws IOException;


	private boolean changeState(State oldState, State newState) {
		return this.state.compareAndSet(oldState, newState);
	}

	private void writeComplete() {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " writeComplete");
		}
		this.state.get().writeComplete(this);

	}

	private void cancel() {
		this.subscription.cancel();
	}


	private enum State {

		UNSUBSCRIBED {

			@Override
			public void onSubscribe(AbstractResponseBodyFlushProcessor processor, Subscription subscription) {
				Objects.requireNonNull(subscription, "Subscription cannot be null");
				if (processor.changeState(this, REQUESTED)) {
					processor.subscription = subscription;
					subscription.request(1);
				}
				else {
					super.onSubscribe(processor, subscription);
				}
			}
		},
		REQUESTED {

			@Override
			public void onNext(AbstractResponseBodyFlushProcessor processor, Publisher<DataBuffer> chunk) {
				if (processor.changeState(this, RECEIVED)) {
					Processor<DataBuffer, Void> chunkProcessor = processor.createBodyProcessor();
					chunk.subscribe(chunkProcessor);
					chunkProcessor.subscribe(new WriteSubscriber(processor));
				}
			}

			@Override
			public void onComplete(AbstractResponseBodyFlushProcessor processor) {
				if (processor.changeState(this, COMPLETED)) {
					processor.resultPublisher.publishComplete();
				}
			}
		},
		RECEIVED {

			@Override
			public void writeComplete(AbstractResponseBodyFlushProcessor processor) {
				try {
					processor.flush();
				}
				catch (IOException ex) {
					processor.cancel();
					processor.onError(ex);
				}

				if (processor.subscriberCompleted) {
					if (processor.changeState(this, COMPLETED)) {
						processor.resultPublisher.publishComplete();
					}
				}
				else {
					if (processor.changeState(this, REQUESTED)) {
						processor.subscription.request(1);
					}
				}
			}

			@Override
			public void onComplete(AbstractResponseBodyFlushProcessor processor) {
				processor.subscriberCompleted = true;
			}
		},
		COMPLETED {

			@Override
			public void onNext(AbstractResponseBodyFlushProcessor processor,
					Publisher<DataBuffer> publisher) {
				// ignore

			}

			@Override
			public void onError(AbstractResponseBodyFlushProcessor processor, Throwable t) {
				// ignore
			}

			@Override
			public void onComplete(AbstractResponseBodyFlushProcessor processor) {
				// ignore
			}
		};

		public void onSubscribe(AbstractResponseBodyFlushProcessor processor, Subscription subscription) {
			subscription.cancel();
		}

		public void onNext(AbstractResponseBodyFlushProcessor processor, Publisher<DataBuffer> publisher) {
			throw new IllegalStateException(toString());
		}

		public void onError(AbstractResponseBodyFlushProcessor processor, Throwable ex) {
			if (processor.changeState(this, COMPLETED)) {
				processor.resultPublisher.publishError(ex);
			}
		}

		public void onComplete(AbstractResponseBodyFlushProcessor processor) {
			throw new IllegalStateException(toString());
		}

		public void writeComplete(AbstractResponseBodyFlushProcessor processor) {
			// ignore
		}


		private static class WriteSubscriber implements Subscriber<Void> {

			private final AbstractResponseBodyFlushProcessor processor;

			public WriteSubscriber(AbstractResponseBodyFlushProcessor processor) {
				this.processor = processor;
			}

			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(Void aVoid) {
			}

			@Override
			public void onError(Throwable ex) {
				this.processor.cancel();
				this.processor.onError(ex);
			}

			@Override
			public void onComplete() {
				this.processor.writeComplete();
			}
		}
	}

}

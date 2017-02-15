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
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.util.Assert;

/**
 * An alternative to {@link AbstractListenerWriteProcessor} but instead writing
 * a {@code Publisher<Publisher<T>>} with flush boundaries enforces after
 * the completion of each nested Publisher.
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractListenerWriteFlushProcessor<T> implements Processor<Publisher<? extends T>, Void> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final WriteResultPublisher resultPublisher = new WriteResultPublisher();

	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	private volatile boolean subscriberCompleted;

	private Subscription subscription;


	// Subscriber implementation...

	@Override
	public final void onSubscribe(Subscription subscription) {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " onSubscribe: " + subscription);
		}
		this.state.get().onSubscribe(this, subscription);
	}

	@Override
	public final void onNext(Publisher<? extends T> publisher) {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " onNext: " + publisher);
		}
		this.state.get().onNext(this, publisher);
	}

	@Override
	public final void onError(Throwable t) {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " onError: " + t);
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


	// Publisher implementation...

	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		this.resultPublisher.subscribe(subscriber);
	}


	/**
	 * Listeners can call this method to cancel further writing.
	 */
	protected void cancel() {
		this.subscription.cancel();
	}


	/**
	 * Create a new processor for subscribing to the next flush boundary.
	 */
	protected abstract Processor<? super T, Void> createWriteProcessor();

	/**
	 * Flush the output.
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


	private enum State {

		UNSUBSCRIBED {
			@Override
			public <T> void onSubscribe(AbstractListenerWriteFlushProcessor<T> processor, Subscription subscription) {
				Assert.notNull(subscription, "Subscription must not be null");
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
			public <T> void onNext(AbstractListenerWriteFlushProcessor<T> processor, Publisher<? extends T> chunk) {
				if (processor.changeState(this, RECEIVED)) {
					Processor<? super T, Void> chunkProcessor = processor.createWriteProcessor();
					chunk.subscribe(chunkProcessor);
					chunkProcessor.subscribe(new WriteSubscriber(processor));
				}
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				if (processor.changeState(this, COMPLETED)) {
					processor.resultPublisher.publishComplete();
				}
			}
		},

		RECEIVED {
			@Override
			public <T> void writeComplete(AbstractListenerWriteFlushProcessor<T> processor) {
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
			public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				processor.subscriberCompleted = true;
			}
		},

		COMPLETED {
			@Override
			public <T> void onNext(AbstractListenerWriteFlushProcessor<T> processor, Publisher<? extends T> publisher) {
				// ignore
			}
			@Override
			public <T> void onError(AbstractListenerWriteFlushProcessor<T> processor, Throwable t) {
				// ignore
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				// ignore
			}
		};

		public <T> void onSubscribe(AbstractListenerWriteFlushProcessor<T> processor, Subscription subscription) {
			subscription.cancel();
		}

		public <T> void onNext(AbstractListenerWriteFlushProcessor<T> processor, Publisher<? extends T> publisher) {
			throw new IllegalStateException(toString());
		}

		public <T> void onError(AbstractListenerWriteFlushProcessor<T> processor, Throwable ex) {
			if (processor.changeState(this, COMPLETED)) {
				processor.resultPublisher.publishError(ex);
			}
		}

		public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
			throw new IllegalStateException(toString());
		}

		public <T> void writeComplete(AbstractListenerWriteFlushProcessor<T> processor) {
			// ignore
		}


		private static class WriteSubscriber implements Subscriber<Void> {

			private final AbstractListenerWriteFlushProcessor<?> processor;

			public WriteSubscriber(AbstractListenerWriteFlushProcessor<?> processor) {
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

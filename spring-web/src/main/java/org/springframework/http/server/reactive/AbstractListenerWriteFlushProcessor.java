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

import org.springframework.lang.Nullable;
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

	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	@Nullable
	private Subscription subscription;

	private volatile boolean subscriberCompleted;

	private final WriteResultPublisher resultPublisher = new WriteResultPublisher();


	// Subscriber methods...

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


	// Publisher method...

	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		this.resultPublisher.subscribe(subscriber);
	}


	// Methods for sub-classes to delegate to, when async I/O events occur...

	protected void cancel() {
		if (this.subscription != null) {
			this.subscription.cancel();
		}
	}


	// Methods for sub-classes to implement or override...

	/**
	 * Create a new processor for subscribing to the next flush boundary.
	 */
	protected abstract Processor<? super T, Void> createWriteProcessor();

	/**
	 * Flush the output if ready, or otherwise {@link #isFlushPending()} should
	 * return true after that.
	 */
	protected abstract void flush() throws IOException;

	/**
	 * Invoked when an error happens while flushing. Defaults to no-op.
	 * Servlet 3.1 based implementations will receive an
	 * {@link javax.servlet.AsyncListener#onError} event.
	 */
	protected void flushingFailed(Throwable t) {
	}

	/**
	 * Whether flushing is pending.
	 */
	protected abstract boolean isFlushPending();

	/**
	 * Listeners can call this to notify when flushing is possible.
	 */
	protected final void onFlushPossible() {
		this.state.get().onFlushPossible(this);
	}

	/**
	 * Whether writing is possible.
	 */
	protected abstract boolean isWritePossible();


	// Private methods for use in State...

	private boolean changeState(State oldState, State newState) {
		return this.state.compareAndSet(oldState, newState);
	}

	private void flushIfPossible() {
		if (isWritePossible()) {
			onFlushPossible();
		}
	}


	/**
	 * Represents a state for the {@link Processor} to be in.
	 *
	 * <p><pre>
	 *        UNSUBSCRIBED
	 *             |
	 *             v
	 *   +--- REQUESTED <--------> RECEIVED ---+
	 *   |                            |        |
	 *   |                            |        |
	 *   |            FLUSHING <------+        |
	 *   |                |                    |
	 *   |                v                    |
	 *   +----------> COMPLETED <--------------+
	 * </pre>
	 */
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
			public <T> void onNext(AbstractListenerWriteFlushProcessor<T> processor,
					Publisher<? extends T> currentPublisher) {

				if (processor.changeState(this, RECEIVED)) {
					Processor<? super T, Void> currentProcessor = processor.createWriteProcessor();
					currentPublisher.subscribe(currentProcessor);
					currentProcessor.subscribe(new WriteResultSubscriber(processor));
				}
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				if (processor.changeState(this, COMPLETED)) {
					processor.resultPublisher.publishComplete();
				}
				else {
					processor.state.get().onComplete(processor);
				}
			}
		},

		RECEIVED {
			@Override
			public <T> void writeComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				try {
					processor.flush();
				}
				catch (Throwable ex) {
					processor.flushingFailed(ex);
					return;
				}
				if (processor.changeState(this, REQUESTED)) {
					if (processor.subscriberCompleted) {
						if (processor.isFlushPending()) {
							// Ensure the final flush
							processor.changeState(REQUESTED, FLUSHING);
							processor.flushIfPossible();
						}
						else if (processor.changeState(REQUESTED, COMPLETED)) {
							processor.resultPublisher.publishComplete();
						}
						else {
							processor.state.get().onComplete(processor);
						}
					}
					else {
						Assert.state(processor.subscription != null, "No subscription");
						processor.subscription.request(1);
					}
				}
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				processor.subscriberCompleted = true;
			}
		},

		FLUSHING {
			public <T> void onFlushPossible(AbstractListenerWriteFlushProcessor<T> processor) {
				try {
					processor.flush();
				}
				catch (Throwable ex) {
					processor.flushingFailed(ex);
					return;
				}
				if (processor.changeState(this, COMPLETED)) {
					processor.resultPublisher.publishComplete();
				}
				else {
					processor.state.get().onComplete(processor);
				}
			}
			public <T> void onNext(AbstractListenerWriteFlushProcessor<T> processor, Publisher<? extends T> publisher) {
				// ignore
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
				// ignore
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
			else {
				processor.state.get().onError(processor, ex);
			}
		}

		public <T> void onComplete(AbstractListenerWriteFlushProcessor<T> processor) {
			throw new IllegalStateException(toString());
		}

		public <T> void writeComplete(AbstractListenerWriteFlushProcessor<T> processor) {
			// ignore
		}

		public <T> void onFlushPossible(AbstractListenerWriteFlushProcessor<T> processor) {
			// ignore
		}


		/**
		 * Subscriber to receive and delegate completion notifications for from
		 * the current Publisher, i.e. within the current flush boundary.
		 */
		private static class WriteResultSubscriber implements Subscriber<Void> {

			private final AbstractListenerWriteFlushProcessor<?> processor;


			public WriteResultSubscriber(AbstractListenerWriteFlushProcessor<?> processor) {
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
				if (this.processor.logger.isTraceEnabled()) {
					this.processor.logger.trace(this.processor.state + " writeComplete");
				}
				this.processor.state.get().writeComplete(this.processor);
			}
		}
	}

}

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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.util.Assert;

/**
 * Abstract base class for {@code Processor} implementations that bridge between
 * event-listener write APIs and Reactive Streams.
 *
 * <p>Specifically a base class for writing to the HTTP response body with
 * Servlet 3.1 non-blocking I/O and Undertow XNIO as well for writing WebSocket
 * messages through the Java WebSocket API (JSR-356), Jetty, and Undertow.
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractListenerWriteProcessor<T> implements Processor<T, Void> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final WriteResultPublisher resultPublisher = new WriteResultPublisher();

	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	protected volatile T currentData;

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
	public final void onNext(T data) {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " onNext: " + data);
		}
		this.state.get().onNext(this, data);
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


	// Listener delegation methods...

	/**
	 * Listeners can call this to notify when writing is possible.
	 */
	public final void onWritePossible() {
		this.state.get().onWritePossible(this);
	}

	/**
	 * Listeners can call this method to cancel further writing.
	 */
	public void cancel() {
		if (this.subscription != null) {
			this.subscription.cancel();
		}
	}


	/**
	 * Called when a data item is received via {@link Subscriber#onNext(Object)}
	 */
	protected void receiveData(T data) {
		if (this.currentData != null) {
			throw new IllegalStateException("Current data not processed yet: " + this.currentData);
		}
		this.currentData = data;
	}

	/**
	 * Called when the current received data item can be released.
	 */
	protected abstract void releaseData();

	/**
	 * Whether the given data item contains any actual data to be processed.
	 */
	protected abstract boolean isDataEmpty(T data);

	/**
	 * Whether writing is possible.
	 */
	protected abstract boolean isWritePossible();

	/**
	 * Writes the given data to the output.
	 * @param data the data to write
	 * @return whether the data was fully written (true)and new data can be
	 * requested or otherwise (false)
	 */
	protected abstract boolean write(T data) throws IOException;


	private boolean changeState(State oldState, State newState) {
		return this.state.compareAndSet(oldState, newState);
	}

	private void writeIfPossible() {
		if (isWritePossible()) {
			onWritePossible();
		}
	}


	/**
	 * Represents a state for the {@link Subscriber} to be in. The following figure
	 * indicate the four different states that exist, and the relationships between them.
	 *
	 * <pre>
	 *       UNSUBSCRIBED
	 *        |
	 *        v
	 * REQUESTED -------------------> RECEIVED
	 *         ^                      ^
	 *         |                      |
	 *         --------- WRITING <-----
	 *                      |
	 *                      v
	 *                  COMPLETED
	 * </pre>
	 * Refer to the individual states for more information.
	 */
	private enum State {

		/**
		 * The initial unsubscribed state. Will respond to {@code onSubscribe} by
		 * requesting 1 data from the subscription, and change state to {@link
		 * #REQUESTED}.
		 */
		UNSUBSCRIBED {
			@Override
			public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
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

		/**
		 * State that gets entered after a data has been
		 * {@linkplain Subscription#request(long) requested}. Responds to {@code onNext}
		 * by changing state to {@link #RECEIVED}, and responds to {@code onComplete} by
		 * changing state to {@link #COMPLETED}.
		 */
		REQUESTED {
			@Override
			public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
				if (processor.isDataEmpty(data)) {
					processor.subscription.request(1);
				}
				else {
					processor.receiveData(data);
					if (processor.changeState(this, RECEIVED)) {
						processor.writeIfPossible();
					}
				}
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				if (processor.changeState(this, COMPLETED)) {
					processor.resultPublisher.publishComplete();
				}
			}
		},

		/**
		 * State that gets entered after a data has been
		 * {@linkplain Subscriber#onNext(Object) received}. Responds to
		 * {@code onWritePossible} by writing the current data and changes
		 * the state to {@link #WRITING}. If it can be written completely,
		 * changes the state to either {@link #REQUESTED} if the subscription
		 * has not been completed; or {@link #COMPLETED} if it has. If it cannot
		 * be written completely the state will be changed to {@code #RECEIVED}.
		 */
		RECEIVED {
			@Override
			public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
				if (processor.changeState(this, WRITING)) {
					T data = processor.currentData;
					try {
						boolean writeCompleted = processor.write(data);
						if (writeCompleted) {
							processor.releaseData();
							if (!processor.subscriberCompleted) {
								processor.changeState(WRITING, REQUESTED);
								processor.subscription.request(1);
							}
							else {
								processor.changeState(WRITING, COMPLETED);
								processor.resultPublisher.publishComplete();
							}
						}
						else {
							processor.changeState(WRITING, RECEIVED);
							processor.writeIfPossible();
						}
					}
					catch (IOException ex) {
						processor.cancel();
						processor.onError(ex);
					}
				}
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				processor.subscriberCompleted = true;
			}
		},

		/**
		 * State that gets entered after a writing of the current data has been
		 * {@code onWritePossible started}.
		 */
		WRITING {
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				processor.subscriberCompleted = true;
			}
		},

		/**
		 * The terminal completed state. Does not respond to any events.
		 */
		COMPLETED {
			@Override
			public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
				// ignore
			}
			@Override
			public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
				// ignore
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				// ignore
			}
		};

		public <T> void onSubscribe(AbstractListenerWriteProcessor<T> processor, Subscription subscription) {
			subscription.cancel();
		}

		public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
			throw new IllegalStateException(toString());
		}

		public <T> void onError(AbstractListenerWriteProcessor<T> processor, Throwable ex) {
			if (processor.changeState(this, COMPLETED)) {
				processor.resultPublisher.publishError(ex);
			}
		}

		public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
			throw new IllegalStateException(toString());
		}

		public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
			// ignore
		}
	}

}

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

import org.springframework.lang.Nullable;
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

	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	@Nullable
	private Subscription subscription;

	@Nullable
	protected volatile T currentData;

	private volatile boolean subscriberCompleted;

	private final WriteResultPublisher resultPublisher = new WriteResultPublisher();


	// Subscriber methods and methods to notify of async I/O events...

	@Override
	public final void onSubscribe(Subscription subscription) {
		this.state.get().onSubscribe(this, subscription);
	}

	@Override
	public final void onNext(T data) {
		logger.trace("Received onNext data item");
		this.state.get().onNext(this, data);
	}

	/**
	 * Notify of an error. This can come from the upstream write Publisher or
	 * from sub-classes as a result of an I/O error.
	 */
	@Override
	public final void onError(Throwable ex) {
		if (logger.isTraceEnabled()) {
			logger.trace("Received onError: " + ex);
		}
		this.state.get().onError(this, ex);
	}

	/**
	 * Notify of completion. This can come from the upstream write Publisher or
	 * from sub-classes as a result of an I/O completion event.
	 */
	@Override
	public final void onComplete() {
		logger.trace("Received onComplete");
		this.state.get().onComplete(this);
	}

	public final void onWritePossible() {
		this.logger.trace("Received onWritePossible");
		this.state.get().onWritePossible(this);
	}

	public void cancel() {
		this.logger.trace("Received request to cancel");
		if (this.subscription != null) {
			this.subscription.cancel();
		}
	}

	// Publisher method...

	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		this.resultPublisher.subscribe(subscriber);
	}


	// Methods for sub-classes to implement or override...

	/**
	 * Whether the given data item has any content to write.
	 * If false the item is not written.
	 */
	protected abstract boolean isDataEmpty(T data);

	/**
	 * Called when a data item is received via {@link Subscriber#onNext(Object)}.
	 * The default implementation saves the data for writing when possible.
	 */
	protected void dataReceived(T data) {
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
	 * Whether writing is possible.
	 */
	protected abstract boolean isWritePossible();

	/**
	 * Write the given item.
	 * @param data the item to write
	 * @return whether the data was fully written ({@code true})
	 * and new data can be requested, or otherwise ({@code false})
	 */
	protected abstract boolean write(T data) throws IOException;

	/**
	 * Suspend writing. Defaults to no-op.
	 */
	protected void suspendWriting() {
	}

	/**
	 * Invoked when writing is complete. Defaults to no-op.
	 */
	protected void writingComplete() {
	}

	/**
	 * Invoked when an error happens while writing.
	 * <p>Defaults to no-op. Servlet 3.1 based implementations will receive
	 * {@code javax.servlet.WriteListener#onError(Throwable)} event.
	 */
	protected void writingFailed(Throwable ex) {
	}


	// Private methods for use in State...

	private boolean changeState(State oldState, State newState) {
		boolean result = this.state.compareAndSet(oldState, newState);
		if (result && logger.isTraceEnabled()) {
			logger.trace(oldState + " -> " + newState);
		}
		return result;
	}

	private void changeStateToComplete(State oldState) {
		if (changeState(oldState, State.COMPLETED)) {
			writingComplete();
			this.resultPublisher.publishComplete();
		}
		else {
			this.state.get().onComplete(this);
		}
	}

	private void writeIfPossible() {
		boolean result = isWritePossible();
		if (logger.isTraceEnabled()) {
			logger.trace("isWritePossible[" + result + "]");
		}
		if (result) {
			onWritePossible();
		}
	}


	/**
	 * Represents a state for the {@link Processor} to be in.
	 *
	 * <p><pre>
	 *        UNSUBSCRIBED
	 *             |
	 *             v
	 *   +--- REQUESTED -------------> RECEIVED ---+
	 *   |        ^                       ^        |
	 *   |        |                       |        |
	 *   |        + ------ WRITING <------+        |
	 *   |                    |                    |
	 *   |                    v                    |
	 *   +--------------> COMPLETED <--------------+
	 * </pre>
	 */
	private enum State {

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

		REQUESTED {
			@Override
			public <T> void onNext(AbstractListenerWriteProcessor<T> processor, T data) {
				if (processor.isDataEmpty(data)) {
					Assert.state(processor.subscription != null, "No subscription");
					processor.subscription.request(1);
				}
				else {
					processor.dataReceived(data);
					if (processor.changeState(this, RECEIVED)) {
						processor.writeIfPossible();
					}
				}
			}
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				processor.changeStateToComplete(this);
			}
		},

		RECEIVED {
			@Override
			public <T> void onWritePossible(AbstractListenerWriteProcessor<T> processor) {
				if (processor.changeState(this, WRITING)) {
					T data = processor.currentData;
					Assert.state(data != null, "No data");
					try {
						boolean writeCompleted = processor.write(data);
						if (writeCompleted) {
							processor.releaseData();
							if (processor.changeState(WRITING, REQUESTED)) {
								if (processor.subscriberCompleted) {
									processor.changeStateToComplete(REQUESTED);
								}
								else {
									processor.suspendWriting();
									Assert.state(processor.subscription != null, "No subscription");
									processor.subscription.request(1);
								}
							}
						}
						else if (processor.changeState(WRITING, RECEIVED)) {
							if (processor.subscriberCompleted) {
								processor.changeStateToComplete(RECEIVED);
							}
							else {
								processor.writeIfPossible();
							}
						}
					}
					catch (IOException ex) {
						processor.writingFailed(ex);
					}
				}
			}

			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				processor.subscriberCompleted = true;
			}
		},

		WRITING {
			@Override
			public <T> void onComplete(AbstractListenerWriteProcessor<T> processor) {
				processor.subscriberCompleted = true;
			}
		},

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
				processor.writingComplete();
				processor.resultPublisher.publishError(ex);
			}
			else {
				processor.state.get().onError(processor, ex);
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

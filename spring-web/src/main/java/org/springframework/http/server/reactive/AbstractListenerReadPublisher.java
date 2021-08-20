/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.server.reactive;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Operators;

import org.springframework.core.log.LogDelegateFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@code Publisher} implementations that bridge between
 * event-listener read APIs and Reactive Streams.
 *
 * <p>Specifically a base class for reading from the HTTP request body with
 * Servlet 3.1 non-blocking I/O and Undertow XNIO as well as handling incoming
 * WebSocket messages with standard Java WebSocket (JSR-356), Jetty, and
 * Undertow.
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the type of element signaled
 */
public abstract class AbstractListenerReadPublisher<T> implements Publisher<T> {

	/**
	 * Special logger for debugging Reactive Streams signals.
	 * @see LogDelegateFactory#getHiddenLog(Class)
	 * @see AbstractListenerWriteProcessor#rsWriteLogger
	 * @see AbstractListenerWriteFlushProcessor#rsWriteFlushLogger
	 * @see WriteResultPublisher#rsWriteResultLogger
	 */
	protected static Log rsReadLogger = LogDelegateFactory.getHiddenLog(AbstractListenerReadPublisher.class);


	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	private volatile long demand;

	@SuppressWarnings("rawtypes")
	private static final AtomicLongFieldUpdater<AbstractListenerReadPublisher> DEMAND_FIELD_UPDATER =
			AtomicLongFieldUpdater.newUpdater(AbstractListenerReadPublisher.class, "demand");

	@Nullable
	private volatile Subscriber<? super T> subscriber;

	private volatile boolean completionPending;

	@Nullable
	private volatile Throwable errorPending;

	private final String logPrefix;


	public AbstractListenerReadPublisher() {
		this("");
	}

	/**
	 * Create an instance with the given log prefix.
	 * @since 5.1
	 */
	public AbstractListenerReadPublisher(String logPrefix) {
		this.logPrefix = logPrefix;
	}


	/**
	 * Return the configured log message prefix.
	 * @since 5.1
	 */
	public String getLogPrefix() {
		return this.logPrefix;
	}


	// Publisher implementation...

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		this.state.get().subscribe(this, subscriber);
	}


	// Async I/O notification methods...

	/**
	 * Invoked when reading is possible, either in the same thread after a check
	 * via {@link #checkOnDataAvailable()}, or as a callback from the underlying
	 * container.
	 */
	public final void onDataAvailable() {
		rsReadLogger.trace(getLogPrefix() + "onDataAvailable");
		this.state.get().onDataAvailable(this);
	}

	/**
	 * Sub-classes can call this method to delegate a contain notification when
	 * all data has been read.
	 */
	public void onAllDataRead() {
		State state = this.state.get();
		if (rsReadLogger.isTraceEnabled()) {
			rsReadLogger.trace(getLogPrefix() + "onAllDataRead [" + state + "]");
		}
		state.onAllDataRead(this);
	}

	/**
	 * Sub-classes can call this to delegate container error notifications.
	 */
	public final void onError(Throwable ex) {
		State state = this.state.get();
		if (rsReadLogger.isTraceEnabled()) {
			rsReadLogger.trace(getLogPrefix() + "onError: " + ex + " [" + state + "]");
		}
		state.onError(this, ex);
	}


	// Read API methods to be implemented or template methods to override...

	/**
	 * Check if data is available and either call {@link #onDataAvailable()}
	 * immediately or schedule a notification.
	 */
	protected abstract void checkOnDataAvailable();

	/**
	 * Read once from the input, if possible.
	 * @return the item that was read; or {@code null}
	 */
	@Nullable
	protected abstract T read() throws IOException;

	/**
	 * Invoked when reading is paused due to a lack of demand.
	 * <p><strong>Note:</strong> This method is guaranteed not to compete with
	 * {@link #checkOnDataAvailable()} so it can be used to safely suspend
	 * reading, if the underlying API supports it, i.e. without competing with
	 * an implicit call to resume via {@code checkOnDataAvailable()}.
	 * @since 5.0.2
	 */
	protected abstract void readingPaused();

	/**
	 * Invoked after an I/O read error from the underlying server or after a
	 * cancellation signal from the downstream consumer to allow sub-classes
	 * to discard any current cached data they might have.
	 * @since 5.0.11
	 */
	protected abstract void discardData();


	// Private methods for use in State...

	/**
	 * Read and publish data one at a time until there is no more data, no more
	 * demand, or perhaps we completed in the mean time.
	 * @return {@code true} if there is more demand; {@code false} if there is
	 * no more demand or we have completed.
	 */
	private boolean readAndPublish() throws IOException {
		long r;
		while ((r = this.demand) > 0 && (this.state.get() != State.COMPLETED)) {
			T data = read();
			if (data != null) {
				if (r != Long.MAX_VALUE) {
					DEMAND_FIELD_UPDATER.addAndGet(this, -1L);
				}
				Subscriber<? super T> subscriber = this.subscriber;
				Assert.state(subscriber != null, "No subscriber");
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "Publishing " + data.getClass().getSimpleName());
				}
				subscriber.onNext(data);
			}
			else {
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "No more to read");
				}
				return true;
			}
		}
		return false;
	}

	private boolean changeState(State oldState, State newState) {
		boolean result = this.state.compareAndSet(oldState, newState);
		if (result && rsReadLogger.isTraceEnabled()) {
			rsReadLogger.trace(getLogPrefix() + oldState + " -> " + newState);
		}
		return result;
	}

	private void changeToDemandState(State oldState) {
		if (changeState(oldState, State.DEMAND)) {
			// Protect from infinite recursion in Undertow, where we can't check if data
			// is available, so all we can do is to try to read.
			// Generally, no need to check if we just came out of readAndPublish()...
			if (oldState != State.READING) {
				checkOnDataAvailable();
			}
		}
	}

	private boolean handlePendingCompletionOrError() {
		State state = this.state.get();
		if (state == State.DEMAND || state  == State.NO_DEMAND) {
			if (this.completionPending) {
				rsReadLogger.trace(getLogPrefix() + "Processing pending completion");
				this.state.get().onAllDataRead(this);
				return true;
			}
			Throwable ex = this.errorPending;
			if (ex != null) {
				if (rsReadLogger.isTraceEnabled()) {
					rsReadLogger.trace(getLogPrefix() + "Processing pending completion with error: " + ex);
				}
				this.state.get().onError(this, ex);
				return true;
			}
		}
		return false;
	}

	private Subscription createSubscription() {
		return new ReadSubscription();
	}


	/**
	 * Subscription that delegates signals to State.
	 */
	private final class ReadSubscription implements Subscription {


		@Override
		public final void request(long n) {
			if (rsReadLogger.isTraceEnabled()) {
				rsReadLogger.trace(getLogPrefix() + "request " + (n != Long.MAX_VALUE ? n : "Long.MAX_VALUE"));
			}
			state.get().request(AbstractListenerReadPublisher.this, n);
		}

		@Override
		public final void cancel() {
			State state = AbstractListenerReadPublisher.this.state.get();
			if (rsReadLogger.isTraceEnabled()) {
				rsReadLogger.trace(getLogPrefix() + "cancel [" + state + "]");
			}
			state.cancel(AbstractListenerReadPublisher.this);
		}
	}


	/**
	 * Represents a state for the {@link Publisher} to be in.
	 * <p><pre>
	 *        UNSUBSCRIBED
	 *             |
	 *             v
	 *        SUBSCRIBING
	 *             |
	 *             v
	 *    +---- NO_DEMAND ---------------> DEMAND ---+
	 *    |        ^                         ^       |
	 *    |        |                         |       |
	 *    |        +------- READING <--------+       |
	 *    |                    |                     |
	 *    |                    v                     |
	 *    +--------------> COMPLETED <---------------+
	 * </pre>
	 */
	private enum State {

		UNSUBSCRIBED {
			@Override
			<T> void subscribe(AbstractListenerReadPublisher<T> publisher, Subscriber<? super T> subscriber) {
				Assert.notNull(publisher, "Publisher must not be null");
				Assert.notNull(subscriber, "Subscriber must not be null");
				if (publisher.changeState(this, SUBSCRIBING)) {
					Subscription subscription = publisher.createSubscription();
					publisher.subscriber = subscriber;
					subscriber.onSubscribe(subscription);
					publisher.changeState(SUBSCRIBING, NO_DEMAND);
					publisher.handlePendingCompletionOrError();
				}
				else {
					throw new IllegalStateException("Failed to transition to SUBSCRIBING, " +
							"subscriber: " + subscriber);
				}
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				publisher.completionPending = true;
				publisher.handlePendingCompletionOrError();
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
				publisher.errorPending = ex;
				publisher.handlePendingCompletionOrError();
			}
		},

		/**
		 * Very brief state where we know we have a Subscriber but must not
		 * send onComplete and onError until we after onSubscribe.
		 */
		SUBSCRIBING {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					publisher.changeToDemandState(this);
				}
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				publisher.completionPending = true;
				publisher.handlePendingCompletionOrError();
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
				publisher.errorPending = ex;
				publisher.handlePendingCompletionOrError();
			}
		},

		NO_DEMAND {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					publisher.changeToDemandState(this);
				}
			}
		},

		DEMAND {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					// Did a concurrent read transition to NO_DEMAND just before us?
					publisher.changeToDemandState(NO_DEMAND);
				}
			}

			@Override
			<T> void onDataAvailable(AbstractListenerReadPublisher<T> publisher) {
				if (publisher.changeState(this, READING)) {
					try {
						boolean demandAvailable = publisher.readAndPublish();
						if (demandAvailable) {
							publisher.changeToDemandState(READING);
							publisher.handlePendingCompletionOrError();
						}
						else {
							publisher.readingPaused();
							if (publisher.changeState(READING, NO_DEMAND)) {
								if (!publisher.handlePendingCompletionOrError()) {
									// Demand may have arrived since readAndPublish returned
									long r = publisher.demand;
									if (r > 0) {
										publisher.changeToDemandState(NO_DEMAND);
									}
								}
							}
						}
					}
					catch (IOException ex) {
						publisher.onError(ex);
					}
				}
				// Else, either competing onDataAvailable (request vs container), or concurrent completion
			}
		},

		READING {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					// Did a concurrent read transition to NO_DEMAND just before us?
					publisher.changeToDemandState(NO_DEMAND);
				}
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				publisher.completionPending = true;
				publisher.handlePendingCompletionOrError();
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
				publisher.errorPending = ex;
				publisher.handlePendingCompletionOrError();
			}
		},

		COMPLETED {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				// ignore
			}
			@Override
			<T> void cancel(AbstractListenerReadPublisher<T> publisher) {
				// ignore
			}
			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				// ignore
			}
			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable t) {
				// ignore
			}
		};

		<T> void subscribe(AbstractListenerReadPublisher<T> publisher, Subscriber<? super T> subscriber) {
			throw new IllegalStateException(toString());
		}

		<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
			throw new IllegalStateException(toString());
		}

		<T> void cancel(AbstractListenerReadPublisher<T> publisher) {
			if (publisher.changeState(this, COMPLETED)) {
				publisher.discardData();
			}
			else {
				publisher.state.get().cancel(publisher);
			}
		}

		<T> void onDataAvailable(AbstractListenerReadPublisher<T> publisher) {
			// ignore
		}

		<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
			if (publisher.changeState(this, COMPLETED)) {
				Subscriber<? super T> s = publisher.subscriber;
				if (s != null) {
					s.onComplete();
				}
			}
			else {
				publisher.state.get().onAllDataRead(publisher);
			}
		}

		<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable t) {
			if (publisher.changeState(this, COMPLETED)) {
				publisher.discardData();
				Subscriber<? super T> s = publisher.subscriber;
				if (s != null) {
					s.onError(t);
				}
			}
			else {
				publisher.state.get().onError(publisher, t);
			}
		}
	}

}

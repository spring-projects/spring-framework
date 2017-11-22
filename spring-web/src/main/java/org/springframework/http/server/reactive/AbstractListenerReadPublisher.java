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
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Operators;

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
 */
public abstract class AbstractListenerReadPublisher<T> implements Publisher<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	private volatile long demand;

	private volatile boolean completionBeforeDemand;

	@Nullable
	private volatile Throwable errorBeforeDemand;

	@SuppressWarnings("rawtypes")
	private static final AtomicLongFieldUpdater<AbstractListenerReadPublisher> DEMAND_FIELD_UPDATER =
			AtomicLongFieldUpdater.newUpdater(AbstractListenerReadPublisher.class, "demand");

	@Nullable
	private Subscriber<? super T> subscriber;


	// Publisher implementation...

	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(this.state + " subscribe: " + subscriber);
		}
		this.state.get().subscribe(this, subscriber);
	}


	// Methods for sub-classes to delegate to, when async I/O events occur...

	public final void onDataAvailable() {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(this.state + " onDataAvailable");
		}
		this.state.get().onDataAvailable(this);
	}

	public void onAllDataRead() {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(this.state + " onAllDataRead");
		}
		this.state.get().onAllDataRead(this);
	}

	public final void onError(Throwable t) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(this.state + " onError: " + t);
		}
		this.state.get().onError(this, t);
	}


	// Methods for sub-classes to implement...

	/**
	 * Check if data is available, calling {@link #onDataAvailable()} either
	 * immediately or later when reading is possible.
	 */
	protected abstract void checkOnDataAvailable();

	/**
	 * Read once from the input, if possible.
	 * @return the item that was read; or {@code null}
	 */
	@Nullable
	protected abstract T read() throws IOException;

	/**
	 * Suspend reading. Defaults to no-op.
	 */
	protected void suspendReading() {
	}


	// Private methods for use in State...

	/**
	 * Read and publish data one at a time until there is no more data, no more
	 * demand, or perhaps we completed in the mean time.
	 * @return {@code true} if there is more demand; {@code false} if there is
	 * no more demand or we have completed.
	 */
	private boolean readAndPublish() throws IOException {
		long r;
		while ((r = this.demand) > 0 && !this.state.get().equals(State.COMPLETED)) {
			T data = read();
			if (data != null) {
				if (r != Long.MAX_VALUE) {
					DEMAND_FIELD_UPDATER.addAndGet(this, -1L);
				}
				Assert.state(this.subscriber != null, "No subscriber");
				this.subscriber.onNext(data);
			}
			else {
				return true;
			}
		}
		return false;
	}

	private boolean changeState(State oldState, State newState) {
		return this.state.compareAndSet(oldState, newState);
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
			if (logger.isTraceEnabled()) {
				logger.trace(state + " request: " + n);
			}
			state.get().request(AbstractListenerReadPublisher.this, n);
		}

		@Override
		public final void cancel() {
			if (logger.isTraceEnabled()) {
				logger.trace(state + " cancel");
			}
			state.get().cancel(AbstractListenerReadPublisher.this);
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
					// Now safe to check "beforeDemand" flags, they won't change once in NO_DEMAND
					if (publisher.completionBeforeDemand) {
						publisher.state.get().onAllDataRead(publisher);
					}
					Throwable ex = publisher.errorBeforeDemand;
					if (ex != null) {
						publisher.state.get().onError(publisher, ex);
					}
				}
				else {
					throw new IllegalStateException("Failed to transition to SUBSCRIBING, " +
							"subscriber: " + subscriber);
				}
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				publisher.completionBeforeDemand = true;
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
				publisher.errorBeforeDemand = ex;
			}
		},

		/**
		 * Very brief state where we know we have a Subscriber but must not
		 * send onComplete and onError until we after onSubscribe.
		 */
		SUBSCRIBING {
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					if (publisher.changeState(this, DEMAND)) {
						publisher.checkOnDataAvailable();
					}
				}
			}

			@Override
			<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
				publisher.completionBeforeDemand = true;
			}

			@Override
			<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable ex) {
				publisher.errorBeforeDemand = ex;
			}
		},

		NO_DEMAND {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					if (publisher.changeState(this, DEMAND)) {
						publisher.checkOnDataAvailable();
					}
					// or else we completed at the same time...
				}
			}
		},

		DEMAND {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					// Did a concurrent read transition to NO_DEMAND just before us?
					if (publisher.changeState(NO_DEMAND, DEMAND)) {
						publisher.checkOnDataAvailable();
					}
				}
			}

			@Override
			<T> void onDataAvailable(AbstractListenerReadPublisher<T> publisher) {
				for (;;) {
					if (!read(publisher)) {
						return;
					}
					// Maybe demand arrived between readAndPublish and READING->NO_DEMAND?
					long r = publisher.demand;
					if (r == 0  || publisher.changeState(NO_DEMAND, this)) {
						break;
					}
				}
			}

			/**
			 * @return whether to exit the read loop; false means stop trying
			 * to read, true means check demand one more time.
			 */
			<T> boolean read(AbstractListenerReadPublisher<T> publisher) {
				if (publisher.changeState(this, READING)) {
					try {
						boolean demandAvailable = publisher.readAndPublish();
						if (demandAvailable) {
							if (publisher.changeState(READING, DEMAND)) {
								publisher.checkOnDataAvailable();
							}
						}
						else if (publisher.changeState(READING, NO_DEMAND)) {
							publisher.suspendReading();
							return true;
						}
					}
					catch (IOException ex) {
						publisher.onError(ex);
					}
				}
				// Either competing onDataAvailable calls (via request or container callback)
				// Or a concurrent completion
				return false;
			}
		},

		READING {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.validate(n)) {
					Operators.addCap(DEMAND_FIELD_UPDATER, publisher, n);
					// Did a concurrent read transition to NO_DEMAND just before us?
					if (publisher.changeState(NO_DEMAND, DEMAND)) {
						publisher.checkOnDataAvailable();
					}
				}
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
			if (!publisher.changeState(this, COMPLETED)) {
				publisher.state.get().cancel(publisher);
			}
		}

		<T> void onDataAvailable(AbstractListenerReadPublisher<T> publisher) {
			// ignore
		}

		<T> void onAllDataRead(AbstractListenerReadPublisher<T> publisher) {
			if (publisher.changeState(this, COMPLETED)) {
				if (publisher.subscriber != null) {
					publisher.subscriber.onComplete();
				}
			}
			else {
				publisher.state.get().onAllDataRead(publisher);
			}
		}

		<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable t) {
			if (publisher.changeState(this, COMPLETED)) {
				if (publisher.subscriber != null) {
					publisher.subscriber.onError(t);
				}
			}
			else {
				publisher.state.get().onError(publisher, t);
			}
		}
	}

}

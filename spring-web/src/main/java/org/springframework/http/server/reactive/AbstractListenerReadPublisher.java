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
import java.nio.channels.Channel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ReadListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Operators;

/**
 * Abstract base class for {@code Publisher} implementations that bridge between
 * event-listener read APIs and Reactive Streams. Specifically, a base class for
 * reading from the HTTP request body with Servlet 3.1 and Undertow as well as
 * handling incoming WebSocket messages with JSR-356, Jetty, and Undertow.
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @since 5.0
 * @see ServletServerHttpRequest
 * @see UndertowHttpHandlerAdapter
 */
public abstract class AbstractListenerReadPublisher<T> implements Publisher<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	private final AtomicLong demand = new AtomicLong();

	private Subscriber<? super T> subscriber;


	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(this.state + " subscribe: " + subscriber);
		}
		this.state.get().subscribe(this, subscriber);
	}

	/**
	 * Called via a listener interface to indicate that reading is possible.
	 * @see ReadListener#onDataAvailable()
	 * @see org.xnio.ChannelListener#handleEvent(Channel)
	 */
	public final void onDataAvailable() {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(this.state + " onDataAvailable");
		}
		this.state.get().onDataAvailable(this);
	}

	/**
	 * Called via a listener interface to indicate that all data has been read.
	 * @see ReadListener#onAllDataRead()
	 * @see org.xnio.ChannelListener#handleEvent(Channel)
	 */
	public final void onAllDataRead() {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(this.state + " onAllDataRead");
		}
		this.state.get().onAllDataRead(this);
	}

	/**
	 * Called by a listener interface to indicate that as error has occurred.
	 * @param t the error
	 * @see ReadListener#onError(Throwable)
	 */
	public final void onError(Throwable t) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(this.state + " onError: " + t, t);
		}
		this.state.get().onError(this, t);
	}

	/**
	 * Reads and publishes data from the input. Continues till either there is no
	 * more demand, or till there is no more data to be read.
	 * @return {@code true} if there is more demand; {@code false} otherwise
	 */
	private boolean readAndPublish() throws IOException {
		while (hasDemand()) {
			T data = read();
			if (data != null) {
				getAndSub(this.demand, 1L);
				this.subscriber.onNext(data);
			}
			else {
				return true;
			}
		}
		return false;
	}

	/**
	 * Concurrent substraction bound to 0 and Long.MAX_VALUE.
	 * Any concurrent write will "happen" before this operation.
	 *
	 * @param sequence current atomic to update
	 * @param toSub    delta to sub
	 * @return value before subscription, 0 or Long.MAX_VALUE
	 */
	private static long getAndSub(AtomicLong sequence, long toSub) {
		long r;
		long u;
		do {
			r = sequence.get();
			if (r == 0 || r == Long.MAX_VALUE) {
				return r;
			}
			u = Operators.subOrZero(r, toSub);
		} while (!sequence.compareAndSet(r, u));

		return r;
	}


	protected abstract void checkOnDataAvailable();

	/**
	 * Reads a data from the input, if possible. Returns {@code null} if a data
	 * could not be read.
	 * @return the data that was read; or {@code null}
	 */
	protected abstract T read() throws IOException;

	private boolean hasDemand() {
		return (this.demand.get() > 0);
	}

	private boolean changeState(State oldState, State newState) {
		return this.state.compareAndSet(oldState, newState);
	}


	private static final class ReadSubscription implements Subscription {

		private final AbstractListenerReadPublisher<?> publisher;

		public ReadSubscription(AbstractListenerReadPublisher<?> publisher) {
			this.publisher = publisher;
		}

		@Override
		public final void request(long n) {
			if (this.publisher.logger.isTraceEnabled()) {
				this.publisher.logger.trace(state() + " request: " + n);
			}
			state().request(this.publisher, n);
		}

		@Override
		public final void cancel() {
			if (this.publisher.logger.isTraceEnabled()) {
				this.publisher.logger.trace(state() + " cancel");
			}
			state().cancel(this.publisher);
		}

		private State state() {
			return this.publisher.state.get();
		}
	}


	/**
	 * Represents a state for the {@link Publisher} to be in. The following figure
	 * indicate the four different states that exist, and the relationships between them.
	 *
	 * <pre>
	 *       UNSUBSCRIBED
	 *        |
	 *        v
	 * NO_DEMAND -------------------> DEMAND
	 *    |    ^                      ^    |
	 *    |    |                      |    |
	 *    |    --------- READING <-----    |
	 *    |                 |              |
	 *    |                 v              |
	 *    ------------> COMPLETED <---------
	 * </pre>
	 * Refer to the individual states for more information.
	 */
	private enum State {

		/**
		 * The initial unsubscribed state. Will respond to {@link
		 * #subscribe(AbstractListenerReadPublisher, Subscriber)} by
		 * changing state to {@link #NO_DEMAND}.
		 */
		UNSUBSCRIBED {
			@Override
			<T> void subscribe(AbstractListenerReadPublisher<T> publisher, Subscriber<? super T> subscriber) {
				Objects.requireNonNull(subscriber);
				if (publisher.changeState(this, NO_DEMAND)) {
					Subscription subscription = new ReadSubscription(publisher);
					publisher.subscriber = subscriber;
					subscriber.onSubscribe(subscription);
				}
				else {
					throw new IllegalStateException(toString());
				}
			}
		},

		/**
		 * State that gets entered when there is no demand. Responds to {@link
		 * #request(AbstractListenerReadPublisher, long)} by increasing the demand,
		 * changing state to {@link #DEMAND} and will check whether there
		 * is data available for reading.
		 */
		NO_DEMAND {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.checkRequest(n, publisher.subscriber)) {
					Operators.addAndGet(publisher.demand, n);
					if (publisher.changeState(this, DEMAND)) {
						publisher.checkOnDataAvailable();
					}
				}
			}
		},

		/**
		 * State that gets entered when there is demand. Responds to
		 * {@link #onDataAvailable(AbstractListenerReadPublisher)} by
		 * reading the available data. The state will be changed to
		 * {@link #NO_DEMAND} if there is no demand.
		 */
		DEMAND {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.checkRequest(n, publisher.subscriber)) {
					Operators.addAndGet(publisher.demand, n);
				}
			}

			@Override
			<T> void onDataAvailable(AbstractListenerReadPublisher<T> publisher) {
				if (publisher.changeState(this, READING)) {
					try {
						boolean demandAvailable = publisher.readAndPublish();
						if (demandAvailable) {
							publisher.changeState(READING, DEMAND);
							publisher.checkOnDataAvailable();
						}
						else {
							publisher.changeState(READING, NO_DEMAND);
						}
					}
					catch (IOException ex) {
						publisher.onError(ex);
					}
				}
			}
		},

		READING {
			@Override
			<T> void request(AbstractListenerReadPublisher<T> publisher, long n) {
				if (Operators.checkRequest(n, publisher.subscriber)) {
					Operators.addAndGet(publisher.demand, n);
				}
			}
		},

		/**
		 * The terminal completed state. Does not respond to any events.
		 */
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
			publisher.changeState(this, COMPLETED);
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
		}

		<T> void onError(AbstractListenerReadPublisher<T> publisher, Throwable t) {
			if (publisher.changeState(this, COMPLETED)) {
				if (publisher.subscriber != null) {
					publisher.subscriber.onError(t);
				}
			}
		}
	}

}

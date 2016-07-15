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

import org.springframework.core.io.buffer.DataBuffer;

/**
 * Abstract base class for {@code Publisher} implementations that bridge between
 * event-listener APIs and Reactive Streams. Specifically, base class for the Servlet 3.1
 * and Undertow support.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @see ServletServerHttpRequest
 * @see UndertowHttpHandlerAdapter
 */
abstract class AbstractRequestBodyPublisher implements Publisher<DataBuffer> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final AtomicReference<State> state =
			new AtomicReference<>(State.UNSUBSCRIBED);

	private final AtomicLong demand = new AtomicLong();

	private Subscriber<? super DataBuffer> subscriber;

	@Override
	public void subscribe(Subscriber<? super DataBuffer> subscriber) {
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
	protected final void onDataAvailable() {
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
	protected final void onAllDataRead() {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(this.state + " onAllDataRead");
		}
		this.state.get().onAllDataRead(this);
	}

	/**
	 * Called by a listener interface to indicate that as error has occured.
	 * @param t the error
	 * @see ReadListener#onError(Throwable)
	 */
	protected final void onError(Throwable t) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(this.state + " onError: " + t, t);
		}
		this.state.get().onError(this, t);
	}

	/**
	 * Reads and publishes data buffers from the input. Continues till either there is no
	 * more demand, or till there is no more data to be read.
	 * @return {@code true} if there is more demand; {@code false} otherwise
	 */
	private boolean readAndPublish() throws IOException {
		while (hasDemand()) {
			DataBuffer dataBuffer = read();
			if (dataBuffer != null) {
				Operators.getAndSub(this.demand, 1L);
				this.subscriber.onNext(dataBuffer);
			}
			else {
				return true;
			}
		}
		return false;
	}

	protected abstract void checkOnDataAvailable();

	/**
	 * Reads a data buffer from the input, if possible. Returns {@code null} if a buffer
	 * could not be read.
	 * @return the data buffer that was read; or {@code null}
	 */
	protected abstract DataBuffer read() throws IOException;

	private boolean hasDemand() {
		return this.demand.get() > 0;
	}

	private boolean changeState(AbstractRequestBodyPublisher.State oldState,
			AbstractRequestBodyPublisher.State newState) {
		return this.state.compareAndSet(oldState, newState);
	}

	private static final class RequestBodySubscription implements Subscription {

		private final AbstractRequestBodyPublisher publisher;

		public RequestBodySubscription(AbstractRequestBodyPublisher publisher) {
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

		private AbstractRequestBodyPublisher.State state() {
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
		 * #subscribe(AbstractRequestBodyPublisher, Subscriber)} by
		 * changing state to {@link #NO_DEMAND}.
		 */
		UNSUBSCRIBED {
			@Override
			void subscribe(AbstractRequestBodyPublisher publisher,
					Subscriber<? super DataBuffer> subscriber) {
				Objects.requireNonNull(subscriber);
				if (publisher.changeState(this, NO_DEMAND)) {
					Subscription subscription = new RequestBodySubscription(
									publisher);
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
		 * #request(AbstractRequestBodyPublisher, long)} by increasing the demand,
		 * changing state to {@link #DEMAND} and will check whether there
		 * is data available for reading.
		 */
		NO_DEMAND {
			@Override
			void request(AbstractRequestBodyPublisher publisher, long n) {
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
		 * {@link #onDataAvailable(AbstractRequestBodyPublisher)} by
		 * reading the available data. The state will be changed to
		 * {@link #NO_DEMAND} if there is no demand.
		 */
		DEMAND {
			@Override
			void onDataAvailable(AbstractRequestBodyPublisher publisher) {
				if (publisher.changeState(this, READING)) {
					try {
						boolean demandAvailable = publisher.readAndPublish();
						if (demandAvailable) {
							publisher.changeState(READING, DEMAND);
							publisher.checkOnDataAvailable();
						} else {
							publisher.changeState(READING, NO_DEMAND);
						}
					} catch (IOException ex) {
						publisher.onError(ex);
					}
				}
			}
		},
		READING {
			@Override
			void request(AbstractRequestBodyPublisher publisher, long n) {
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
			void request(AbstractRequestBodyPublisher publisher, long n) {
				// ignore
			}

			@Override
			void cancel(AbstractRequestBodyPublisher publisher) {
				// ignore
			}

			@Override
			void onAllDataRead(AbstractRequestBodyPublisher publisher) {
				// ignore
			}

			@Override
			void onError(AbstractRequestBodyPublisher publisher, Throwable t) {
				// ignore
			}
		};

		void subscribe(AbstractRequestBodyPublisher publisher,
				Subscriber<? super DataBuffer> subscriber) {
			throw new IllegalStateException(toString());
		}

		void request(AbstractRequestBodyPublisher publisher, long n) {
			throw new IllegalStateException(toString());
		}

		void cancel(AbstractRequestBodyPublisher publisher) {
			publisher.changeState(this, COMPLETED);
		}

		void onDataAvailable(AbstractRequestBodyPublisher publisher) {
			// ignore
		}

		void onAllDataRead(AbstractRequestBodyPublisher publisher) {
			if (publisher.changeState(this, COMPLETED)) {
				if (publisher.subscriber != null) {
					publisher.subscriber.onComplete();
				}
			}
		}

		void onError(AbstractRequestBodyPublisher publisher, Throwable t) {
			if (publisher.changeState(this, COMPLETED)) {
				if (publisher.subscriber != null) {
					publisher.subscriber.onError(t);
				}
			}
		}

	}
}

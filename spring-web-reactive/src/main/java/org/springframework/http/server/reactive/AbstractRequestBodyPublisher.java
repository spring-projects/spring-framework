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
import reactor.core.util.BackpressureUtils;

import org.springframework.core.io.buffer.DataBuffer;

/**
 * Abstract base class for {@code Publisher} implementations that bridge between
 * event-listener APIs and Reactive Streams. Specifically, base class for the Servlet 3.1
 * and Undertow support.
 *
 * @author Arjen Poutsma
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
	 * @return {@code true} if there is more data to be read; {@code false} otherwise
	 */
	private boolean readAndPublish() {
		try {
			while (hasDemand()) {
				DataBuffer dataBuffer = read();
				if (dataBuffer != null) {
					BackpressureUtils.getAndSub(this.demand, 1L);
					this.subscriber.onNext(dataBuffer);
				}
				else {
					return false;
				}
			}
			return true;
		}
		catch (IOException ex) {
			onError(ex);
			return false;
		}
	}

	/**
	 * Reads a data buffer from the input, if possible. Returns {@code null} if a buffer
	 * could not be read.
	 * @return the data buffer that was read; or {@code null}
	 */
	protected abstract DataBuffer read() throws IOException;

	/**
	 * Closes the input.
	 */
	protected abstract void close();

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
	 * DATA_UNAVAILABLE <---> DATA_AVAILABLE
	 *                |       |
	 *                v       v
	 *                COMPLETED
	 * </pre>
	 * Refer to the individual states for more information.
	 */

	private enum State {
		/**
		 * The initial unsubscribed state. Will respond to {@link
		 * #subscribe(AbstractRequestBodyPublisher, Subscriber)} by
		 * changing state to {@link #DATA_UNAVAILABLE}.
		 */
		UNSUBSCRIBED {
			@Override
			void subscribe(AbstractRequestBodyPublisher publisher,
					Subscriber<? super DataBuffer> subscriber) {
				Objects.requireNonNull(subscriber);
				if (publisher.changeState(this, DATA_UNAVAILABLE)) {
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
		 * State that gets entered when there is no data to be read. Responds to {@link
		 * #request(AbstractRequestBodyPublisher, long)} by increasing the demand, and
		 * responds to {@link #onDataAvailable(AbstractRequestBodyPublisher)} by
		 * reading the available data and changing state to {@link #DATA_AVAILABLE} if
		 * there continues to be more data available after the demand has been satisfied.
		 */
		DATA_UNAVAILABLE {
			@Override
			void request(AbstractRequestBodyPublisher publisher, long n) {
				if (BackpressureUtils.checkRequest(n, publisher.subscriber)) {
					BackpressureUtils.addAndGet(publisher.demand, n);
				}
			}

			@Override
			void onDataAvailable(AbstractRequestBodyPublisher publisher) {
				boolean dataAvailable = publisher.readAndPublish();
				if (dataAvailable) {
					publisher.changeState(this, DATA_AVAILABLE);
				}
			}

		},
		/**
		 * State that gets entered when there is data to be read. Responds to {@link
		 * #request(AbstractRequestBodyPublisher, long)} by increasing the demand, and
		 * by reading the available data and changing state to {@link #DATA_UNAVAILABLE}
		 * if there is no more data available.
		 */
		DATA_AVAILABLE {
			@Override
			void request(AbstractRequestBodyPublisher publisher, long n) {
				if (BackpressureUtils.checkRequest(n, publisher.subscriber)) {
					BackpressureUtils.addAndGet(publisher.demand, n);
					boolean dataAvailable = publisher.readAndPublish();
					if (!dataAvailable) {
						publisher.changeState(this, DATA_UNAVAILABLE);
					}
				}
			}
		},
		/**
		 * The terminal completed state. Does not respond to any events.
		 */
		COMPLETED {
			@Override
			void subscribe(AbstractRequestBodyPublisher publisher,
					Subscriber<? super DataBuffer> subscriber) {
				// ignore
			}

			@Override
			void request(AbstractRequestBodyPublisher publisher, long n) {
				// ignore
			}

			@Override
			void cancel(AbstractRequestBodyPublisher publisher) {
				// ignore
			}

			@Override
			void onDataAvailable(AbstractRequestBodyPublisher publisher) {
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
			if (publisher.changeState(this, COMPLETED)) {
				publisher.close();
			}
		}

		void onDataAvailable(AbstractRequestBodyPublisher publisher) {
			throw new IllegalStateException(toString());
		}

		void onAllDataRead(AbstractRequestBodyPublisher publisher) {
			if (publisher.changeState(this, COMPLETED)) {
				publisher.close();
				if (publisher.subscriber != null) {
					publisher.subscriber.onComplete();
				}
			}
		}

		void onError(AbstractRequestBodyPublisher publisher, Throwable t) {
			if (publisher.changeState(this, COMPLETED)) {
				publisher.close();
				if (publisher.subscriber != null) {
					publisher.subscriber.onError(t);
				}
			}
		}

	}
}

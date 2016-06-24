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
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.WriteListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.util.BackpressureUtils;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.FlushingDataBuffer;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@code Subscriber} implementations that bridge between
 * event-listener APIs and Reactive Streams. Specifically, base class for the Servlet 3.1
 * and Undertow support.
 * @author Arjen Poutsma
 * @see ServletServerHttpRequest
 * @see UndertowHttpHandlerAdapter
 */
abstract class AbstractResponseBodySubscriber implements Subscriber<DataBuffer> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final AtomicReference<State> state =
			new AtomicReference<>(State.UNSUBSCRIBED);

	private volatile DataBuffer currentBuffer;

	private volatile boolean subscriptionCompleted;

	private Subscription subscription;

	@Override
	public final void onSubscribe(Subscription subscription) {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " onSubscribe: " + subscription);
		}
		this.state.get().onSubscribe(this, subscription);
	}

	@Override
	public final void onNext(DataBuffer dataBuffer) {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " onNext: " + dataBuffer);
		}
		this.state.get().onNext(this, dataBuffer);
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

	/**
	 * Called via a listener interface to indicate that writing is possible.
	 * @see WriteListener#onWritePossible()
	 * @see org.xnio.ChannelListener#handleEvent(Channel)
	 */
	protected final void onWritePossible() {
		this.state.get().onWritePossible(this);
	}

	/**
	 * Called when a {@link DataBuffer} is received via {@link Subscriber#onNext(Object)}
	 * @param dataBuffer the buffer that was received.
	 */
	protected void receiveBuffer(DataBuffer dataBuffer) {
		Assert.state(this.currentBuffer == null);
		this.currentBuffer = dataBuffer;
	}

	/**
	 * Called when the current buffer should be
	 * {@linkplain DataBufferUtils#release(DataBuffer) released}.
	 */
	protected void releaseBuffer() {
		if (logger.isTraceEnabled()) {
			logger.trace("releaseBuffer: " + this.currentBuffer);
		}
		DataBufferUtils.release(this.currentBuffer);
		this.currentBuffer = null;
	}

	/**
	 * Writes the given data buffer to the output, indicating if the entire buffer was
	 * written.
	 * @param dataBuffer the data buffer to write
	 * @return {@code true} if {@code dataBuffer} was fully written and a new buffer
	 * can be requested; {@code false} otherwise
	 */
	protected abstract boolean write(DataBuffer dataBuffer) throws IOException;

	/**
	 * Writes the given exception to the output.
	 */
	protected abstract void writeError(Throwable t);

	/**
	 * Flushes the output.
	 */
	protected abstract void flush() throws IOException;

	/**
	 * Closes the output.
	 */
	protected abstract void close();

	private void changeState(State oldState, State newState) {
		this.state.compareAndSet(oldState, newState);
	}

	/**
	 * Represents a state for the {@link Subscriber} to be in. The following figure
	 * indicate the four different states that exist, and the relationships between them.
	 *
	 * <pre>
	 *       UNSUBSCRIBED
	 *        |
	 *        v
	 * REQUESTED <---> RECEIVED
	 *         |       |
	 *         v       v
	 *         COMPLETED
	 * </pre>
	 * Refer to the individual states for more information.
	 */
	private enum State {

		/**
		 * The initial unsubscribed state. Will respond to {@code onSubscribe} by
		 * requesting 1 buffer from the subscription, and change state to {@link
		 * #REQUESTED}.
		 */
		UNSUBSCRIBED {
			@Override
			void onSubscribe(AbstractResponseBodySubscriber subscriber,
					Subscription subscription) {
				if (BackpressureUtils.validate(subscriber.subscription, subscription)) {
					subscriber.subscription = subscription;
					subscriber.changeState(this, REQUESTED);
					subscription.request(1);
				}
			}
		},
		/**
		 * State that gets entered after a buffer has been
		 * {@linkplain Subscription#request(long) requested}. Responds to {@code onNext}
		 * by changing state to {@link #RECEIVED}, and responds to {@code onComplete} by
		 * changing state to {@link #COMPLETED}.
		 */
		REQUESTED {
			@Override
			void onNext(AbstractResponseBodySubscriber subscriber,
					DataBuffer dataBuffer) {
				subscriber.changeState(this, RECEIVED);
				subscriber.receiveBuffer(dataBuffer);
			}

			@Override
			void onComplete(AbstractResponseBodySubscriber subscriber) {
				subscriber.subscriptionCompleted = true;
				subscriber.changeState(this, COMPLETED);
				subscriber.close();
			}
		},
		/**
		 * State that gets entered after a buffer has been
		 * {@linkplain Subscriber#onNext(Object) received}. Responds to
		 * {@code onWritePossible} by writing the current buffer, and if it can be
		 * written completely, changes state to either {@link #REQUESTED} if the
		 * subscription has not been completed; or {@link #COMPLETED} if it has.
		 */
		RECEIVED {
			@Override
			void onWritePossible(AbstractResponseBodySubscriber subscriber) {
				DataBuffer dataBuffer = subscriber.currentBuffer;
				try {
					boolean writeCompleted = subscriber.write(dataBuffer);
					if (writeCompleted) {
						if (dataBuffer instanceof FlushingDataBuffer) {
							subscriber.flush();
						}
						subscriber.releaseBuffer();
						boolean subscriptionCompleted = subscriber.subscriptionCompleted;
						if (!subscriptionCompleted) {
							subscriber.changeState(this, REQUESTED);
							subscriber.subscription.request(1);
						}
						else {
							subscriber.changeState(this, COMPLETED);
							subscriber.close();
						}
					}
				}
				catch (IOException ex) {
					subscriber.onError(ex);
				}
			}

			@Override
			void onComplete(AbstractResponseBodySubscriber subscriber) {
				subscriber.subscriptionCompleted = true;
			}
		},
		/**
		 * The terminal completed state. Does not respond to any events.
		 */
		COMPLETED {
			@Override
			void onNext(AbstractResponseBodySubscriber subscriber,
					DataBuffer dataBuffer) {
				// ignore
			}

			@Override
			void onError(AbstractResponseBodySubscriber subscriber, Throwable t) {
				// ignore
			}

			@Override
			void onComplete(AbstractResponseBodySubscriber subscriber) {
				// ignore
			}
		};

		void onSubscribe(AbstractResponseBodySubscriber subscriber, Subscription s) {
			throw new IllegalStateException(toString());
		}

		void onNext(AbstractResponseBodySubscriber subscriber, DataBuffer dataBuffer) {
			throw new IllegalStateException(toString());
		}

		void onError(AbstractResponseBodySubscriber subscriber, Throwable t) {
			subscriber.changeState(this, COMPLETED);
			subscriber.writeError(t);
			subscriber.close();
		}

		void onComplete(AbstractResponseBodySubscriber subscriber) {
			throw new IllegalStateException(toString());
		}

		void onWritePossible(AbstractResponseBodySubscriber subscriber) {
			// ignore
		}

	}

}

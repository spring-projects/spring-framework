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
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.WriteListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@code Processor} implementations that bridge between
 * event-listener APIs and Reactive Streams. Specifically, base class for the
 * Servlet 3.1 and Undertow support.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @see ServletServerHttpRequest
 * @see UndertowHttpHandlerAdapter
 * @see ServerHttpResponse#writeWith(Publisher)
 */
abstract class AbstractResponseBodyProcessor implements Processor<DataBuffer, Void> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final ResponseBodyWriteResultPublisher publisherDelegate =
			new ResponseBodyWriteResultPublisher();

	private final AtomicReference<State> state =
			new AtomicReference<>(State.UNSUBSCRIBED);

	private volatile DataBuffer currentBuffer;

	private volatile boolean subscriberCompleted;

	private Subscription subscription;

	// Subscriber

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

	// Publisher

	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		this.publisherDelegate.subscribe(subscriber);
	}

	// listener methods

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
	 * Called when a {@link DataBuffer} is received via {@link Subscriber#onNext(Object)}
	 * or when only partial data from the {@link DataBuffer} was written.
	 */
	private void writeIfPossible() {
		if (isWritePossible()) {
			onWritePossible();
		}
	}

	/**
	 * Called via a listener interface to determine whether writing is possible.
	 */
	protected boolean isWritePossible() {
		return false;
	}

	/**
	 * Writes the given data buffer to the output, indicating if the entire buffer was
	 * written.
	 * @param dataBuffer the data buffer to write
	 * @return {@code true} if {@code dataBuffer} was fully written and a new buffer
	 * can be requested; {@code false} otherwise
	 */
	protected abstract boolean write(DataBuffer dataBuffer) throws IOException;

	private boolean changeState(State oldState, State newState) {
		return this.state.compareAndSet(oldState, newState);
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
		 * requesting 1 buffer from the subscription, and change state to {@link
		 * #REQUESTED}.
		 */
		UNSUBSCRIBED {
			@Override
			void onSubscribe(AbstractResponseBodyProcessor processor,
					Subscription subscription) {
				Objects.requireNonNull(subscription, "Subscription cannot be null");
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
		 * State that gets entered after a buffer has been
		 * {@linkplain Subscription#request(long) requested}. Responds to {@code onNext}
		 * by changing state to {@link #RECEIVED}, and responds to {@code onComplete} by
		 * changing state to {@link #COMPLETED}.
		 */
		REQUESTED {
			@Override
			void onNext(AbstractResponseBodyProcessor processor, DataBuffer dataBuffer) {
				if (processor.changeState(this, RECEIVED)) {
					processor.receiveBuffer(dataBuffer);
					processor.writeIfPossible();
				}
			}

			@Override
			void onComplete(AbstractResponseBodyProcessor processor) {
				if (processor.changeState(this, COMPLETED)) {
					processor.subscriberCompleted = true;
					processor.publisherDelegate.publishComplete();
				}
			}
		},
		/**
		 * State that gets entered after a buffer has been
		 * {@linkplain Subscriber#onNext(Object) received}. Responds to
		 * {@code onWritePossible} by writing the current buffer and changes
		 * the state to {@link #WRITING}. If it can be written completely,
		 * changes the state to either {@link #REQUESTED} if the subscription
		 * has not been completed; or {@link #COMPLETED} if it has. If it cannot
		 * be written completely the state will be changed to {@link #RECEIVED}.
		 */
		RECEIVED {
			@Override
			void onWritePossible(AbstractResponseBodyProcessor processor) {
				if (processor.changeState(this, WRITING)) {
					DataBuffer dataBuffer = processor.currentBuffer;
					try {
						boolean writeCompleted = processor.write(dataBuffer);
						if (writeCompleted) {
							processor.releaseBuffer();
							if (!processor.subscriberCompleted) {
								processor.changeState(WRITING, REQUESTED);
								processor.subscription.request(1);
							}
							else {
								processor.changeState(WRITING, COMPLETED);
								processor.publisherDelegate.publishComplete();
							}
						}
						else {
							processor.changeState(WRITING, RECEIVED);
							processor.writeIfPossible();
						}
					}
					catch (IOException ex) {
						processor.onError(ex);
					}
				}
			}

			@Override
			void onComplete(AbstractResponseBodyProcessor processor) {
				processor.subscriberCompleted = true;
			}
		},
		/**
		 * State that gets entered after a writing of the current buffer has been
		 * {@code onWritePossible started}.
		 */
		WRITING {
			@Override
			void onComplete(AbstractResponseBodyProcessor processor) {
				processor.subscriberCompleted = true;
			}
		},
		/**
		 * The terminal completed state. Does not respond to any events.
		 */
		COMPLETED {
			@Override
			void onNext(AbstractResponseBodyProcessor processor, DataBuffer dataBuffer) {
				// ignore
			}

			@Override
			void onError(AbstractResponseBodyProcessor processor, Throwable t) {
				// ignore
			}

			@Override
			void onComplete(AbstractResponseBodyProcessor processor) {
				// ignore
			}
		};

		void onSubscribe(AbstractResponseBodyProcessor processor, Subscription s) {
			s.cancel();
		}

		void onNext(AbstractResponseBodyProcessor processor, DataBuffer dataBuffer) {
			throw new IllegalStateException(toString());
		}

		void onError(AbstractResponseBodyProcessor processor, Throwable t) {
			if (processor.changeState(this, COMPLETED)) {
				processor.publisherDelegate.publishError(t);
			}
		}

		void onComplete(AbstractResponseBodyProcessor processor) {
			throw new IllegalStateException(toString());
		}
		void onWritePossible(AbstractResponseBodyProcessor processor) {
			// ignore
		}

	}

}

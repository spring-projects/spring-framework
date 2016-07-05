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
abstract class AbstractResponseBodyProcessor implements Processor<DataBuffer, Void> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final AtomicReference<SubscriberState> subscriberState =
			new AtomicReference<>(SubscriberState.UNSUBSCRIBED);

	private final AtomicReference<PublisherState> publisherState =
			new AtomicReference<>(PublisherState.UNSUBSCRIBED);

	private volatile DataBuffer currentBuffer;

	private volatile boolean subscriberCompleted;

	private volatile boolean publisherCompleted;

	private volatile Throwable publisherError;

	private Subscription subscription;

	private Subscriber<? super Void> subscriber;

	// Subscriber

	@Override
	public final void onSubscribe(Subscription subscription) {
		if (logger.isTraceEnabled()) {
			logger.trace("SUB " + this.subscriberState + " onSubscribe: " + subscription);
		}
		this.subscriberState.get().onSubscribe(this, subscription);
	}

	@Override
	public final void onNext(DataBuffer dataBuffer) {
		if (logger.isTraceEnabled()) {
			logger.trace("SUB " + this.subscriberState + " onNext: " + dataBuffer);
		}
		this.subscriberState.get().onNext(this, dataBuffer);
	}

	@Override
	public final void onError(Throwable t) {
		if (logger.isErrorEnabled()) {
			logger.error("SUB " + this.subscriberState + " publishError: " + t, t);
		}
		this.subscriberState.get().onError(this, t);
	}

	@Override
	public final void onComplete() {
		if (logger.isTraceEnabled()) {
			logger.trace("SUB " + this.subscriberState + " onComplete");
		}
		this.subscriberState.get().onComplete(this);
	}

	// Publisher

	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		if (logger.isTraceEnabled()) {
			logger.trace("PUB " + this.publisherState + " subscribe: " + subscriber);
		}
		this.publisherState.get().subscribe(this, subscriber);
	}

	private void publishComplete() {
		if (logger.isTraceEnabled()) {
			logger.trace("PUB " + this.publisherState + " publishComplete");
		}
		this.publisherState.get().publishComplete(this);
	}

	private void publishError(Throwable t) {
		if (logger.isTraceEnabled()) {
			logger.trace("PUB " + this.publisherState + " publishError: " + t);
		}
		this.publisherState.get().publishError(this, t);
	}

	// listener methods

	/**
	 * Called via a listener interface to indicate that writing is possible.
	 * @see WriteListener#onWritePossible()
	 * @see org.xnio.ChannelListener#handleEvent(Channel)
	 */
	protected final void onWritePossible() {
		this.subscriberState.get().onWritePossible(this);
	}

	/**
	 * Called when a {@link DataBuffer} is received via {@link Subscriber#onNext(Object)}
	 * @param dataBuffer the buffer that was received.
	 */
	protected void receiveBuffer(DataBuffer dataBuffer) {
		Assert.state(this.currentBuffer == null);
		this.currentBuffer = dataBuffer;

		checkOnWritePossible();
	}

	/**
	 * Called when a {@link DataBuffer} is received via {@link Subscriber#onNext(Object)}
	 * or when only partial data from the {@link DataBuffer} was written.
	 */
	protected void checkOnWritePossible() {
		// no-op
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
	 * Flushes the output.
	 */
	protected abstract void flush() throws IOException;

	/**
	 * Closes the output.
	 */
	protected abstract void close();

	private boolean changeSubscriberState(SubscriberState oldState,
			SubscriberState newState) {
		return this.subscriberState.compareAndSet(oldState, newState);
	}

	private boolean changePublisherState(PublisherState oldState,
			PublisherState newState) {
		return this.publisherState.compareAndSet(oldState, newState);
	}

	private static final class ResponseBodySubscription implements Subscription {

		private final AbstractResponseBodyProcessor processor;

		public ResponseBodySubscription(AbstractResponseBodyProcessor processor) {
			this.processor = processor;
		}

		@Override
		public final void request(long n) {
			if (this.processor.logger.isTraceEnabled()) {
				this.processor.logger.trace("PUB " + state() + " request: " + n);
			}
			state().request(this.processor, n);
		}

		@Override
		public final void cancel() {
			if (this.processor.logger.isTraceEnabled()) {
				this.processor.logger.trace("PUB " + state() + " cancel");
			}
			state().cancel(this.processor);
		}

		private PublisherState state() {
			return this.processor.publisherState.get();
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
	private enum SubscriberState {

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
				if (processor.changeSubscriberState(this, REQUESTED)) {
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
				if (processor.changeSubscriberState(this, RECEIVED)) {
					processor.receiveBuffer(dataBuffer);
				}
			}

			@Override
			void onComplete(AbstractResponseBodyProcessor processor) {
				if (processor.changeSubscriberState(this, COMPLETED)) {
					processor.subscriberCompleted = true;
					processor.close();
					processor.publishComplete();
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
				if (processor.changeSubscriberState(this, WRITING)) {
					DataBuffer dataBuffer = processor.currentBuffer;
					try {
						boolean writeCompleted = processor.write(dataBuffer);
						if (writeCompleted) {
							if (dataBuffer instanceof FlushingDataBuffer) {
								processor.flush();
							}
							processor.releaseBuffer();
							if (!processor.subscriberCompleted) {
								processor.changeSubscriberState(WRITING, REQUESTED);
								processor.subscription.request(1);
							}
							else {
								processor.changeSubscriberState(WRITING, COMPLETED);
								processor.close();
								processor.publishComplete();
							}
						}
						else {
							processor.changeSubscriberState(WRITING, RECEIVED);
							processor.checkOnWritePossible();
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
			if (processor.changeSubscriberState(this, COMPLETED)) {
				processor.publishError(t);
			}
		}

		void onComplete(AbstractResponseBodyProcessor processor) {
			throw new IllegalStateException(toString());
		}

		void onWritePossible(AbstractResponseBodyProcessor processor) {
			// ignore
		}
	}

	private enum PublisherState {
		UNSUBSCRIBED {
			@Override
			void subscribe(AbstractResponseBodyProcessor processor,
					Subscriber<? super Void> subscriber) {
				Objects.requireNonNull(subscriber);
				if (processor.changePublisherState(this, SUBSCRIBED)) {
					Subscription subscription = new ResponseBodySubscription(processor);
					processor.subscriber = subscriber;
					subscriber.onSubscribe(subscription);
					if (processor.publisherCompleted) {
						processor.publishComplete();
					}
					else if (processor.publisherError != null) {
						processor.publishError(processor.publisherError);
					}
				}
				else {
					throw new IllegalStateException(toString());
				}
			}

			@Override
			void publishComplete(AbstractResponseBodyProcessor processor) {
				processor.publisherCompleted = true;
			}

			@Override
			void publishError(AbstractResponseBodyProcessor processor, Throwable t) {
				processor.publisherError = t;
			}
		},
		SUBSCRIBED {
			@Override
			void request(AbstractResponseBodyProcessor processor, long n) {
				BackpressureUtils.checkRequest(n, processor.subscriber);
			}

			@Override
			void publishComplete(AbstractResponseBodyProcessor processor) {
				if (processor.changePublisherState(this, COMPLETED)) {
					processor.subscriber.onComplete();
				}
			}

			@Override
			void publishError(AbstractResponseBodyProcessor processor, Throwable t) {
				if (processor.changePublisherState(this, COMPLETED)) {
					processor.subscriber.onError(t);
				}
			}

		},
		COMPLETED {
			@Override
			void request(AbstractResponseBodyProcessor processor, long n) {
				// ignore
			}

			@Override
			void cancel(AbstractResponseBodyProcessor processor) {
				// ignore
			}

			@Override
			void publishComplete(AbstractResponseBodyProcessor processor) {
				// ignore
			}

			@Override
			void publishError(AbstractResponseBodyProcessor processor, Throwable t) {
				// ignore
			}
		};

		void subscribe(AbstractResponseBodyProcessor processor,
				Subscriber<? super Void> subscriber) {
			throw new IllegalStateException(toString());
		}

		void request(AbstractResponseBodyProcessor processor, long n) {
			throw new IllegalStateException(toString());
		}

		void cancel(AbstractResponseBodyProcessor processor) {
			processor.changePublisherState(this, COMPLETED);
		}

		void publishComplete(AbstractResponseBodyProcessor processor) {
			throw new IllegalStateException(toString());
		}

		void publishError(AbstractResponseBodyProcessor processor, Throwable t) {
			throw new IllegalStateException(toString());
		}

	}

}

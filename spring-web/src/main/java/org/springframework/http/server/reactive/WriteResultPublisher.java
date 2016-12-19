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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Operators;

/**
 * Publisher returned from {@link ServerHttpResponse#writeWith(Publisher)}.
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @since 5.0
 */
class WriteResultPublisher implements Publisher<Void> {

	private static final Log logger = LogFactory.getLog(WriteResultPublisher.class);

	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	private Subscriber<? super Void> subscriber;

	private volatile boolean publisherCompleted;

	private volatile Throwable publisherError;


	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " subscribe: " + subscriber);
		}
		this.state.get().subscribe(this, subscriber);
	}

	private boolean changeState(State oldState, State newState) {
		return this.state.compareAndSet(oldState, newState);
	}

	/**
	 * Publishes the complete signal to the subscriber of this publisher.
	 */
	public void publishComplete() {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " publishComplete");
		}
		this.state.get().publishComplete(this);
	}

	/**
	 * Publishes the given error signal to the subscriber of this publisher.
	 */
	public void publishError(Throwable t) {
		if (logger.isTraceEnabled()) {
			logger.trace(this.state + " publishError: " + t);
		}
		this.state.get().publishError(this, t);
	}


	private static final class ResponseBodyWriteResultSubscription implements Subscription {

		private final WriteResultPublisher publisher;

		public ResponseBodyWriteResultSubscription(WriteResultPublisher publisher) {
			this.publisher = publisher;
		}

		@Override
		public final void request(long n) {
			if (logger.isTraceEnabled()) {
				logger.trace(state() + " request: " + n);
			}
			state().request(this.publisher, n);
		}

		@Override
		public final void cancel() {
			if (logger.isTraceEnabled()) {
				logger.trace(state() + " cancel");
			}
			state().cancel(this.publisher);
		}

		private State state() {
			return this.publisher.state.get();
		}
	}


	private enum State {

		UNSUBSCRIBED {
			@Override
			void subscribe(WriteResultPublisher publisher,
					Subscriber<? super Void> subscriber) {
				Objects.requireNonNull(subscriber);
				if (publisher.changeState(this, SUBSCRIBED)) {
					Subscription subscription =
							new ResponseBodyWriteResultSubscription(publisher);
					publisher.subscriber = subscriber;
					subscriber.onSubscribe(subscription);
					if (publisher.publisherCompleted) {
						publisher.publishComplete();
					}
					else if (publisher.publisherError != null) {
						publisher.publishError(publisher.publisherError);
					}
				}
				else {
					throw new IllegalStateException(toString());
				}
			}
			@Override
			void publishComplete(WriteResultPublisher publisher) {
				publisher.publisherCompleted = true;
			}
			@Override
			void publishError(WriteResultPublisher publisher, Throwable t) {
				publisher.publisherError = t;
			}
		},

		SUBSCRIBED {
			@Override
			void request(WriteResultPublisher publisher, long n) {
				Operators.checkRequest(n, publisher.subscriber);
			}
			@Override
			void publishComplete(WriteResultPublisher publisher) {
				if (publisher.changeState(this, COMPLETED)) {
					publisher.subscriber.onComplete();
				}
			}
			@Override
			void publishError(WriteResultPublisher publisher, Throwable t) {
				if (publisher.changeState(this, COMPLETED)) {
					publisher.subscriber.onError(t);
				}
			}
		},

		COMPLETED {
			@Override
			void request(WriteResultPublisher publisher, long n) {
				// ignore
			}
			@Override
			void cancel(WriteResultPublisher publisher) {
				// ignore
			}
			@Override
			void publishComplete(WriteResultPublisher publisher) {
				// ignore
			}
			@Override
			void publishError(WriteResultPublisher publisher, Throwable t) {
				// ignore
			}
		};

		void subscribe(WriteResultPublisher publisher, Subscriber<? super Void> subscriber) {
			throw new IllegalStateException(toString());
		}

		void request(WriteResultPublisher publisher, long n) {
			throw new IllegalStateException(toString());
		}

		void cancel(WriteResultPublisher publisher) {
			publisher.changeState(this, COMPLETED);
		}

		void publishComplete(WriteResultPublisher publisher) {
			throw new IllegalStateException(toString());
		}

		void publishError(WriteResultPublisher publisher, Throwable t) {
			throw new IllegalStateException(toString());
		}
	}

}

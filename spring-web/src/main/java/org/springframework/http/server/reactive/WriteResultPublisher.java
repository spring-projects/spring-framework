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
 * Publisher returned from {@link ServerHttpResponse#writeWith(Publisher)}.
 *
 * @author Arjen Poutsma
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class WriteResultPublisher implements Publisher<Void> {

	/**
	 * Special logger for debugging Reactive Streams signals.
	 * @see LogDelegateFactory#getHiddenLog(Class)
	 * @see AbstractListenerReadPublisher#rsReadLogger
	 * @see AbstractListenerWriteProcessor#rsWriteLogger
	 * @see AbstractListenerWriteFlushProcessor#rsWriteFlushLogger
	 */
	private static final Log rsWriteResultLogger = LogDelegateFactory.getHiddenLog(WriteResultPublisher.class);


	private final AtomicReference<State> state = new AtomicReference<>(State.UNSUBSCRIBED);

	private final Runnable cancelTask;

	@Nullable
	private volatile Subscriber<? super Void> subscriber;

	private volatile boolean completedBeforeSubscribed;

	@Nullable
	private volatile Throwable errorBeforeSubscribed;

	private final String logPrefix;


	public WriteResultPublisher(String logPrefix, Runnable cancelTask) {
		this.cancelTask = cancelTask;
		this.logPrefix = logPrefix;
	}


	@Override
	public final void subscribe(Subscriber<? super Void> subscriber) {
		if (rsWriteResultLogger.isTraceEnabled()) {
			rsWriteResultLogger.trace(this.logPrefix + "got subscriber " + subscriber);
		}
		this.state.get().subscribe(this, subscriber);
	}

	/**
	 * Invoke this to delegate a completion signal to the subscriber.
	 */
	public void publishComplete() {
		State state = this.state.get();
		if (rsWriteResultLogger.isTraceEnabled()) {
			rsWriteResultLogger.trace(this.logPrefix + "completed [" + state + "]");
		}
		state.publishComplete(this);
	}

	/**
	 * Invoke this to delegate an error signal to the subscriber.
	 */
	public void publishError(Throwable t) {
		State state = this.state.get();
		if (rsWriteResultLogger.isTraceEnabled()) {
			rsWriteResultLogger.trace(this.logPrefix + "failed: " + t + " [" + state + "]");
		}
		state.publishError(this, t);
	}

	private boolean changeState(State oldState, State newState) {
		return this.state.compareAndSet(oldState, newState);
	}


	/**
	 * Subscription to receive and delegate request and cancel signals from the
	 * subscriber to this publisher.
	 */
	private static final class WriteResultSubscription implements Subscription {

		private final WriteResultPublisher publisher;

		public WriteResultSubscription(WriteResultPublisher publisher) {
			this.publisher = publisher;
		}

		@Override
		public final void request(long n) {
			if (rsWriteResultLogger.isTraceEnabled()) {
				rsWriteResultLogger.trace(this.publisher.logPrefix +
						"request " + (n != Long.MAX_VALUE ? n : "Long.MAX_VALUE"));
			}
			getState().request(this.publisher, n);
		}

		@Override
		public final void cancel() {
			State state = getState();
			if (rsWriteResultLogger.isTraceEnabled()) {
				rsWriteResultLogger.trace(this.publisher.logPrefix + "cancel [" + state + "]");
			}
			state.cancel(this.publisher);
		}

		private State getState() {
			return this.publisher.state.get();
		}
	}


	/**
	 * Represents a state for the {@link Publisher} to be in.
	 * <p><pre>
	 *     UNSUBSCRIBED
	 *          |
	 *          v
	 *     SUBSCRIBING
	 *          |
	 *          v
	 *      SUBSCRIBED
	 *          |
	 *          v
	 *      COMPLETED
	 * </pre>
	 */
	private enum State {

		UNSUBSCRIBED {
			@Override
			void subscribe(WriteResultPublisher publisher, Subscriber<? super Void> subscriber) {
				Assert.notNull(subscriber, "Subscriber must not be null");
				if (publisher.changeState(this, SUBSCRIBING)) {
					Subscription subscription = new WriteResultSubscription(publisher);
					publisher.subscriber = subscriber;
					subscriber.onSubscribe(subscription);
					publisher.changeState(SUBSCRIBING, SUBSCRIBED);
					// Now safe to check "beforeSubscribed" flags, they won't change once in NO_DEMAND
					if (publisher.completedBeforeSubscribed) {
						publisher.state.get().publishComplete(publisher);
					}
					Throwable ex = publisher.errorBeforeSubscribed;
					if (ex != null) {
						publisher.state.get().publishError(publisher, ex);
					}
				}
				else {
					throw new IllegalStateException(toString());
				}
			}
			@Override
			void publishComplete(WriteResultPublisher publisher) {
				publisher.completedBeforeSubscribed = true;
				if(State.SUBSCRIBED == publisher.state.get()) {
					publisher.state.get().publishComplete(publisher);
				}
			}
			@Override
			void publishError(WriteResultPublisher publisher, Throwable ex) {
				publisher.errorBeforeSubscribed = ex;
				if(State.SUBSCRIBED == publisher.state.get()) {
					publisher.state.get().publishError(publisher, ex);
				}
			}
		},

		SUBSCRIBING {
			@Override
			void request(WriteResultPublisher publisher, long n) {
				Operators.validate(n);
			}
			@Override
			void publishComplete(WriteResultPublisher publisher) {
				publisher.completedBeforeSubscribed = true;
				if(State.SUBSCRIBED == publisher.state.get()) {
					publisher.state.get().publishComplete(publisher);
				}
			}
			@Override
			void publishError(WriteResultPublisher publisher, Throwable ex) {
				publisher.errorBeforeSubscribed = ex;
				if(State.SUBSCRIBED == publisher.state.get()) {
					publisher.state.get().publishError(publisher, ex);
				}
			}
		},

		SUBSCRIBED {
			@Override
			void request(WriteResultPublisher publisher, long n) {
				Operators.validate(n);
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
			if (publisher.changeState(this, COMPLETED)) {
				publisher.cancelTask.run();
			}
			else {
				publisher.state.get().cancel(publisher);
			}
		}

		void publishComplete(WriteResultPublisher publisher) {
			if (publisher.changeState(this, COMPLETED)) {
				Subscriber<? super Void> s = publisher.subscriber;
				Assert.state(s != null, "No subscriber");
				s.onComplete();
			}
			else {
				publisher.state.get().publishComplete(publisher);
			}
		}

		void publishError(WriteResultPublisher publisher, Throwable t) {
			if (publisher.changeState(this, COMPLETED)) {
				Subscriber<? super Void> s = publisher.subscriber;
				Assert.state(s != null, "No subscriber");
				s.onError(t);
			}
			else {
				publisher.state.get().publishError(publisher, t);
			}
		}
	}

}

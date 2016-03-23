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
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.util.BackpressureUtils;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.Assert;

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

	private ResponseBodySubscription subscription;

	private volatile boolean stalled;

	@Override
	public void subscribe(Subscriber<? super DataBuffer> subscriber) {
		Objects.requireNonNull(subscriber);
		Assert.state(this.subscription == null, "Only a single subscriber allowed");

		this.subscription = new ResponseBodySubscription(subscriber);
		subscriber.onSubscribe(this.subscription);
	}

	/**
	 * Publishes the given signal to the subscriber.
	 * @param dataBuffer the signal to publish
	 * @see Subscriber#onNext(Object)
	 */
	protected final void publishOnNext(DataBuffer dataBuffer) {
		Assert.state(this.subscription != null);
		this.subscription.publishOnNext(dataBuffer);
	}

	/**
	 * Publishes the given error to the subscriber.
	 * @param t the error to publish
	 * @see Subscriber#onError(Throwable)
	 */
	protected final void publishOnError(Throwable t) {
		if (this.subscription != null) {
			this.subscription.publishOnError(t);
		}
	}

	/**
	 * Publishes the complete signal to the subscriber.
	 * @see Subscriber#onComplete()
	 */
	protected final void publishOnComplete() {
		if (this.subscription != null) {
			this.subscription.publishOnComplete();
		}
	}

	/**
	 * Returns true if the {@code Subscriber} associated with this {@code Publisher} has
	 * cancelled its {@code Subscription}.
	 * @return {@code true} if a subscriber has been registered and its subscription has
	 * been cancelled; {@code false} otherwise
	 * @see ResponseBodySubscription#isCancelled()
	 * @see Subscription#cancel()
	 */
	protected final boolean isSubscriptionCancelled() {
		return (this.subscription != null && this.subscription.isCancelled());
	}

	/**
	 * Checks the subscription for demand, and marks this publisher as "stalled" if there
	 * is none. The next time the subscriber {@linkplain Subscription#request(long)
	 * requests} more events, the {@link #noLongerStalled()} method is called.
	 * @return {@code true} if there is demand; {@code false} otherwise
	 */
	protected final boolean checkSubscriptionForDemand() {
		if (this.subscription == null || !this.subscription.hasDemand()) {
			this.stalled = true;
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Abstract template method called when this publisher is no longer "stalled". Used in
	 * sub-classes to resume reading from the request.
	 */
	protected abstract void noLongerStalled();

	private final class ResponseBodySubscription implements Subscription {

		private final Subscriber<? super DataBuffer> subscriber;

		private final AtomicLong demand = new AtomicLong();

		private boolean cancelled;

		public ResponseBodySubscription(Subscriber<? super DataBuffer> subscriber) {
			Assert.notNull(subscriber, "'subscriber' must not be null");

			this.subscriber = subscriber;
		}

		@Override
		public final void cancel() {
			this.cancelled = true;
		}

		/**
		 * Indicates whether this subscription has been cancelled.
		 * @see #cancel()
		 */
		protected final boolean isCancelled() {
			return this.cancelled;
		}

		@Override
		public final void request(long n) {
			if (!isCancelled() && BackpressureUtils.checkRequest(n, this.subscriber)) {
				long demand = BackpressureUtils.addAndGet(this.demand, n);

				if (stalled && demand > 0) {
					stalled = false;
					noLongerStalled();
				}
			}
		}

		/**
		 * Indicates whether this subscription has demand.
		 * @see #request(long)
		 */
		protected final boolean hasDemand() {
			return this.demand.get() > 0;
		}

		/**
		 * Publishes the given signal to the subscriber wrapped by this subscription, if
		 * it has not been cancelled. If there is {@linkplain #hasDemand() no demand} for
		 * the signal, an exception will be thrown.
		 * @param dataBuffer the signal to publish
		 * @see Subscriber#onNext(Object)
		 */
		protected final void publishOnNext(DataBuffer dataBuffer) {
			if (!isCancelled()) {
				if (hasDemand()) {
					BackpressureUtils.getAndSub(this.demand, 1L);
					this.subscriber.onNext(dataBuffer);
				}
				else {
					throw new IllegalStateException("No demand for: " + dataBuffer);
				}
			}
		}

		/**
		 * Publishes the given error to the subscriber wrapped by this subscription, if it
		 * has not been cancelled.
		 * @param t the error to publish
		 * @see Subscriber#onError(Throwable)
		 */
		protected final void publishOnError(Throwable t) {
			if (!isCancelled()) {
				this.subscriber.onError(t);
			}
		}

		/**
		 * Publishes the complete signal to the subscriber wrapped by this subscription,
		 * if it has not been cancelled.
		 * @see Subscriber#onComplete()
		 */
		protected final void publishOnComplete() {
			if (!isCancelled()) {
				this.subscriber.onComplete();
			}
		}
	}
}

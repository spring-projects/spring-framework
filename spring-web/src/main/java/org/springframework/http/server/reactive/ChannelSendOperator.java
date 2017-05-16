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

import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.core.publisher.MonoSource;
import reactor.core.publisher.Operators;

import org.springframework.util.Assert;

/**
 * Given a write function that accepts a source {@code Publisher<T>} to write
 * with and returns {@code Publisher<Void>} for the result, this operator helps
 * to defer the invocation of the write function, until we know if the source
 * publisher will begin publishing without an error. If the first emission is
 * an error, the write function is bypassed, and the error is sent directly
 * through the result publisher. Otherwise the write function is invoked.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 5.0
 */
public class ChannelSendOperator<T> extends MonoSource<T, Void> {

	private final Function<Publisher<T>, Publisher<Void>> writeFunction;


	public ChannelSendOperator(Publisher<? extends T> source, Function<Publisher<T>, Publisher<Void>> writeFunction) {
		super(source);
		this.writeFunction = writeFunction;
	}


	@Override
	public void subscribe(Subscriber<? super Void> s) {
		this.source.subscribe(new WriteWithBarrier(s));
	}


	@SuppressWarnings("deprecation")
	private class WriteWithBarrier extends SubscriberAdapter<T, Void> implements Publisher<T> {

		/**
		 * We've at at least one emission, we've called the write function, the write
		 * subscriber has subscribed and cached signals have been emitted to it.
		 * We're now simply passing data through to the write subscriber.
		 **/
		private boolean readyToWrite = false;

		/** No emission from upstream yet */
		private boolean beforeFirstEmission = true;

		/** Cached signal before readyToWrite */
		private T item;

		/** Cached 1st/2nd signal before readyToWrite */
		private Throwable error;

		/** Cached 1st/2nd signal before readyToWrite */
		private boolean completed = false;

		/** The actual writeSubscriber vs the downstream completion subscriber */
		private Subscriber<? super T> writeSubscriber;

		public WriteWithBarrier(Subscriber<? super Void> subscriber) {
			super(subscriber);
		}

		@Override
		protected void doOnSubscribe(Subscription subscription) {
			super.doOnSubscribe(subscription);
			super.upstream().request(1);  // bypass doRequest
		}

		@Override
		public void doNext(T item) {
			if (this.readyToWrite) {
				this.writeSubscriber.onNext(item);
				return;
			}
			synchronized (this) {
				if (this.readyToWrite) {
					this.writeSubscriber.onNext(item);
				}
				else if (this.beforeFirstEmission) {
					this.item = item;
					this.beforeFirstEmission = false;
					writeFunction.apply(this).subscribe(new DownstreamBridge(downstream()));
				}
				else {
					subscription.cancel();
					downstream().onError(new IllegalStateException("Unexpected item."));
				}
			}
		}

		@Override
		public void doError(Throwable ex) {
			if (this.readyToWrite) {
				this.writeSubscriber.onError(ex);
				return;
			}
			synchronized (this) {
				if (this.readyToWrite) {
					this.writeSubscriber.onError(ex);
				}
				else if (this.beforeFirstEmission) {
					this.beforeFirstEmission = false;
					downstream().onError(ex);
				}
				else {
					this.error = ex;
				}
			}
		}

		@Override
		public void doComplete() {
			if (this.readyToWrite) {
				this.writeSubscriber.onComplete();
				return;
			}
			synchronized (this) {
				if (this.readyToWrite) {
					this.writeSubscriber.onComplete();
				}
				else if (this.beforeFirstEmission) {
					this.completed = true;
					this.beforeFirstEmission = false;
					writeFunction.apply(this).subscribe(new DownstreamBridge(downstream()));
				}
				else {
					this.completed = true;
				}
			}
		}

		@Override
		public void subscribe(Subscriber<? super T> writeSubscriber) {
			synchronized (this) {
				Assert.isNull(this.writeSubscriber, "Only one writeSubscriber supported");
				this.writeSubscriber = writeSubscriber;

				if (this.error != null || this.completed) {
					this.writeSubscriber.onSubscribe(Operators.emptySubscription());
					emitCachedSignals();
				}
				else {
					this.writeSubscriber.onSubscribe(this);
				}
			}
		}

		/**
		 * Emit cached signals to the write subscriber.
		 * @return true if no more signals expected
		 */
		private boolean emitCachedSignals() {
			if (this.item != null) {
				this.writeSubscriber.onNext(this.item);
			}
			if (this.error != null) {
				this.writeSubscriber.onError(this.error);
				return true;
			}
			if (this.completed) {
				this.writeSubscriber.onComplete();
				return true;
			}
			return false;
		}

		@Override
		protected void doRequest(long n) {
			if (readyToWrite) {
				super.doRequest(n);
				return;
			}
			synchronized (this) {
				if (this.writeSubscriber != null) {
					readyToWrite = true;
					if (emitCachedSignals()) {
						return;
					}
					n--;
					if (n == 0) {
						return;
					}
					super.doRequest(n);
				}
			}
		}
	}

	// TODO Remove this copy of Reactor 3.0.x Operators.SubscriberAdapter
	private static class SubscriberAdapter<I, O> implements Subscriber<I>, Subscription {

		protected final Subscriber<? super O> subscriber;

		protected Subscription subscription;

		public SubscriberAdapter(Subscriber<? super O> subscriber) {
			this.subscriber = subscriber;
		}

		public Subscriber<? super O> downstream() {
			return subscriber;
		}

		@Override
		public final void cancel() {
			try {
				doCancel();
			} catch (Throwable throwable) {
				doOnSubscriberError(Operators.onOperatorError(subscription, throwable));
			}
		}

		@Override
		public final void onComplete() {
			try {
				doComplete();
			} catch (Throwable throwable) {
				doOnSubscriberError(Operators.onOperatorError(throwable));
			}
		}

		@Override
		public final void onError(Throwable t) {
			if (t == null) {
				throw Exceptions.argumentIsNullException();
			}
			doError(t);
		}

		@Override
		public final void onNext(I i) {
			if (i == null) {
				throw Exceptions.argumentIsNullException();
			}
			try {
				doNext(i);
			}
			catch (Throwable throwable) {
				doOnSubscriberError(Operators.onOperatorError(subscription, throwable, i));
			}
		}

		@Override
		public final void onSubscribe(Subscription s) {
			if (Operators.validate(subscription, s)) {
				try {
					subscription = s;
					doOnSubscribe(s);
				}
				catch (Throwable throwable) {
					doOnSubscriberError(Operators.onOperatorError(s, throwable));
				}
			}
		}

		@Override
		public final void request(long n) {
			try {
				Operators.checkRequest(n);
				doRequest(n);
			} catch (Throwable throwable) {
				doCancel();
				doOnSubscriberError(Operators.onOperatorError(throwable));
			}
		}

		@Override
		public String toString() {
			return getClass().getSimpleName();
		}

		/**
		 * Hook for further processing of onSubscribe's Subscription.
		 * @param subscription the subscription to optionally process
		 */
		protected void doOnSubscribe(Subscription subscription) {
			subscriber.onSubscribe(this);
		}

		public Subscription upstream() {
			return subscription;
		}

		@SuppressWarnings("unchecked")
		protected void doNext(I i) {
			subscriber.onNext((O) i);
		}

		protected void doError(Throwable throwable) {
			subscriber.onError(throwable);
		}

		protected void doOnSubscriberError(Throwable throwable){
			subscriber.onError(throwable);
		}

		protected void doComplete() {
			subscriber.onComplete();
		}

		protected void doRequest(long n) {
			Subscription s = this.subscription;
			if (s != null) {
				s.request(n);
			}
		}

		protected void doCancel() {
			Subscription s = this.subscription;
			if (s != null) {
				this.subscription = null;
				s.cancel();
			}
		}
	}


	private class DownstreamBridge implements Subscriber<Void> {

		private final Subscriber<? super Void> downstream;

		public DownstreamBridge(Subscriber<? super Void> downstream) {
			this.downstream = downstream;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Void aVoid) {
		}

		@Override
		public void onError(Throwable ex) {
			this.downstream.onError(ex);
		}

		@Override
		public void onComplete() {
			this.downstream.onComplete();
		}
	}

}

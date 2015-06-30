/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.rx.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
public abstract class AbstractUnicastAsyncSubscriber<T> implements Subscriber<T> {

	private final Executor executor;

	private Subscription subscription;

	private boolean done;

	protected AbstractUnicastAsyncSubscriber(Executor executor) {
		Assert.notNull(executor, "'executor' must not be null");

		this.executor = executor;
	}

	private void done() {
		done = true;

		if (subscription != null) {
			subscription.cancel();
		}
	}

	// This method is invoked when the OnNext signals arrive
	// Returns whether more elements are desired or not, and if no more elements are desired,
	// for convenience.
	protected abstract boolean whenNext(final T element);

	// This method is invoked when the OnComplete signal arrives
	// override this method to implement your own custom onComplete logic.
	protected void whenComplete() {
	}

	// This method is invoked if the OnError signal arrives
	// override this method to implement your own custom onError logic.
	protected void whenError(Throwable error) {
	}

	private void handleOnSubscribe(Subscription subscription) {
		if (subscription == null) {
			return;
		}
		if (this.subscription != null) {
			subscription.cancel();
		}
		else {
			this.subscription = subscription;
			this.subscription.request(1);
		}
	}

	private void handleOnNext(final T element) {
		if (!done) {
			try {
				if (whenNext(element)) {
					subscription.request(1);
				}
				else {
					done();
				}
			}
			catch (final Throwable t) {
				done();
				onError(t);
			}
		}
	}

	private void handleOnComplete() {
		done = true;
		whenComplete();
	}

	private void handleOnError(final Throwable error) {
		done = true;
		whenError(error);
	}

	// We implement the OnX methods on `Subscriber` to send Signals that we will process asycnhronously, but only one at a time

	@Override
	public final void onSubscribe(final Subscription s) {
		// As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `Subscription` is `null`
		if (s == null) {
			throw null;
		}

		signal(new OnSubscribe(s));
	}

	@Override
	public final void onNext(final T element) {
		// As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `element` is `null`
		if (element == null) {
			throw null;
		}

		signal(new OnNext<T>(element));
	}

	@Override
	public final void onError(final Throwable t) {
		// As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `Throwable` is `null`
		if (t == null) {
			throw null;
		}

		signal(new OnError(t));
	}

	@Override
	public final void onComplete() {
		signal(OnComplete.INSTANCE);
	}

	private final ConcurrentLinkedQueue<Signal<T>> inboundSignals =
			new ConcurrentLinkedQueue<Signal<T>>();

	private final AtomicBoolean enabled = new AtomicBoolean(false);

	// What `signal` does is that it sends signals to the `Subscription` asynchronously
	private void signal(final Signal signal) {
		if (inboundSignals
				.offer(signal)) // No need to null-check here as ConcurrentLinkedQueue does this for us
		{
			tryScheduleToExecute(); // Then we try to schedule it for execution, if it isn't already
		}
	}

	// This method makes sure that this `Subscriber` is only executing on one Thread at a time
	private void tryScheduleToExecute() {
		if (enabled.compareAndSet(false, true)) {
			try {
				executor.execute(new SignalRunnable());
			}
			catch (Throwable t) { // If we can't run on the `Executor`, we need to fail gracefully and not violate rule 2.13
				if (!done) {
					try {
						done(); // First of all, this failure is not recoverable, so we need to cancel our subscription
					}
					finally {
						inboundSignals.clear(); // We're not going to need these anymore
						// This subscription is cancelled by now, but letting the Subscriber become schedulable again means
						// that we can drain the inboundSignals queue if anything arrives after clearing
						enabled.set(false);
					}
				}
			}
		}
	}

	private class SignalRunnable implements Runnable {

		@Override
		public void run() {
			if (enabled.get()) {
				try {
					Signal<T> s = inboundSignals.poll();
					if (!done) {
						if (s.isOnNext()) {
							handleOnNext(s.next());
						}
						else if (s.isOnSubscribe()) {
							handleOnSubscribe(s.subscription());
						}
						else if (s.isOnError()) {
							handleOnError(s.error());
						}
						else if (s.isComplete()) {
							handleOnComplete();
						}
					}
				}
				finally {
					enabled.set(false);

					if (!inboundSignals.isEmpty()) {
						tryScheduleToExecute();
					}
				}
			}

		}
	}
}
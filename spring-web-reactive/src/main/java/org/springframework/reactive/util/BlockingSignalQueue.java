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

package org.springframework.reactive.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.util.Assert;

/**
 * A {@link BlockingQueue} aimed at working with {@code Publisher<ByteBuf>} instances.
 * Mainly meant to bridge between reactive and non-reactive APIs, such as blocking
 * streams.
 *
 * <p>Typically, this class will be used by two threads: one thread to put new elements on
 * the stack by calling {@link #putSignal(Object)}, possibly {@link #putError(Throwable)}
 * and finally {@link #complete()}. The other thread will read elements by calling {@link
 * #isHeadSignal()}/{@link #pollSignal()} and {@link #isHeadError()}/{@link #pollError()},
 * while keeping an eye on {@link #isComplete()}.
 * @author Arjen Poutsma
 */
public class BlockingSignalQueue<T> {

	private static final int DEFAULT_REQUEST_SIZE_SUBSCRIBER = 1;

	private final BlockingQueue<Signal<T>> queue = new LinkedBlockingQueue<Signal<T>>();


	/**
	 * Inserts the specified signal into this queue, waiting if necessary for space to
	 * become available.
	 * @param t the signal to add
	 */
	public void putSignal(T t) throws InterruptedException {
		Assert.notNull(t, "'t' must not be null");
		Assert.state(!isComplete(), "Cannot put signal in queue after complete()");
		this.queue.put(new OnNext<T>(t));
	}

	/**
	 * Inserts the specified error into this queue, waiting if necessary for space to
	 * become available.
	 * @param error the error to add
	 */
	public void putError(Throwable error) throws InterruptedException {
		Assert.notNull(error, "'error' must not be null");
		Assert.state(!isComplete(), "Cannot putSignal errors in queue after complete()");
		this.queue.put(new OnError<T>(error));
	}

	/**
	 * Marks the queue as complete.
	 */
	@SuppressWarnings("unchecked")
	public void complete() throws InterruptedException {
		this.queue.put(OnComplete.INSTANCE);
	}

	/**
	 * Indicates whether the current head of this queue is a signal.
	 * @return {@code true} if the current head is a signal; {@code false} otherwise
	 */
	public boolean isHeadSignal() {
		Signal signal = this.queue.peek();
		return signal != null && signal.isOnNext();
	}

	/**
	 * Indicates whether the current head of this queue is a {@link Throwable}.
	 * @return {@code true} if the current head is an error; {@code false} otherwise
	 */
	public boolean isHeadError() {
		Signal signal = this.queue.peek();
		return signal != null && signal.isOnError();
	}

	/**
	 * Indicates whether there are more buffers or errors in this queue.
	 * @return {@code true} if there more elements in this queue; {@code false} otherwise
	 */
	public boolean isComplete() {
		Signal signal = this.queue.peek();
		return signal != null && signal.isComplete();
	}

	/**
	 * Retrieves and removes the signal head of this queue. Should only be called after
	 * {@link #isHeadSignal()} returns {@code true}.
	 * @return the head of the queue
	 * @throws IllegalStateException if the current head of this queue is not a buffer
	 * @see #isHeadSignal()
	 */
	public T pollSignal() throws InterruptedException {
		Signal<T> signal = this.queue.take();
		return signal != null ? signal.next() : null;
	}

	/**
	 * Retrieves and removes the buffer error of this queue. Should only be called after
	 * {@link #isHeadError()} returns {@code true}.
	 * @return the head of the queue, as error
	 * @throws IllegalStateException if the current head of this queue is not a error
	 * @see #isHeadError()
	 */
	public Throwable pollError() throws InterruptedException {
		Signal signal = this.queue.take();
		return signal != null ? signal.error() : null;
	}

	/**
	 * Returns a {@code Publisher} backed by this queue.
	 */
	public Publisher<T> publisher() {
		return new BlockingSignalQueuePublisher();
	}

	/**
	 * Returns a {@code Subscriber} backed by this queue.
	 */
	public Subscriber<T> subscriber() {
		return subscriber(DEFAULT_REQUEST_SIZE_SUBSCRIBER);
	}

	/**
	 * Returns a {@code Subscriber} backed by this queue, with the given request size.
	 * @see Subscription#request(long)
	 */
	public Subscriber<T> subscriber(long requestSize) {
		return new BlockingSignalQueueSubscriber(requestSize);
	}

	private class BlockingSignalQueuePublisher implements Publisher<T> {

		private Subscriber<? super T> subscriber;

		private final Object subscriberMutex = new Object();

		@Override
		public void subscribe(Subscriber<? super T> subscriber) {
			synchronized (this.subscriberMutex) {
				if (this.subscriber != null) {
					subscriber.onError(
							new IllegalStateException("Only one subscriber allowed"));
				}
				else {
					this.subscriber = subscriber;
					final SubscriptionThread thread = new SubscriptionThread();
					this.subscriber.onSubscribe(new Subscription() {
						@Override
						public void request(long n) {
							thread.request(n);
						}

						@Override
						public void cancel() {
							thread.cancel();
						}
					});
					thread.start();
				}
			}
		}

		private class SubscriptionThread extends Thread {

			private volatile long demand = 0;

			@Override
			public void run() {
				try {
					while (!Thread.currentThread().isInterrupted()) {
						if (demand > 0 && isHeadSignal()) {
							subscriber.onNext(pollSignal());
							if (demand != Long.MAX_VALUE) {
								demand--;
							}
						}
						else if (isHeadError()) {
							subscriber.onError(pollError());
							break;
						}
						else if (isComplete()) {
							subscriber.onComplete();
							break;
						}
					}
				}
				catch (InterruptedException ex) {
					// Allow thread to exit
				}
			}

			public void request(long n) {
				if (n != Long.MAX_VALUE) {
					this.demand += n;
				}
				else {
					this.demand = Long.MAX_VALUE;
				}
			}

			public void cancel() {
				interrupt();
			}
		}
	}

	private class BlockingSignalQueueSubscriber implements Subscriber<T> {

		private final long requestSize;

		private Subscription subscription;

		public BlockingSignalQueueSubscriber(long requestSize) {
			this.requestSize = requestSize;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;

			this.subscription.request(this.requestSize);
		}

		@Override
		public void onNext(T t) {
			try {
				putSignal(t);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			this.subscription.request(requestSize);
		}

		@Override
		public void onError(Throwable t) {
			try {
				putError(t);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			this.subscription.request(requestSize);
		}

		@Override
		public void onComplete() {
			try {
				complete();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}
}

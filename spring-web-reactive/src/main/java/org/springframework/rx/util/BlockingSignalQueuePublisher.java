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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
public class BlockingSignalQueuePublisher<T> implements Publisher<T> {

	private final BlockingSignalQueue<T> queue;

	private Subscriber<? super T> subscriber;

	private final Object subscriberMutex = new Object();

	public BlockingSignalQueuePublisher(BlockingSignalQueue<T> queue) {
		Assert.notNull(queue, "'queue' must not be null");
		this.queue = queue;
	}

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

		private volatile long requestCount = 0;

		private long l = 0;

		@Override
		public void run() {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					if ((l < requestCount || requestCount == Long.MAX_VALUE) &&
							queue.isHeadSignal()) {
						subscriber.onNext(queue.pollSignal());
						l++;
					}
					else if (queue.isHeadError()) {
						subscriber.onError(queue.pollError());
						break;
					}
					else if (queue.isComplete()) {
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
				this.requestCount += n;
			}
			else {
				this.requestCount = Long.MAX_VALUE;
			}
		}

		public void cancel() {
			interrupt();
		}
	}
}

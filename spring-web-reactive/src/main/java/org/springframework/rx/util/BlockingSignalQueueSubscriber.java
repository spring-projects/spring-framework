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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.util.Assert;

/**
 * A simple byte array {@link Subscriber} that puts all published bytes on a
 * {@link @BlockingSignalQueue}.
 *
 * @author Arjen Poutsma
 */
public class BlockingSignalQueueSubscriber<T> implements Subscriber<T> {

	/**
	 * The default request size to use.
	 */
	public static final int DEFAULT_REQUEST_SIZE = 1;

	private final BlockingSignalQueue<T> queue;

	private Subscription subscription;

	private int initialRequestSize = DEFAULT_REQUEST_SIZE;

	private int requestSize = DEFAULT_REQUEST_SIZE;


	/**
	 * Creates a new {@code BlockingSignalQueueSubscriber} using the given queue.
	 * @param queue the queue to use
	 */
	public BlockingSignalQueueSubscriber(BlockingSignalQueue<T> queue) {
		Assert.notNull(queue, "'queue' must not be null");
		this.queue = queue;
	}

	/**
	 * Sets the request size used when subscribing, in {@link #onSubscribe(Subscription)}.
	 * Defaults to {@link #DEFAULT_REQUEST_SIZE}.
	 * @param initialRequestSize the initial request size
	 * @see Subscription#request(long)
	 */
	public void setInitialRequestSize(int initialRequestSize) {
		this.initialRequestSize = initialRequestSize;
	}

	/**
	 * Sets the request size used after data or an error comes in, in {@link
	 * #onNext(Object)} and {@link #onError(Throwable)}. Defaults to {@link
	 * #DEFAULT_REQUEST_SIZE}.
	 * @see Subscription#request(long)
	 */
	public void setRequestSize(int requestSize) {
		this.requestSize = requestSize;
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;

		this.subscription.request(this.initialRequestSize);
	}

	@Override
	public void onNext(T t) {
		try {
			this.queue.putSignal(t);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		this.subscription.request(requestSize);
	}

	@Override
	public void onError(Throwable t) {
		try {
			this.queue.putError(t);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		this.subscription.request(requestSize);
	}

	@Override
	public void onComplete() {
		try {
			this.queue.complete();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}

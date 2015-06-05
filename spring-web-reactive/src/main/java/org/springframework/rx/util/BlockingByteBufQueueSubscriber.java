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

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
public class BlockingByteBufQueueSubscriber implements Subscriber<ByteBuf> {

	private final BlockingByteBufQueue queue;

	private Subscription subscription;

	public BlockingByteBufQueueSubscriber(BlockingByteBufQueue queue) {
		Assert.notNull(queue, "'queue' must not be null");
		this.queue = queue;
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		this.subscription = subscription;

		this.subscription.request(1);
	}

	@Override
	public void onNext(ByteBuf byteBuf) {
		try {
			this.queue.putBuffer(byteBuf);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		this.subscription.request(1);
	}

	@Override
	public void onError(Throwable t) {
		try {
			this.queue.putError(t);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		this.subscription.request(1);
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

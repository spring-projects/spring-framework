/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.io.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;

import org.springframework.util.Assert;

/**
 * An {@link InputStream} backed by {@link Flow.Subscriber Flow.Subscriber}
 * receiving byte buffers from a {@link Flow.Publisher} source.
 *
 * <p>Byte buffers are stored in a queue. The {@code demand} constructor value
 * determines the number of buffers requested initially. When storage falls
 * below a {@code (demand - (demand >> 2))} limit, a request is made to refill
 * the queue.
 *
 * <p>The {@code InputStream} terminates after an onError or onComplete signal,
 * and stored buffers are read. If the {@code InputStream} is closed,
 * the {@link Flow.Subscription} is cancelled, and stored buffers released.
 *
 * <p>Note that this class has a near duplicate in
 * {@link org.springframework.http.client.SubscriberInputStream}.
 *
 * @author Oleh Dokuka
 * @author Rossen Stoyanchev
 * @since 6.2
 */
final class SubscriberInputStream extends InputStream implements Subscriber<DataBuffer> {

	private static final Object READY = new Object();

	private static final DataBuffer DONE = DefaultDataBufferFactory.sharedInstance.allocateBuffer(0);

	private static final DataBuffer CLOSED = DefaultDataBufferFactory.sharedInstance.allocateBuffer(0);


	private final int prefetch;

	private final int limit;

	private final ReentrantLock lock;

	private final Queue<DataBuffer> queue;

	private final AtomicReference<Object> parkedThread = new AtomicReference<>();

	private final AtomicInteger workAmount = new AtomicInteger();

	private volatile boolean closed;

	private int consumed;

	private @Nullable DataBuffer available;

	private @Nullable Subscription subscription;

	private boolean done;

	private @Nullable Throwable error;


	/**
	 * Create an instance.
	 * @param demand the number of buffers to request initially, and buffer
	 * internally on an ongoing basis.
	 */
	SubscriberInputStream(int demand) {
		this.prefetch = demand;
		this.limit = (demand == Integer.MAX_VALUE ? Integer.MAX_VALUE : demand - (demand >> 2));
		this.queue = new ArrayBlockingQueue<>(demand);
		this.lock = new ReentrantLock(false);
	}


	@Override
	public void onSubscribe(Subscription subscription) {
		if (this.subscription != null) {
			subscription.cancel();
			return;
		}

		this.subscription = subscription;
		subscription.request(this.prefetch == Integer.MAX_VALUE ? Long.MAX_VALUE : this.prefetch);
	}

	@Override
	public void onNext(DataBuffer buffer) {
		Assert.notNull(buffer, "DataBuffer must not be null");

		if (this.done) {
			discard(buffer);
			return;
		}

		if (!this.queue.offer(buffer)) {
			discard(buffer);
			this.error = new RuntimeException("Buffer overflow");
			this.done = true;
		}

		int previousWorkState = addWork();
		if (previousWorkState == Integer.MIN_VALUE) {
			DataBuffer value = this.queue.poll();
			if (value != null) {
				discard(value);
			}
			return;
		}

		if (previousWorkState == 0) {
			resume();
		}
	}

	@Override
	public void onError(Throwable throwable) {
		if (this.done) {
			return;
		}
		this.error = throwable;
		this.done = true;

		if (addWork() == 0) {
			resume();
		}
	}

	@Override
	public void onComplete() {
		if (this.done) {
			return;
		}

		this.done = true;

		if (addWork() == 0) {
			resume();
		}
	}

	int addWork() {
		for (;;) {
			int produced = this.workAmount.getPlain();

			if (produced == Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}

			int nextProduced = (produced == Integer.MAX_VALUE ? 1 : produced + 1);

			if (this.workAmount.weakCompareAndSetRelease(produced, nextProduced)) {
				return produced;
			}
		}
	}

	private void resume() {
		if (this.parkedThread != READY) {
			Object old = this.parkedThread.getAndSet(READY);
			if (old != READY) {
				LockSupport.unpark((Thread)old);
			}
		}
	}

	/* InputStream implementation */

	@Override
	public int read() throws IOException {
		if (!this.lock.tryLock()) {
			if (this.closed) {
				return -1;
			}
			throw new ConcurrentModificationException("Concurrent access is not allowed");
		}

		try {
			DataBuffer next = getNextOrAwait();

			if (next == DONE) {
				this.closed = true;
				cleanAndFinalize();
				if (this.error == null) {
					return -1;
				}
				else {
					throw Exceptions.propagate(this.error);
				}
			}
			else if (next == CLOSED) {
				cleanAndFinalize();
				return -1;
			}

			return next.read() & 0xFF;
		}
		catch (Throwable ex) {
			this.closed = true;
			requiredSubscriber().cancel();
			cleanAndFinalize();
			throw Exceptions.propagate(ex);
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, b.length);
		if (len == 0) {
			return 0;
		}

		if (!this.lock.tryLock()) {
			if (this.closed) {
				return -1;
			}
			throw new ConcurrentModificationException("concurrent access is disallowed");
		}

		try {
			for (int j = 0; j < len;) {
				DataBuffer next = getNextOrAwait();

				if (next == DONE) {
					cleanAndFinalize();
					if (this.error == null) {
						this.closed = true;
						return j == 0 ? -1 : j;
					}
					else {
						if (j == 0) {
							this.closed = true;
							throw Exceptions.propagate(this.error);
						}

						return j;
					}
				}
				else if (next == CLOSED) {
					requiredSubscriber().cancel();
					cleanAndFinalize();
					return -1;
				}
				int initialReadPosition = next.readPosition();
				next.read(b, off + j, Math.min(len - j, next.readableByteCount()));
				j += next.readPosition() - initialReadPosition;
			}

			return len;
		}
		catch (Throwable ex) {
			this.closed = true;
			requiredSubscriber().cancel();
			cleanAndFinalize();
			throw Exceptions.propagate(ex);
		}
		finally {
			this.lock.unlock();
		}
	}

	private DataBuffer getNextOrAwait() {
		if (this.available == null || this.available.readableByteCount() == 0) {
			discard(this.available);
			this.available = null;

			int actualWorkAmount = this.workAmount.getAcquire();
			for (;;) {
				if (this.closed) {
					return CLOSED;
				}

				boolean done = this.done;
				DataBuffer buffer = this.queue.poll();
				if (buffer != null) {
					int consumed = ++this.consumed;
					this.available = buffer;
					if (consumed == this.limit) {
						this.consumed = 0;
						requiredSubscriber().request(this.limit);
					}
					break;
				}

				if (done) {
					return DONE;
				}

				actualWorkAmount = this.workAmount.addAndGet(-actualWorkAmount);
				if (actualWorkAmount == 0) {
					await();
				}
			}
		}

		return this.available;
	}

	private void cleanAndFinalize() {
		discard(this.available);
		this.available = null;

		for (;;) {
			int workAmount = this.workAmount.getPlain();
			DataBuffer value;
			while ((value = this.queue.poll()) != null) {
				discard(value);
			}

			if (this.workAmount.weakCompareAndSetPlain(workAmount, Integer.MIN_VALUE)) {
				return;
			}
		}
	}

	@Override
	public void close() throws IOException {
		if (this.closed) {
			return;
		}

		this.closed = true;

		if (!this.lock.tryLock()) {
			if (addWork() == 0) {
				resume();
			}
			return;
		}

		try {
			requiredSubscriber().cancel();
			cleanAndFinalize();
		}
		finally {
			this.lock.unlock();
		}
	}

	private Subscription requiredSubscriber() {
		Assert.state(this.subscription != null, "Subscriber must be subscribed to use InputStream");
		return this.subscription;
	}

	private void discard(@Nullable DataBuffer buffer) {
		DataBufferUtils.release(buffer);
	}

	private void await() {
		Thread toUnpark = Thread.currentThread();

		while (true) {
			Object current = this.parkedThread.get();
			if (current == READY) {
				break;
			}

			if (current != null && current != toUnpark) {
				throw new IllegalStateException("Only one (Virtual)Thread can await!");
			}

			if (this.parkedThread.compareAndSet( null, toUnpark)) {
				LockSupport.park();
				// we don't just break here because park() can wake up spuriously
				// if we got a proper resume, get() == READY and the loop will quit above
			}
		}
		// clear the resume indicator so that the next await call will park without a resume()
		this.parkedThread.lazySet(null);
	}

}

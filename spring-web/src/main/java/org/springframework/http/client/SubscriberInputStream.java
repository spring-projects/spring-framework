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

package org.springframework.http.client;

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
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Exceptions;

import org.springframework.lang.Nullable;
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
 * {@link org.springframework.core.io.buffer.SubscriberInputStream}.
 *
 * @author Oleh Dokuka
 * @author Rossen Stoyanchev
 * @since 6.2
 * @param <T> the publisher byte buffer type
 */
final class SubscriberInputStream<T> extends InputStream implements Flow.Subscriber<T> {

	private static final Log logger = LogFactory.getLog(SubscriberInputStream.class);

	private static final Object READY = new Object();

	private static final byte[] DONE = new byte[0];

	private static final byte[] CLOSED = new byte[0];


	private final Function<T, byte[]> mapper;

	private final Consumer<T> onDiscardHandler;

	private final int prefetch;

	private final int limit;

	private final ReentrantLock lock;

	private final Queue<T> queue;

	private final AtomicReference<Object> parkedThread = new AtomicReference<>();

	private final AtomicInteger workAmount = new AtomicInteger();

	volatile boolean closed;

	private int consumed;

	@Nullable
	private byte[] available;

	private int position;

	@Nullable
	private Flow.Subscription subscription;

	private boolean done;

	@Nullable
	private Throwable error;


	/**
	 * Create an instance.
	 * @param mapper function to transform byte buffers to {@code byte[]};
	 * the function should also release the byte buffer if necessary.
	 * @param onDiscardHandler a callback to release byte buffers if the
	 * {@link InputStream} is closed prematurely.
	 * @param demand the number of buffers to request initially, and buffer
	 * internally on an ongoing basis.
	 */
	SubscriberInputStream(Function<T, byte[]> mapper, Consumer<T> onDiscardHandler, int demand) {
		Assert.notNull(mapper, "mapper must not be null");
		Assert.notNull(onDiscardHandler, "onDiscardHandler must not be null");
		Assert.isTrue(demand > 0, "demand must be greater than 0");

		this.mapper = mapper;
		this.onDiscardHandler = onDiscardHandler;
		this.prefetch = demand;
		this.limit = (demand == Integer.MAX_VALUE ? Integer.MAX_VALUE : demand - (demand >> 2));
		this.queue = new ArrayBlockingQueue<>(demand);
		this.lock = new ReentrantLock(false);
	}


	@Override
	public void onSubscribe(Flow.Subscription subscription) {
		if (this.subscription != null) {
			subscription.cancel();
			return;
		}

		this.subscription = subscription;
		subscription.request(this.prefetch == Integer.MAX_VALUE ? Long.MAX_VALUE : this.prefetch);
	}

	@Override
	public void onNext(T buffer) {
		Assert.notNull(buffer, "Buffer must not be null");

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
			T value = this.queue.poll();
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
				LockSupport.unpark((Thread) old);
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
			byte[] next = getNextOrAwait();

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

			return next[this.position++] & 0xFF;
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
				byte[] next = getNextOrAwait();

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
				int i = this.position;
				for (; i < next.length && j < len; i++, j++) {
					b[off + j] = next[i];
				}
				this.position = i;
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

	byte[] getNextOrAwait() {
		if (this.available == null || this.available.length - this.position == 0) {
			this.available = null;

			int actualWorkAmount = this.workAmount.getAcquire();
			for (;;) {
				if (this.closed) {
					return CLOSED;
				}

				boolean done = this.done;
				T buffer = this.queue.poll();
				if (buffer != null) {
					int consumed = ++this.consumed;
					this.position = 0;
					this.available = Objects.requireNonNull(this.mapper.apply(buffer));
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

	void cleanAndFinalize() {
		this.available = null;

		for (;;) {
			int workAmount = this.workAmount.getPlain();
			T value;
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

	private Flow.Subscription requiredSubscriber() {
		Assert.state(this.subscription != null, "Subscriber must be subscribed to use InputStream");
		return this.subscription;
	}

	void discard(T buffer) {
		try {
			this.onDiscardHandler.accept(buffer);
		}
		catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to release " + buffer.getClass().getSimpleName() + ": " + buffer, ex);
			}
		}
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

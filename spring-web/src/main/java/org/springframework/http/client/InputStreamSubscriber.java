package org.springframework.http.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import reactor.core.Exceptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Bridges between {@link Flow.Publisher Flow.Publisher&lt;T&gt;} and {@link InputStream}.
 *
 * <p>Note that this class has a near duplicate in
 * {@link org.springframework.core.io.buffer.InputStreamSubscriber}.
 *
 * @author Oleh Dokuka
 * @since 6.1
 */
class InputStreamSubscriber<T> extends InputStream implements Flow.Subscriber<T> {

	private static final Log logger = LogFactory.getLog(InputStreamSubscriber.class);

	static final Object READY = new Object();
	static final byte[] DONE = new byte[0];
	static final byte[] CLOSED = new byte[0];

	final int prefetch;
	final int limit;
	final Function<T, byte[]> mapper;
	final Consumer<T>         onDiscardHandler;
	final ReentrantLock       lock;
	final Queue<T> queue = new ConcurrentLinkedQueue<>();

	final AtomicReference<Object> parkedThread = new AtomicReference<>();
	final AtomicInteger workAmount = new AtomicInteger();

	volatile boolean closed;
	int consumed;

	@Nullable
	byte[] available;
	int position;

	@Nullable
	Flow.Subscription s;
	boolean done;
	@Nullable
	Throwable error;

	InputStreamSubscriber(Function<T, byte[]> mapper, Consumer<T> onDiscardHandler, int prefetch) {
		this.prefetch = prefetch;
		this.limit = prefetch == Integer.MAX_VALUE ? Integer.MAX_VALUE : prefetch - (prefetch >> 2);
		this.mapper = mapper;
		this.onDiscardHandler = onDiscardHandler;
		this.lock = new ReentrantLock(false);
	}

	@Override
	public void onSubscribe(Flow.Subscription subscription) {
		if (this.s != null) {
			subscription.cancel();
			return;
		}

		this.s = subscription;
		subscription.request(prefetch == Integer.MAX_VALUE ? Long.MAX_VALUE : prefetch);
	}

	@Override
	public void onNext(T t) {
		if (this.done) {
			discard(t);
			return;
		}

		queue.offer(t);

		int previousWorkState = addWork();
		if (previousWorkState == Integer.MIN_VALUE) {
			T value = queue.poll();
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

			int nextProduced = produced == Integer.MAX_VALUE ? 1 : produced + 1;


			if (workAmount.weakCompareAndSetRelease(produced, nextProduced)) {
				return produced;
			}
		}
	}

	@Override
	public int read() throws IOException {
		if (!lock.tryLock()) {
			if (this.closed) {
				return -1;
			}
			throw new ConcurrentModificationException("concurrent access is disallowed");
		}

		try {
			byte[] bytes = getBytesOrAwait();

			if (bytes == DONE) {
				this.closed = true;
				cleanAndFinalize();
				if (this.error == null) {
					return -1;
				}
				else {
					throw Exceptions.propagate(error);
				}
			} else if (bytes == CLOSED) {
				cleanAndFinalize();
				return -1;
			}

			return bytes[this.position++] & 0xFF;
		}
		catch (Throwable t) {
			this.closed = true;
			this.s.cancel();
			cleanAndFinalize();
			throw Exceptions.propagate(t);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, b.length);
		if (len == 0) {
			return 0;
		}

		if (!lock.tryLock()) {
			if (this.closed) {
				return -1;
			}
			throw new ConcurrentModificationException("concurrent access is disallowed");
		}

		try {
			for (int j = 0; j < len;) {
				byte[] bytes = getBytesOrAwait();

				if (bytes == DONE) {
					this.closed = true;
					cleanAndFinalize();
					if (this.error == null) {
						return j == 0 ? -1 : j;
					}
					else {
						throw Exceptions.propagate(error);
					}
				} else if (bytes == CLOSED) {
					this.s.cancel();
					cleanAndFinalize();
					return -1;
				}

				int i = this.position;
				for (; i < bytes.length && j < len; i++, j++) {
					b[off + j] = bytes[i];
				}
				this.position = i;
			}

			return len;
		}
		catch (Throwable t) {
			this.closed = true;
			this.s.cancel();
			cleanAndFinalize();
			throw Exceptions.propagate(t);
		}
		finally {
			lock.unlock();
		}
	}

	byte[] getBytesOrAwait() {
		if (this.available == null || this.available.length - this.position == 0) {
			this.available = null;

			int actualWorkAmount = this.workAmount.getAcquire();
			for (;;) {
				if (this.closed) {
					return CLOSED;
				}

				boolean d = this.done;
				T t = this.queue.poll();
				if (t != null) {
					int consumed = ++this.consumed;
					this.position = 0;
					this.available = Objects.requireNonNull(this.mapper.apply(t));
					if (consumed == this.limit) {
						this.consumed = 0;
						this.s.request(this.limit);
					}
					break;
				}

				if (d) {
					return DONE;
				}

				actualWorkAmount = workAmount.addAndGet(-actualWorkAmount);
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

			while((value = queue.poll()) != null) {
				discard(value);
			}

			if (this.workAmount.weakCompareAndSetPlain(workAmount, Integer.MIN_VALUE)) {
				return;
			}
		}
	}

	void discard(@Nullable T value) {
		if (value == null) {
			return;
		}

		try {
			this.onDiscardHandler.accept(value);
		} catch (Throwable t) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to release " + value.getClass().getSimpleName() + ": " + value, t);
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
			this.s.cancel();
			cleanAndFinalize();
		}
		finally {
			this.lock.unlock();
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

			if (parkedThread.compareAndSet( null, toUnpark)) {
				LockSupport.park();
				// we don't just break here because park() can wake up spuriously
				// if we got a proper resume, get() == READY and the loop will quit above
			}
		}
		// clear the resume indicator so that the next await call will park without a resume()
		this.parkedThread.lazySet(null);
	}

	private void resume() {
		if (this.parkedThread != READY) {
			Object old = parkedThread.getAndSet(READY);
			if (old != READY) {
				LockSupport.unpark((Thread)old);
			}
		}
	}

}

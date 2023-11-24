package org.springframework.core.io.buffer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.lang.Nullable;
import reactor.core.Exceptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bridges between {@link Publisher Publisher&lt;DataBuffer&gt;} and {@link InputStream}.
 *
 * <p>Note that this class has a near duplicate in
 * {@link org.springframework.http.client.InputStreamSubscriber}.
 *
 * @author Oleh Dokuka
 * @since 6.1
 */
final class InputStreamSubscriber extends InputStream implements Subscriber<DataBuffer> {

	static final Object READY = new Object();
	static final DataBuffer DONE = DefaultDataBuffer.fromEmptyByteBuffer(DefaultDataBufferFactory.sharedInstance, ByteBuffer.allocate(0));
	static final DataBuffer CLOSED = DefaultDataBuffer.fromEmptyByteBuffer(DefaultDataBufferFactory.sharedInstance, ByteBuffer.allocate(0));

	final int prefetch;
	final int limit;
	final ReentrantLock       lock;
	final Queue<DataBuffer> queue;

	final AtomicReference<Object> parkedThread = new AtomicReference<>();
	final AtomicInteger workAmount = new AtomicInteger();

	volatile boolean closed;
	int consumed;

	@Nullable
	DataBuffer available;

	@Nullable
	Subscription s;
	boolean done;
	@Nullable
	Throwable error;

	InputStreamSubscriber(int prefetch) {
		this.prefetch = prefetch;
		this.limit = prefetch == Integer.MAX_VALUE ? Integer.MAX_VALUE : prefetch - (prefetch >> 2);
		this.queue = new ArrayBlockingQueue<>(prefetch);
		this.lock = new ReentrantLock(false);
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		if (this.s != null) {
			subscription.cancel();
			return;
		}

		this.s = subscription;
		subscription.request(prefetch == Integer.MAX_VALUE ? Long.MAX_VALUE : prefetch);
	}

	@Override
	public void onNext(DataBuffer t) {
		if (this.done) {
			discard(t);
			return;
		}

		if (!queue.offer(t)) {
			discard(t);
			error = new RuntimeException("Buffer overflow");
			done = true;
		}

		int previousWorkState = addWork();
		if (previousWorkState == Integer.MIN_VALUE) {
			DataBuffer value = queue.poll();
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
			DataBuffer bytes = getBytesOrAwait();

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

			return bytes.read() & 0xFF;
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
				DataBuffer bytes = getBytesOrAwait();

				if (bytes == DONE) {
					cleanAndFinalize();
					if (this.error == null) {
						this.closed = true;
						return j == 0 ? -1 : j;
					}
					else {
						if (j == 0) {
							this.closed = true;
							throw Exceptions.propagate(error);
						}

						return j;
					}
				} else if (bytes == CLOSED) {
					this.s.cancel();
					cleanAndFinalize();
					return -1;
				}
				int initialReadPosition = bytes.readPosition();
				bytes.read(b, off + j, Math.min(len - j, bytes.readableByteCount()));
				j += bytes.readPosition() - initialReadPosition;
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

	DataBuffer getBytesOrAwait() {
		if (this.available == null || this.available.readableByteCount() == 0) {

			discard(this.available);
			this.available = null;

			int actualWorkAmount = this.workAmount.getAcquire();
			for (;;) {
				if (this.closed) {
					return CLOSED;
				}

				boolean d = this.done;
				DataBuffer t = this.queue.poll();
				if (t != null) {
					int consumed = ++this.consumed;
					this.available = t;
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
		discard(this.available);
		this.available = null;

		for (;;) {
			int workAmount = this.workAmount.getPlain();
			DataBuffer value;

			while((value = queue.poll()) != null) {
				discard(value);
			}

			if (this.workAmount.weakCompareAndSetPlain(workAmount, Integer.MIN_VALUE)) {
				return;
			}
		}
	}

	void discard(@Nullable DataBuffer value) {
		DataBufferUtils.release(value);
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

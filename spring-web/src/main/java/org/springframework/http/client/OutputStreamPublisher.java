/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Bridges between {@link OutputStream} and
 * {@link Flow.Publisher Flow.Publisher&lt;ByteBuffer&gt;}.
 *
 * @author Oleh Dokuka
 * @author Arjen Poutsma
 * @since 6.1
 * @see #create(OutputStreamHandler, Executor)
 */
final class OutputStreamPublisher implements Flow.Publisher<ByteBuffer> {

	private final OutputStreamHandler outputStreamHandler;

	private final Executor executor;


	private OutputStreamPublisher(OutputStreamHandler outputStreamHandler, Executor executor) {
		this.outputStreamHandler = outputStreamHandler;
		this.executor = executor;
	}


	/**
	 * Creates a new {@code Publisher<ByteBuffer>} based on bytes written to a
	 * {@code OutputStream}.
	 * <ul>
	 * <li>The parameter {@code outputStreamHandler} is invoked once per
	 * subscription of the returned {@code Publisher}, when the first
	 * {@code ByteBuffer} is
	 * {@linkplain Flow.Subscription#request(long) requested}.</li>
	 * <li>Each {@link OutputStream#write(byte[], int, int) OutputStream.write()}
	 * invocation that {@code outputStreamHandler} makes will result in a
	 * {@linkplain Flow.Subscriber#onNext(Object) published} {@code ByteBuffer}
	 * if there is {@linkplain Flow.Subscription#request(long) demand}.</li>
	 * <li>If there is <em>no demand</em>, {@code OutputStream.write()} will block
	 * until there is.</li>
	 * <li>If the subscription is {@linkplain Flow.Subscription#cancel() cancelled},
	 * {@code OutputStream.write()} will throw a {@code IOException}.</li>
	 * <li>{@linkplain OutputStream#close() Closing} the {@code OutputStream}
	 * will result in a {@linkplain Flow.Subscriber#onComplete() complete} signal.</li>
	 * <li>Any {@code IOException}s thrown from {@code outputStreamHandler} will
	 * be dispatched to the {@linkplain Flow.Subscriber#onError(Throwable) Subscriber}.
	 * </ul>
	 * @param outputStreamHandler invoked when the first buffer is requested
	 * @param executor used to invoke the {@code outputStreamHandler}
	 * @return a {@code Publisher<ByteBuffer>} based on bytes written by
	 * {@code outputStreamHandler}
	 */
	public static Flow.Publisher<ByteBuffer> create(OutputStreamHandler outputStreamHandler, Executor executor) {
		Assert.notNull(outputStreamHandler, "OutputStreamHandler must not be null");
		Assert.notNull(executor, "Executor must not be null");

		return new OutputStreamPublisher(outputStreamHandler, executor);
	}


	@Override
	public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
		Objects.requireNonNull(subscriber, "Subscriber must not be null");

		OutputStreamSubscription subscription = new OutputStreamSubscription(subscriber, this.outputStreamHandler);
		subscriber.onSubscribe(subscription);
		this.executor.execute(subscription::invokeHandler);
	}


	/**
	 * Defines the contract for handling the {@code OutputStream} provided by
	 * the {@code OutputStreamPublisher}.
	 */
	@FunctionalInterface
	public interface OutputStreamHandler {

		/**
		 * Use the given stream for writing.
		 * <ul>
		 * <li>If the linked subscription has
		 * {@linkplain Flow.Subscription#request(long) demand}, any
		 * {@linkplain OutputStream#write(byte[], int, int) written} bytes
		 * will be {@linkplain Flow.Subscriber#onNext(Object) published} to the
		 * {@link Flow.Subscriber Subscriber}.</li>
		 * <li>If there is no demand, any
		 * {@link OutputStream#write(byte[], int, int) write()} invocations will
		 * block until there is demand.</li>
		 * <li>If the linked subscription is
		 * {@linkplain Flow.Subscription#cancel() cancelled},
		 * {@link OutputStream#write(byte[], int, int) write()} invocations will
		 * result in a {@code IOException}.</li>
		 * </ul>
		 * @param outputStream the stream to write to
		 * @throws IOException any thrown I/O errors will be dispatched to the
		 * {@linkplain Flow.Subscriber#onError(Throwable) Subscriber}
		 */
		void handle(OutputStream outputStream) throws IOException;

	}


	private static final class OutputStreamSubscription extends OutputStream implements Flow.Subscription {

		static final Object READY = new Object();

		private final Flow.Subscriber<? super ByteBuffer> actual;

		private final OutputStreamHandler outputStreamHandler;

		private final AtomicLong requested = new AtomicLong();

		private final AtomicReference<Object> parkedThreadAtomic = new AtomicReference<>();

		@Nullable
		private volatile Throwable error;

		private long produced;


		public OutputStreamSubscription(Flow.Subscriber<? super ByteBuffer> actual,
										OutputStreamHandler outputStreamHandler) {
			this.actual = actual;
			this.outputStreamHandler = outputStreamHandler;
		}

		@Override
		public void write(int b) throws IOException {
			checkDemandAndAwaitIfNeeded();

			ByteBuffer byteBuffer = ByteBuffer.allocate(1);
			byteBuffer.put((byte) b);
			byteBuffer.flip();

			this.actual.onNext(byteBuffer);

			this.produced++;
		}

		@Override
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			checkDemandAndAwaitIfNeeded();

			ByteBuffer byteBuffer = ByteBuffer.allocate(len);
			byteBuffer.put(b, off, len);
			byteBuffer.flip();

			this.actual.onNext(byteBuffer);

			this.produced++;
		}

		private void checkDemandAndAwaitIfNeeded() throws IOException {
			long r = this.requested.get();

			if (isTerminated(r) || isCancelled(r)) {
				throw new IOException("Subscription has been terminated");
			}

			long p = this.produced;
			if (p == r) {
				if (p > 0) {
					r = tryProduce(p);
					this.produced = 0;
				}

				while (true) {
					if (isTerminated(r) || isCancelled(r)) {
						throw new IOException("Subscription has been terminated");
					}

					if (r != 0) {
						return;
					}

					await();

					r = this.requested.get();
				}
			}
		}

		private void invokeHandler() {
			// assume sync write within try-with-resource block

			// use BufferedOutputStream, so that written bytes are buffered
			// before publishing as byte buffer
			try (OutputStream outputStream = new BufferedOutputStream(this)) {
				this.outputStreamHandler.handle(outputStream);
			}
			catch (IOException ex) {
				long previousState = tryTerminate();
				if (isCancelled(previousState)) {
					return;
				}

				if (isTerminated(previousState)) {
					// failure due to illegal requestN
					this.actual.onError(this.error);
					return;
				}

				this.actual.onError(ex);
				return;
			}

			long previousState = tryTerminate();
			if (isCancelled(previousState)) {
				return;
			}

			if (isTerminated(previousState)) {
				// failure due to illegal requestN
				this.actual.onError(this.error);
				return;
			}

			this.actual.onComplete();
		}


		@Override
		public void request(long n) {
			if (n <= 0) {
				this.error = new IllegalArgumentException("request should be a positive number");
				long previousState = tryTerminate();

				if (isTerminated(previousState) || isCancelled(previousState)) {
					return;
				}

				if (previousState > 0) {
					// error should eventually be observed and propagated
					return;
				}

				// resume parked thread, so it can observe error and propagate it
				resume();
				return;
			}

			if (addCap(n) == 0) {
				// resume parked thread so it can continue the work
				resume();
			}
		}

		@Override
		public void cancel() {
			long previousState = tryCancel();
			if (isCancelled(previousState) || previousState > 0) {
				return;
			}

			// resume parked thread, so it can be unblocked and close all the resources
			resume();
		}

		private void await() {
			Thread toUnpark = Thread.currentThread();

			while (true) {
				Object current = this.parkedThreadAtomic.get();
				if (current == READY) {
					break;
				}

				if (current != null && current != toUnpark) {
					throw new IllegalStateException("Only one (Virtual)Thread can await!");
				}

				if (this.parkedThreadAtomic.compareAndSet(null, toUnpark)) {
					LockSupport.park();
					// we don't just break here because park() can wake up spuriously
					// if we got a proper resume, get() == READY and the loop will quit above
				}
			}
			// clear the resume indicator so that the next await call will park without a resume()
			this.parkedThreadAtomic.lazySet(null);
		}

		private void resume() {
			if (this.parkedThreadAtomic.get() != READY) {
				Object old = this.parkedThreadAtomic.getAndSet(READY);
				if (old != READY) {
					LockSupport.unpark((Thread)old);
				}
			}
		}

		private long tryCancel() {
			while (true) {
				long r = this.requested.get();

				if (isCancelled(r)) {
					return r;
				}

				if (this.requested.compareAndSet(r, Long.MIN_VALUE)) {
					return r;
				}
			}
		}

		private long tryTerminate() {
			while (true) {
				long r = this.requested.get();

				if (isCancelled(r) || isTerminated(r)) {
					return r;
				}

				if (this.requested.compareAndSet(r, Long.MIN_VALUE | Long.MAX_VALUE)) {
					return r;
				}
			}
		}

		private long tryProduce(long n) {
			while (true) {
				long current = this.requested.get();
				if (isTerminated(current) || isCancelled(current)) {
					return current;
				}
				if (current == Long.MAX_VALUE) {
					return Long.MAX_VALUE;
				}
				long update = current - n;
				if (update < 0L) {
					update = 0L;
				}
				if (this.requested.compareAndSet(current, update)) {
					return update;
				}
			}
		}

		private long addCap(long n) {
			while (true) {
				long r = this.requested.get();
				if (isTerminated(r) || isCancelled(r) || r == Long.MAX_VALUE) {
					return r;
				}
				long u = addCap(r, n);
				if (this.requested.compareAndSet(r, u)) {
					return r;
				}
			}
		}

		private static boolean isTerminated(long state) {
			return state == (Long.MIN_VALUE | Long.MAX_VALUE);
		}

		private static boolean isCancelled(long state) {
			return state == Long.MIN_VALUE;
		}

		private static long addCap(long a, long b) {
			long res = a + b;
			if (res < 0L) {
				return Long.MAX_VALUE;
			}
			return res;
		}
	}
}

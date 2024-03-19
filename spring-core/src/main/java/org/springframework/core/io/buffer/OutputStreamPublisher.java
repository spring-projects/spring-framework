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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.lang.Nullable;

/**
 * Bridges between {@link OutputStream} and
 * {@link Publisher Publisher&lt;DataBuffer&gt;}.
 *
 * <p>Note that this class has a near duplicate in
 * {@link org.springframework.http.client.OutputStreamPublisher}.
 *
 * @author Oleh Dokuka
 * @author Arjen Poutsma
 * @since 6.1
 */
final class OutputStreamPublisher implements Publisher<DataBuffer> {

	private final Consumer<OutputStream> outputStreamConsumer;

	private final DataBufferFactory bufferFactory;

	private final Executor executor;

	private final int chunkSize;


	OutputStreamPublisher(Consumer<OutputStream> outputStreamConsumer, DataBufferFactory bufferFactory,
			Executor executor, int chunkSize) {

		this.outputStreamConsumer = outputStreamConsumer;
		this.bufferFactory = bufferFactory;
		this.executor = executor;
		this.chunkSize = chunkSize;
	}


	@Override
	public void subscribe(Subscriber<? super DataBuffer> subscriber) {
		// We don't use Assert.notNull(), because a NullPointerException is required
		// for Reactive Streams compliance.
		Objects.requireNonNull(subscriber, "Subscriber must not be null");

		OutputStreamSubscription subscription = new OutputStreamSubscription(
				subscriber, this.outputStreamConsumer, this.bufferFactory, this.chunkSize);

		subscriber.onSubscribe(subscription);
		this.executor.execute(subscription::invokeHandler);
	}


	private static final class OutputStreamSubscription extends OutputStream implements Subscription {

		private static final Object READY = new Object();

		private final Subscriber<? super DataBuffer> actual;

		private final Consumer<OutputStream> outputStreamHandler;

		private final DataBufferFactory bufferFactory;

		private final int chunkSize;

		private final AtomicLong requested = new AtomicLong();

		private final AtomicReference<Object> parkedThread = new AtomicReference<>();

		@Nullable
		private volatile Throwable error;

		private long produced;

		OutputStreamSubscription(Subscriber<? super DataBuffer> actual,
				Consumer<OutputStream> outputStreamConsumer, DataBufferFactory bufferFactory, int chunkSize) {

			this.actual = actual;
			this.outputStreamHandler = outputStreamConsumer;
			this.bufferFactory = bufferFactory;
			this.chunkSize = chunkSize;
		}

		@Override
		public void write(int b) throws IOException {
			checkDemandAndAwaitIfNeeded();

			DataBuffer next = this.bufferFactory.allocateBuffer(1);
			next.write((byte) b);

			this.actual.onNext(next);

			this.produced++;
		}

		@Override
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			checkDemandAndAwaitIfNeeded();

			DataBuffer next = this.bufferFactory.allocateBuffer(len);
			next.write(b, off, len);

			this.actual.onNext(next);

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
			try (OutputStream outputStream = new BufferedOutputStream(this, this.chunkSize)) {
				this.outputStreamHandler.accept(outputStream);
			}
			catch (Exception ex) {
				long previousState = tryTerminate();
				if (isCancelled(previousState)) {
					return;
				}
				if (isTerminated(previousState)) {
					// failure due to illegal requestN
					Throwable error = this.error;
					if (error != null) {
						this.actual.onError(error);
						return;
					}
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
				Throwable error = this.error;
				if (error != null) {
					this.actual.onError(error);
					return;
				}
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
				Object current = this.parkedThread.get();
				if (current == READY) {
					break;
				}

				if (current != null && current != toUnpark) {
					throw new IllegalStateException("Only one (Virtual)Thread can await!");
				}

				if (this.parkedThread.compareAndSet(null, toUnpark)) {
					LockSupport.park();
					// we don't just break here because park() can wake up spuriously
					// if we got a proper resume, get() == READY and the loop will quit above
				}
			}
			// clear the resume indicator so that the next await call will park without a resume()
			this.parkedThread.lazySet(null);
		}

		private void resume() {
			if (this.parkedThread.get() != READY) {
				Object old = this.parkedThread.getAndSet(READY);
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

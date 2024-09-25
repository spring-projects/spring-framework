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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Bridges between {@link OutputStream} and {@link Publisher Publisher&lt;DataBuffer&gt;}.
 *
 * <p>When there is demand on the Reactive Streams subscription, any write to
 * the OutputStream is mapped to a buffer and published to the subscriber.
 * If there is no demand, writes block until demand materializes.
 * If the subscription is cancelled, further writes raise {@code IOException}.
 *
 * <p>Note that this class has a near duplicate in
 * {@link org.springframework.http.client.OutputStreamPublisher}.
 *
 * @author Oleh Dokuka
 * @author Arjen Poutsma
 * @since 6.1
 * @param <T> the published byte buffer type
 */
final class OutputStreamPublisher<T> implements Publisher<T> {

	private static final int DEFAULT_CHUNK_SIZE = 1024;


	private final OutputStreamHandler outputStreamHandler;

	private final ByteMapper<T> byteMapper;

	private final Executor executor;

	private final int chunkSize;


	/**
	 * Create an instance.
	 * @param outputStreamHandler invoked when the first buffer is requested
	 * @param byteMapper maps written bytes to {@code T}
	 * @param executor used to invoke the {@code outputStreamHandler}
	 * @param chunkSize the chunk sizes to be produced by the publisher
	 */
	OutputStreamPublisher(
			OutputStreamHandler outputStreamHandler, ByteMapper<T> byteMapper,
			Executor executor, @Nullable Integer chunkSize) {

		Assert.notNull(outputStreamHandler, "OutputStreamHandler must not be null");
		Assert.notNull(byteMapper, "ByteMapper must not be null");
		Assert.notNull(executor, "Executor must not be null");
		Assert.isTrue(chunkSize == null || chunkSize > 0, "ChunkSize must be larger than 0");

		this.outputStreamHandler = outputStreamHandler;
		this.byteMapper = byteMapper;
		this.executor = executor;
		this.chunkSize = (chunkSize != null ? chunkSize : DEFAULT_CHUNK_SIZE);
	}


	@Override
	public void subscribe(Subscriber<? super T> subscriber) {
		// We don't use Assert.notNull(), because a NullPointerException is required
		// for Reactive Streams compliance.
		Objects.requireNonNull(subscriber, "Subscriber must not be null");

		OutputStreamSubscription<T> subscription = new OutputStreamSubscription<>(
				subscriber, this.outputStreamHandler, this.byteMapper, this.chunkSize);

		subscriber.onSubscribe(subscription);
		this.executor.execute(subscription::invokeHandler);
	}


	/**
	 * Contract to provide callback access to the {@link OutputStream}.
	 */
	@FunctionalInterface
	public interface OutputStreamHandler {

		void handle(OutputStream outputStream) throws Exception;

	}


	/**
	 * Maps from bytes to byte buffers.
	 * @param <T> the type of byte buffer to map to
	 */
	public interface ByteMapper<T> {

		T map(int b);

		T map(byte[] b, int off, int len);

	}


	private static final class OutputStreamSubscription<T> extends OutputStream implements Subscription {

		private static final Object READY = new Object();

		private final Subscriber<? super T> actual;

		private final OutputStreamHandler outputStreamHandler;

		private final ByteMapper<T> byteMapper;

		private final int chunkSize;

		private final AtomicLong requested = new AtomicLong();

		private final AtomicReference<Object> parkedThread = new AtomicReference<>();

		@Nullable
		private volatile Throwable error;

		private long produced;

		OutputStreamSubscription(
				Subscriber<? super T> actual, OutputStreamHandler outputStreamHandler,
				ByteMapper<T> byteMapper, int chunkSize) {

			this.actual = actual;
			this.outputStreamHandler = outputStreamHandler;
			this.byteMapper = byteMapper;
			this.chunkSize = chunkSize;
		}

		@Override
		public void write(int b) throws IOException {
			checkDemandAndAwaitIfNeeded();
			T next = this.byteMapper.map(b);
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
			T next = this.byteMapper.map(b, off, len);
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
				this.outputStreamHandler.handle(outputStream);
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

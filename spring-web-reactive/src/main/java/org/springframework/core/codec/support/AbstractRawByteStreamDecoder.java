/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.codec.support;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.subscriber.SubscriberBarrier;
import reactor.core.util.BackpressureUtils;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Abstract {@link Decoder} that plugs a {@link SubscriberBarrier} into the {@code Flux}
 * pipeline in order to apply splitting/aggregation operations on the stream of data.
 *
 * @author Brian Clozel
 */
public abstract class AbstractRawByteStreamDecoder<T> extends AbstractDecoder<T> {

	private final DataBufferAllocator allocator;

	public AbstractRawByteStreamDecoder(DataBufferAllocator allocator,
			MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
		Assert.notNull(allocator, "'allocator' must not be null");

		this.allocator = allocator;
	}

	@Override
	public Flux<T> decode(Publisher<DataBuffer> inputStream, ResolvableType type,
			MimeType mimeType, Object... hints) {

		return decodeInternal(Flux.from(inputStream).lift(bbs -> subscriberBarrier(bbs)),
				type, mimeType, hints);
	}

	/**
	 * Create a {@link SubscriberBarrier} instance that will be plugged into the Publisher pipeline
	 *
	 * <p>Implementations should provide their own {@link SubscriberBarrier} or use one of the
	 * provided implementations by this class
	 */
	public abstract SubscriberBarrier<DataBuffer, DataBuffer> subscriberBarrier(
			Subscriber<? super DataBuffer> subscriber);

	public abstract Flux<T> decodeInternal(Publisher<DataBuffer> inputStream,
			ResolvableType type
			, MimeType mimeType, Object... hints);


	/**
	 * {@code SubscriberBarrier} implementation that buffers all received elements and emits a single
	 * {@code DataBuffer} once the incoming stream has been completed
	 */
	public static class ReduceSingleByteStreamBarrier
			extends SubscriberBarrier<DataBuffer, DataBuffer> {

		@SuppressWarnings("rawtypes")
		static final AtomicLongFieldUpdater<ReduceSingleByteStreamBarrier> REQUESTED =
				AtomicLongFieldUpdater.newUpdater(ReduceSingleByteStreamBarrier.class, "requested");

		static final AtomicIntegerFieldUpdater<ReduceSingleByteStreamBarrier> TERMINATED =
				AtomicIntegerFieldUpdater.newUpdater(ReduceSingleByteStreamBarrier.class, "terminated");

		private volatile long requested;

		private volatile int terminated;

		private DataBuffer buffer;

		public ReduceSingleByteStreamBarrier(Subscriber<? super DataBuffer> subscriber,
				DataBufferAllocator allocator) {
			super(subscriber);
			this.buffer = allocator.allocateBuffer();
		}

		@Override
		protected void doRequest(long n) {
			BackpressureUtils.getAndAdd(REQUESTED, this, n);
			if (TERMINATED.compareAndSet(this, 1, 2)) {
				drainLast();
			}
			else {
				super.doRequest(Long.MAX_VALUE);
			}
		}

		@Override
		protected void doComplete() {
			if (TERMINATED.compareAndSet(this, 0, 1)) {
				drainLast();
			}
		}

		/*
		 * TODO: when available, wrap buffers with a single buffer and avoid copying data for every method call.
		 */
		@Override
		protected void doNext(DataBuffer dataBuffer) {
			this.buffer.write(dataBuffer);
		}

		protected void drainLast() {
			if (BackpressureUtils.getAndSub(REQUESTED, this, 1L) > 0) {
				subscriber.onNext(this.buffer);
				super.doComplete();
			}
		}
	}

	/**
	 * {@code SubscriberBarrier} implementation that splits incoming elements
	 * using line return delimiters: {@code "\n"} and {@code "\r\n"}
	 */
	public static class SplitLinesByteStreamBarrier
			extends SubscriberBarrier<DataBuffer, DataBuffer> {

		@SuppressWarnings("rawtypes")
		static final AtomicLongFieldUpdater<SplitLinesByteStreamBarrier> REQUESTED =
				AtomicLongFieldUpdater.newUpdater(SplitLinesByteStreamBarrier.class, "requested");

		static final AtomicIntegerFieldUpdater<SplitLinesByteStreamBarrier> TERMINATED =
				AtomicIntegerFieldUpdater.newUpdater(SplitLinesByteStreamBarrier.class, "terminated");

		private final DataBufferAllocator allocator;


		private volatile long requested;

		private volatile int terminated;

		private DataBuffer buffer;

		public SplitLinesByteStreamBarrier(Subscriber<? super DataBuffer> subscriber,
				DataBufferAllocator allocator) {
			super(subscriber);
			this.allocator = allocator;
			this.buffer = allocator.allocateBuffer();
		}

		@Override
		protected void doRequest(long n) {
			BackpressureUtils.getAndAdd(REQUESTED, this, n);
			if (TERMINATED.compareAndSet(this, 1, 2)) {
				drainLast();
			}
			else {
				super.doRequest(n);
			}
		}

		@Override
		protected void doComplete() {
			if (TERMINATED.compareAndSet(this, 0, 1)) {
				drainLast();
			}
		}

		/*
		 * TODO: when available, wrap buffers with a single buffer and avoid copying data for every method call.
		 */
		@Override
		protected void doNext(DataBuffer dataBuffer) {
			this.buffer.write(dataBuffer);

			while (REQUESTED.get(this) > 0) {
				int separatorIndex = findEndOfLine(this.buffer);
				if (separatorIndex != -1) {
					if (BackpressureUtils.getAndSub(REQUESTED, this, 1L) > 0) {
						byte[] message = new byte[separatorIndex];
						this.buffer.read(message);
						consumeSeparator(this.buffer);
//						this.buffer = this.buffer.slice();
						DataBuffer buffer2 = allocator.allocateBuffer(message.length);
						buffer2.write(message);
						super.doNext(buffer2);
					}
				}
				else {
					super.doRequest(1);
				}
			}
		}

		protected int findEndOfLine(DataBuffer buffer) {

			final int n = buffer.readableByteCount();
			for (int i = 0; i < n; i++) {
				final byte b = buffer.get(i);
				if (b == '\n') {
					return i;
				}
				else if (b == '\r' && i < n - 1 && buffer.get(i + 1) == '\n') {
					return i;
				}
			}

			return -1;
		}

		protected void consumeSeparator(DataBuffer buffer) {
			byte sep = buffer.read();
			if (sep == '\r') {
				buffer.read();
			}
		}

		protected void drainLast() {
			if (BackpressureUtils.getAndSub(REQUESTED, this, 1L) > 0) {
				subscriber.onNext(this.buffer);
				super.doComplete();
			}
		}
	}

}

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

package org.springframework.core.io.buffer.support;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSource;
import reactor.core.subscriber.SubscriberWithContext;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.util.Assert;

/**i
 * Utility class for working with {@link DataBuffer}s.
 *
 * @author Arjen Poutsma
 */
public abstract class DataBufferUtils {

	private static final Consumer<? extends Closeable> CLOSE_CONSUMER = closeable -> {
		try {
			if (closeable != null) {
				closeable.close();
			}
		}
		catch (IOException ignored) {
		}
	};

	/**
	 * Reads the given {@code ReadableByteChannel} into a {@code Flux} of
	 * {@code DataBuffer}s. Closes the channel when the flux is terminated.
	 * @param channel the channel to read from
	 * @param allocator the allocator to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(ReadableByteChannel channel,
			DataBufferAllocator allocator, int bufferSize) {
		Assert.notNull(channel, "'channel' must not be null");
		Assert.notNull(allocator, "'allocator' must not be null");

		return Flux.create(new ReadableByteChannelConsumer(allocator, bufferSize),
				subscriber -> channel, closeConsumer());
	}

	/**
	 * Reads the given {@code InputStream} into a {@code Flux} of
	 * {@code DataBuffer}s. Closes the stream when the flux inputStream terminated.
	 * @param inputStream the input stream to read from
	 * @param allocator the allocator to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(InputStream inputStream,
			DataBufferAllocator allocator, int bufferSize) {
		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(allocator, "'allocator' must not be null");

		ReadableByteChannel channel = Channels.newChannel(inputStream);
		return read(channel, allocator, bufferSize);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Closeable> Consumer<T> closeConsumer() {
		return (Consumer<T>) CLOSE_CONSUMER;
	}

	/**
	 * Relays buffers from the given {@link Publisher} until the total
	 * {@linkplain DataBuffer#readableByteCount() byte count} reaches the given maximum
	 * byte count, or until the publisher is complete.
	 * @param publisher the publisher to filter
	 * @param maxByteCount the maximum byte count
	 * @return a flux whose maximum byte count is {@code maxByteCount}
	 */
	public static Flux<DataBuffer> takeUntilByteCount(Publisher<DataBuffer> publisher,
			long maxByteCount) {
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.isTrue(maxByteCount >= 0, "'maxByteCount' must be a positive number");

		return new TakeByteUntilCount(publisher, maxByteCount);
	}

	/**
	 * Releases the given data buffer, if it is a {@link PooledDataBuffer}.
	 * @param dataBuffer the data buffer to release
	 * @return {@code true} if the buffer was released; {@code false} otherwise.
	 */
	public static boolean release(DataBuffer dataBuffer) {
		if (dataBuffer instanceof PooledDataBuffer) {
			return ((PooledDataBuffer) dataBuffer).release();
		}
		return false;
	}

	private static final class TakeByteUntilCount extends FluxSource<DataBuffer, DataBuffer> {

		final long maxByteCount;

		TakeByteUntilCount(Publisher<? extends DataBuffer> source, long maxByteCount) {
			super(source);
			this.maxByteCount = maxByteCount;
		}

		@Override
		public void subscribe(Subscriber<? super DataBuffer> subscriber) {
			source.subscribe(new Subscriber<DataBuffer>() {

				private Subscription subscription;

				private final AtomicLong byteCount = new AtomicLong();

				@Override
				public void onSubscribe(Subscription s) {
					this.subscription = s;
					subscriber.onSubscribe(s);
				}

				@Override
				public void onNext(DataBuffer dataBuffer) {
					int delta = dataBuffer.readableByteCount();
					long currentCount = this.byteCount.addAndGet(delta);
					if (currentCount > maxByteCount) {
						int size = (int) (maxByteCount - currentCount + delta);
						ByteBuffer byteBuffer =
								(ByteBuffer) dataBuffer.asByteBuffer().limit(size);
						DataBuffer partialBuffer =
								dataBuffer.allocator().allocateBuffer(size);
						partialBuffer.write(byteBuffer);

						subscriber.onNext(partialBuffer);
						subscriber.onComplete();
						this.subscription.cancel();
					}
					else {
						subscriber.onNext(dataBuffer);
					}
				}

				@Override
				public void onError(Throwable t) {
					subscriber.onError(t);
				}

				@Override
				public void onComplete() {
					subscriber.onComplete();
				}
			});
		}
	}

	private static class ReadableByteChannelConsumer
			implements Consumer<SubscriberWithContext<DataBuffer, ReadableByteChannel>> {

		private final DataBufferAllocator allocator;

		private final int chunkSize;

		public ReadableByteChannelConsumer(DataBufferAllocator allocator, int chunkSize) {
			this.allocator = allocator;
			this.chunkSize = chunkSize;
		}

		@Override
		public void accept(SubscriberWithContext<DataBuffer, ReadableByteChannel> sub) {
			try {
				ByteBuffer byteBuffer = ByteBuffer.allocate(chunkSize);
				int read;
				ReadableByteChannel channel = sub.context();
				if ((read = channel.read(byteBuffer)) > 0) {
					byteBuffer.flip();
					boolean release = true;
					DataBuffer dataBuffer = this.allocator.allocateBuffer(read);
					try {
						dataBuffer.write(byteBuffer);
						release = false;
						sub.onNext(dataBuffer);
					}
					finally {
						if (release) {
							release(dataBuffer);
						}
					}
				}
				else {
					sub.onComplete();
				}
			}
			catch (IOException ex) {
				sub.onError(ex);
			}
		}
	}

}

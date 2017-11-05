/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.io.buffer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SynchronousSink;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utility class for working with {@link DataBuffer}s.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class DataBufferUtils {

	private static final Consumer<DataBuffer> RELEASE_CONSUMER = DataBufferUtils::release;

	//---------------------------------------------------------------------
	// Reading
	//---------------------------------------------------------------------

	/**
	 * Read the given {@code InputStream} into a {@code Flux} of
	 * {@code DataBuffer}s. Closes the input stream when the flux is terminated.
	 * @param inputStream the input stream to read from
	 * @param dataBufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(InputStream inputStream,
			DataBufferFactory dataBufferFactory, int bufferSize) {

		Assert.notNull(inputStream, "InputStream must not be null");

		ReadableByteChannel channel = Channels.newChannel(inputStream);
		return read(channel, dataBufferFactory, bufferSize);
	}

	/**
	 * Read the given {@code ReadableByteChannel} into a {@code Flux} of
	 * {@code DataBuffer}s. Closes the channel when the flux is terminated.
	 * @param channel the channel to read from
	 * @param dataBufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(ReadableByteChannel channel,
			DataBufferFactory dataBufferFactory, int bufferSize) {

		Assert.notNull(channel, "ReadableByteChannel must not be null");
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be > 0");

		return Flux.generate(() -> channel,
				new ReadableByteChannelGenerator(dataBufferFactory, bufferSize),
				DataBufferUtils::closeChannel);
	}

	/**
	 * Read the given {@code AsynchronousFileChannel} into a {@code Flux} of
	 * {@code DataBuffer}s. Closes the channel when the flux is terminated.
	 * @param channel the channel to read from
	 * @param dataBufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(AsynchronousFileChannel channel,
			DataBufferFactory dataBufferFactory, int bufferSize) {
		return read(channel, 0, dataBufferFactory, bufferSize);
	}

	/**
	 * Read the given {@code AsynchronousFileChannel} into a {@code Flux} of
	 * {@code DataBuffer}s, starting at the given position. Closes the channel when the flux is
	 * terminated.
	 * @param channel the channel to read from
	 * @param position the position to start reading from
	 * @param dataBufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(AsynchronousFileChannel channel,
			long position, DataBufferFactory dataBufferFactory, int bufferSize) {

		Assert.notNull(channel, "'channel' must not be null");
		Assert.notNull(dataBufferFactory, "'dataBufferFactory' must not be null");
		Assert.isTrue(position >= 0, "'position' must be >= 0");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be > 0");

		DataBuffer dataBuffer = dataBufferFactory.allocateBuffer(bufferSize);
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, bufferSize);

		return Flux.create(sink -> {
			sink.onDispose(() -> closeChannel(channel));
			CompletionHandler<Integer, DataBuffer> completionHandler =
					new AsynchronousFileChannelReadCompletionHandler(channel, sink, position,
							dataBufferFactory, bufferSize);
			channel.read(byteBuffer, position, dataBuffer, completionHandler);
		});
	}

	/**
	 * Read the given {@code Resource} into a {@code Flux} of {@code DataBuffer}s.
	 * <p>If the resource is a file, it is read into an
	 * {@code AsynchronousFileChannel} and turned to {@code Flux} via
	 * {@link #read(AsynchronousFileChannel, DataBufferFactory, int)} or else
	 * fall back on {@link #read(InputStream, DataBufferFactory, int)} closes
	 * the channel when the flux is terminated.
	 * @param resource the resource to read from
	 * @param dataBufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(Resource resource,
			DataBufferFactory dataBufferFactory, int bufferSize) {

		return read(resource, 0, dataBufferFactory, bufferSize);
	}

	/**
	 * Read the given {@code Resource} into a {@code Flux} of {@code DataBuffer}s
	 * starting at the given position.
	 * <p>If the resource is a file, it is read into an
	 * {@code AsynchronousFileChannel} and turned to {@code Flux} via
	 * {@link #read(AsynchronousFileChannel, DataBufferFactory, int)} or else
	 * fall back on {@link #read(InputStream, DataBufferFactory, int)}. Closes
	 * the channel when the flux is terminated.
	 * @param resource the resource to read from
	 * @param position the position to start reading from
	 * @param dataBufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(Resource resource, long position,
			DataBufferFactory dataBufferFactory, int bufferSize) {

		try {
			if (resource.isFile()) {
				File file = resource.getFile();
				AsynchronousFileChannel channel =
						AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ);
				return DataBufferUtils.read(channel, position, dataBufferFactory, bufferSize);
			}
		}
		catch (IOException ignore) {
			// fallback to resource.readableChannel(), below
		}

		try {
			ReadableByteChannel channel = resource.readableChannel();
			Flux<DataBuffer> in = DataBufferUtils.read(channel, dataBufferFactory, bufferSize);
			return DataBufferUtils.skipUntilByteCount(in, position);
		}
		catch (IOException ex) {
			return Flux.error(ex);
		}
	}


	//---------------------------------------------------------------------
	// Writing
	//---------------------------------------------------------------------

	/**
	 * Write the given stream of {@link DataBuffer}s to the given {@code OutputStream}. Does
	 * <strong>not</strong> close the output stream when the flux is terminated, and does
	 * <strong>not</strong> {@linkplain #release(DataBuffer) release} the data buffers in the
	 * source. If releasing is required, then subscribe to the returned {@code Flux} with a
	 * {@link #releaseConsumer()}.
	 * <p>Note that the writing process does not start until the returned {@code Flux} is subscribed
	 * to.
	 * @param source the stream of data buffers to be written
	 * @param outputStream the output stream to write to
	 * @return a flux containing the same buffers as in {@code source}, that starts the writing
	 * process when subscribed to, and that publishes any writing errors and the completion signal
	 */
	public static Flux<DataBuffer> write(Publisher<DataBuffer> source,
			OutputStream outputStream) {

		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(outputStream, "'outputStream' must not be null");

		WritableByteChannel channel = Channels.newChannel(outputStream);
		return write(source, channel);
	}

	/**
	 * Write the given stream of {@link DataBuffer}s to the given {@code WritableByteChannel}. Does
	 * <strong>not</strong> close the channel when the flux is terminated, and does
	 * <strong>not</strong> {@linkplain #release(DataBuffer) release} the data buffers in the
	 * source. If releasing is required, then subscribe to the returned {@code Flux} with a
	 * {@link #releaseConsumer()}.
	 * <p>Note that the writing process does not start until the returned {@code Flux} is subscribed
	 * to.
	 * @param source the stream of data buffers to be written
	 * @param channel the channel to write to
	 * @return a flux containing the same buffers as in {@code source}, that starts the writing
	 * process when subscribed to, and that publishes any writing errors and the completion signal
	 */
	public static Flux<DataBuffer> write(Publisher<DataBuffer> source,
			WritableByteChannel channel) {

		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(channel, "'channel' must not be null");

		Flux<DataBuffer> flux = Flux.from(source);

		return Flux.create(sink ->
				flux.subscribe(dataBuffer -> {
							try {
								ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
								while (byteBuffer.hasRemaining()) {
									channel.write(byteBuffer);
								}
								sink.next(dataBuffer);
							}
							catch (IOException ex) {
								sink.error(ex);
							}

						},
						sink::error,
						sink::complete));
	}

	/**
	 * Write the given stream of {@link DataBuffer}s to the given {@code AsynchronousFileChannel}.
	 * Does <strong>not</strong> close the channel when the flux is terminated, and does
	 * <strong>not</strong> {@linkplain #release(DataBuffer) release} the data buffers in the
	 * source. If releasing is required, then subscribe to the returned {@code Flux} with a
	 * {@link #releaseConsumer()}.
	 * <p>Note that the writing process does not start until the returned {@code Flux} is subscribed
	 * to.
	 * @param source the stream of data buffers to be written
	 * @param channel the channel to write to
	 * @return a flux containing the same buffers as in {@code source}, that starts the writing
	 * process when subscribed to, and that publishes any writing errors and the completion signal
	 */
	public static Flux<DataBuffer> write(Publisher<DataBuffer> source, AsynchronousFileChannel channel,
			long position) {

		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(channel, "'channel' must not be null");
		Assert.isTrue(position >= 0, "'position' must be >= 0");

		Flux<DataBuffer> flux = Flux.from(source);

		return Flux.create(sink -> {
			BaseSubscriber<DataBuffer> subscriber =
					new AsynchronousFileChannelWriteCompletionHandler(sink, channel, position);
			flux.subscribe(subscriber);
		});
	}


	private static void closeChannel(@Nullable Channel channel) {
		try {
			if (channel != null) {
				channel.close();
			}
		}
		catch (IOException ignored) {
		}
	}

	/**
	 * Relay buffers from the given {@link Publisher} until the total
	 * {@linkplain DataBuffer#readableByteCount() byte count} reaches
	 * the given maximum byte count, or until the publisher is complete.
	 * @param publisher the publisher to filter
	 * @param maxByteCount the maximum byte count
	 * @return a flux whose maximum byte count is {@code maxByteCount}
	 */
	public static Flux<DataBuffer> takeUntilByteCount(Publisher<DataBuffer> publisher, long maxByteCount) {
		Assert.notNull(publisher, "Publisher must not be null");
		Assert.isTrue(maxByteCount >= 0, "'maxByteCount' must be a positive number");
		AtomicLong byteCountDown = new AtomicLong(maxByteCount);

		return Flux.from(publisher).
				takeWhile(dataBuffer -> {
					int delta = -dataBuffer.readableByteCount();
					long currentCount = byteCountDown.getAndAdd(delta);
					return currentCount >= 0;
				}).
				map(dataBuffer -> {
					long currentCount = byteCountDown.get();
					if (currentCount >= 0) {
						return dataBuffer;
					}
					else {
						// last buffer
						int size = (int) (currentCount + dataBuffer.readableByteCount());
						return dataBuffer.slice(0, size);
					}
				});
	}

	//---------------------------------------------------------------------
	// Various
	//---------------------------------------------------------------------

	/**
	 * Skip buffers from the given {@link Publisher} until the total
	 * {@linkplain DataBuffer#readableByteCount() byte count} reaches
	 * the given maximum byte count, or until the publisher is complete.
	 * @param publisher the publisher to filter
	 * @param maxByteCount the maximum byte count
	 * @return a flux with the remaining part of the given publisher
	 */
	public static Flux<DataBuffer> skipUntilByteCount(Publisher<DataBuffer> publisher, long maxByteCount) {
		Assert.notNull(publisher, "Publisher must not be null");
		Assert.isTrue(maxByteCount >= 0, "'maxByteCount' must be a positive number");
		AtomicLong byteCountDown = new AtomicLong(maxByteCount);

		return Flux.from(publisher).
				skipUntil(dataBuffer -> {
					int delta = -dataBuffer.readableByteCount();
					long currentCount = byteCountDown.addAndGet(delta);
					if(currentCount < 0) {
						return true;
					} else {
						DataBufferUtils.release(dataBuffer);
						return false;
					}
				}).
				map(dataBuffer -> {
					long currentCount = byteCountDown.get();
					// slice first buffer, then let others flow through
					if (currentCount < 0) {
						int skip = (int) (currentCount + dataBuffer.readableByteCount());
						byteCountDown.set(0);
						return dataBuffer.slice(skip, dataBuffer.readableByteCount() - skip);
					}
					return dataBuffer;
				});
	}

	/**
	 * Retain the given data buffer, it it is a {@link PooledDataBuffer}.
	 * @param dataBuffer the data buffer to retain
	 * @return the retained buffer
	 */
	@SuppressWarnings("unchecked")
	public static <T extends DataBuffer> T retain(T dataBuffer) {
		if (dataBuffer instanceof PooledDataBuffer) {
			return (T) ((PooledDataBuffer) dataBuffer).retain();
		}
		else {
			return dataBuffer;
		}
	}

	/**
	 * Release the given data buffer, if it is a {@link PooledDataBuffer}.
	 * @param dataBuffer the data buffer to release
	 * @return {@code true} if the buffer was released; {@code false} otherwise.
	 */
	public static boolean release(@Nullable DataBuffer dataBuffer) {
		if (dataBuffer instanceof PooledDataBuffer) {
			return ((PooledDataBuffer) dataBuffer).release();
		}
		return false;
	}

	/**
	 * Returns a consumer that calls {@link #release(DataBuffer)} on all
	 * passed data buffers.
	 */
	public static Consumer<DataBuffer> releaseConsumer() {
		return RELEASE_CONSUMER;
	}


	private static class ReadableByteChannelGenerator
			implements BiFunction<ReadableByteChannel, SynchronousSink<DataBuffer>, ReadableByteChannel> {

		private final DataBufferFactory dataBufferFactory;

		private final int bufferSize;

		public ReadableByteChannelGenerator(DataBufferFactory dataBufferFactory, int bufferSize) {
			this.dataBufferFactory = dataBufferFactory;
			this.bufferSize = bufferSize;
		}

		@Override
		public ReadableByteChannel apply(ReadableByteChannel channel,
				SynchronousSink<DataBuffer> sub) {
			boolean release = true;
			DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer(this.bufferSize);
			try {
				int read;
				ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, dataBuffer.capacity());
				if ((read = channel.read(byteBuffer)) >= 0) {
					dataBuffer.writePosition(read);
					release = false;
					sub.next(dataBuffer);
				}
				else {
					sub.complete();
				}
			}
			catch (IOException ex) {
				sub.error(ex);
			}
			finally {
				if (release) {
					release(dataBuffer);
				}
			}
			return channel;
		}
	}

	private static class AsynchronousFileChannelReadCompletionHandler
			implements CompletionHandler<Integer, DataBuffer> {

		private final AsynchronousFileChannel channel;

		private final FluxSink<DataBuffer> sink;

		private final DataBufferFactory dataBufferFactory;

		private final int bufferSize;

		private AtomicLong position;

		private AsynchronousFileChannelReadCompletionHandler(
				AsynchronousFileChannel channel, FluxSink<DataBuffer> sink,
				long position, DataBufferFactory dataBufferFactory, int bufferSize) {
			this.channel = channel;
			this.sink = sink;
			this.position = new AtomicLong(position);
			this.dataBufferFactory = dataBufferFactory;
			this.bufferSize = bufferSize;
		}

		@Override
		public void completed(Integer read, DataBuffer dataBuffer) {
			if (read != -1) {
				long pos = this.position.addAndGet(read);
				dataBuffer.writePosition(read);
				this.sink.next(dataBuffer);

				if (!this.sink.isCancelled()) {
					DataBuffer newDataBuffer =
							this.dataBufferFactory.allocateBuffer(this.bufferSize);
					ByteBuffer newByteBuffer = newDataBuffer.asByteBuffer(0, this.bufferSize);
					this.channel.read(newByteBuffer, pos, newDataBuffer, this);
				}
			}
			else {
				release(dataBuffer);
				closeChannel(this.channel);
				this.sink.complete();
			}
		}

		@Override
		public void failed(Throwable exc, DataBuffer dataBuffer) {
			release(dataBuffer);
			closeChannel(this.channel);
			this.sink.error(exc);
		}
	}

	private static class AsynchronousFileChannelWriteCompletionHandler
			extends BaseSubscriber<DataBuffer>
			implements CompletionHandler<Integer, ByteBuffer> {

		private final FluxSink<DataBuffer> sink;

		private final AsynchronousFileChannel channel;

		private final AtomicBoolean completed = new AtomicBoolean();

		private final AtomicLong position;

		@Nullable
		private DataBuffer dataBuffer;

		public AsynchronousFileChannelWriteCompletionHandler(
				FluxSink<DataBuffer> sink, AsynchronousFileChannel channel, long position) {
			this.sink = sink;
			this.channel = channel;
			this.position = new AtomicLong(position);
		}

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			request(1);
		}

		@Override
		protected void hookOnNext(DataBuffer value) {
			this.dataBuffer = value;
			ByteBuffer byteBuffer = value.asByteBuffer();

			this.channel.write(byteBuffer, this.position.get(), byteBuffer, this);
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			this.sink.error(throwable);
		}

		@Override
		protected void hookOnComplete() {
			this.completed.set(true);

			if (this.dataBuffer == null) {
				this.sink.complete();
			}
		}

		@Override
		public void completed(Integer written, ByteBuffer byteBuffer) {
			long pos = this.position.addAndGet(written);
			if (byteBuffer.hasRemaining()) {
				this.channel.write(byteBuffer, pos, byteBuffer, this);
				return;
			}

			if (this.dataBuffer != null) {
				this.sink.next(this.dataBuffer);
				this.dataBuffer = null;
			}
			if (this.completed.get()) {
				this.sink.complete();
			}
			else {
				request(1);
			}
		}

		@Override
		public void failed(Throwable exc, ByteBuffer byteBuffer) {
			this.sink.error(exc);
		}
	}
}

/*
 * Copyright 2002-present the original author or authors.
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
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Utility class for working with {@link DataBuffer DataBuffers}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class DataBufferUtils {

	private static final Log logger = LogFactory.getLog(DataBufferUtils.class);

	private static final Consumer<DataBuffer> RELEASE_CONSUMER = DataBufferUtils::release;


	//---------------------------------------------------------------------
	// Reading
	//---------------------------------------------------------------------

	/**
	 * Obtain an {@link InputStream} from the given supplier, and read it into a
	 * {@code Flux} of {@code DataBuffer}s. Closes the input stream when the
	 * Flux is terminated.
	 * @param inputStreamSupplier the supplier for the input stream to read from
	 * @param bufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a Flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> readInputStream(
			Callable<InputStream> inputStreamSupplier, DataBufferFactory bufferFactory, int bufferSize) {

		Assert.notNull(inputStreamSupplier, "'inputStreamSupplier' must not be null");
		return readByteChannel(() -> Channels.newChannel(inputStreamSupplier.call()), bufferFactory, bufferSize);
	}

	/**
	 * Obtain a {@link ReadableByteChannel} from the given supplier, and read
	 * it into a {@code Flux} of {@code DataBuffer}s. Closes the channel when
	 * the Flux is terminated.
	 * @param channelSupplier the supplier for the channel to read from
	 * @param bufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a Flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> readByteChannel(
			Callable<ReadableByteChannel> channelSupplier, DataBufferFactory bufferFactory, int bufferSize) {

		Assert.notNull(channelSupplier, "'channelSupplier' must not be null");
		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be > 0");

		return Flux.using(channelSupplier,
				channel -> Flux.generate(new ReadableByteChannelGenerator(channel, bufferFactory, bufferSize)),
				DataBufferUtils::closeChannel);

		// No doOnDiscard as operators used do not cache
	}

	/**
	 * Obtain a {@code AsynchronousFileChannel} from the given supplier, and read
	 * it into a {@code Flux} of {@code DataBuffer}s. Closes the channel when
	 * the Flux is terminated.
	 * @param channelSupplier the supplier for the channel to read from
	 * @param bufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a Flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> readAsynchronousFileChannel(
			Callable<AsynchronousFileChannel> channelSupplier, DataBufferFactory bufferFactory, int bufferSize) {

		return readAsynchronousFileChannel(channelSupplier, 0, bufferFactory, bufferSize);
	}

	/**
	 * Obtain an {@code AsynchronousFileChannel} from the given supplier, and
	 * read it into a {@code Flux} of {@code DataBuffer}s, starting at the given
	 * position. Closes the channel when the Flux is terminated.
	 * @param channelSupplier the supplier for the channel to read from
	 * @param position the position to start reading from
	 * @param bufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a Flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> readAsynchronousFileChannel(
			Callable<AsynchronousFileChannel> channelSupplier, long position,
			DataBufferFactory bufferFactory, int bufferSize) {

		Assert.notNull(channelSupplier, "'channelSupplier' must not be null");
		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.isTrue(position >= 0, "'position' must be >= 0");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be > 0");

		Flux<DataBuffer> flux = Flux.using(channelSupplier,
				channel -> Flux.create(sink -> {
					ReadCompletionHandler handler =
							new ReadCompletionHandler(channel, sink, position, bufferFactory, bufferSize);
					sink.onCancel(handler::cancel);
					sink.onRequest(handler::request);
				}),
				channel -> {
					// Do not close channel from here, rather wait for the current read callback
					// and then complete after releasing the DataBuffer.
				});

		return flux.doOnDiscard(DataBuffer.class, DataBufferUtils::release);
	}

	/**
	 * Read bytes from the given file {@code Path} into a {@code Flux} of {@code DataBuffer}s.
	 * The method ensures that the file is closed when the flux is terminated.
	 * @param path the path to read bytes from
	 * @param bufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a Flux of data buffers read from the given channel
	 * @since 5.2
	 */
	public static Flux<DataBuffer> read(
			Path path, DataBufferFactory bufferFactory, int bufferSize, OpenOption... options) {

		Assert.notNull(path, "Path must not be null");
		Assert.notNull(bufferFactory, "DataBufferFactory must not be null");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be > 0");
		if (options.length > 0) {
			for (OpenOption option : options) {
				Assert.isTrue(!(option == StandardOpenOption.APPEND || option == StandardOpenOption.WRITE),
						() -> "'" + option + "' not allowed");
			}
		}

		return readAsynchronousFileChannel(() -> AsynchronousFileChannel.open(path, options),
				bufferFactory, bufferSize);
	}

	/**
	 * Read the given {@code Resource} into a {@code Flux} of {@code DataBuffer}s.
	 * <p>If the resource is a file, it is read into an
	 * {@code AsynchronousFileChannel} and turned to {@code Flux} via
	 * {@link #readAsynchronousFileChannel(Callable, DataBufferFactory, int)} or else
	 * fall back to {@link #readByteChannel(Callable, DataBufferFactory, int)}.
	 * Closes the channel when the flux is terminated.
	 * @param resource the resource to read from
	 * @param bufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a Flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(Resource resource, DataBufferFactory bufferFactory, int bufferSize) {
		return read(resource, 0, bufferFactory, bufferSize);
	}

	/**
	 * Read the given {@code Resource} into a {@code Flux} of {@code DataBuffer}s
	 * starting at the given position.
	 * <p>If the resource is a file, it is read into an
	 * {@code AsynchronousFileChannel} and turned to {@code Flux} via
	 * {@link #readAsynchronousFileChannel(Callable, DataBufferFactory, int)} or else
	 * fall back on {@link #readByteChannel(Callable, DataBufferFactory, int)}.
	 * Closes the channel when the flux is terminated.
	 * @param resource the resource to read from
	 * @param position the position to start reading from
	 * @param bufferFactory the factory to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a Flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(
			Resource resource, long position, DataBufferFactory bufferFactory, int bufferSize) {

		try {
			if (resource.isFile()) {
				File file = resource.getFile();
				return readAsynchronousFileChannel(
						() -> AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ),
						position, bufferFactory, bufferSize);
			}
		}
		catch (IOException ignore) {
			// fallback to resource.readableChannel(), below
		}
		Flux<DataBuffer> result = readByteChannel(resource::readableChannel, bufferFactory, bufferSize);
		return position == 0 ? result : skipUntilByteCount(result, position);
	}


	//---------------------------------------------------------------------
	// Writing
	//---------------------------------------------------------------------

	/**
	 * Write the given stream of {@link DataBuffer DataBuffers} to the given
	 * {@code OutputStream}. Does <strong>not</strong> close the output stream
	 * when the flux is terminated, and does <strong>not</strong>
	 * {@linkplain #release(DataBuffer) release} the data buffers in the source.
	 * If releasing is required, then subscribe to the returned {@code Flux}
	 * with a {@link #releaseConsumer()}.
	 * <p>Note that the writing process does not start until the returned
	 * {@code Flux} is subscribed to.
	 * @param source the stream of data buffers to be written
	 * @param outputStream the output stream to write to
	 * @return a Flux containing the same buffers as in {@code source}, that
	 * starts the writing process when subscribed to, and that publishes any
	 * writing errors and the completion signal
	 */
	public static Flux<DataBuffer> write(Publisher<DataBuffer> source, OutputStream outputStream) {
		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(outputStream, "'outputStream' must not be null");

		WritableByteChannel channel = Channels.newChannel(outputStream);
		return write(source, channel);
	}

	/**
	 * Write the given stream of {@link DataBuffer DataBuffers} to the given
	 * {@code WritableByteChannel}. Does <strong>not</strong> close the channel
	 * when the flux is terminated, and does <strong>not</strong>
	 * {@linkplain #release(DataBuffer) release} the data buffers in the source.
	 * If releasing is required, then subscribe to the returned {@code Flux}
	 * with a {@link #releaseConsumer()}.
	 * <p>Note that the writing process does not start until the returned
	 * {@code Flux} is subscribed to.
	 * @param source the stream of data buffers to be written
	 * @param channel the channel to write to
	 * @return a Flux containing the same buffers as in {@code source}, that
	 * starts the writing process when subscribed to, and that publishes any
	 * writing errors and the completion signal
	 */
	public static Flux<DataBuffer> write(Publisher<DataBuffer> source, WritableByteChannel channel) {
		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(channel, "'channel' must not be null");

		Flux<DataBuffer> flux = Flux.from(source);
		return Flux.create(sink -> {
			WritableByteChannelSubscriber subscriber = new WritableByteChannelSubscriber(sink, channel);
			sink.onDispose(subscriber);
			flux.subscribe(subscriber);
		});
	}

	/**
	 * Write the given stream of {@link DataBuffer DataBuffers} to the given
	 * {@code AsynchronousFileChannel}. Does <strong>not</strong> close the
	 * channel when the flux is terminated, and does <strong>not</strong>
	 * {@linkplain #release(DataBuffer) release} the data buffers in the source.
	 * If releasing is required, then subscribe to the returned {@code Flux}
	 * with a {@link #releaseConsumer()}.
	 * <p>Note that the writing process does not start until the returned
	 * {@code Flux} is subscribed to.
	 * @param source the stream of data buffers to be written
	 * @param channel the channel to write to
	 * @return a Flux containing the same buffers as in {@code source}, that
	 * starts the writing process when subscribed to, and that publishes any
	 * writing errors and the completion signal
	 * @since 5.0.10
	 */
	public static Flux<DataBuffer> write(Publisher<DataBuffer> source, AsynchronousFileChannel channel) {
		return write(source, channel, 0);
	}

	/**
	 * Write the given stream of {@link DataBuffer DataBuffers} to the given
	 * {@code AsynchronousFileChannel}. Does <strong>not</strong> close the channel
	 * when the flux is terminated, and does <strong>not</strong>
	 * {@linkplain #release(DataBuffer) release} the data buffers in the source.
	 * If releasing is required, then subscribe to the returned {@code Flux} with a
	 * {@link #releaseConsumer()}.
	 * <p>Note that the writing process does not start until the returned
	 * {@code Flux} is subscribed to.
	 * @param source the stream of data buffers to be written
	 * @param channel the channel to write to
	 * @param position the file position where writing is to begin; must be non-negative
	 * @return a flux containing the same buffers as in {@code source}, that
	 * starts the writing process when subscribed to, and that publishes any
	 * writing errors and the completion signal
	 */
	public static Flux<DataBuffer> write(
			Publisher<? extends DataBuffer> source, AsynchronousFileChannel channel, long position) {

		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(channel, "'channel' must not be null");
		Assert.isTrue(position >= 0, "'position' must be >= 0");

		Flux<DataBuffer> flux = Flux.from(source);
		return Flux.create(sink -> {
			WriteCompletionHandler handler = new WriteCompletionHandler(sink, channel, position);
			sink.onDispose(handler);
			flux.subscribe(handler);
		});


	}

	/**
	 * Write the given stream of {@link DataBuffer DataBuffers} to the given
	 * file {@link Path}. The optional {@code options} parameter specifies
	 * how the file is created or opened (defaults to
	 * {@link StandardOpenOption#CREATE CREATE},
	 * {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING}, and
	 * {@link StandardOpenOption#WRITE WRITE}).
	 * @param source the stream of data buffers to be written
	 * @param destination the path to the file
	 * @param options the options specifying how the file is opened
	 * @return a {@link Mono} that indicates completion or error
	 * @since 5.2
	 */
	public static Mono<Void> write(Publisher<DataBuffer> source, Path destination, OpenOption... options) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(destination, "Destination must not be null");

		Set<OpenOption> optionSet = checkWriteOptions(options);

		return Mono.create(sink -> {
			try {
				AsynchronousFileChannel channel = AsynchronousFileChannel.open(destination, optionSet, null);
				sink.onDispose(() -> closeChannel(channel));
				write(source, channel).subscribe(DataBufferUtils::release,
						sink::error,
						sink::success,
						Context.of(sink.contextView()));
			}
			catch (IOException ex) {
				sink.error(ex);
			}
		});
	}

	private static Set<OpenOption> checkWriteOptions(OpenOption[] options) {
		int length = options.length;
		Set<OpenOption> result = CollectionUtils.newHashSet(length > 0 ? length : 2);
		if (length == 0) {
			result.add(StandardOpenOption.CREATE);
			result.add(StandardOpenOption.TRUNCATE_EXISTING);
		}
		else {
			for (OpenOption opt : options) {
				if (opt == StandardOpenOption.READ) {
					throw new IllegalArgumentException("READ not allowed");
				}
				result.add(opt);
			}
		}
		result.add(StandardOpenOption.WRITE);
		return result;
	}

	static void closeChannel(@Nullable Channel channel) {
		if (channel != null && channel.isOpen()) {
			try {
				channel.close();
			}
			catch (IOException ignored) {
			}
		}
	}


	/**
	 * Create a new {@code Publisher<DataBuffer>} based on bytes written to a
	 * {@code OutputStream}.
	 * <ul>
	 * <li>The parameter {@code outputStreamConsumer} is invoked once per
	 * subscription of the returned {@code Publisher}, when the first
	 * item is
	 * {@linkplain Subscription#request(long) requested}.</li>
	 * <li>{@link OutputStream#write(byte[], int, int) OutputStream.write()}
	 * invocations made by {@code outputStreamConsumer} are buffered until they
	 * exceed the default chunk size of 1024, or when the stream is
	 * {@linkplain OutputStream#flush() flushed} and then result in a
	 * {@linkplain Subscriber#onNext(Object) published} item
	 * if there is {@linkplain Subscription#request(long) demand}.</li>
	 * <li>If there is <em>no demand</em>, {@code OutputStream.write()} will block
	 * until there is.</li>
	 * <li>If the subscription is {@linkplain Subscription#cancel() cancelled},
	 * {@code OutputStream.write()} will throw a {@code IOException}.</li>
	 * <li>The subscription is
	 * {@linkplain Subscriber#onComplete() completed} when
	 * {@code outputStreamHandler} completes.</li>
	 * <li>Any exceptions thrown from {@code outputStreamHandler} will
	 * be dispatched to the {@linkplain Subscriber#onError(Throwable) Subscriber}.
	 * </ul>
	 * @param consumer invoked when the first buffer is requested
	 * @param executor used to invoke the {@code outputStreamHandler}
	 * @return a {@code Publisher<DataBuffer>} based on bytes written by
	 * {@code outputStreamHandler}
	 * @since 6.1
	 */
	public static Publisher<DataBuffer> outputStreamPublisher(
			Consumer<OutputStream> consumer, DataBufferFactory bufferFactory, Executor executor) {

		return new OutputStreamPublisher<>(
				consumer::accept, new DataBufferMapper(bufferFactory), executor, null);
	}

	/**
	 * Variant of {@link #outputStreamPublisher(Consumer, DataBufferFactory, Executor)}
	 * providing control over the chunk sizes to be produced by the publisher.
	 * @since 6.1
	 */
	public static Publisher<DataBuffer> outputStreamPublisher(
			Consumer<OutputStream> consumer, DataBufferFactory bufferFactory, Executor executor, int chunkSize) {

		return new OutputStreamPublisher<>(
				consumer::accept, new DataBufferMapper(bufferFactory), executor, chunkSize);
	}

	/**
	 * Subscribe to given {@link Publisher} of {@code DataBuffer}s, and return an
	 * {@link InputStream} to consume the byte content with.
	 * <p>Byte buffers are stored in a queue. The {@code demand} constructor value
	 * determines the number of buffers requested initially. When storage falls
	 * below a {@code (demand - (demand >> 2))} limit, a request is made to refill
	 * the queue.
	 * <p>The {@code InputStream} terminates after an onError or onComplete signal,
	 * and stored buffers are read. If the {@code InputStream} is closed,
	 * the {@link Flow.Subscription} is cancelled, and stored buffers released.
	 * @param publisher the source of {@code DataBuffer}s
	 * @param demand the number of buffers to request initially, and buffer
	 * internally on an ongoing basis.
	 * @return an {@link InputStream} backed by the {@link Publisher}
	 */
	public static <T extends DataBuffer> InputStream subscriberInputStream(Publisher<T> publisher, int demand) {
		Assert.notNull(publisher, "Publisher must not be null");
		Assert.isTrue(demand > 0, "maxBufferCount must be > 0");

		SubscriberInputStream subscriber = new SubscriberInputStream(demand);
		publisher.subscribe(subscriber);
		return subscriber;
	}


	//---------------------------------------------------------------------
	// Various
	//---------------------------------------------------------------------

	/**
	 * Relay buffers from the given {@link Publisher} until the total
	 * {@linkplain DataBuffer#readableByteCount() byte count} reaches
	 * the given maximum byte count, or until the publisher is complete.
	 * @param publisher the publisher to filter
	 * @param maxByteCount the maximum byte count
	 * @return a flux whose maximum byte count is {@code maxByteCount}
	 */
	@SuppressWarnings("unchecked")
	public static <T extends DataBuffer> Flux<T> takeUntilByteCount(Publisher<T> publisher, long maxByteCount) {
		Assert.notNull(publisher, "Publisher must not be null");
		Assert.isTrue(maxByteCount >= 0, "'maxByteCount' must be >= 0");

		return Flux.defer(() -> {
			AtomicLong countDown = new AtomicLong(maxByteCount);
			return Flux.from(publisher)
					.map(buffer -> {
						long remainder = countDown.addAndGet(-buffer.readableByteCount());
						if (remainder < 0) {
							int index = buffer.readableByteCount() + (int) remainder;
							DataBuffer split = buffer.split(index);
							release(buffer);
							return (T)split;
						}
						else {
							return buffer;
						}
					})
					.takeUntil(buffer -> countDown.get() <= 0);
		});

		// No doOnDiscard as operators used do not cache (and drop) buffers
	}

	/**
	 * Skip buffers from the given {@link Publisher} until the total
	 * {@linkplain DataBuffer#readableByteCount() byte count} reaches
	 * the given maximum byte count, or until the publisher is complete.
	 * @param publisher the publisher to filter
	 * @param maxByteCount the maximum byte count
	 * @return a flux with the remaining part of the given publisher
	 */
	public static <T extends DataBuffer> Flux<T> skipUntilByteCount(Publisher<T> publisher, long maxByteCount) {
		Assert.notNull(publisher, "Publisher must not be null");
		Assert.isTrue(maxByteCount >= 0, "'maxByteCount' must be >= 0");

		return Flux.defer(() -> {
			AtomicLong countDown = new AtomicLong(maxByteCount);
			return Flux.from(publisher)
					.skipUntil(buffer -> {
						long remainder = countDown.addAndGet(-buffer.readableByteCount());
						return remainder < 0;
					})
					.map(buffer -> {
						long remainder = countDown.get();
						if (remainder < 0) {
							countDown.set(0);
							int start = buffer.readableByteCount() + (int)remainder;
							DataBuffer split = buffer.split(start);
							release(split);
							return buffer;
						}
						else {
							return buffer;
						}
					});
		}).doOnDiscard(DataBuffer.class, DataBufferUtils::release);
	}

	/**
	 * Retain the given data buffer, if it is a {@link PooledDataBuffer}.
	 * @param dataBuffer the data buffer to retain
	 * @return the retained buffer
	 */
	@SuppressWarnings("unchecked")
	public static <T extends DataBuffer> T retain(T dataBuffer) {
		if (dataBuffer instanceof PooledDataBuffer pooledDataBuffer) {
			return (T) pooledDataBuffer.retain();
		}
		else {
			return dataBuffer;
		}
	}

	/**
	 * Associate the given hint with the data buffer if it is a pooled buffer
	 * and supports leak tracking.
	 * @param dataBuffer the data buffer to attach the hint to
	 * @param hint the hint to attach
	 * @return the input buffer
	 * @since 5.3.2
	 */
	@SuppressWarnings("unchecked")
	public static <T extends DataBuffer> T touch(T dataBuffer, Object hint) {
		if (dataBuffer instanceof TouchableDataBuffer touchableDataBuffer) {
			return (T) touchableDataBuffer.touch(hint);
		}
		else {
			return dataBuffer;
		}
	}

	/**
	 * Release the given data buffer. If it is a {@link PooledDataBuffer} and
	 * has been {@linkplain PooledDataBuffer#isAllocated() allocated}, this
	 * method will call {@link PooledDataBuffer#release()}. If it is a
	 * {@link CloseableDataBuffer}, this method will call
	 * {@link CloseableDataBuffer#close()}.
	 * @param dataBuffer the data buffer to release
	 * @return {@code true} if the buffer was released; {@code false} otherwise.
	 */
	public static boolean release(@Nullable DataBuffer dataBuffer) {
		if (dataBuffer instanceof PooledDataBuffer pooledDataBuffer) {
			if (pooledDataBuffer.isAllocated()) {
				try {
					return pooledDataBuffer.release();
				}
				catch (IllegalStateException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to release PooledDataBuffer: " + dataBuffer, ex);
					}
					return false;
				}
			}
		}
		else if (dataBuffer instanceof CloseableDataBuffer closeableDataBuffer) {
			try {
				closeableDataBuffer.close();
				return true;
			}
			catch (IllegalStateException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to release CloseableDataBuffer " + dataBuffer, ex);
				}
				return false;

			}
		}
		return false;
	}

	/**
	 * Return a consumer that calls {@link #release(DataBuffer)} on all
	 * passed data buffers.
	 */
	public static Consumer<DataBuffer> releaseConsumer() {
		return RELEASE_CONSUMER;
	}

	/**
	 * Return a new {@code DataBuffer} composed of joining together the given
	 * {@code dataBuffers} elements. Depending on the {@link DataBuffer} type,
	 * the returned buffer may be a single buffer containing all data of the
	 * provided buffers, or it may be a zero-copy, composite with references to
	 * the given buffers.
	 * <p>If {@code dataBuffers} produces an error or if there is a cancel
	 * signal, then all accumulated buffers will be
	 * {@linkplain #release(DataBuffer) released}.
	 * <p>Note that the given data buffers do <strong>not</strong> have to be
	 * released. They will be released as part of the returned composite.
	 * @param dataBuffers the data buffers that are to be composed
	 * @return a buffer that is composed of the {@code dataBuffers} argument
	 * @since 5.0.3
	 */
	public static Mono<DataBuffer> join(Publisher<? extends DataBuffer> dataBuffers) {
		return join(dataBuffers, -1);
	}

	/**
	 * Variant of {@link #join(Publisher)} that behaves the same way up until
	 * the specified max number of bytes to buffer. Once the limit is exceeded,
	 * {@link DataBufferLimitException} is raised.
	 * @param buffers the data buffers that are to be composed
	 * @param maxByteCount the max number of bytes to buffer, or -1 for unlimited
	 * @return a buffer with the aggregated content, possibly an empty Mono if
	 * the max number of bytes to buffer is exceeded.
	 * @throws DataBufferLimitException if maxByteCount is exceeded
	 * @since 5.1.11
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Mono<DataBuffer> join(Publisher<? extends DataBuffer> buffers, int maxByteCount) {
		Assert.notNull(buffers, "'buffers' must not be null");

		if (buffers instanceof Mono mono) {
			return mono;
		}

		return Flux.from(buffers)
				.collect(() -> new LimitedDataBufferList(maxByteCount), LimitedDataBufferList::add)
				.filter(list -> !list.isEmpty())
				.map(list -> list.get(0).factory().join(list))
				.doOnDiscard(DataBuffer.class, DataBufferUtils::release);
	}

	/**
	 * Return a {@link Matcher} for the given delimiter.
	 * The matcher can be used to find the delimiters in a stream of data buffers.
	 * @param delimiter the delimiter bytes to find
	 * @return the matcher
	 * @since 5.2
	 */
	public static Matcher matcher(byte[] delimiter) {
		return createMatcher(delimiter);
	}

	/**
	 * Return a {@link Matcher} for the given delimiters.
	 * The matcher can be used to find the delimiters in a stream of data buffers.
	 * @param delimiters the delimiters bytes to find
	 * @return the matcher
	 * @since 5.2
	 */
	public static Matcher matcher(byte[]... delimiters) {
		Assert.isTrue(delimiters.length > 0, "Delimiters must not be empty");
		return (delimiters.length == 1 ? createMatcher(delimiters[0]) : new CompositeMatcher(delimiters));
	}

	private static NestedMatcher createMatcher(byte[] delimiter) {
		// extract length due to Eclipse IDE compiler error in switch expression
		int length = delimiter.length;
		Assert.isTrue(length > 0, "Delimiter must not be empty");
		return switch (length) {
			case 1 -> (delimiter[0] == 10 ? SingleByteMatcher.NEWLINE_MATCHER : new SingleByteMatcher(delimiter));
			case 2 -> new TwoByteMatcher(delimiter);
			default -> new KnuthMorrisPrattMatcher(delimiter);
		};
	}


	/**
	 * Contract to find delimiter(s) against one or more data buffers that can
	 * be passed one at a time to the {@link #match(DataBuffer)} method.
	 *
	 * @since 5.2
	 * @see #match(DataBuffer)
	 */
	public interface Matcher {

		/**
		 * Find the first matching delimiter and return the index of the last
		 * byte of the delimiter, or {@code -1} if not found.
		 */
		int match(DataBuffer dataBuffer);

		/**
		 * Return the delimiter from the last invocation of {@link #match(DataBuffer)}.
		 */
		byte[] delimiter();

		/**
		 * Reset the state of this matcher.
		 */
		void reset();
	}


	/**
	 * Matcher that supports searching for multiple delimiters.
	 */
	private static class CompositeMatcher implements Matcher {

		private static final byte[] NO_DELIMITER = new byte[0];


		private final NestedMatcher[] matchers;

		byte[] longestDelimiter = NO_DELIMITER;

		CompositeMatcher(byte[][] delimiters) {
			this.matchers = initMatchers(delimiters);
		}

		private static NestedMatcher[] initMatchers(byte[][] delimiters) {
			NestedMatcher[] matchers = new NestedMatcher[delimiters.length];
			for (int i = 0; i < delimiters.length; i++) {
				matchers[i] = createMatcher(delimiters[i]);
			}
			return matchers;
		}

		@Override
		public int match(DataBuffer dataBuffer) {
			this.longestDelimiter = NO_DELIMITER;

			for (int pos = dataBuffer.readPosition(); pos < dataBuffer.writePosition(); pos++) {
				byte b = dataBuffer.getByte(pos);

				for (NestedMatcher matcher : this.matchers) {
					if (matcher.match(b) && matcher.delimiter().length > this.longestDelimiter.length) {
						this.longestDelimiter = matcher.delimiter();
					}
				}

				if (this.longestDelimiter != NO_DELIMITER) {
					reset();
					return pos;
				}
			}
			return -1;
		}

		@Override
		public byte[] delimiter() {
			Assert.state(this.longestDelimiter != NO_DELIMITER, "'delimiter' not set");
			return this.longestDelimiter;
		}

		@Override
		public void reset() {
			for (NestedMatcher matcher : this.matchers) {
				matcher.reset();
			}
		}
	}


	/**
	 * Matcher that can be nested within {@link CompositeMatcher} where multiple
	 * matchers advance together using the same index, one byte at a time.
	 */
	private interface NestedMatcher extends Matcher {

		/**
		 * Perform a match against the next byte of the stream and return true
		 * if the delimiter is fully matched.
		 */
		boolean match(byte b);

	}


	/**
	 * Matcher for a single byte delimiter.
	 */
	private static class SingleByteMatcher implements NestedMatcher {

		static final SingleByteMatcher NEWLINE_MATCHER = new SingleByteMatcher(new byte[] {10});

		private final byte[] delimiter;

		SingleByteMatcher(byte[] delimiter) {
			Assert.isTrue(delimiter.length == 1, "Expected a 1 byte delimiter");
			this.delimiter = delimiter;
		}

		@Override
		public int match(DataBuffer dataBuffer) {
			for (int pos = dataBuffer.readPosition(); pos < dataBuffer.writePosition(); pos++) {
				byte b = dataBuffer.getByte(pos);
				if (match(b)) {
					return pos;
				}
			}
			return -1;
		}

		@Override
		public boolean match(byte b) {
			return this.delimiter[0] == b;
		}

		@Override
		public byte[] delimiter() {
			return this.delimiter;
		}

		@Override
		public void reset() {
		}
	}


	/**
	 * Base class for a {@link NestedMatcher}.
	 */
	private abstract static class AbstractNestedMatcher implements NestedMatcher {

		private final byte[] delimiter;

		private int matches = 0;


		protected AbstractNestedMatcher(byte[] delimiter) {
			this.delimiter = delimiter;
		}

		protected void setMatches(int index) {
			this.matches = index;
		}

		protected int getMatches() {
			return this.matches;
		}

		@Override
		public int match(DataBuffer dataBuffer) {
			for (int pos = dataBuffer.readPosition(); pos < dataBuffer.writePosition(); pos++) {
				byte b = dataBuffer.getByte(pos);
				if (match(b)) {
					reset();
					return pos;
				}
			}
			return -1;
		}

		@Override
		public boolean match(byte b) {
			if (b == this.delimiter[this.matches]) {
				this.matches++;
				return (this.matches == delimiter().length);
			}
			return false;
		}

		@Override
		public byte[] delimiter() {
			return this.delimiter;
		}

		@Override
		public void reset() {
			this.matches = 0;
		}
	}


	/**
	 * Matcher with a 2 byte delimiter that does not benefit from a
	 * Knuth-Morris-Pratt suffix-prefix table.
	 */
	private static class TwoByteMatcher extends AbstractNestedMatcher {

		protected TwoByteMatcher(byte[] delimiter) {
			super(delimiter);
			Assert.isTrue(delimiter.length == 2, "Expected a 2-byte delimiter");
		}
	}


	/**
	 * Implementation of {@link Matcher} that uses the Knuth-Morris-Pratt algorithm.
	 * @see <a href="https://www.nayuki.io/page/knuth-morris-pratt-string-matching">Knuth-Morris-Pratt string matching</a>
	 */
	private static class KnuthMorrisPrattMatcher extends AbstractNestedMatcher {

		private final int[] table;

		public KnuthMorrisPrattMatcher(byte[] delimiter) {
			super(delimiter);
			this.table = longestSuffixPrefixTable(delimiter);
		}

		private static int[] longestSuffixPrefixTable(byte[] delimiter) {
			int[] result = new int[delimiter.length];
			result[0] = 0;
			for (int i = 1; i < delimiter.length; i++) {
				int j = result[i - 1];
				while (j > 0 && delimiter[i] != delimiter[j]) {
					j = result[j - 1];
				}
				if (delimiter[i] == delimiter[j]) {
					j++;
				}
				result[i] = j;
			}
			return result;
		}

		@Override
		public boolean match(byte b) {
			while (getMatches() > 0 && b != delimiter()[getMatches()]) {
				setMatches(this.table[getMatches() - 1]);
			}
			return super.match(b);
		}
	}


	private static class ReadableByteChannelGenerator implements Consumer<SynchronousSink<DataBuffer>> {

		private final ReadableByteChannel channel;

		private final DataBufferFactory dataBufferFactory;

		private final int bufferSize;

		public ReadableByteChannelGenerator(
				ReadableByteChannel channel, DataBufferFactory dataBufferFactory, int bufferSize) {

			this.channel = channel;
			this.dataBufferFactory = dataBufferFactory;
			this.bufferSize = bufferSize;
		}

		@Override
		public void accept(SynchronousSink<DataBuffer> sink) {
			int read = -1;
			DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer(this.bufferSize);
			try {
				try (DataBuffer.ByteBufferIterator iterator = dataBuffer.writableByteBuffers()) {
					Assert.state(iterator.hasNext(), "No ByteBuffer available");
					ByteBuffer byteBuffer = iterator.next();
					read = this.channel.read(byteBuffer);
				}
				if (read >= 0) {
					dataBuffer.writePosition(read);
					sink.next(dataBuffer);
				}
				else {
					sink.complete();
				}
			}
			catch (IOException ex) {
				sink.error(ex);
			}
			finally {
				if (read == -1) {
					release(dataBuffer);
				}
			}
		}
	}


	private static class ReadCompletionHandler implements CompletionHandler<Integer, ReadCompletionHandler.Attachment> {

		private final AsynchronousFileChannel channel;

		private final FluxSink<DataBuffer> sink;

		private final DataBufferFactory dataBufferFactory;

		private final int bufferSize;

		private final AtomicLong position;

		private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);

		public ReadCompletionHandler(AsynchronousFileChannel channel,
				FluxSink<DataBuffer> sink, long position, DataBufferFactory dataBufferFactory, int bufferSize) {

			this.channel = channel;
			this.sink = sink;
			this.position = new AtomicLong(position);
			this.dataBufferFactory = dataBufferFactory;
			this.bufferSize = bufferSize;
		}

		/**
		 * Invoked when Reactive Streams consumer signals demand.
		 */
		public void request(long n) {
			tryRead();
		}

		/**
		 * Invoked when Reactive Streams consumer cancels.
		 */
		public void cancel() {
			this.state.getAndSet(State.DISPOSED);

			// According java.nio.channels.AsynchronousChannel "if an I/O operation is outstanding
			// on the channel and the channel's close method is invoked, then the I/O operation
			// fails with the exception AsynchronousCloseException". That should invoke the failed
			// callback below and the current DataBuffer should be released.

			closeChannel(this.channel);
		}

		private void tryRead() {
			if (this.sink.requestedFromDownstream() > 0 && this.state.compareAndSet(State.IDLE, State.READING)) {
				read();
			}
		}

		private void read() {
			DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer(this.bufferSize);
			DataBuffer.ByteBufferIterator iterator = dataBuffer.writableByteBuffers();
			Assert.state(iterator.hasNext(), "No ByteBuffer available");
			ByteBuffer byteBuffer = iterator.next();
			Attachment attachment = new Attachment(dataBuffer, iterator);
			this.channel.read(byteBuffer, this.position.get(), attachment, this);
		}

		@Override
		public void completed(Integer read, Attachment attachment) {
			attachment.iterator().close();
			DataBuffer dataBuffer = attachment.dataBuffer();

			if (this.state.get() == State.DISPOSED) {
				release(dataBuffer);
				closeChannel(this.channel);
				return;
			}

			if (read == -1) {
				release(dataBuffer);
				closeChannel(this.channel);
				this.state.set(State.DISPOSED);
				this.sink.complete();
				return;
			}

			this.position.addAndGet(read);
			dataBuffer.writePosition(read);
			this.sink.next(dataBuffer);

			// Stay in READING mode if there is demand
			if (this.sink.requestedFromDownstream() > 0) {
				read();
				return;
			}

			// Release READING mode and then try again in case of concurrent "request"
			if (this.state.compareAndSet(State.READING, State.IDLE)) {
				tryRead();
			}
		}

		@Override
		public void failed(Throwable ex, Attachment attachment) {
			attachment.iterator().close();
			release(attachment.dataBuffer());

			closeChannel(this.channel);
			this.state.set(State.DISPOSED);
			this.sink.error(ex);
		}

		private enum State {
			IDLE, READING, DISPOSED
		}

		private record Attachment(DataBuffer dataBuffer, DataBuffer.ByteBufferIterator iterator) {}
	}


	private static class WritableByteChannelSubscriber extends BaseSubscriber<DataBuffer> {

		private final FluxSink<DataBuffer> sink;

		private final WritableByteChannel channel;

		public WritableByteChannelSubscriber(FluxSink<DataBuffer> sink, WritableByteChannel channel) {
			this.sink = sink;
			this.channel = channel;
		}

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			request(1);
		}

		@Override
		protected void hookOnNext(DataBuffer dataBuffer) {
			try {
				try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
					ByteBuffer byteBuffer = iterator.next();
					while (byteBuffer.hasRemaining()) {
						this.channel.write(byteBuffer);
					}
				}
				this.sink.next(dataBuffer);
				request(1);
			}
			catch (IOException ex) {
				this.sink.next(dataBuffer);
				this.sink.error(ex);
			}
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			this.sink.error(throwable);
		}

		@Override
		protected void hookOnComplete() {
			this.sink.complete();
		}

		@Override
		public Context currentContext() {
			return Context.of(this.sink.contextView());
		}
	}


	private static class WriteCompletionHandler extends BaseSubscriber<DataBuffer>
			implements CompletionHandler<Integer, WriteCompletionHandler.Attachment> {

		private final FluxSink<DataBuffer> sink;

		private final AsynchronousFileChannel channel;

		private final AtomicBoolean writing = new AtomicBoolean();

		private final AtomicBoolean completed = new AtomicBoolean();

		private final AtomicReference<Throwable> error = new AtomicReference<>();

		private final AtomicLong position;

		public WriteCompletionHandler(
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
		protected void hookOnNext(DataBuffer dataBuffer) {
			DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers();
			if (iterator.hasNext()) {
				ByteBuffer byteBuffer = iterator.next();
				long pos = this.position.get();
				Attachment attachment = new Attachment(byteBuffer, dataBuffer, iterator);
				this.writing.set(true);
				this.channel.write(byteBuffer, pos, attachment, this);
			}
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			this.error.set(throwable);

			if (!this.writing.get()) {
				this.sink.error(throwable);
			}
		}

		@Override
		protected void hookOnComplete() {
			this.completed.set(true);

			if (!this.writing.get()) {
				this.sink.complete();
			}
		}

		@Override
		public void completed(Integer written, Attachment attachment) {
			DataBuffer.ByteBufferIterator iterator = attachment.iterator();
			iterator.close();

			long pos = this.position.addAndGet(written);
			ByteBuffer byteBuffer = attachment.byteBuffer();

			if (byteBuffer.hasRemaining()) {
				this.channel.write(byteBuffer, pos, attachment, this);
			}
			else if (iterator.hasNext()) {
				ByteBuffer next = iterator.next();
				this.channel.write(next, pos, attachment, this);
			}
			else {
				this.sink.next(attachment.dataBuffer());
				this.writing.set(false);

				Throwable throwable = this.error.get();
				if (throwable != null) {
					this.sink.error(throwable);
				}
				else if (this.completed.get()) {
					this.sink.complete();
				}
				else {
					request(1);
				}
			}
		}

		@Override
		public void failed(Throwable ex, Attachment attachment) {
			attachment.iterator().close();

			this.sink.next(attachment.dataBuffer());
			this.writing.set(false);

			this.sink.error(ex);
		}

		@Override
		public Context currentContext() {
			return Context.of(this.sink.contextView());
		}

		private record Attachment(ByteBuffer byteBuffer, DataBuffer dataBuffer, DataBuffer.ByteBufferIterator iterator) {}
	}


	private static final class DataBufferMapper implements OutputStreamPublisher.ByteMapper<DataBuffer> {

		private final DataBufferFactory bufferFactory;

		private DataBufferMapper(DataBufferFactory bufferFactory) {
			this.bufferFactory = bufferFactory;
		}

		@Override
		public DataBuffer map(int b) {
			DataBuffer buffer = this.bufferFactory.allocateBuffer(1);
			buffer.write((byte) b);
			return buffer;
		}

		@Override
		public DataBuffer map(byte[] b, int off, int len) {
			DataBuffer buffer = this.bufferFactory.allocateBuffer(len);
			buffer.write(b, off, len);
			return buffer;
		}

	}

}

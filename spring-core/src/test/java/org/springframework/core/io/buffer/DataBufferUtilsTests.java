/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.testfixture.io.buffer.AbstractDataBufferAllocatingTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class DataBufferUtilsTests extends AbstractDataBufferAllocatingTests {

	private final Resource resource;
	private final Path tempFile;


	DataBufferUtilsTests() throws Exception {
		this.resource = new ClassPathResource("DataBufferUtilsTests.txt", getClass());
		this.tempFile = Files.createTempFile("DataBufferUtilsTests", null);
	}


	@ParameterizedDataBufferAllocatingTest
	void readInputStream(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> flux = DataBufferUtils.readInputStream(
				() -> this.resource.getInputStream(), super.bufferFactory, 3);

		verifyReadData(flux);
	}

	@ParameterizedDataBufferAllocatingTest
	void readByteChannel(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		URI uri = this.resource.getURI();
		Flux<DataBuffer> result =
				DataBufferUtils.readByteChannel(() -> FileChannel.open(Paths.get(uri), StandardOpenOption.READ),
					super.bufferFactory, 3);

		verifyReadData(result);
	}

	@ParameterizedDataBufferAllocatingTest
	void readByteChannelError(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		ReadableByteChannel channel = mock(ReadableByteChannel.class);
		given(channel.read(any()))
				.willAnswer(invocation -> {
					ByteBuffer buffer = invocation.getArgument(0);
					buffer.put("foo".getBytes(StandardCharsets.UTF_8));
					buffer.flip();
					return 3;
				})
				.willThrow(new IOException());

		Flux<DataBuffer> result =
				DataBufferUtils.readByteChannel(() -> channel, super.bufferFactory, 3);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.expectError(IOException.class)
				.verify(Duration.ofSeconds(3));
	}

	@ParameterizedDataBufferAllocatingTest
	void readByteChannelCancel(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		URI uri = this.resource.getURI();
		Flux<DataBuffer> result =
				DataBufferUtils.readByteChannel(() -> FileChannel.open(Paths.get(uri), StandardOpenOption.READ),
					super.bufferFactory, 3);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.thenCancel()
				.verify();
	}

	@ParameterizedDataBufferAllocatingTest
	void readAsynchronousFileChannel(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		URI uri = this.resource.getURI();
		Flux<DataBuffer> flux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ),
				super.bufferFactory, 3);

		verifyReadData(flux);
	}

	@ParameterizedDataBufferAllocatingTest
	void readAsynchronousFileChannelPosition(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		URI uri = this.resource.getURI();
		Flux<DataBuffer> flux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ),
				9, super.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedDataBufferAllocatingTest
	void readAsynchronousFileChannelError(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		AsynchronousFileChannel channel = mock(AsynchronousFileChannel.class);
		willAnswer(invocation -> {
			ByteBuffer byteBuffer = invocation.getArgument(0);
			byteBuffer.put("foo".getBytes(StandardCharsets.UTF_8));
			byteBuffer.flip();
			long pos = invocation.getArgument(1);
			assertThat(pos).isEqualTo(0);
			DataBuffer dataBuffer = invocation.getArgument(2);
			CompletionHandler<Integer, DataBuffer> completionHandler = invocation.getArgument(3);
			completionHandler.completed(3, dataBuffer);
			return null;
		}).willAnswer(invocation -> {
			DataBuffer dataBuffer = invocation.getArgument(2);
			CompletionHandler<Integer, DataBuffer> completionHandler = invocation.getArgument(3);
			completionHandler.failed(new IOException(), dataBuffer);
			return null;
		})
		.given(channel).read(any(), anyLong(), any(), any());

		Flux<DataBuffer> result =
				DataBufferUtils.readAsynchronousFileChannel(() -> channel, super.bufferFactory, 3);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.expectError(IOException.class)
				.verify(Duration.ofSeconds(3));
	}

	@ParameterizedDataBufferAllocatingTest
	void readAsynchronousFileChannelCancel(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		URI uri = this.resource.getURI();
		Flux<DataBuffer> flux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ),
				super.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("foo"))
				.thenCancel()
				.verify();
	}

	@ParameterizedDataBufferAllocatingTest // gh-22107
	void readAsynchronousFileChannelCancelWithoutDemand(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		URI uri = this.resource.getURI();
		Flux<DataBuffer> flux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ),
				super.bufferFactory, 3);

		BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
		flux.subscribe(subscriber);
		subscriber.cancel();
	}

	@ParameterizedDataBufferAllocatingTest
	void readPath(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> flux = DataBufferUtils.read(this.resource.getFile().toPath(), super.bufferFactory, 3);

		verifyReadData(flux);
	}

	@ParameterizedDataBufferAllocatingTest
	void readResource(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> flux = DataBufferUtils.read(this.resource, super.bufferFactory, 3);

		verifyReadData(flux);
	}

	@ParameterizedDataBufferAllocatingTest
	void readResourcePosition(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> flux = DataBufferUtils.read(this.resource, 9, super.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	private void verifyReadData(Flux<DataBuffer> buffers) {
		StepVerifier.create(buffers)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(3));
	}

	@ParameterizedDataBufferAllocatingTest
	void readResourcePositionAndTakeUntil(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		Resource resource = new ClassPathResource("DataBufferUtilsTests.txt", getClass());
		Flux<DataBuffer> flux = DataBufferUtils.read(resource, 3, super.bufferFactory, 3);

		flux = DataBufferUtils.takeUntilByteCount(flux, 5);


		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("ba"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedDataBufferAllocatingTest
	void readByteArrayResourcePositionAndTakeUntil(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		Resource resource = new ByteArrayResource("foobarbazqux" .getBytes());
		Flux<DataBuffer> flux = DataBufferUtils.read(resource, 3, super.bufferFactory, 3);

		flux = DataBufferUtils.takeUntilByteCount(flux, 5);


		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("ba"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedDataBufferAllocatingTest
	void writeOutputStream(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		DataBuffer qux = stringBuffer("qux");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz, qux);

		OutputStream os = Files.newOutputStream(tempFile);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, os);
		verifyWrittenData(writeResult);
		os.close();
	}

	@ParameterizedDataBufferAllocatingTest
	void writeWritableByteChannel(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		DataBuffer qux = stringBuffer("qux");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz, qux);

		WritableByteChannel channel = Files.newByteChannel(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		verifyWrittenData(writeResult);
		channel.close();
	}

	@ParameterizedDataBufferAllocatingTest
	void writeWritableByteChannelErrorInFlux(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar).concatWith(Flux.error(new RuntimeException()));

		WritableByteChannel channel = Files.newByteChannel(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.expectError()
				.verify(Duration.ofSeconds(5));

		String result = String.join("", Files.readAllLines(tempFile));

		assertThat(result).isEqualTo("foobar");
		channel.close();
	}

	@ParameterizedDataBufferAllocatingTest
	void writeWritableByteChannelErrorInWrite(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar);

		WritableByteChannel channel = mock(WritableByteChannel.class);
		given(channel.write(any()))
				.willAnswer(invocation -> {
					ByteBuffer buffer = invocation.getArgument(0);
					int written = buffer.remaining();
					buffer.position(buffer.limit());
					return written;
				})
				.willThrow(new IOException());

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.expectError(IOException.class)
				.verify(Duration.ofSeconds(3));

		channel.close();
	}

	@ParameterizedDataBufferAllocatingTest
	void writeWritableByteChannelCancel(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar);

		WritableByteChannel channel = Files.newByteChannel(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult, 1)
				.consumeNextWith(stringConsumer("foo"))
				.thenCancel()
				.verify(Duration.ofSeconds(5));

		String result = String.join("", Files.readAllLines(tempFile));

		assertThat(result).isEqualTo("foo");
		channel.close();

		flux.subscribe(DataBufferUtils::release);
	}

	@ParameterizedDataBufferAllocatingTest
	void writeAsynchronousFileChannel(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		DataBuffer qux = stringBuffer("qux");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz, qux);

		AsynchronousFileChannel channel =
				AsynchronousFileChannel.open(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		verifyWrittenData(writeResult);
		channel.close();
	}

	private void verifyWrittenData(Flux<DataBuffer> writeResult) throws IOException {
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(3));

		String result = String.join("", Files.readAllLines(tempFile));

		assertThat(result).isEqualTo("foobarbazqux");
	}

	@ParameterizedDataBufferAllocatingTest
	void writeAsynchronousFileChannelErrorInFlux(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux =
				Flux.just(foo, bar).concatWith(Mono.error(new RuntimeException()));

		AsynchronousFileChannel channel =
				AsynchronousFileChannel.open(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.expectError(RuntimeException.class)
				.verify();

		String result = String.join("", Files.readAllLines(tempFile));

		assertThat(result).isEqualTo("foobar");
		channel.close();
	}

	@ParameterizedDataBufferAllocatingTest
	@SuppressWarnings("unchecked")
	void writeAsynchronousFileChannelErrorInWrite(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar);

		AsynchronousFileChannel channel = mock(AsynchronousFileChannel.class);
		willAnswer(invocation -> {
			ByteBuffer buffer = invocation.getArgument(0);
			long pos = invocation.getArgument(1);
			CompletionHandler<Integer, ByteBuffer> completionHandler = invocation.getArgument(3);

			assertThat(pos).isEqualTo(0);

			int written = buffer.remaining();
			buffer.position(buffer.limit());
			completionHandler.completed(written, buffer);

			return null;
		})
		.willAnswer(invocation -> {
			ByteBuffer buffer = invocation.getArgument(0);
			CompletionHandler<Integer, ByteBuffer> completionHandler =
					invocation.getArgument(3);
			completionHandler.failed(new IOException(), buffer);
			return null;
		})
		.given(channel).write(isA(ByteBuffer.class), anyLong(), isA(ByteBuffer.class), isA(CompletionHandler.class));

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.expectError(IOException.class)
				.verify();

		channel.close();
	}

	@ParameterizedDataBufferAllocatingTest
	void writeAsynchronousFileChannelCanceled(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar);

		AsynchronousFileChannel channel =
				AsynchronousFileChannel.open(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult, 1)
				.consumeNextWith(stringConsumer("foo"))
				.thenCancel()
				.verify();

		String result = String.join("", Files.readAllLines(tempFile));

		assertThat(result).isEqualTo("foo");
		channel.close();

		flux.subscribe(DataBufferUtils::release);
	}

	@ParameterizedDataBufferAllocatingTest
	void writePath(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar);

		Mono<Void> result = DataBufferUtils.write(flux, tempFile);

		StepVerifier.create(result)
				.verifyComplete();

		List<String> written = Files.readAllLines(tempFile);
		assertThat(written).contains("foobar");
	}

	@ParameterizedDataBufferAllocatingTest
	void readAndWriteByteChannel(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		Path source = Paths.get(
				DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI());
		Flux<DataBuffer> sourceFlux =
				DataBufferUtils
						.readByteChannel(() -> FileChannel.open(source, StandardOpenOption.READ),
							super.bufferFactory, 3);

		Path destination = Files.createTempFile("DataBufferUtilsTests", null);
		WritableByteChannel channel = Files.newByteChannel(destination, StandardOpenOption.WRITE);

		DataBufferUtils.write(sourceFlux, channel)
				.subscribe(DataBufferUtils.releaseConsumer(),
						throwable -> {
							throw new AssertionError(throwable.getMessage(), throwable);
						},
						() -> {
							try {
								String expected = String.join("", Files.readAllLines(source));
								String result = String.join("", Files.readAllLines(destination));
								assertThat(result).isEqualTo(expected);
							}
							catch (IOException e) {
								throw new AssertionError(e.getMessage(), e);
							}
							finally {
								DataBufferUtils.closeChannel(channel);
							}
						});
	}

	@ParameterizedDataBufferAllocatingTest
	void readAndWriteAsynchronousFileChannel(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		Path source = Paths.get(
				DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI());
		Flux<DataBuffer> sourceFlux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(source, StandardOpenOption.READ),
				super.bufferFactory, 3);

		Path destination = Files.createTempFile("DataBufferUtilsTests", null);
		AsynchronousFileChannel channel =
				AsynchronousFileChannel.open(destination, StandardOpenOption.WRITE);

		CountDownLatch latch = new CountDownLatch(1);

		DataBufferUtils.write(sourceFlux, channel)
				.subscribe(DataBufferUtils::release,
						throwable -> {
							throw new AssertionError(throwable.getMessage(), throwable);
						},
						() -> {
							try {
								String expected = String.join("", Files.readAllLines(source));
								String result = String.join("", Files.readAllLines(destination));

								assertThat(result).isEqualTo(expected);
								latch.countDown();

							}
							catch (IOException e) {
								throw new AssertionError(e.getMessage(), e);
							}
							finally {
								DataBufferUtils.closeChannel(channel);
							}
						});

		latch.await();
	}

	@ParameterizedDataBufferAllocatingTest
	void takeUntilByteCount(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> result = DataBufferUtils.takeUntilByteCount(
				Flux.just(stringBuffer("foo"), stringBuffer("bar")), 5L);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("ba"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedDataBufferAllocatingTest
	void takeUntilByteCountCanceled(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> source = Flux.concat(
				deferStringBuffer("foo"),
				deferStringBuffer("bar")
		);
		Flux<DataBuffer> result = DataBufferUtils.takeUntilByteCount(
				source, 5L);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.thenCancel()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedDataBufferAllocatingTest
	void takeUntilByteCountError(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> source = Flux.concat(
				Mono.defer(() -> Mono.just(stringBuffer("foo"))),
				Mono.error(new RuntimeException())
		);

		Flux<DataBuffer> result = DataBufferUtils.takeUntilByteCount(source, 5L);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.expectError(RuntimeException.class)
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedDataBufferAllocatingTest
	void takeUntilByteCountExact(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> source = Flux.concat(
				deferStringBuffer("foo"),
				deferStringBuffer("bar"),
				deferStringBuffer("baz")
		);

		Flux<DataBuffer> result = DataBufferUtils.takeUntilByteCount(source, 6L);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedDataBufferAllocatingTest
	void skipUntilByteCount(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> source = Flux.concat(
				deferStringBuffer("foo"),
				deferStringBuffer("bar"),
				deferStringBuffer("baz")
		);
		Flux<DataBuffer> result = DataBufferUtils.skipUntilByteCount(source, 5L);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("r"))
				.consumeNextWith(stringConsumer("baz"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedDataBufferAllocatingTest
	void skipUntilByteCountCancelled(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> source = Flux.concat(
				deferStringBuffer("foo"),
				deferStringBuffer("bar")
		);
		Flux<DataBuffer> result = DataBufferUtils.skipUntilByteCount(source, 5L);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("r"))
				.thenCancel()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedDataBufferAllocatingTest
	void skipUntilByteCountErrorInFlux(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		Flux<DataBuffer> flux =
				Flux.just(foo).concatWith(Mono.error(new RuntimeException()));
		Flux<DataBuffer> result = DataBufferUtils.skipUntilByteCount(flux, 3L);

		StepVerifier.create(result)
				.expectError(RuntimeException.class)
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedDataBufferAllocatingTest
	void skipUntilByteCountShouldSkipAll(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Flux<DataBuffer> result = DataBufferUtils.skipUntilByteCount(flux, 9L);

		StepVerifier.create(result)
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedDataBufferAllocatingTest
	void releaseConsumer(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);

		flux.subscribe(DataBufferUtils.releaseConsumer());

		assertReleased(foo);
		assertReleased(bar);
		assertReleased(baz);
	}

	private static void assertReleased(DataBuffer dataBuffer) {
		if (dataBuffer instanceof NettyDataBuffer) {
			ByteBuf byteBuf = ((NettyDataBuffer) dataBuffer).getNativeBuffer();
			assertThat(byteBuf.refCnt()).isEqualTo(0);
		}
	}

	@ParameterizedDataBufferAllocatingTest
	void SPR16070(String displayName, DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		ReadableByteChannel channel = mock(ReadableByteChannel.class);
		given(channel.read(any()))
				.willAnswer(putByte('a'))
				.willAnswer(putByte('b'))
				.willAnswer(putByte('c'))
				.willReturn(-1);

		Flux<DataBuffer> read =
				DataBufferUtils.readByteChannel(() -> channel, super.bufferFactory, 1);

		StepVerifier.create(read)
				.consumeNextWith(stringConsumer("a"))
				.consumeNextWith(stringConsumer("b"))
				.consumeNextWith(stringConsumer("c"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));

	}

	private Answer<Integer> putByte(int b) {
		return invocation -> {
			ByteBuffer buffer = invocation.getArgument(0);
			buffer.put((byte) b);
			return 1;
		};
	}

	@ParameterizedDataBufferAllocatingTest
	void join(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Mono<DataBuffer> result = DataBufferUtils.join(flux);

		StepVerifier.create(result)
				.consumeNextWith(buf -> {
					assertThat(buf.toString(StandardCharsets.UTF_8)).isEqualTo("foobarbaz");
					release(buf);
				})
				.verifyComplete();
	}

	@ParameterizedDataBufferAllocatingTest
	void joinWithLimit(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Mono<DataBuffer> result = DataBufferUtils.join(flux, 8);

		StepVerifier.create(result)
				.verifyError(DataBufferLimitException.class);
	}

	@Test // gh-26060
	void joinWithLimitDoesNotOverRelease() {
		NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);
		byte[] bytes = "foo-bar-baz".getBytes(StandardCharsets.UTF_8);

		NettyDataBuffer buffer = bufferFactory.allocateBuffer(bytes.length);
		buffer.getNativeBuffer().retain(); // should be at 2 now
		buffer.write(bytes);

		Mono<DataBuffer> result = DataBufferUtils.join(Flux.just(buffer), 8);

		StepVerifier.create(result).verifyError(DataBufferLimitException.class);
		assertThat(buffer.getNativeBuffer().refCnt()).isEqualTo(1);
		buffer.release();
	}

	@ParameterizedDataBufferAllocatingTest
	void joinErrors(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar).concatWith(Flux.error(new RuntimeException()));
		Mono<DataBuffer> result = DataBufferUtils.join(flux);

		StepVerifier.create(result)
				.expectError(RuntimeException.class)
				.verify();
	}

	@ParameterizedDataBufferAllocatingTest
	void joinCanceled(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> source = Flux.concat(
				deferStringBuffer("foo"),
				deferStringBuffer("bar"),
				deferStringBuffer("baz")
		);
		Mono<DataBuffer> result = DataBufferUtils.join(source);

		StepVerifier.create(result)
				.thenCancel()
				.verify();
	}

	@ParameterizedDataBufferAllocatingTest
	void matcher(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");

		byte[] delims = "ooba".getBytes(StandardCharsets.UTF_8);
		DataBufferUtils.Matcher matcher = DataBufferUtils.matcher(delims);
		int result = matcher.match(foo);
		assertThat(result).isEqualTo(-1);
		result = matcher.match(bar);
		assertThat(result).isEqualTo(1);


		release(foo, bar);
	}

	@ParameterizedDataBufferAllocatingTest
	void matcher2(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foooobar");

		byte[] delims = "oo".getBytes(StandardCharsets.UTF_8);
		DataBufferUtils.Matcher matcher = DataBufferUtils.matcher(delims);
		int endIndex = matcher.match(foo);
		assertThat(endIndex).isEqualTo(2);
		foo.readPosition(endIndex + 1);
		endIndex = matcher.match(foo);
		assertThat(endIndex).isEqualTo(4);
		foo.readPosition(endIndex + 1);
		endIndex = matcher.match(foo);
		assertThat(endIndex).isEqualTo(-1);

		release(foo);
	}

	@ParameterizedDataBufferAllocatingTest
	void matcher3(String displayName, DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foooobar");

		byte[] delims = "oo".getBytes(StandardCharsets.UTF_8);
		DataBufferUtils.Matcher matcher = DataBufferUtils.matcher(delims);
		int endIndex = matcher.match(foo);
		assertThat(endIndex).isEqualTo(2);
		foo.readPosition(endIndex + 1);
		endIndex = matcher.match(foo);
		assertThat(endIndex).isEqualTo(4);
		foo.readPosition(endIndex + 1);
		endIndex = matcher.match(foo);
		assertThat(endIndex).isEqualTo(-1);

		release(foo);
	}

	@ParameterizedDataBufferAllocatingTest
	void propagateContextByteChannel(String displayName, DataBufferFactory bufferFactory) throws IOException {
		Path path = Paths.get(this.resource.getURI());
		try (SeekableByteChannel out = Files.newByteChannel(this.tempFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			Flux<DataBuffer> result = DataBufferUtils.read(path, bufferFactory, 1024, StandardOpenOption.READ)
					.transformDeferredContextual((f, ctx) -> {
						assertThat(ctx.getOrDefault("key", "EMPTY")).isEqualTo("TEST");
						return f;
					})
					.transform(f -> DataBufferUtils.write(f, out))
					.transformDeferredContextual((f, ctx) -> {
						assertThat(ctx.getOrDefault("key", "EMPTY")).isEqualTo("TEST");
						return f;
					})
					.contextWrite(Context.of("key", "TEST"));

			StepVerifier.create(result)
					.consumeNextWith(DataBufferUtils::release)
					.verifyComplete();


		}
	}

	@ParameterizedDataBufferAllocatingTest
	void propagateContextAsynchronousFileChannel(String displayName, DataBufferFactory bufferFactory) throws IOException {
		Path path = Paths.get(this.resource.getURI());
		try (AsynchronousFileChannel out = AsynchronousFileChannel.open(this.tempFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			Flux<DataBuffer> result = DataBufferUtils.read(path, bufferFactory, 1024, StandardOpenOption.READ)
					.transformDeferredContextual((f, ctx) -> {
						assertThat(ctx.getOrDefault("key", "EMPTY")).isEqualTo("TEST");
						return f;
					})
					.transform(f -> DataBufferUtils.write(f, out))
					.transformDeferredContextual((f, ctx) -> {
						assertThat(ctx.getOrDefault("key", "EMPTY")).isEqualTo("TEST");
						return f;
					})
					.contextWrite(Context.of("key", "TEST"));

			StepVerifier.create(result)
					.consumeNextWith(DataBufferUtils::release)
					.verifyComplete();


		}
	}

	private static class ZeroDemandSubscriber extends BaseSubscriber<DataBuffer> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			// Just subscribe without requesting
		}
	}

}

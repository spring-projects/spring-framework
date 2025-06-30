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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Publisher;
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
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
	void readInputStream(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> flux = DataBufferUtils.readInputStream(
				this.resource::getInputStream, super.bufferFactory, 3);

		verifyReadData(flux);
	}

	@ParameterizedDataBufferAllocatingTest
	void readByteChannel(DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		URI uri = this.resource.getURI();
		Flux<DataBuffer> result =
				DataBufferUtils.readByteChannel(() -> FileChannel.open(Paths.get(uri), StandardOpenOption.READ),
					super.bufferFactory, 3);

		verifyReadData(result);
	}

	@ParameterizedDataBufferAllocatingTest
	void readByteChannelError(DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		ReadableByteChannel channel = mock();
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
	void readByteChannelCancel(DataBufferFactory bufferFactory) throws Exception {
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
	void readAsynchronousFileChannel(DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		URI uri = this.resource.getURI();
		Flux<DataBuffer> flux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ),
				super.bufferFactory, 3);

		verifyReadData(flux);
	}

	@ParameterizedDataBufferAllocatingTest
	void readAsynchronousFileChannelPosition(DataBufferFactory bufferFactory) throws Exception {
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
	void readAsynchronousFileChannelError(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		AsynchronousFileChannel channel = mock();
		willAnswer(invocation -> {
			ByteBuffer byteBuffer = invocation.getArgument(0);
			byteBuffer.put("foo".getBytes(StandardCharsets.UTF_8));
			long pos = invocation.getArgument(1);
			assertThat(pos).isEqualTo(0);
			Object attachment = invocation.getArgument(2);
			CompletionHandler<Integer, Object> completionHandler = invocation.getArgument(3);
			completionHandler.completed(3, attachment);
			return null;
		}).willAnswer(invocation -> {
			Object attachment = invocation.getArgument(2);
			CompletionHandler<Integer, Object> completionHandler = invocation.getArgument(3);
			completionHandler.failed(new IOException(), attachment);
			return null;
		})
		.given(channel).read(any(), anyLong(), any(), any());

		Flux<DataBuffer> result=
				DataBufferUtils.readAsynchronousFileChannel(() -> channel, super.bufferFactory, 3);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.expectError(IOException.class)
				.verify(Duration.ofSeconds(3));
	}

	@ParameterizedDataBufferAllocatingTest
	void readAsynchronousFileChannelCancel(DataBufferFactory bufferFactory) throws Exception {
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

	@ParameterizedDataBufferAllocatingTest  // gh-22107
	void readAsynchronousFileChannelCancelWithoutDemand(DataBufferFactory bufferFactory) throws Exception {
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
	void readPath(DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> flux = DataBufferUtils.read(this.resource.getFile().toPath(), super.bufferFactory, 3);

		verifyReadData(flux);
	}

	@ParameterizedDataBufferAllocatingTest
	void readResource(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		Flux<DataBuffer> flux = DataBufferUtils.read(this.resource, super.bufferFactory, 3);

		verifyReadData(flux);
	}

	@ParameterizedDataBufferAllocatingTest
	void readResourcePosition(DataBufferFactory bufferFactory) {
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
	void readResourcePositionAndTakeUntil(DataBufferFactory bufferFactory) {
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
	void readByteArrayResourcePositionAndTakeUntil(DataBufferFactory bufferFactory) {
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
	void writeOutputStream(DataBufferFactory bufferFactory) throws Exception {
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
	void writeWritableByteChannel(DataBufferFactory bufferFactory) throws Exception {
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
	void writeWritableByteChannelErrorInFlux(DataBufferFactory bufferFactory) throws Exception {
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
	void writeWritableByteChannelErrorInWrite(DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar);

		WritableByteChannel channel = mock();
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
	void writeWritableByteChannelCancel(DataBufferFactory bufferFactory) throws Exception {
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
	void writeAsynchronousFileChannel(DataBufferFactory bufferFactory) throws Exception {
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
	void writeAsynchronousFileChannelErrorInFlux(DataBufferFactory bufferFactory) throws Exception {
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
	void writeAsynchronousFileChannelErrorInWrite(DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar);

		AsynchronousFileChannel channel = mock();
		willAnswer(invocation -> {
			ByteBuffer buffer = invocation.getArgument(0);
			long pos = invocation.getArgument(1);
			assertThat(pos).isEqualTo(0);
			Object attachment = invocation.getArgument(2);
			CompletionHandler<Integer, Object> completionHandler = invocation.getArgument(3);
			int written = buffer.remaining();
			buffer.position(buffer.limit());
			completionHandler.completed(written, attachment);
			return null;
		})
		.willAnswer(invocation -> {
			Object attachment = invocation.getArgument(2);
			CompletionHandler<Integer, Object> completionHandler = invocation.getArgument(3);
			completionHandler.failed(new IOException(), attachment);
			return null;
		})
		.given(channel).write(any(), anyLong(), any(), any());

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.expectError(IOException.class)
				.verify();

		channel.close();
	}

	@ParameterizedDataBufferAllocatingTest
	void writeAsynchronousFileChannelCanceled(DataBufferFactory bufferFactory) throws Exception {
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
	void writePath(DataBufferFactory bufferFactory) throws Exception {
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
	void outputStreamPublisher(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		byte[] foo = "foo".getBytes(StandardCharsets.UTF_8);
		byte[] bar = "bar".getBytes(StandardCharsets.UTF_8);
		byte[] baz = "baz".getBytes(StandardCharsets.UTF_8);

		Publisher<DataBuffer> publisher = DataBufferUtils.outputStreamPublisher(outputStream -> {
			try {
				outputStream.write(foo);
				outputStream.write(bar);
				outputStream.write(baz);
			}
			catch (IOException ex) {
				fail(ex.getMessage(), ex);
			}
		}, super.bufferFactory, Executors.newSingleThreadExecutor());

		StepVerifier.create(publisher)
				.consumeNextWith(stringConsumer("foobarbaz"))
				.verifyComplete();
	}

	@ParameterizedDataBufferAllocatingTest
	void outputStreamPublisherFlush(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		byte[] foo = "foo".getBytes(StandardCharsets.UTF_8);
		byte[] bar = "bar".getBytes(StandardCharsets.UTF_8);
		byte[] baz = "baz".getBytes(StandardCharsets.UTF_8);

		Publisher<DataBuffer> publisher = DataBufferUtils.outputStreamPublisher(outputStream -> {
			try {
				outputStream.write(foo);
				outputStream.flush();
				outputStream.write(bar);
				outputStream.flush();
				outputStream.write(baz);
				outputStream.flush();
			}
			catch (IOException ex) {
				fail(ex.getMessage(), ex);
			}
		}, super.bufferFactory, Executors.newSingleThreadExecutor());

		StepVerifier.create(publisher)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.verifyComplete();
	}

	@ParameterizedDataBufferAllocatingTest
	void outputStreamPublisherChunkSize(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		byte[] foo = "foo".getBytes(StandardCharsets.UTF_8);
		byte[] bar = "bar".getBytes(StandardCharsets.UTF_8);
		byte[] baz = "baz".getBytes(StandardCharsets.UTF_8);

		Publisher<DataBuffer> publisher = DataBufferUtils.outputStreamPublisher(outputStream -> {
			try {
				outputStream.write(foo);
				outputStream.write(bar);
				outputStream.write(baz);
			}
			catch (IOException ex) {
				fail(ex.getMessage(), ex);
			}
		}, super.bufferFactory, Executors.newSingleThreadExecutor(), 3);

		StepVerifier.create(publisher)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.verifyComplete();
	}

	@ParameterizedDataBufferAllocatingTest
	void outputStreamPublisherCancel(DataBufferFactory bufferFactory) throws InterruptedException {
		super.bufferFactory = bufferFactory;

		byte[] foo = "foo".getBytes(StandardCharsets.UTF_8);
		byte[] bar = "bar".getBytes(StandardCharsets.UTF_8);

		CountDownLatch latch = new CountDownLatch(1);

		Publisher<DataBuffer> publisher = DataBufferUtils.outputStreamPublisher(outputStream -> {
			try {
				assertThatIOException()
						.isThrownBy(() -> {
							outputStream.write(foo);
							outputStream.flush();
							outputStream.write(bar);
							outputStream.flush();
						})
						.withMessage("Subscription has been terminated");
			}
			finally {
				latch.countDown();
			}
		}, super.bufferFactory, Executors.newSingleThreadExecutor());

		StepVerifier.create(publisher, 1)
				.consumeNextWith(stringConsumer("foo"))
				.thenCancel()
				.verify();

		latch.await();
	}

	@ParameterizedDataBufferAllocatingTest
	void outputStreamPublisherClosed(DataBufferFactory bufferFactory) throws InterruptedException {
		super.bufferFactory = bufferFactory;

		CountDownLatch latch = new CountDownLatch(1);

		Publisher<DataBuffer> publisher = DataBufferUtils.outputStreamPublisher(outputStream -> {
			try {
				OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
				writer.write("foo");
				writer.close();
				assertThatIOException().isThrownBy(() -> writer.write("bar"))
						.withMessage("Stream closed");
			}
			catch (IOException ex) {
				fail(ex.getMessage(), ex);
			}
			finally {
				latch.countDown();
			}
		}, super.bufferFactory, Executors.newSingleThreadExecutor());

		StepVerifier.create(publisher)
				.consumeNextWith(stringConsumer("foo"))
				.verifyComplete();

		latch.await();
	}

	@ParameterizedDataBufferAllocatingTest
	void inputStreamSubscriberChunkSize(DataBufferFactory factory) {
		genericInputStreamSubscriberTest(
				factory, 3, 3, 64, List.of("foo", "bar", "baz"), List.of("foo", "bar", "baz"));
	}

	@ParameterizedDataBufferAllocatingTest
	void inputStreamSubscriberChunkSize2(DataBufferFactory factory) {
		genericInputStreamSubscriberTest(
				factory, 3, 3, 1, List.of("foo", "bar", "baz"), List.of("foo", "bar", "baz"));
	}

	@ParameterizedDataBufferAllocatingTest
	void inputStreamSubscriberChunkSize3(DataBufferFactory factory) {
		genericInputStreamSubscriberTest(factory, 3, 12, 1, List.of("foo", "bar", "baz"), List.of("foobarbaz"));
	}

	@ParameterizedDataBufferAllocatingTest
	void inputStreamSubscriberChunkSize4(DataBufferFactory factory) {
		genericInputStreamSubscriberTest(
				factory, 3, 1, 1, List.of("foo", "bar", "baz"), List.of("f", "o", "o", "b", "a", "r", "b", "a", "z"));
	}

	@ParameterizedDataBufferAllocatingTest
	void inputStreamSubscriberChunkSize5(DataBufferFactory factory) {
		genericInputStreamSubscriberTest(
				factory, 3, 2, 1, List.of("foo", "bar", "baz"), List.of("fo", "ob", "ar", "ba", "z"));
	}

	@ParameterizedDataBufferAllocatingTest
	void inputStreamSubscriberChunkSize6(DataBufferFactory factory) {
		genericInputStreamSubscriberTest(
				factory, 1, 3, 1, List.of("foo", "bar", "baz"), List.of("foo", "bar", "baz"));
	}

	@ParameterizedDataBufferAllocatingTest
	void inputStreamSubscriberChunkSize7(DataBufferFactory factory) {
		genericInputStreamSubscriberTest(
				factory, 1, 3, 64, List.of("foo", "bar", "baz"), List.of("foo", "bar", "baz"));
	}

	void genericInputStreamSubscriberTest(
			DataBufferFactory factory, int writeChunkSize, int readChunkSize, int bufferSize,
			List<String> input, List<String> expectedOutput) {

		super.bufferFactory = factory;

		Publisher<DataBuffer> publisher = DataBufferUtils.outputStreamPublisher(
				out -> {
					try {
						for (String word : input) {
							out.write(word.getBytes(StandardCharsets.UTF_8));
						}
					}
					catch (IOException ex) {
						fail(ex.getMessage(), ex);
					}
				},
				super.bufferFactory, Executors.newSingleThreadExecutor(), writeChunkSize);

		byte[] chunk = new byte[readChunkSize];
		List<String> words = new ArrayList<>();

		try (InputStream in = DataBufferUtils.subscriberInputStream(publisher, bufferSize)) {
			int read;
			while ((read = in.read(chunk)) > -1) {
				words.add(new String(chunk, 0, read, StandardCharsets.UTF_8));
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		assertThat(words).containsExactlyElementsOf(expectedOutput);
	}

	@ParameterizedDataBufferAllocatingTest
	void inputStreamSubscriberError(DataBufferFactory factory) {
		super.bufferFactory = factory;

		var input = List.of("foo ", "bar ", "baz");

		Publisher<DataBuffer> publisher = DataBufferUtils.outputStreamPublisher(
				out -> {
					try {
						for (String word : input) {
							out.write(word.getBytes(StandardCharsets.UTF_8));
						}
						throw new RuntimeException("boom");
					}
					catch (IOException ex) {
						fail(ex.getMessage(), ex);
					}
				},
				super.bufferFactory, Executors.newSingleThreadExecutor(), 1);


		RuntimeException error = null;
		byte[] chunk = new byte[4];
		List<String> words = new ArrayList<>();

		try (InputStream in = DataBufferUtils.subscriberInputStream(publisher, 1)) {
			int read;
			while ((read = in.read(chunk)) > -1) {
				words.add(new String(chunk, 0, read, StandardCharsets.UTF_8));
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		catch (RuntimeException e) {
			error = e;
		}
		assertThat(words).containsExactlyElementsOf(List.of("foo ", "bar ", "baz"));
		assertThat(error).hasMessage("boom");
	}

	@ParameterizedDataBufferAllocatingTest
	void inputStreamSubscriberMixedReadMode(DataBufferFactory factory) {
		super.bufferFactory = factory;

		var input = List.of("foo ", "bar ", "baz");

		Publisher<DataBuffer> publisher = DataBufferUtils.outputStreamPublisher(
				out -> {
					try {
						for (String word : input) {
							out.write(word.getBytes(StandardCharsets.UTF_8));
						}
					}
					catch (IOException ex) {
						fail(ex.getMessage(), ex);
					}
				},
				super.bufferFactory, Executors.newSingleThreadExecutor(), 1);


		byte[] chunk = new byte[3];
		ArrayList<String> words = new ArrayList<>();

		try (InputStream inputStream = DataBufferUtils.subscriberInputStream(publisher, 1)) {
			words.add(new String(chunk,0, inputStream.read(chunk), StandardCharsets.UTF_8));
			assertThat(inputStream.read()).isEqualTo(' ' & 0xFF);
			words.add(new String(chunk,0, inputStream.read(chunk), StandardCharsets.UTF_8));
			assertThat(inputStream.read()).isEqualTo(' ' & 0xFF);
			words.add(new String(chunk,0, inputStream.read(chunk), StandardCharsets.UTF_8));
			assertThat(inputStream.read()).isEqualTo(-1);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		assertThat(words).containsExactlyElementsOf(List.of("foo", "bar", "baz"));
	}

	@ParameterizedDataBufferAllocatingTest
	void inputStreamSubscriberClose(DataBufferFactory bufferFactory) throws InterruptedException {
		for (int i = 1; i < 100; i++) {
			CountDownLatch latch = new CountDownLatch(1);
			super.bufferFactory = bufferFactory;

			var input = List.of("foo", "bar", "baz");

			Publisher<DataBuffer> publisher = DataBufferUtils.outputStreamPublisher(
					out -> {
						try {
							assertThatIOException()
									.isThrownBy(() -> {
										for (String word : input) {
											out.write(word.getBytes(StandardCharsets.UTF_8));
											out.flush();
										}
									})
									.withMessage("Subscription has been terminated");
						}
						finally {
							latch.countDown();
						}
					},
					super.bufferFactory, Executors.newSingleThreadExecutor(), 1);


			byte[] chunk = new byte[3];
			ArrayList<String> words = new ArrayList<>();

			try (InputStream in = DataBufferUtils.subscriberInputStream(publisher, ThreadLocalRandom.current().nextInt(1, 4))) {
				in.read(chunk);
				String word = new String(chunk, StandardCharsets.UTF_8);
				words.add(word);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
			assertThat(words).containsExactlyElementsOf(List.of("foo"));
			latch.await();
		}
	}

	@ParameterizedDataBufferAllocatingTest
	void readAndWriteByteChannel(DataBufferFactory bufferFactory) throws Exception {
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
	void readAndWriteAsynchronousFileChannel(DataBufferFactory bufferFactory) throws Exception {
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
	void takeUntilByteCount(DataBufferFactory bufferFactory) {
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
	void takeUntilByteCountCanceled(DataBufferFactory bufferFactory) {
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
	void takeUntilByteCountError(DataBufferFactory bufferFactory) {
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
	void takeUntilByteCountExact(DataBufferFactory bufferFactory) {
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
	void skipUntilByteCount(DataBufferFactory bufferFactory) {
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
	void skipUntilByteCountCancelled(DataBufferFactory bufferFactory) {
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
	void skipUntilByteCountErrorInFlux(DataBufferFactory bufferFactory) {
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
	void skipUntilByteCountShouldSkipAll(DataBufferFactory bufferFactory) {
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
	void releaseConsumer(DataBufferFactory bufferFactory) {
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
		if (dataBuffer instanceof NettyDataBuffer nettyDataBuffer) {
			ByteBuf byteBuf = nettyDataBuffer.getNativeBuffer();
			assertThat(byteBuf.refCnt()).isEqualTo(0);
		}
	}

	@ParameterizedDataBufferAllocatingTest
	void SPR16070(DataBufferFactory bufferFactory) throws Exception {
		super.bufferFactory = bufferFactory;

		ReadableByteChannel channel = mock();
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
	void join(DataBufferFactory bufferFactory) {
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
	void joinWithLimit(DataBufferFactory bufferFactory) {
		super.bufferFactory = bufferFactory;

		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Mono<DataBuffer> result = DataBufferUtils.join(flux, 8);

		StepVerifier.create(result)
				.verifyError(DataBufferLimitException.class);
	}

	@Test  // gh-26060
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
	void joinErrors(DataBufferFactory bufferFactory) {
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
	void joinCanceled(DataBufferFactory bufferFactory) {
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
	void matcher(DataBufferFactory bufferFactory) {
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
	void matcher2(DataBufferFactory bufferFactory) {
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
	void matcher3(DataBufferFactory bufferFactory) {
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
	void propagateContextByteChannel(DataBufferFactory bufferFactory) throws IOException {
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
	void propagateContextAsynchronousFileChannel(DataBufferFactory bufferFactory) throws IOException {
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

	@ParameterizedDataBufferAllocatingTest
	void propagateContextPath(DataBufferFactory bufferFactory) throws IOException {
		Path path = Paths.get(this.resource.getURI());
		Path out = Files.createTempFile("data-buffer-utils-tests", ".tmp");

		Flux<Void> result = DataBufferUtils.read(path, bufferFactory, 1024, StandardOpenOption.READ)
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
				.verifyComplete();
	}


	private static class ZeroDemandSubscriber extends BaseSubscriber<DataBuffer> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			// Just subscribe without requesting
		}
	}

}

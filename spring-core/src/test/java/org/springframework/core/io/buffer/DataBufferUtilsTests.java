/*
 * Copyright 2002-2018 the original author or authors.
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
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import io.netty.buffer.ByteBuf;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Arjen Poutsma
 */
public class DataBufferUtilsTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void readByteChannel() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI();
		Flux<DataBuffer> flux =
				DataBufferUtils.readByteChannel(() -> FileChannel.open(Paths.get(uri), StandardOpenOption.READ),
						this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void readAsynchronousFileChannel() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI();
		Flux<DataBuffer> flux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ),
				this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void readAsynchronousFileChannelPosition() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI();
		Flux<DataBuffer> flux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ),
				3, this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void readInputStream() throws Exception {
		Flux<DataBuffer> flux = DataBufferUtils.readInputStream(
				() -> DataBufferUtilsTests.class.getResourceAsStream("DataBufferUtilsTests.txt"),
				this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void readResource() throws Exception {
		Resource resource = new ClassPathResource("DataBufferUtilsTests.txt", getClass());
		Flux<DataBuffer> flux = DataBufferUtils.read(resource, this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void readResourcePosition() throws Exception {
		Resource resource = new ClassPathResource("DataBufferUtilsTests.txt", getClass());
		Flux<DataBuffer> flux = DataBufferUtils.read(resource, 3, this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void writeOutputStream() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		DataBuffer qux = stringBuffer("qux");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz, qux);

		Path tempFile = Files.createTempFile("DataBufferUtilsTests", null);
		OutputStream os = Files.newOutputStream(tempFile);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, os);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		String result = String.join("", Files.readAllLines(tempFile));

		assertEquals("foobarbazqux", result);
		os.close();
	}

	@Test
	public void writeWritableByteChannel() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		DataBuffer qux = stringBuffer("qux");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz, qux);

		Path tempFile = Files.createTempFile("DataBufferUtilsTests", null);
		WritableByteChannel channel = Files.newByteChannel(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		String result = String.join("", Files.readAllLines(tempFile));

		assertEquals("foobarbazqux", result);
		channel.close();
	}

	@Test
	public void writeWritableByteChannelErrorInFlux() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar).concatWith(Flux.error(new RuntimeException()));

		Path tempFile = Files.createTempFile("DataBufferUtilsTests", null);
		WritableByteChannel channel = Files.newByteChannel(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.expectError()
				.verify(Duration.ofSeconds(5));

		String result = String.join("", Files.readAllLines(tempFile));

		assertEquals("foobar", result);
		channel.close();
	}

	@Test
	public void writeWritableByteChannelErrorInWrite() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar);

		WritableByteChannel channel = mock(WritableByteChannel.class);
		when(channel.write(any()))
				.thenAnswer(invocation -> {
					ByteBuffer buffer = invocation.getArgument(0);
					int written = buffer.remaining();
					buffer.position(buffer.limit());
					return written;
				})
				.thenThrow(new IOException());

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.expectError(IOException.class)
				.verify();

		channel.close();
	}

	@Test
	public void writeAsynchronousFileChannel() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		DataBuffer qux = stringBuffer("qux");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz, qux);

		Path tempFile = Files.createTempFile("DataBufferUtilsTests", null);
		AsynchronousFileChannel channel =
				AsynchronousFileChannel.open(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("baz"))
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		String result = String.join("", Files.readAllLines(tempFile));

		assertEquals("foobarbazqux", result);
		channel.close();
	}

	@Test
	public void writeAsynchronousFileChannelErrorInFlux() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux =
				Flux.just(foo, bar).concatWith(Mono.error(new RuntimeException()));

		Path tempFile = Files.createTempFile("DataBufferUtilsTests", null);
		AsynchronousFileChannel channel =
				AsynchronousFileChannel.open(tempFile, StandardOpenOption.WRITE);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.expectError(RuntimeException.class)
				.verify();

		String result = String.join("", Files.readAllLines(tempFile));

		assertEquals("foobar", result);
		channel.close();
	}


	@Test
	public void writeAsynchronousFileChannelErrorInWrite() throws Exception {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar);

		AsynchronousFileChannel channel = mock(AsynchronousFileChannel.class);
		doAnswer(invocation -> {
			ByteBuffer buffer = invocation.getArgument(0);
			long pos = invocation.getArgument(1);
			CompletionHandler<Integer, ByteBuffer> completionHandler = invocation.getArgument(3);

			assertEquals(0, pos);

			int written = buffer.remaining();
			buffer.position(buffer.limit());
			completionHandler.completed(written, buffer);

			return null;
		})
				.doAnswer(invocation -> {
					ByteBuffer buffer = invocation.getArgument(0);
					CompletionHandler<Integer, ByteBuffer> completionHandler =
							invocation.getArgument(3);
					completionHandler.failed(new IOException(), buffer);
					return null;
				})
				.when(channel).write(isA(ByteBuffer.class), anyLong(), isA(ByteBuffer.class),
				isA(CompletionHandler.class));

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, channel);
		StepVerifier.create(writeResult)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.expectError(IOException.class)
				.verify();

		channel.close();
	}

	@Test
	public void readAndWriteByteChannel() throws Exception {
		Path source = Paths.get(
				DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI());
		Flux<DataBuffer> sourceFlux =
				DataBufferUtils
						.readByteChannel(() -> FileChannel.open(source, StandardOpenOption.READ),
								this.bufferFactory, 3);

		Path destination = Files.createTempFile("DataBufferUtilsTests", null);
		WritableByteChannel channel = Files.newByteChannel(destination, StandardOpenOption.WRITE);

		DataBufferUtils.write(sourceFlux, channel)
				.subscribe(DataBufferUtils.releaseConsumer(),
						throwable -> fail(throwable.getMessage()),
						() -> {
							try {
								String expected = String.join("", Files.readAllLines(source));
								String result = String.join("", Files.readAllLines(destination));

								assertEquals(expected, result);
								channel.close();

							}
							catch (IOException e) {
								fail(e.getMessage());
							}
						});
	}

	@Test
	public void readAndWriteAsynchronousFileChannel() throws Exception {
		Path source = Paths.get(
				DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt").toURI());
		Flux<DataBuffer> sourceFlux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(source, StandardOpenOption.READ),
				this.bufferFactory, 3);

		Path destination = Files.createTempFile("DataBufferUtilsTests", null);
		AsynchronousFileChannel channel =
				AsynchronousFileChannel.open(destination, StandardOpenOption.WRITE);

		CountDownLatch latch = new CountDownLatch(1);

		DataBufferUtils.write(sourceFlux, channel)
				.subscribe(DataBufferUtils::release,
						throwable -> fail(throwable.getMessage()),
						() -> {
							try {
								String expected = String.join("", Files.readAllLines(source));
								String result = String.join("", Files.readAllLines(destination));

								assertEquals(expected, result);
								channel.close();
								latch.countDown();

							}
							catch (IOException e) {
								fail(e.getMessage());
							}
						});

		latch.await();
	}

	@Test
	public void takeUntilByteCount() {

		Flux<DataBuffer> result = DataBufferUtils.takeUntilByteCount(
				Flux.just(stringBuffer("foo"), stringBuffer("bar")), 5L);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("ba"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void takeUntilByteCountExact() {

		DataBuffer extraBuffer = stringBuffer("baz");

		Flux<DataBuffer> result = DataBufferUtils.takeUntilByteCount(
				Flux.just(stringBuffer("foo"), stringBuffer("bar"), extraBuffer), 6L);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.consumeNextWith(stringConsumer("bar"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));

		release(extraBuffer);
	}

	@Test
	public void skipUntilByteCount() {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Flux<DataBuffer> result = DataBufferUtils.skipUntilByteCount(flux, 5L);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("r"))
				.consumeNextWith(stringConsumer("baz"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void skipUntilByteCountShouldSkipAll() {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Flux<DataBuffer> result = DataBufferUtils.skipUntilByteCount(flux, 9L);

		StepVerifier.create(result)
				.expectNextCount(0)
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void releaseConsumer() {
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
			assertEquals(0, byteBuf.refCnt());
		}
	}

	@Test
	public void SPR16070() throws Exception {
		ReadableByteChannel channel = mock(ReadableByteChannel.class);
		when(channel.read(any()))
				.thenAnswer(putByte('a'))
				.thenAnswer(putByte('b'))
				.thenAnswer(putByte('c'))
				.thenReturn(-1);

		Flux<DataBuffer> read =
				DataBufferUtils.readByteChannel(() -> channel, this.bufferFactory, 1);

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

	@Test
	public void join() {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		DataBuffer baz = stringBuffer("baz");
		Flux<DataBuffer> flux = Flux.just(foo, bar, baz);
		Mono<DataBuffer> result = DataBufferUtils.join(flux);

		StepVerifier.create(result)
				.consumeNextWith(dataBuffer -> {
					assertEquals("foobarbaz",
							DataBufferTestUtils.dumpString(dataBuffer, StandardCharsets.UTF_8));
					release(dataBuffer);
				})
				.verifyComplete();
	}

	@Test
	public void joinErrors() {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar).concatWith(Flux.error(new RuntimeException()));
		Mono<DataBuffer> result = DataBufferUtils.join(flux);

		StepVerifier.create(result)
				.expectError(RuntimeException.class)
				.verify();
	}

}

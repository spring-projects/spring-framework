/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.netty.buffer.ByteBuf;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
public class DataBufferUtilsTests extends AbstractDataBufferAllocatingTestCase {

	private Resource resource;

	private Path tempFile;


	@Before
	public void setUp() throws IOException {
		this.resource = new ClassPathResource("DataBufferUtilsTests.txt", getClass());
		this.tempFile = Files.createTempFile("DataBufferUtilsTests", null);
	}

	@Test
	public void readInputStream() {
		Flux<DataBuffer> flux = DataBufferUtils.readInputStream(
				() -> this.resource.getInputStream(), this.bufferFactory, 3);

		verifyReadData(flux);
	}

	@Test
	public void readByteChannel() throws Exception {
		URI uri = this.resource.getURI();
		Flux<DataBuffer> result =
				DataBufferUtils.readByteChannel(() -> FileChannel.open(Paths.get(uri), StandardOpenOption.READ),
						this.bufferFactory, 3);

		verifyReadData(result);
	}

	@Test
	public void readByteChannelError() throws Exception {
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
				DataBufferUtils.readByteChannel(() -> channel, this.bufferFactory, 3);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.expectError(IOException.class)
				.verify(Duration.ofSeconds(3));
	}

	@Test
	public void readByteChannelCancel() throws Exception {
		URI uri = this.resource.getURI();
		Flux<DataBuffer> result =
				DataBufferUtils.readByteChannel(() -> FileChannel.open(Paths.get(uri), StandardOpenOption.READ),
						this.bufferFactory, 3);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.thenCancel()
				.verify();
	}

	@Test
	public void readAsynchronousFileChannel() throws Exception {
		URI uri = this.resource.getURI();
		Flux<DataBuffer> flux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ),
				this.bufferFactory, 3);

		verifyReadData(flux);
	}

	@Test
	public void readAsynchronousFileChannelPosition() throws Exception {
		URI uri = this.resource.getURI();
		Flux<DataBuffer> flux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ),
				9, this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("qux"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void readAsynchronousFileChannelError() throws Exception {
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
				DataBufferUtils.readAsynchronousFileChannel(() -> channel, this.bufferFactory, 3);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("foo"))
				.expectError(IOException.class)
				.verify(Duration.ofSeconds(3));
	}

	@Test
	public void readAsynchronousFileChannelCancel() throws Exception {
		URI uri = this.resource.getURI();
		Flux<DataBuffer> flux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ),
				this.bufferFactory, 3);

		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("foo"))
				.thenCancel()
				.verify();
	}

	@Test // gh-22107
	public void readAsynchronousFileChannelCancelWithoutDemand() throws Exception {
		URI uri = this.resource.getURI();
		Flux<DataBuffer> flux = DataBufferUtils.readAsynchronousFileChannel(
				() -> AsynchronousFileChannel.open(Paths.get(uri), StandardOpenOption.READ),
				this.bufferFactory, 3);

		BaseSubscriber<DataBuffer> subscriber = new ZeroDemandSubscriber();
		flux.subscribe(subscriber);
		subscriber.cancel();
	}

	@Test
	public void readPath() throws IOException {
		Flux<DataBuffer> flux = DataBufferUtils.read(this.resource.getFile().toPath(), this.bufferFactory, 3);

		verifyReadData(flux);
	}

	@Test
	public void readResource() throws Exception {
		Flux<DataBuffer> flux = DataBufferUtils.read(this.resource, this.bufferFactory, 3);

		verifyReadData(flux);
	}

	@Test
	public void readResourcePosition() throws Exception {
		Flux<DataBuffer> flux = DataBufferUtils.read(this.resource, 9, this.bufferFactory, 3);

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

	@Test
	public void readResourcePositionAndTakeUntil() throws Exception {
		Resource resource = new ClassPathResource("DataBufferUtilsTests.txt", getClass());
		Flux<DataBuffer> flux = DataBufferUtils.read(resource, 3, this.bufferFactory, 3);

		flux = DataBufferUtils.takeUntilByteCount(flux, 5);


		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("ba"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void readByteArrayResourcePositionAndTakeUntil() throws Exception {
		Resource resource = new ByteArrayResource("foobarbazqux" .getBytes());
		Flux<DataBuffer> flux = DataBufferUtils.read(resource, 3, this.bufferFactory, 3);

		flux = DataBufferUtils.takeUntilByteCount(flux, 5);


		StepVerifier.create(flux)
				.consumeNextWith(stringConsumer("bar"))
				.consumeNextWith(stringConsumer("ba"))
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

		OutputStream os = Files.newOutputStream(tempFile);

		Flux<DataBuffer> writeResult = DataBufferUtils.write(flux, os);
		verifyWrittenData(writeResult);
		os.close();
	}

	@Test
	public void writeWritableByteChannel() throws Exception {
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

	@Test
	public void writeWritableByteChannelErrorInFlux() throws Exception {
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

	@Test
	public void writeWritableByteChannelErrorInWrite() throws Exception {
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

	@Test
	public void writeWritableByteChannelCancel() throws Exception {
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

	@Test
	public void writeAsynchronousFileChannel() throws Exception {
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

	@Test
	public void writeAsynchronousFileChannelErrorInFlux() throws Exception {
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

	@Test
	@SuppressWarnings("unchecked")
	public void writeAsynchronousFileChannelErrorInWrite() throws Exception {
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

	@Test
	public void writeAsynchronousFileChannelCanceled() throws Exception {
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

	@Test
	public void writePath() throws IOException {
		DataBuffer foo = stringBuffer("foo");
		DataBuffer bar = stringBuffer("bar");
		Flux<DataBuffer> flux = Flux.just(foo, bar);

		Mono<Void> result = DataBufferUtils.write(flux, tempFile);

		StepVerifier.create(result)
				.verifyComplete();

		List<String> written = Files.readAllLines(tempFile);
		assertThat(written).contains("foobar");
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
	public void takeUntilByteCountCanceled() {
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

	@Test
	public void takeUntilByteCountError() {
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

	@Test
	public void takeUntilByteCountExact() {
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

	@Test
	public void skipUntilByteCount() {
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

	@Test
	public void skipUntilByteCountCancelled() {
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

	@Test
	public void skipUntilByteCountErrorInFlux() {
		DataBuffer foo = stringBuffer("foo");
		Flux<DataBuffer> flux =
				Flux.just(foo).concatWith(Mono.error(new RuntimeException()));
		Flux<DataBuffer> result = DataBufferUtils.skipUntilByteCount(flux, 3L);

		StepVerifier.create(result)
				.expectError(RuntimeException.class)
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
			assertThat(byteBuf.refCnt()).isEqualTo(0);
		}
	}

	@Test
	public void SPR16070() throws Exception {
		ReadableByteChannel channel = mock(ReadableByteChannel.class);
		given(channel.read(any()))
				.willAnswer(putByte('a'))
				.willAnswer(putByte('b'))
				.willAnswer(putByte('c'))
				.willReturn(-1);

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
					assertThat(DataBufferTestUtils.dumpString(dataBuffer, StandardCharsets.UTF_8)).isEqualTo("foobarbaz");
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

	@Test
	public void joinCanceled() {
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

	@Test
	public void matcher() {
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

	@Test
	public void matcher2() {
		DataBuffer foo = stringBuffer("fooobar");

		byte[] delims = "oo".getBytes(StandardCharsets.UTF_8);
		DataBufferUtils.Matcher matcher = DataBufferUtils.matcher(delims);
		int result = matcher.match(foo);
		assertThat(result).isEqualTo(2);
		foo.readPosition(2);
		result = matcher.match(foo);
		assertThat(result).isEqualTo(3);
		foo.readPosition(3);
		result = matcher.match(foo);
		assertThat(result).isEqualTo(-1);

		release(foo);
	}


	private static class ZeroDemandSubscriber extends BaseSubscriber<DataBuffer> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			// Just subscribe without requesting
		}
	}

}

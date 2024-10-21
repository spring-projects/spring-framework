/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.client;

import org.junit.jupiter.api.Test;
import org.reactivestreams.FlowAdapters;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Arjen Poutsma
 * @author Oleh Dokuka
 */
class InputStreamSubscriberTests {

	private static final byte[] FOO = "foo".getBytes(StandardCharsets.UTF_8);

	private static final byte[] BAR = "bar".getBytes(StandardCharsets.UTF_8);

	private static final byte[] BAZ = "baz".getBytes(StandardCharsets.UTF_8);


	private final Executor executor = Executors.newSingleThreadExecutor();

	private final OutputStreamPublisher.ByteMapper<byte[]> byteMapper =
			new OutputStreamPublisher.ByteMapper<>() {
				@Override
				public byte[] map(int b) {
					return new byte[]{(byte) b};
				}

				@Override
				public byte[] map(byte[] b, int off, int len) {
					byte[] result = new byte[len];
					System.arraycopy(b, off, result, 0, len);
					return result;
				}
			};


	@Test
	void basic() {
		Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			outputStream.write(FOO);
			outputStream.write(BAR);
			outputStream.write(BAZ);
		}, this.byteMapper, this.executor);
		Flux<String> flux = toString(flowPublisher);

		StepVerifier.create(flux)
				.assertNext(s -> assertThat(s).isEqualTo("foobarbaz"))
				.verifyComplete();
	}

	@Test
	void flush() {
		Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			outputStream.write(FOO);
			outputStream.flush();
			outputStream.write(BAR);
			outputStream.flush();
			outputStream.write(BAZ);
			outputStream.flush();
		}, this.byteMapper, this.executor);
		Flux<String> flux = toString(flowPublisher);

		try (InputStream is = InputStreamSubscriber.subscribeTo(FlowAdapters.toFlowPublisher(flux), (s) -> s.getBytes(StandardCharsets.UTF_8), (ignore) -> {}, 1)) {
			byte[] chunk = new byte[3];

			assertThat(is.read(chunk)).isEqualTo(3);
			assertThat(chunk).containsExactly(FOO);
			assertThat(is.read(chunk)).isEqualTo(3);
			assertThat(chunk).containsExactly(BAR);
			assertThat(is.read(chunk)).isEqualTo(3);
			assertThat(chunk).containsExactly(BAZ);
			assertThat(is.read(chunk)).isEqualTo(-1);
		}
		catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	@Test
	void chunkSize() {
		Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			outputStream.write(FOO);
			outputStream.write(BAR);
			outputStream.write(BAZ);
		}, this.byteMapper, this.executor, 2);
		Flux<String> flux = toString(flowPublisher);

		try (InputStream is = InputStreamSubscriber.subscribeTo(FlowAdapters.toFlowPublisher(flux), (s) -> s.getBytes(StandardCharsets.UTF_8), (ignore) -> {}, 1)) {
			StringBuilder stringBuilder = new StringBuilder();
			byte[] chunk = new byte[3];


			stringBuilder
					.append(new String(new byte[]{(byte)is.read()}, StandardCharsets.UTF_8));
			assertThat(is.read(chunk)).isEqualTo(3);
			stringBuilder
					.append(new String(chunk, StandardCharsets.UTF_8));
			assertThat(is.read(chunk)).isEqualTo(3);
			stringBuilder
					.append(new String(chunk, StandardCharsets.UTF_8));
			assertThat(is.read(chunk)).isEqualTo(2);
			stringBuilder
					.append(new String(chunk,0, 2, StandardCharsets.UTF_8));
			assertThat(is.read()).isEqualTo(-1);

			assertThat(stringBuilder.toString()).isEqualTo("foobarbaz");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void cancel() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);

		Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			assertThatIOException()
					.isThrownBy(() -> {
						outputStream.write(FOO);
						outputStream.flush();
						outputStream.write(BAR);
						outputStream.flush();
						outputStream.write(BAZ);
						outputStream.flush();
					})
					.withMessage("Subscription has been terminated");
			latch.countDown();

		}, this.byteMapper, this.executor);
		Flux<String> flux = toString(flowPublisher);
		List<String> discarded = new ArrayList<>();

		try (InputStream is = InputStreamSubscriber.subscribeTo(FlowAdapters.toFlowPublisher(flux), (s) -> s.getBytes(StandardCharsets.UTF_8), discarded::add, 1)) {
			byte[] chunk = new byte[3];

			assertThat(is.read(chunk)).isEqualTo(3);
			assertThat(chunk).containsExactly(FOO);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		latch.await();

		assertThat(discarded).containsExactly("bar");
	}

	@Test
	void closed() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);

		Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
			writer.write("foo");
			writer.close();
			assertThatIOException().isThrownBy(() -> writer.write("bar"))
					.withMessage("Stream closed");
			latch.countDown();
		}, this.byteMapper, this.executor);
		Flux<String> flux = toString(flowPublisher);

		try (InputStream is = InputStreamSubscriber.subscribeTo(FlowAdapters.toFlowPublisher(flux), (s) -> s.getBytes(StandardCharsets.UTF_8), ig -> {}, 1)) {
			byte[] chunk = new byte[3];

			assertThat(is.read(chunk)).isEqualTo(3);
			assertThat(chunk).containsExactly(FOO);

			assertThat(is.read(chunk)).isEqualTo(-1);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		latch.await();
	}

	@Test
	void mapperThrowsException() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);

		Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
				outputStream.write(FOO);
				outputStream.flush();
				assertThatIOException().isThrownBy(() -> {
					outputStream.write(BAR);
					outputStream.flush();
				}).withMessage("Subscription has been terminated");
				latch.countDown();
		}, this.byteMapper, this.executor);
		Throwable ex = null;

		StringBuilder stringBuilder = new StringBuilder();
		try (InputStream is = InputStreamSubscriber.subscribeTo(flowPublisher, (s) -> {
			throw new NullPointerException("boom");
		}, ig -> {}, 1)) {
			byte[] chunk = new byte[3];

			stringBuilder
					.append(new String(new byte[]{(byte)is.read()}, StandardCharsets.UTF_8));
			assertThat(is.read(chunk)).isEqualTo(3);
			stringBuilder
					.append(new String(chunk, StandardCharsets.UTF_8));
			assertThat(is.read(chunk)).isEqualTo(3);
			stringBuilder
					.append(new String(chunk, StandardCharsets.UTF_8));
			assertThat(is.read(chunk)).isEqualTo(2);
			stringBuilder
					.append(new String(chunk,0, 2, StandardCharsets.UTF_8));
			assertThat(is.read()).isEqualTo(-1);
		}
		catch (Throwable e) {
			ex = e;
        }

        latch.await();

		assertThat(stringBuilder.toString()).isEqualTo("");
		assertThat(ex).hasMessage("boom");
	}

	private static Flux<String> toString(Flow.Publisher<byte[]> flowPublisher) {
		return Flux.from(FlowAdapters.toPublisher(flowPublisher))
				.map(bytes -> new String(bytes, StandardCharsets.UTF_8));
	}

}

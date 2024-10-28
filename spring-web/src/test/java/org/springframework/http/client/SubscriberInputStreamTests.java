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

package org.springframework.http.client;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.Test;
import org.reactivestreams.FlowAdapters;
import reactor.test.StepVerifier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Unit tests for {@link SubscriberInputStream}.
 *
 * @author Arjen Poutsma
 * @author Oleh Dokuka
 */
class SubscriberInputStreamTests {

	private static final byte[] FOO = "foo".getBytes(UTF_8);

	private static final byte[] BAR = "bar".getBytes(UTF_8);

	private static final byte[] BAZ = "baz".getBytes(UTF_8);


	private final Executor executor = Executors.newSingleThreadExecutor();

	private final OutputStreamPublisher.ByteMapper<byte[]> byteMapper =
			new OutputStreamPublisher.ByteMapper<>() {

				@Override
				public byte[] map(int b) {
					return new byte[] {(byte) b};
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
		Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
				out -> {
					out.write(FOO);
					out.write(BAR);
					out.write(BAZ);
				},
				this.byteMapper, this.executor, null);

		StepVerifier.create(FlowAdapters.toPublisher(publisher))
				.assertNext(s -> assertThat(s).containsExactly("foobarbaz".getBytes(UTF_8)))
				.verifyComplete();
	}

	@Test
	void flush() throws IOException {
		Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
				out -> {
					out.write(FOO);
					out.flush();
					out.write(BAR);
					out.flush();
					out.write(BAZ);
					out.flush();
				},
				this.byteMapper, this.executor, null);


		try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, s -> {}, 1)) {
			publisher.subscribe(is);

			byte[] chunk = new byte[3];

			assertThat(is.read(chunk)).isEqualTo(3);
			assertThat(chunk).containsExactly(FOO);

			assertThat(is.read(chunk)).isEqualTo(3);
			assertThat(chunk).containsExactly(BAR);

			assertThat(is.read(chunk)).isEqualTo(3);
			assertThat(chunk).containsExactly(BAZ);

			assertThat(is.read(chunk)).isEqualTo(-1);
		}
	}

	@Test
	void chunkSize() {
		Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
				out -> {
					out.write(FOO);
					out.write(BAR);
					out.write(BAZ);
				},
				this.byteMapper, this.executor, 2);

		try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, s -> {}, 1)) {
			publisher.subscribe(is);

			StringBuilder stringBuilder = new StringBuilder();
			byte[] chunk = new byte[3];

			stringBuilder.append(new String(new byte[]{(byte)is.read()}, UTF_8));
			assertThat(is.read(chunk)).isEqualTo(3);

			stringBuilder.append(new String(chunk, UTF_8));
			assertThat(is.read(chunk)).isEqualTo(3);

			stringBuilder.append(new String(chunk, UTF_8));
			assertThat(is.read(chunk)).isEqualTo(2);

			stringBuilder.append(new String(chunk,0, 2, UTF_8));
			assertThat(is.read()).isEqualTo(-1);
			assertThat(stringBuilder.toString()).isEqualTo("foobarbaz");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void cancel() throws InterruptedException, IOException {
		CountDownLatch latch = new CountDownLatch(1);

		Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
				out -> {
					assertThatIOException().isThrownBy(() -> {
								out.write(FOO);
								out.flush();
								out.write(BAR);
								out.flush();
								out.write(BAZ);
								out.flush();
							}).withMessage("Subscription has been terminated");
					latch.countDown();

				}, this.byteMapper, this.executor, null);

		List<byte[]> discarded = new ArrayList<>();

		try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, discarded::add, 1)) {
			publisher.subscribe(is);
			byte[] chunk = new byte[3];

			assertThat(is.read(chunk)).isEqualTo(3);
			assertThat(chunk).containsExactly(FOO);
		}

		latch.await();

		assertThat(discarded).containsExactly("bar".getBytes(UTF_8));
	}

	@Test
	void closed() throws InterruptedException, IOException {
		CountDownLatch latch = new CountDownLatch(1);

		Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
				out -> {
					OutputStreamWriter writer = new OutputStreamWriter(out, UTF_8);
					writer.write("foo");
					writer.close();
					assertThatIOException().isThrownBy(() -> writer.write("bar")).withMessage("Stream closed");
					latch.countDown();
				},
				this.byteMapper, this.executor, null);

		try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(s -> s, s -> {}, 1)) {
			publisher.subscribe(is);
			byte[] chunk = new byte[3];

			assertThat(is.read(chunk)).isEqualTo(3);
			assertThat(chunk).containsExactly(FOO);
			assertThat(is.read(chunk)).isEqualTo(-1);
		}

		latch.await();
	}

	@Test
	void mapperThrowsException() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);

		Flow.Publisher<byte[]> publisher = new OutputStreamPublisher<>(
				out -> {
					out.write(FOO);
					out.flush();
					assertThatIOException().isThrownBy(() -> {
								out.write(BAR);
								out.flush();
							})
							.withMessage("Subscription has been terminated");
					latch.countDown();
				},
				this.byteMapper, this.executor, null);

		Throwable savedEx = null;

		StringBuilder sb = new StringBuilder();
		try (SubscriberInputStream<byte[]> is = new SubscriberInputStream<>(
				s -> { throw new NullPointerException("boom"); }, s -> {}, 1)) {

			publisher.subscribe(is);
			sb.append(new String(new byte[] {(byte) is.read()}, UTF_8));
		}
		catch (Throwable ex) {
			savedEx = ex;
		}

		latch.await();

		assertThat(sb.toString()).isEqualTo("");
		assertThat(savedEx).hasMessage("boom");
	}

}

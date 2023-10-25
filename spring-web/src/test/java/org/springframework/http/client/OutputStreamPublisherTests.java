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

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.Test;
import org.reactivestreams.FlowAdapters;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Arjen Poutsma
 * @author Oleh Dokuka
 */
class OutputStreamPublisherTests {

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

		StepVerifier.create(flux)
				.assertNext(s -> assertThat(s).isEqualTo("foo"))
				.assertNext(s -> assertThat(s).isEqualTo("bar"))
				.assertNext(s -> assertThat(s).isEqualTo("baz"))
				.verifyComplete();
	}

	@Test
	void chunkSize() {
		Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			outputStream.write(FOO);
			outputStream.write(BAR);
			outputStream.write(BAZ);
		}, this.byteMapper, this.executor, 3);
		Flux<String> flux = toString(flowPublisher);

		StepVerifier.create(flux)
				.assertNext(s -> assertThat(s).isEqualTo("foo"))
				.assertNext(s -> assertThat(s).isEqualTo("bar"))
				.assertNext(s -> assertThat(s).isEqualTo("baz"))
				.verifyComplete();
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
					})
					.withMessage("Subscription has been terminated");
			latch.countDown();

		}, this.byteMapper, this.executor);
		Flux<String> flux = toString(flowPublisher);

		StepVerifier.create(flux, 1)
				.assertNext(s -> assertThat(s).isEqualTo("foo"))
				.thenCancel()
				.verify();

		latch.await();
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

		StepVerifier.create(flux)
				.assertNext(s -> assertThat(s).isEqualTo("foo"))
				.verifyComplete();

		latch.await();
	}

	@Test
	void negativeRequestN() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);

		Flow.Publisher<byte[]> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			try (outputStream) {
				outputStream.write(FOO);
				outputStream.flush();
				outputStream.write(BAR);
				outputStream.flush();
			}
			finally {
				latch.countDown();
			}
		}, this.byteMapper, this.executor);
		Flow.Subscription[] subscriptions = new Flow.Subscription[1];
		Flux<String> flux = toString(a-> flowPublisher.subscribe(new Flow.Subscriber<>() {
			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscriptions[0] = subscription;
				a.onSubscribe(subscription);
			}

			@Override
			public void onNext(byte[] item) {
				a.onNext(item);
			}

			@Override
			public void onError(Throwable throwable) {
				a.onError(throwable);
			}

			@Override
			public void onComplete() {
				a.onComplete();
			}
		}));

		StepVerifier.create(flux, 1)
				.assertNext(s -> assertThat(s).isEqualTo("foo"))
				.then(() -> subscriptions[0].request(-1))
				.expectErrorMessage("request should be a positive number")
				.verify();

		latch.await();
	}

	private static Flux<String> toString(Flow.Publisher<byte[]> flowPublisher) {
		return Flux.from(FlowAdapters.toPublisher(flowPublisher))
				.map(bytes -> new String(bytes, StandardCharsets.UTF_8));
	}

}

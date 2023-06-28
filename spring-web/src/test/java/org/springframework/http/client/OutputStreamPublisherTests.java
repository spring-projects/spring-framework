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
import java.io.Writer;
import java.nio.ByteBuffer;
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

	private final Executor executor = Executors.newSingleThreadExecutor();

	@Test
	void basic() {
		Flow.Publisher<ByteBuffer> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
				writer.write("foo");
				writer.write("bar");
				writer.write("baz");
			}
		}, this.executor);
		Flux<String> flux = toString(flowPublisher);

		StepVerifier.create(flux)
				.assertNext(s -> assertThat(s).isEqualTo("foobarbaz"))
				.verifyComplete();
	}

	@Test
	void flush() {
		Flow.Publisher<ByteBuffer> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
				writer.write("foo");
				writer.flush();
				writer.write("bar");
				writer.flush();
				writer.write("baz");
				writer.flush();
			}
		}, this.executor);
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

		Flow.Publisher<ByteBuffer> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
				assertThatIOException()
						.isThrownBy(() -> {
							writer.write("foo");
							writer.flush();
							writer.write("bar");
							writer.flush();
						})
						.withMessage("Subscription has been terminated");
				latch.countDown();
			}
		}, this.executor);
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

		Flow.Publisher<ByteBuffer> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
			writer.write("foo");
			writer.close();
			assertThatIOException().isThrownBy(() -> writer.write("bar"))
					.withMessage("Stream closed");
			latch.countDown();
		}, this.executor);
		Flux<String> flux = toString(flowPublisher);

		StepVerifier.create(flux)
				.assertNext(s -> assertThat(s).isEqualTo("foo"))
				.verifyComplete();

		latch.await();
	}

	@Test
	void negativeRequestN() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);

		Flow.Publisher<ByteBuffer> flowPublisher = OutputStreamPublisher.create(outputStream -> {
			try(Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
				writer.write("foo");
				writer.flush();
				writer.write("foo");
				writer.flush();
		}
			finally {
				latch.countDown();
			}
		}, this.executor);
		Flow.Subscription[] subscriptions = new Flow.Subscription[1];
		Flux<String> flux = toString(a-> flowPublisher.subscribe(new Flow.Subscriber<>() {
			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscriptions[0] = subscription;
				a.onSubscribe(subscription);
			}

			@Override
			public void onNext(ByteBuffer item) {
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

	private static Flux<String> toString(Flow.Publisher<ByteBuffer> flowPublisher) {
		return Flux.from(FlowAdapters.toPublisher(flowPublisher))
				.map(bb -> StandardCharsets.UTF_8.decode(bb).toString());
	}

}

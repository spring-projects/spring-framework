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

import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OutputStreamPublisher}.
 *
 * @author Arjen Poutsma
 * @author Oleh Dokuka
 */
class OutputStreamPublisherTests {

	private static final byte[] FOO = "foo".getBytes(StandardCharsets.UTF_8);

	private static final byte[] BAR = "bar".getBytes(StandardCharsets.UTF_8);

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

	@Test // gh-37621
	void interruptWhileAwaitingDemand() throws InterruptedException {
		CountDownLatch done = new CountDownLatch(1);
		AtomicReference<Thread> writer = new AtomicReference<>();
		AtomicReference<Throwable> savedEx = new AtomicReference<>();
		AtomicBoolean interruptStatus = new AtomicBoolean();

		ExecutorService executor = Executors.newSingleThreadExecutor();
		OutputStreamPublisher<byte[]> publisher = new OutputStreamPublisher<>(outputStream -> {
			writer.set(Thread.currentThread());
			try {
				outputStream.write(FOO);
				outputStream.flush();
				outputStream.write(BAR);
				outputStream.flush();
			}
			catch (Throwable ex) {
				savedEx.set(ex);
				interruptStatus.set(Thread.currentThread().isInterrupted());
				throw ex;
			}
			finally {
				done.countDown();
			}
		}, this.byteMapper, executor, null);

		publisher.subscribe(new Subscriber<>() {
			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(1);
			}

			@Override
			public void onNext(byte[] item) {
			}

			@Override
			public void onError(Throwable throwable) {
			}

			@Override
			public void onComplete() {
			}
		});

		for (int i = 0; i < 100 && (writer.get() == null || writer.get().getState() != Thread.State.WAITING); i++) {
			Thread.sleep(20);
		}
		assertThat(writer.get()).isNotNull();
		assertThat(writer.get().getState()).isEqualTo(Thread.State.WAITING);

		executor.shutdownNow();
		assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(savedEx.get()).isInstanceOf(InterruptedIOException.class)
				.hasMessage("Interrupted while awaiting demand");
		assertThat(interruptStatus).as("interrupt status restored").isTrue();
	}

}

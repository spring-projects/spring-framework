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
package org.springframework.util.concurrent;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MonoToListenableFutureAdapter}.
 * @author Rossen Stoyanchev
 */
class MonoToListenableFutureAdapterTests {

	@Test
	void success() {
		String expected = "one";
		AtomicReference<Object> actual = new AtomicReference<>();
		ListenableFuture<String> future = new MonoToListenableFutureAdapter<>(Mono.just(expected));
		future.addCallback(actual::set, actual::set);

		assertThat(actual.get()).isEqualTo(expected);
	}

	@Test
	void failure() {
		Throwable expected = new IllegalStateException("oops");
		AtomicReference<Object> actual = new AtomicReference<>();
		ListenableFuture<String> future = new MonoToListenableFutureAdapter<>(Mono.error(expected));
		future.addCallback(actual::set, actual::set);

		assertThat(actual.get()).isEqualTo(expected);
	}

	@Test
	void cancellation() {
		Mono<Long> mono = Mono.delay(Duration.ofSeconds(60));
		Future<Long> future = new MonoToListenableFutureAdapter<>(mono);

		assertThat(future.cancel(true)).isTrue();
		assertThat(future.isCancelled()).isTrue();
	}

	@Test
	void cancellationAfterTerminated() {
		Future<Void> future = new MonoToListenableFutureAdapter<>(Mono.empty());

		assertThat(future.cancel(true)).as("Should return false if task already completed").isFalse();
		assertThat(future.isCancelled()).isFalse();
	}

}

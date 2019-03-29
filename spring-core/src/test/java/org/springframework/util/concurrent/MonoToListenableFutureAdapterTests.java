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
package org.springframework.util.concurrent;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import reactor.core.publisher.Mono;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MonoToListenableFutureAdapter}.
 * @author Rossen Stoyanchev
 */
public class MonoToListenableFutureAdapterTests {

	@Test
	public void success() {
		String expected = "one";
		AtomicReference<Object> actual = new AtomicReference<>();
		ListenableFuture<String> future = new MonoToListenableFutureAdapter<>(Mono.just(expected));
		future.addCallback(actual::set, actual::set);

		assertEquals(expected, actual.get());
	}

	@Test
	public void failure() {
		Throwable expected = new IllegalStateException("oops");
		AtomicReference<Object> actual = new AtomicReference<>();
		ListenableFuture<String> future = new MonoToListenableFutureAdapter<>(Mono.error(expected));
		future.addCallback(actual::set, actual::set);

		assertEquals(expected, actual.get());
	}

	@Test
	public void cancellation() {
		Mono<Long> mono = Mono.delay(Duration.ofSeconds(60));
		Future<Long> future = new MonoToListenableFutureAdapter<>(mono);

		assertTrue(future.cancel(true));
		assertTrue(future.isCancelled());
	}

	@Test
	public void cancellationAfterTerminated() {
		Future<Void> future = new MonoToListenableFutureAdapter<>(Mono.empty());

		assertFalse("Should return false if task already completed", future.cancel(true));
		assertFalse(future.isCancelled());
	}

}

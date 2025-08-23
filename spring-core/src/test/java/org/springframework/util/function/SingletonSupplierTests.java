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

package org.springframework.util.function;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SingletonSupplier}.
 *
 * @author Dmytro Nosan
 */
class SingletonSupplierTests {

	@Test
	void shouldReturnDefaultWhenInstanceSupplierReturnsNull() {
		SingletonSupplier<String> singletonSupplier = new SingletonSupplier<>(() -> null, () -> "Default");
		assertThat(singletonSupplier.get()).isEqualTo("Default");
	}

	@Test
	void shouldReturnNullForOfNullableWithNullInstance() {
		SingletonSupplier<String> singletonSupplier = SingletonSupplier.ofNullable((String) null);
		assertThat(singletonSupplier).isNull();
	}

	@Test
	void shouldReturnNullForOfNullableWithNullSupplier() {
		SingletonSupplier<String> singletonSupplier = SingletonSupplier.ofNullable((Supplier<String>) null);
		assertThat(singletonSupplier).isNull();
	}

	@Test
	void shouldReturnNullWhenAllSuppliersReturnNull() {
		SingletonSupplier<String> singletonSupplier = new SingletonSupplier<>(() -> null, () -> null);
		assertThat(singletonSupplier.get()).isNull();
	}

	@Test
	void shouldReturnNullWhenNoInstanceOrDefaultSupplier() {
		SingletonSupplier<String> singletonSupplier = new SingletonSupplier<>((String) null, null);
		assertThat(singletonSupplier.get()).isNull();
	}

	@Test
	void shouldReturnSingletonInstanceOnMultipleCalls() {
		SingletonSupplier<String> singletonSupplier = SingletonSupplier.of("Hello");
		assertThat(singletonSupplier.get()).isEqualTo("Hello");
		assertThat(singletonSupplier.get()).isEqualTo("Hello");
	}


	@Test
	void shouldReturnSingletonInstanceOnMultipleSupplierCalls() {
		SingletonSupplier<String> singletonSupplier = SingletonSupplier.of(new HelloStringSupplier());
		assertThat(singletonSupplier.get()).isEqualTo("Hello 0");
		assertThat(singletonSupplier.get()).isEqualTo("Hello 0");
	}

	@Test
	void shouldReturnSupplierForOfNullableWithNonNullInstance() {
		SingletonSupplier<String> singletonSupplier = SingletonSupplier.ofNullable("Hello");
		assertThat(singletonSupplier).isNotNull();
		assertThat(singletonSupplier.get()).isEqualTo("Hello");
	}

	@Test
	void shouldReturnSupplierForOfNullableWithNonNullSupplier() {
		SingletonSupplier<String> singletonSupplier = SingletonSupplier.ofNullable(() -> "Hello");
		assertThat(singletonSupplier).isNotNull();
		assertThat(singletonSupplier.get()).isEqualTo("Hello");
	}

	@Test
	void shouldThrowWhenObtainCalledAndNoInstanceAvailable() {
		SingletonSupplier<String> singletonSupplier = new SingletonSupplier<>((String) null, null);
		assertThatThrownBy(singletonSupplier::obtain).isInstanceOf(IllegalStateException.class)
				.hasMessage("No instance from Supplier");
	}

	@Test
	void shouldUseDefaultSupplierWhenInstanceIsNull() {
		SingletonSupplier<String> singletonSupplier = new SingletonSupplier<>((String) null, () -> "defaultSupplier");
		assertThat(singletonSupplier.get()).isEqualTo("defaultSupplier");
	}

	@Test
	void shouldUseDefaultSupplierWhenInstanceSupplierReturnsNull() {
		SingletonSupplier<String> singletonSupplier = new SingletonSupplier<>((Supplier<String>) null, () -> "defaultSupplier");
		assertThat(singletonSupplier.get()).isEqualTo("defaultSupplier");
	}

	@Test
	void shouldUseInstanceSupplierWhenProvidedAndIgnoreDefaultSupplier() {
		AtomicInteger defaultValue = new AtomicInteger();
		SingletonSupplier<Integer> singletonSupplier = new SingletonSupplier<>(() -> -1, defaultValue::incrementAndGet);
		assertThat(singletonSupplier.get()).isEqualTo(-1);
		assertThat(defaultValue.get()).isEqualTo(0);
	}

	@Test
	void shouldUseInstanceWhenProvidedAndIgnoreDefaultSupplier() {
		AtomicInteger defaultValue = new AtomicInteger();
		SingletonSupplier<Integer> singletonSupplier = new SingletonSupplier<>(-1, defaultValue::incrementAndGet);
		assertThat(singletonSupplier.get()).isEqualTo(-1);
		assertThat(defaultValue.get()).isEqualTo(0);
	}

	@Test
	void shouldReturnConsistentlyNullSingletonInstanceOnMultipleSupplierCalls() {
		SingletonSupplier<String> singletonSupplier = SingletonSupplier.of(new Supplier<>() {

			int count = 0;

			@Override
			public String get() {
				if (this.count++ == 0) {
					return null;
				}
				return "Hello";
			}
		});

		assertThat(singletonSupplier.get()).isNull();
		assertThat(singletonSupplier.get()).isNull();
	}

	@RepeatedTest(100)
	void shouldReturnSingletonInstanceOnMultipleConcurrentSupplierCalls() throws Exception {
		int numberOfThreads = 4;
		CountDownLatch ready = new CountDownLatch(numberOfThreads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<String>> futures = new ArrayList<>();
		SingletonSupplier<String> singletonSupplier = SingletonSupplier.of(new HelloStringSupplier());
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
		try {
			for (int i = 0; i < numberOfThreads; i++) {
				futures.add(executorService.submit(() -> {
					ready.countDown();
					start.await();
					return singletonSupplier.obtain();
				}));
			}
			ready.await();
			start.countDown();
			assertThat(futures).extracting(Future::get).containsOnly("Hello 0");
		}
		finally {
			executorService.shutdown();
		}
	}


	private static final class HelloStringSupplier implements Supplier<String> {

		private final AtomicInteger count = new AtomicInteger();

		@Override
		public String get() {
			return "Hello " + this.count.getAndIncrement();
		}
	}

}

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SingletonSupplier}.
 *
 * @author Dmytro Nosan
 * @author Sam Brannen
 * @since 7.0
 */
class SingletonSupplierTests {

	@Test
	void shouldReturnDefaultWhenInstanceSupplierReturnsNull() {
		var singletonSupplier = new SingletonSupplier<>(() -> null, () -> "Default");

		assertThat(singletonSupplier.get()).isEqualTo("Default");
	}

	@Test
	void shouldReturnNullForOfNullableWithNullInstance() {
		var singletonSupplier = SingletonSupplier.ofNullable((String) null);

		assertThat(singletonSupplier).isNull();
	}

	@Test
	void shouldReturnNullForOfNullableWithNullSupplier() {
		var singletonSupplier = SingletonSupplier.ofNullable((Supplier<String>) null);

		assertThat(singletonSupplier).isNull();
	}

	@Test
	void shouldReturnNullWhenAllSuppliersReturnNull() {
		var singletonSupplier = new SingletonSupplier<>(() -> null, () -> null);

		assertThat(singletonSupplier.get()).isNull();
	}

	@Test
	void shouldReturnNullWhenNoInstanceOrDefaultSupplier() {
		var singletonSupplier = new SingletonSupplier<>((String) null, null);

		assertThat(singletonSupplier.get()).isNull();
	}

	@Test
	void shouldReturnSingletonInstanceOnMultipleCalls() {
		var singletonSupplier = SingletonSupplier.of("Hello");

		assertThat(singletonSupplier.get()).isEqualTo("Hello");
		assertThat(singletonSupplier.get()).isEqualTo("Hello");
	}

	@Test
	void shouldReturnSingletonInstanceOnMultipleSupplierCalls() {
		var singletonSupplier = SingletonSupplier.of(new HelloStringSupplier());

		assertThat(singletonSupplier.get()).isEqualTo("Hello 0");
		assertThat(singletonSupplier.get()).isEqualTo("Hello 0");
	}

	@Test
	void shouldReturnSupplierForOfNullableWithNonNullInstance() {
		var singletonSupplier = SingletonSupplier.ofNullable("Hello");

		assertThat(singletonSupplier).isNotNull();
		assertThat(singletonSupplier.get()).isEqualTo("Hello");
	}

	@Test
	void shouldReturnSupplierForOfNullableWithNonNullSupplier() {
		var singletonSupplier = SingletonSupplier.ofNullable(() -> "Hello");

		assertThat(singletonSupplier).isNotNull();
		assertThat(singletonSupplier.get()).isEqualTo("Hello");
	}

	@Test
	void shouldThrowWhenObtainCalledAndNoInstanceAvailable() {
		var singletonSupplier = new SingletonSupplier<>((String) null, null);

		assertThatIllegalStateException()
				.isThrownBy(singletonSupplier::obtain)
				.withMessage("No instance from Supplier");
	}

	@Test
	void shouldUseDefaultSupplierWhenInstanceIsNull() {
		var singletonSupplier = new SingletonSupplier<>((String) null, () -> "defaultSupplier");

		assertThat(singletonSupplier.get()).isEqualTo("defaultSupplier");
	}

	@Test
	void shouldUseDefaultSupplierWhenInstanceSupplierReturnsNull() {
		var singletonSupplier = new SingletonSupplier<>((Supplier<String>) null, () -> "defaultSupplier");

		assertThat(singletonSupplier.get()).isEqualTo("defaultSupplier");
	}

	@Test
	void shouldUseInstanceSupplierWhenProvidedAndIgnoreDefaultSupplier() {
		var defaultValue = new AtomicInteger();
		var singletonSupplier = new SingletonSupplier<>(() -> -1, defaultValue::incrementAndGet);

		assertThat(singletonSupplier.get()).isEqualTo(-1);
		assertThat(defaultValue.get()).isZero();
	}

	@Test
	void shouldUseInstanceWhenProvidedAndIgnoreDefaultSupplier() {
		var defaultValue = new AtomicInteger();
		var singletonSupplier = new SingletonSupplier<>(-1, defaultValue::incrementAndGet);

		assertThat(singletonSupplier.get()).isEqualTo(-1);
		assertThat(defaultValue.get()).isZero();
	}

	@Test
	void shouldReturnConsistentlyNullSingletonInstanceOnMultipleSupplierCalls() {
		var count = new AtomicInteger();
		var singletonSupplier = SingletonSupplier.of(() -> (count.getAndIncrement() == 0 ? null : "Hello"));

		assertThat(singletonSupplier.get()).isNull();
		assertThat(singletonSupplier.get()).isNull();
	}

	@RepeatedTest(100)
	void shouldReturnSingletonInstanceOnMultipleConcurrentSupplierCalls() throws Exception {
		int numberOfThreads = 4;
		var ready = new CountDownLatch(numberOfThreads);
		var start = new CountDownLatch(1);
		var futures = new ArrayList<Future<String>>();
		var singletonSupplier = SingletonSupplier.of(new HelloStringSupplier());
		var executorService = Executors.newFixedThreadPool(numberOfThreads);

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

		final AtomicInteger count = new AtomicInteger();

		@Override
		public String get() {
			return "Hello " + this.count.getAndIncrement();
		}
	}

}

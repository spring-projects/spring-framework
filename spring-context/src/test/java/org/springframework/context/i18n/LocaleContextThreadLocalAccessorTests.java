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

package org.springframework.context.i18n;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LocaleContextThreadLocalAccessor}.
 *
 * @author Tadaya Tsuyukubo
 */
class LocaleContextThreadLocalAccessorTests {

	private final ContextRegistry registry = new ContextRegistry()
			.registerThreadLocalAccessor(new LocaleContextThreadLocalAccessor());

	@AfterEach
	void cleanUp() {
		LocaleContextHolder.resetLocaleContext();
	}

	@ParameterizedTest
	@MethodSource
	void propagation(@Nullable LocaleContext previous, LocaleContext current) throws Exception {
		LocaleContextHolder.setLocaleContext(current);
		ContextSnapshot snapshot = ContextSnapshotFactory.builder()
				.contextRegistry(this.registry)
				.clearMissing(true)
				.build()
				.captureAll();

		AtomicReference<LocaleContext> previousHolder = new AtomicReference<>();
		AtomicReference<LocaleContext> currentHolder = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);
		new Thread(() -> {
			LocaleContextHolder.setLocaleContext(previous);
			try (ContextSnapshot.Scope scope = snapshot.setThreadLocals()) {
				currentHolder.set(LocaleContextHolder.getLocaleContext());
			}
			previousHolder.set(LocaleContextHolder.getLocaleContext());
			latch.countDown();
		}).start();

		latch.await(1, TimeUnit.SECONDS);
		assertThat(previousHolder).hasValueSatisfying(value -> assertThat(value).isSameAs(previous));
		assertThat(currentHolder).hasValueSatisfying(value -> assertThat(value).isSameAs(current));
	}

	private static Stream<Arguments> propagation() {
		LocaleContext previous = new SimpleLocaleContext(Locale.ENGLISH);
		LocaleContext current = new SimpleLocaleContext(Locale.ENGLISH);
		return Stream.of(
				Arguments.of(null, current),
				Arguments.of(previous, current)
		);
	}
}

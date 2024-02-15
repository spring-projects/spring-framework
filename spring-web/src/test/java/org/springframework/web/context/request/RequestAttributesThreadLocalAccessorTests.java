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

package org.springframework.web.context.request;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshot.Scope;
import io.micrometer.context.ContextSnapshotFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RequestAttributesThreadLocalAccessor}.
 *
 * @author Tadaya Tsuyukubo
 */
class RequestAttributesThreadLocalAccessorTests {

	private final ContextRegistry registry = new ContextRegistry()
			.registerThreadLocalAccessor(new RequestAttributesThreadLocalAccessor());

	@AfterEach
	void cleanUp() {
		RequestContextHolder.resetRequestAttributes();
	}

	@ParameterizedTest
	@MethodSource
	@SuppressWarnings({ "try", "unused" })
	void propagation(@Nullable RequestAttributes previous, RequestAttributes current) throws Exception {
		RequestContextHolder.setRequestAttributes(current);
		ContextSnapshot snapshot = ContextSnapshotFactory.builder()
				.contextRegistry(this.registry)
				.clearMissing(true)
				.build()
				.captureAll();

		AtomicReference<RequestAttributes> previousHolder = new AtomicReference<>();
		AtomicReference<RequestAttributes> currentHolder = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);
		new Thread(() -> {
			RequestContextHolder.setRequestAttributes(previous);
			try (Scope scope = snapshot.setThreadLocals()) {
				currentHolder.set(RequestContextHolder.getRequestAttributes());
			}
			previousHolder.set(RequestContextHolder.getRequestAttributes());
			latch.countDown();
		}).start();

		latch.await(1, TimeUnit.SECONDS);
		assertThat(previousHolder).hasValueSatisfying(value -> assertThat(value).isSameAs(previous));
		assertThat(currentHolder).hasValueSatisfying(value -> assertThat(value).isSameAs(current));
	}

	private static Stream<Arguments> propagation() {
		RequestAttributes previous = mock(RequestAttributes.class);
		RequestAttributes current = mock(RequestAttributes.class);
		return Stream.of(
				Arguments.of(null, current),
				Arguments.of(previous, current)
		);
	}

}

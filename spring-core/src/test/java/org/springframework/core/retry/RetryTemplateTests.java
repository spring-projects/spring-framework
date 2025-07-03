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

package org.springframework.core.retry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments.ArgumentSet;
import org.junit.jupiter.params.provider.FieldSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Integration tests for {@link RetryTemplate} and {@link RetryPolicy}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 * @see RetryPolicyTests
 */
class RetryTemplateTests {

	private final RetryTemplate retryTemplate = new RetryTemplate();


	@BeforeEach
	void configureRetryTemplate() {
		var retryPolicy = RetryPolicy.builder()
				.maxAttempts(3)
				.delay(Duration.ZERO)
				.build();

		retryTemplate.setRetryPolicy(retryPolicy);
	}

	@Test
	void retryWithImmediateSuccess() throws Exception {
		AtomicInteger invocationCount = new AtomicInteger();
		Retryable<String> retryable = () -> {
			invocationCount.incrementAndGet();
			return "always succeeds";
		};

		assertThat(invocationCount).hasValue(0);
		assertThat(retryTemplate.execute(retryable)).isEqualTo("always succeeds");
		assertThat(invocationCount).hasValue(1);
	}

	@Test
	void retryWithSuccessAfterInitialFailures() throws Exception {
		AtomicInteger invocationCount = new AtomicInteger();
		Retryable<String> retryable = () -> {
			if (invocationCount.incrementAndGet() <= 2) {
				throw new Exception("Boom!");
			}
			return "finally succeeded";
		};

		assertThat(invocationCount).hasValue(0);
		assertThat(retryTemplate.execute(retryable)).isEqualTo("finally succeeded");
		assertThat(invocationCount).hasValue(3);
	}

	@Test
	void retryWithExhaustedPolicy() {
		var invocationCount = new AtomicInteger();
		var exception = new RuntimeException("Boom!");

		var retryable = new Retryable<>() {
			@Override
			public String execute() {
				invocationCount.incrementAndGet();
				throw exception;
			}

			@Override
			public String getName() {
				return "test";
			}
		};

		assertThat(invocationCount).hasValue(0);
		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessage("Retry policy for operation 'test' exhausted; aborting execution")
				.withCause(exception);
		// 4 = 1 initial invocation + 3 retry attempts
		assertThat(invocationCount).hasValue(4);
	}

	@Test
	void retryWithFailingRetryableAndMultiplePredicates() {
		var invocationCount = new AtomicInteger();
		var exception = new NumberFormatException("Boom!");

		var retryable = new Retryable<>() {
			@Override
			public String execute() {
				invocationCount.incrementAndGet();
				throw exception;
			}

			@Override
			public String getName() {
				return "always fails";
			}
		};

		var retryPolicy = RetryPolicy.builder()
				.maxAttempts(5)
				.delay(Duration.ofMillis(1))
				.predicate(NumberFormatException.class::isInstance)
				.predicate(t -> t.getMessage().equals("Boom!"))
				.build();

		retryTemplate.setRetryPolicy(retryPolicy);

		assertThat(invocationCount).hasValue(0);
		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessage("Retry policy for operation 'always fails' exhausted; aborting execution")
				.withCause(exception);
		// 6 = 1 initial invocation + 5 retry attempts
		assertThat(invocationCount).hasValue(6);
	}

	@Test
	void retryWithExceptionIncludes() {
		var invocationCount = new AtomicInteger();

		var retryable = new Retryable<>() {
			@Override
			public String execute() throws Exception {
				return switch (invocationCount.incrementAndGet()) {
					case 1 -> throw new FileNotFoundException();
					case 2 -> throw new IOException();
					case 3 -> throw new IllegalStateException();
					default -> "success";
				};
			}

			@Override
			public String getName() {
				return "test";
			}
		};

		var retryPolicy = RetryPolicy.builder()
				.maxAttempts(Integer.MAX_VALUE)
				.delay(Duration.ZERO)
				.includes(IOException.class)
				.build();

		retryTemplate.setRetryPolicy(retryPolicy);

		assertThat(invocationCount).hasValue(0);
		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessage("Retry policy for operation 'test' exhausted; aborting execution")
				.withCauseExactlyInstanceOf(IllegalStateException.class)
				.satisfies(hasSuppressedExceptionsSatisfyingExactly(
						suppressed1 -> assertThat(suppressed1).isExactlyInstanceOf(FileNotFoundException.class),
						suppressed2 -> assertThat(suppressed2).isExactlyInstanceOf(IOException.class)
				));
		// 3 = 1 initial invocation + 2 retry attempts
		assertThat(invocationCount).hasValue(3);
	}

	static final List<ArgumentSet> includesAndExcludesRetryPolicies = List.of(
			argumentSet("Excludes",
						RetryPolicy.builder()
							.maxAttempts(Integer.MAX_VALUE)
							.delay(Duration.ZERO)
							.excludes(FileNotFoundException.class)
							.build()),
			argumentSet("Includes & Excludes",
						RetryPolicy.builder()
							.maxAttempts(Integer.MAX_VALUE)
							.delay(Duration.ZERO)
							.includes(IOException.class)
							.excludes(FileNotFoundException.class)
							.build())
		);

	@ParameterizedTest
	@FieldSource("includesAndExcludesRetryPolicies")
	void retryWithExceptionIncludesAndExcludes(RetryPolicy retryPolicy) {
		retryTemplate.setRetryPolicy(retryPolicy);

		var invocationCount = new AtomicInteger();

		var retryable = new Retryable<>() {
			@Override
			public String execute() throws Exception {
				return switch (invocationCount.incrementAndGet()) {
					case 1 -> throw new IOException();
					case 2 -> throw new IOException();
					case 3 -> throw new CustomFileNotFoundException();
					default -> "success";
				};
			}

			@Override
			public String getName() {
				return "test";
			}
		};

		assertThat(invocationCount).hasValue(0);
		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessage("Retry policy for operation 'test' exhausted; aborting execution")
				.withCauseExactlyInstanceOf(CustomFileNotFoundException.class)
				.satisfies(hasSuppressedExceptionsSatisfyingExactly(
					suppressed1 -> assertThat(suppressed1).isExactlyInstanceOf(IOException.class),
					suppressed2 -> assertThat(suppressed2).isExactlyInstanceOf(IOException.class)
				));
		// 3 = 1 initial invocation + 2 retry attempts
		assertThat(invocationCount).hasValue(3);
	}


	@SafeVarargs
	private static final Consumer<Throwable> hasSuppressedExceptionsSatisfyingExactly(
			ThrowingConsumer<? super Throwable>... requirements) {
		return throwable -> assertThat(throwable.getSuppressed()).satisfiesExactly(requirements);
	}


	@SuppressWarnings("serial")
	private static class CustomFileNotFoundException extends FileNotFoundException {
	}

}

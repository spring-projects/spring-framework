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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments.ArgumentSet;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.platform.commons.util.ExceptionUtils;
import org.mockito.InOrder;

import org.springframework.util.backoff.BackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Integration tests for {@link RetryTemplate}, {@link RetryPolicy} and
 * {@link RetryListener}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 7.0
 * @see RetryPolicyTests
 */
class RetryTemplateTests {

	private final RetryPolicy retryPolicy = RetryPolicy.builder().maxRetries(3).delay(Duration.ZERO).build();

	private final RetryTemplate retryTemplate = new RetryTemplate(retryPolicy);

	private final RetryListener retryListener = mock();

	private final InOrder inOrder = inOrder(retryListener);


	@BeforeEach
	void configureRetryTemplate() {
		retryTemplate.setRetryListener(retryListener);
	}

	@Test
	void checkRetryTemplateConfiguration() {
		assertThat(retryTemplate.getRetryPolicy()).isSameAs(retryPolicy);
		assertThat(retryTemplate.getRetryListener()).isSameAs(retryListener);
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

		// RetryListener interactions:
		verifyNoInteractions(retryListener);
	}

	@Test
	void retryWithInitialFailureAndZeroRetriesRetryPolicy() {
		RetryPolicy retryPolicy = throwable -> false; // Zero retries
		RetryTemplate retryTemplate = new RetryTemplate(retryPolicy);
		retryTemplate.setRetryListener(retryListener);
		Exception exception = new RuntimeException("Boom!");
		Retryable<String> retryable = () -> {
			throw exception;
		};

		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessageMatching("Retry policy for operation '.+?' exhausted; aborting execution")
				.withCause(exception)
				.satisfies(throwable -> assertThat(throwable.getSuppressed()).isEmpty())
				.satisfies(throwable -> assertThat(throwable.getRetryCount()).isZero())
				.satisfies(throwable -> inOrder.verify(retryListener).onRetryPolicyExhaustion(retryPolicy, retryable, throwable));

		verifyNoMoreInteractions(retryListener);
	}

	@Test
	void retryWithInitialFailureAndZeroRetriesFixedBackOffPolicy() {
		RetryPolicy retryPolicy = RetryPolicy.withMaxRetries(0);

		RetryTemplate retryTemplate = new RetryTemplate(retryPolicy);
		retryTemplate.setRetryListener(retryListener);
		Exception exception = new RuntimeException("Boom!");
		Retryable<String> retryable = () -> {
			throw exception;
		};

		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessageMatching("Retry policy for operation '.+?' exhausted; aborting execution")
				.withCause(exception)
				.satisfies(throwable -> assertThat(throwable.getSuppressed()).isEmpty())
				.satisfies(throwable -> assertThat(throwable.getRetryCount()).isZero())
				.satisfies(throwable -> inOrder.verify(retryListener).onRetryPolicyExhaustion(retryPolicy, retryable, throwable));

		verifyNoMoreInteractions(retryListener);
	}

	@Test
	void retryWithInitialFailureAndZeroRetriesBackOffPolicyFromBuilder() {
		RetryPolicy retryPolicy = RetryPolicy.builder().maxRetries(0).build();

		RetryTemplate retryTemplate = new RetryTemplate(retryPolicy);
		retryTemplate.setRetryListener(retryListener);
		Exception exception = new RuntimeException("Boom!");
		Retryable<String> retryable = () -> {
			throw exception;
		};

		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessageMatching("Retry policy for operation '.+?' exhausted; aborting execution")
				.withCause(exception)
				.satisfies(throwable -> assertThat(throwable.getSuppressed()).isEmpty())
				.satisfies(throwable -> assertThat(throwable.getRetryCount()).isZero())
				.satisfies(throwable -> inOrder.verify(retryListener).onRetryPolicyExhaustion(retryPolicy, retryable, throwable));

		verifyNoMoreInteractions(retryListener);
	}

	@Test
	void retryWithSuccessAfterInitialFailures() throws Exception {
		AtomicInteger invocationCount = new AtomicInteger();
		Retryable<String> retryable = () -> {
			if (invocationCount.incrementAndGet() <= 2) {
				throw new CustomException("Boom " + invocationCount.get());
			}
			return "finally succeeded";
		};

		assertThat(invocationCount).hasValue(0);
		assertThat(retryTemplate.execute(retryable)).isEqualTo("finally succeeded");
		assertThat(invocationCount).hasValue(3);

		// RetryListener interactions:
		inOrder.verify(retryListener).beforeRetry(retryPolicy, retryable);
		inOrder.verify(retryListener).onRetryFailure(retryPolicy, retryable, new CustomException("Boom 2"));
		inOrder.verify(retryListener).beforeRetry(retryPolicy, retryable);
		inOrder.verify(retryListener).onRetrySuccess(retryPolicy, retryable, "finally succeeded");
		verifyNoMoreInteractions(retryListener);
	}

	@Test
	void retryWithExhaustedPolicy() {
		var invocationCount = new AtomicInteger();

		var retryable = new Retryable<>() {
			@Override
			public String execute() {
				throw new CustomException("Boom " + invocationCount.incrementAndGet());
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
				.withCause(new CustomException("Boom 4"))
				.satisfies(throwable -> {
					invocationCount.set(1);
					repeat(3, () -> {
						inOrder.verify(retryListener).beforeRetry(retryPolicy, retryable);
						inOrder.verify(retryListener).onRetryFailure(retryPolicy, retryable,
								new CustomException("Boom " + invocationCount.incrementAndGet()));
					});
					inOrder.verify(retryListener).onRetryPolicyExhaustion(retryPolicy, retryable, throwable);
				});
		// 4 = 1 initial invocation + 3 retry attempts
		assertThat(invocationCount).hasValue(4);

		verifyNoMoreInteractions(retryListener);
	}

	@Test
	void retryWithInterruptionDuringSleep() {
		Exception exception = new RuntimeException("Boom!");
		InterruptedException interruptedException = new InterruptedException();

		// Simulates interruption during sleep:
		BackOff backOff = () -> () -> {
			throw ExceptionUtils.throwAsUncheckedException(interruptedException);
		};

		RetryPolicy retryPolicy = RetryPolicy.builder().backOff(backOff).build();
		RetryTemplate retryTemplate = new RetryTemplate(retryPolicy);
		retryTemplate.setRetryListener(retryListener);
		Retryable<String> retryable = () -> {
			throw exception;
		};

		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessageMatching("Unable to back off for retryable operation '.+?'")
				.withCause(interruptedException)
				.satisfies(throwable -> assertThat(throwable.getSuppressed()).containsExactly(exception))
				.satisfies(throwable -> assertThat(throwable.getRetryCount()).isZero())
				.satisfies(throwable -> inOrder.verify(retryListener).onRetryPolicyInterruption(retryPolicy, retryable, throwable));

		verifyNoMoreInteractions(retryListener);
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
				.maxRetries(5)
				.delay(Duration.ofMillis(1))
				.predicate(NumberFormatException.class::isInstance)
				.predicate(t -> t.getMessage().equals("Boom!"))
				.build();

		retryTemplate.setRetryPolicy(retryPolicy);

		assertThat(invocationCount).hasValue(0);
		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessage("Retry policy for operation 'always fails' exhausted; aborting execution")
				.withCause(exception)
				.satisfies(throwable -> {
					repeat(5, () -> {
						inOrder.verify(retryListener).beforeRetry(retryPolicy, retryable);
						inOrder.verify(retryListener).onRetryFailure(retryPolicy, retryable, exception);
					});
					inOrder.verify(retryListener).onRetryPolicyExhaustion(retryPolicy, retryable, throwable);
				});
		// 6 = 1 initial invocation + 5 retry attempts
		assertThat(invocationCount).hasValue(6);

		verifyNoMoreInteractions(retryListener);
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
				.maxRetries(Integer.MAX_VALUE)
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
				))
				.satisfies(throwable -> assertThat(throwable.getRetryCount()).isEqualTo(2))
				.satisfies(throwable -> {
					repeat(2, () -> {
						inOrder.verify(retryListener).beforeRetry(retryPolicy, retryable);
						inOrder.verify(retryListener).onRetryFailure(eq(retryPolicy), eq(retryable), any(Exception.class));
					});
					inOrder.verify(retryListener).onRetryPolicyExhaustion(retryPolicy, retryable, throwable);
				});
		// 3 = 1 initial invocation + 2 retry attempts
		assertThat(invocationCount).hasValue(3);

		verifyNoMoreInteractions(retryListener);
	}

	static final List<ArgumentSet> includesAndExcludesRetryPolicies = List.of(
			argumentSet("Excludes",
						RetryPolicy.builder()
							.maxRetries(Integer.MAX_VALUE)
							.delay(Duration.ZERO)
							.excludes(FileNotFoundException.class)
							.build()),
			argumentSet("Includes & Excludes",
						RetryPolicy.builder()
							.maxRetries(Integer.MAX_VALUE)
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
					case 2 -> throw new RuntimeException(new IOException());
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
					suppressed2 -> assertThat(suppressed2).isExactlyInstanceOf(RuntimeException.class)
							.hasCauseExactlyInstanceOf(IOException.class)
				))
				.satisfies(throwable -> assertThat(throwable.getRetryCount()).isEqualTo(2))
				.satisfies(throwable -> {
					inOrder.verify(retryListener).beforeRetry(retryPolicy, retryable);
					inOrder.verify(retryListener).onRetryFailure(eq(retryPolicy), eq(retryable), any(RuntimeException.class));
					inOrder.verify(retryListener).beforeRetry(retryPolicy, retryable);
					inOrder.verify(retryListener).onRetryFailure(eq(retryPolicy), eq(retryable), any(CustomFileNotFoundException.class));
					inOrder.verify(retryListener).onRetryPolicyExhaustion(retryPolicy, retryable, throwable);
				});
		// 3 = 1 initial invocation + 2 retry attempts
		assertThat(invocationCount).hasValue(3);

		verifyNoMoreInteractions(retryListener);
	}


	private static void repeat(int times, Runnable runnable) {
		for (int i = 0; i < times; i++) {
			runnable.run();
		}
	}

	@SafeVarargs
	private static Consumer<Throwable> hasSuppressedExceptionsSatisfyingExactly(
			ThrowingConsumer<? super Throwable>... requirements) {

		return throwable -> assertThat(throwable.getSuppressed()).satisfiesExactly(requirements);
	}


	@SuppressWarnings("serial")
	private static class CustomFileNotFoundException extends FileNotFoundException {
	}


	/**
	 * Custom {@link RuntimeException} that implements {@link #equals(Object)}
	 * and {@link #hashCode()} for use in assertions that check for equality.
	 */
	@SuppressWarnings("serial")
	private static class CustomException extends RuntimeException {

		CustomException(String message) {
			super(message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(getMessage());
		}

		@Override
		public boolean equals(Object other) {
			return (this == other ||
					(other instanceof CustomException that && getMessage().equals(that.getMessage())));
		}
	}

}

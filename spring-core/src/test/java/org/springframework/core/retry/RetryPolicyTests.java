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
import java.nio.file.FileSystemException;
import java.time.Duration;
import java.util.List;
import java.util.StringJoiner;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link RetryPolicy} and its builder.
 *
 * @author Sam Brannen
 * @since 7.0
 * @see RetryTemplateTests
 */
class RetryPolicyTests {

	@Nested
	class FactoryMethodTests {

		@Test
		void withDefaults() {
			var policy = RetryPolicy.withDefaults();

			assertThat(policy.shouldRetry(new AssertionError())).isTrue();
			assertThat(policy.shouldRetry(new IOException())).isTrue();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(FixedBackOff.class))
					.satisfies(backOff -> {
						assertThat(backOff.getMaxAttempts()).isEqualTo(3);
						assertThat(backOff.getInterval()).isEqualTo(1000);
					});
		}

		@Test
		void withMaxAttemptsPreconditions() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.withMaxAttempts(0))
					.withMessage("Max attempts must be greater than zero");
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.withMaxAttempts(-1))
					.withMessage("Max attempts must be greater than zero");
		}

		@Test
		void withMaxAttempts() {
			var policy = RetryPolicy.withMaxAttempts(5);

			assertThat(policy.shouldRetry(new AssertionError())).isTrue();
			assertThat(policy.shouldRetry(new IOException())).isTrue();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(FixedBackOff.class))
					.satisfies(backOff -> {
						assertThat(backOff.getMaxAttempts()).isEqualTo(5);
						assertThat(backOff.getInterval()).isEqualTo(1000);
					});
		}

		@Test
		void withMaxElapsedTimePreconditions() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.withMaxElapsedTime(Duration.ofMillis(0)))
					.withMessage("Invalid duration (0ms): maxElapsedTime must be positive.");
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.withMaxElapsedTime(Duration.ofMillis(-1)))
					.withMessage("Invalid duration (-1ms): maxElapsedTime must be positive.");
		}

		@Test
		void withMaxElapsedTime() {
			var policy = RetryPolicy.withMaxElapsedTime(Duration.ofMillis(42));

			assertThat(policy.shouldRetry(new AssertionError())).isTrue();
			assertThat(policy.shouldRetry(new IOException())).isTrue();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(hasDefaultMaxAttemptsAndDelay())
					.extracting(ExponentialBackOff::getMaxElapsedTime).isEqualTo(42L);
		}
	}


	@Nested
	class BuilderTests {

		@Test
		void backOffPlusConflictingConfig() {
			assertThatIllegalStateException()
					.isThrownBy(() -> RetryPolicy.builder().backOff(mock()).delay(Duration.ofMillis(10)).build())
					.withMessage("""
							The following configuration options are not supported with a custom BackOff strategy: \
							maxAttempts, delay, jitter, multiplier, maxDelay, or maxElapsedTime.""");
		}

		@Test
		void backOff() {
			var backOff = new FixedBackOff();
			var policy = RetryPolicy.builder().backOff(backOff).build();

			assertThat(policy.getBackOff()).isEqualTo(backOff);

			assertThat(policy).asString()
					.isEqualTo("DefaultRetryPolicy[backOff=FixedBackOff[interval=5000, maxAttempts=unlimited]]");
		}

		@Test
		void maxAttemptsPreconditions() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().maxAttempts(0))
					.withMessage("Max attempts must be greater than zero");
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().maxAttempts(-1))
					.withMessage("Max attempts must be greater than zero");
		}

		@Test
		void maxAttempts() {
			var policy = RetryPolicy.builder().maxAttempts(5).build();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(backOff -> {
						assertThat(backOff.getMaxAttempts()).isEqualTo(5);
						assertThat(backOff.getInitialInterval()).isEqualTo(1000);
					});

			assertToString(policy, 1000, 0, 1, Long.MAX_VALUE, Long.MAX_VALUE, 5);
		}

		@Test
		void delayPreconditions() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().delay(Duration.ofMillis(0)))
					.withMessage("Invalid duration (0ms): delay must be positive.");
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().delay(Duration.ofMillis(-1)))
					.withMessage("Invalid duration (-1ms): delay must be positive.");
		}

		@Test
		void delay() {
			var policy = RetryPolicy.builder().delay(Duration.ofMillis(42)).build();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(backOff -> {
						assertThat(backOff.getInitialInterval()).isEqualTo(42);
						assertThat(backOff.getMaxAttempts()).isEqualTo(3);
					});

			assertToString(policy, 42, 0, 1, Long.MAX_VALUE, Long.MAX_VALUE, 3);
		}

		@Test
		void jitterPreconditions() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().jitter(Duration.ofMillis(-1)))
					.withMessage("Invalid jitter (-1ms): must be >= 0.");
		}

		@Test
		void jitter() {
			var policy = RetryPolicy.builder().jitter(Duration.ofMillis(42)).build();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(hasDefaultMaxAttemptsAndDelay())
					.extracting(ExponentialBackOff::getJitter).isEqualTo(42L);

			assertToString(policy, 1000, 42, 1, Long.MAX_VALUE, Long.MAX_VALUE, 3);
		}

		@Test
		void multiplierPreconditions() {
			String template = """
					Invalid multiplier '%s': must be greater than or equal to 1. \
					A multiplier of 1 is equivalent to a fixed delay.""";

			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().multiplier(-1))
					.withMessage(template, "-1.0");
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().multiplier(0))
					.withMessage(template, "0.0");
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().multiplier(0.5))
					.withMessage(template, "0.5");
		}

		@Test
		void multiplier() {
			var policy = RetryPolicy.builder().multiplier(1.5).build();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(hasDefaultMaxAttemptsAndDelay())
					.extracting(ExponentialBackOff::getMultiplier).isEqualTo(1.5);

			assertToString(policy, 1000, 0, 1.5, Long.MAX_VALUE, Long.MAX_VALUE, 3);
		}

		@Test
		void maxDelayPreconditions() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().maxDelay(Duration.ofMillis(0)))
					.withMessage("Invalid duration (0ms): maxDelay must be positive.");
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().maxDelay(Duration.ofMillis(-1)))
					.withMessage("Invalid duration (-1ms): maxDelay must be positive.");
		}

		@Test
		void maxDelay() {
			var policy = RetryPolicy.builder().maxDelay(Duration.ofMillis(42)).build();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(hasDefaultMaxAttemptsAndDelay())
					.extracting(ExponentialBackOff::getMaxInterval).isEqualTo(42L);

			assertToString(policy, 1000, 0, 1, 42, Long.MAX_VALUE, 3);
		}

		@Test
		void maxElapsedTimePreconditions() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().maxElapsedTime(Duration.ofMillis(0)))
					.withMessage("Invalid duration (0ms): maxElapsedTime must be positive.");
			assertThatIllegalArgumentException()
					.isThrownBy(() -> RetryPolicy.builder().maxElapsedTime(Duration.ofMillis(-1)))
					.withMessage("Invalid duration (-1ms): maxElapsedTime must be positive.");
		}

		@Test
		void maxElapsedTime() {
			var policy = RetryPolicy.builder().maxElapsedTime(Duration.ofMillis(42)).build();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(hasDefaultMaxAttemptsAndDelay())
					.extracting(ExponentialBackOff::getMaxElapsedTime).isEqualTo(42L);

			assertToString(policy, 1000, 0, 1, Long.MAX_VALUE, 42, 3);
		}

		@Test
		void includes() {
			var policy = RetryPolicy.builder()
					.includes(FileNotFoundException.class, IllegalArgumentException.class)
					.includes(List.of(NumberFormatException.class, AssertionError.class))
					.build();

			assertThat(policy.shouldRetry(new FileNotFoundException())).isTrue();
			assertThat(policy.shouldRetry(new IllegalArgumentException())).isTrue();
			assertThat(policy.shouldRetry(new NumberFormatException())).isTrue();
			assertThat(policy.shouldRetry(new AssertionError())).isTrue();

			assertThat(policy.shouldRetry(new Throwable())).isFalse();
			assertThat(policy.shouldRetry(new FileSystemException("fs"))).isFalse();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(hasDefaultMaxAttemptsAndDelay());

			String filters = "includes=" + names(FileNotFoundException.class, IllegalArgumentException.class,
					NumberFormatException.class, AssertionError.class) + ", ";
			assertToString(policy, filters, 1000, 0, 1, Long.MAX_VALUE, Long.MAX_VALUE, 3);
		}

		@Test
		void includesSubtypeMatching() {
			var policy = RetryPolicy.builder().includes(IOException.class).build();

			assertThat(policy.shouldRetry(new FileNotFoundException())).isTrue();
			assertThat(policy.shouldRetry(new FileSystemException("fs"))).isTrue();

			assertThat(policy.shouldRetry(new Throwable())).isFalse();
			assertThat(policy.shouldRetry(new AssertionError())).isFalse();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(hasDefaultMaxAttemptsAndDelay());

			assertToString(policy, "includes=[java.io.IOException], ", 1000, 0, 1, Long.MAX_VALUE, Long.MAX_VALUE, 3);
		}

		@Test
		void excludes() {
			var policy = RetryPolicy.builder()
					.excludes(FileNotFoundException.class, IllegalArgumentException.class)
					.excludes(List.of(NumberFormatException.class, AssertionError.class))
					.build();

			assertThat(policy.shouldRetry(new FileNotFoundException())).isFalse();
			assertThat(policy.shouldRetry(new IllegalArgumentException())).isFalse();
			assertThat(policy.shouldRetry(new NumberFormatException())).isFalse();
			assertThat(policy.shouldRetry(new AssertionError())).isFalse();

			assertThat(policy.shouldRetry(new Throwable())).isTrue();
			assertThat(policy.shouldRetry(new FileSystemException("fs"))).isTrue();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(hasDefaultMaxAttemptsAndDelay());

			String filters = "excludes=" + names(FileNotFoundException.class, IllegalArgumentException.class,
					NumberFormatException.class, AssertionError.class) + ", ";
			assertToString(policy, filters, 1000, 0, 1, Long.MAX_VALUE, Long.MAX_VALUE, 3);
		}

		@Test
		void excludesSubtypeMatching() {
			var policy = RetryPolicy.builder().excludes(IOException.class).build();

			assertThat(policy.shouldRetry(new IOException("fs"))).isFalse();
			assertThat(policy.shouldRetry(new FileNotFoundException())).isFalse();
			assertThat(policy.shouldRetry(new FileSystemException("fs"))).isFalse();

			assertThat(policy.shouldRetry(new Throwable())).isTrue();
			assertThat(policy.shouldRetry(new AssertionError())).isTrue();

			assertToString(policy, "excludes=[java.io.IOException], ", 1000, 0, 1, Long.MAX_VALUE, Long.MAX_VALUE, 3);
		}

		@Test
		void predicate() {
			var policy = RetryPolicy.builder()
					.predicate(new NumberFormatExceptionMatcher())
					.build();

			assertThat(policy.shouldRetry(new NumberFormatException())).isTrue();
			assertThat(policy.shouldRetry(new CustomNumberFormatException())).isTrue();

			assertThat(policy.shouldRetry(new Throwable())).isFalse();
			assertThat(policy.shouldRetry(new Exception())).isFalse();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(hasDefaultMaxAttemptsAndDelay());

			assertToString(policy, "predicate=NumberFormatExceptionMatcher, ",
					1000, 0, 1, Long.MAX_VALUE, Long.MAX_VALUE, 3);
		}

		@Test
		void predicatesCombined() {
			var BOOM = "Boom!";
			var policy = RetryPolicy.builder()
					.predicate(new NumberFormatExceptionMatcher())
					.predicate(throwable -> BOOM.equals(throwable.getMessage()))
					.build();

			assertThat(policy.shouldRetry(new NumberFormatException(BOOM))).isTrue();
			assertThat(policy.shouldRetry(new CustomNumberFormatException(BOOM))).isTrue();

			assertThat(policy.shouldRetry(new NumberFormatException())).isFalse();
			assertThat(policy.shouldRetry(new CustomNumberFormatException())).isFalse();
			assertThat(policy.shouldRetry(new Throwable())).isFalse();
			assertThat(policy.shouldRetry(new Exception())).isFalse();

			assertThat(policy.getBackOff())
					.asInstanceOf(type(ExponentialBackOff.class))
					.satisfies(hasDefaultMaxAttemptsAndDelay());

			assertThat(policy).asString()
					.matches("DefaultRetryPolicy\\[predicate=Predicate.+?Lambda.+?, backOff=ExponentialBackOff\\[.+?]]");
		}


		private static void assertToString(RetryPolicy policy, long initialInterval, long jitter,
				double multiplier, long maxInterval, long maxElapsedTime, int maxAttempts) {

			assertToString(policy, "", initialInterval, jitter, multiplier, maxInterval, maxElapsedTime, maxAttempts);
		}

		private static void assertToString(RetryPolicy policy, String filters, long initialInterval, long jitter,
				double multiplier, long maxInterval, long maxElapsedTime, int maxAttempts) {

			assertThat(policy).asString()
				.isEqualTo("""
						DefaultRetryPolicy[%sbackOff=ExponentialBackOff[\
						initialInterval=%d, \
						jitter=%d, \
						multiplier=%s, \
						maxInterval=%d, \
						maxElapsedTime=%d, \
						maxAttempts=%d\
						]]""",
						filters, initialInterval, jitter, multiplier, maxInterval, maxElapsedTime, maxAttempts);
		}

		@SafeVarargs
		@SuppressWarnings("unchecked")
		private static String names(Class<? extends Throwable>... types) {
			StringJoiner result = new StringJoiner(", ", "[", "]");
			for (Class<? extends Throwable> type : types) {
				String name = type.getCanonicalName();
				result.add(name != null? name : type.getName());
			}
			return result.toString();
		}
	}


	private static ThrowingConsumer<? super ExponentialBackOff> hasDefaultMaxAttemptsAndDelay() {
		return backOff -> {
			assertThat(backOff.getMaxAttempts()).isEqualTo(3);
			assertThat(backOff.getInitialInterval()).isEqualTo(1000);
		};
	}


	@SuppressWarnings("serial")
	private static class CustomNumberFormatException extends NumberFormatException {

		CustomNumberFormatException() {
		}

		CustomNumberFormatException(String s) {
			super(s);
		}
	}

}

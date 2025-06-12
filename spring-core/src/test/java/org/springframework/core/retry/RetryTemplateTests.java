/*
 * Copyright 2002-2025 the original author or authors.
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

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.util.backoff.FixedBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link RetryTemplate}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 */
class RetryTemplateTests {

	private final RetryTemplate retryTemplate = new RetryTemplate();


	@BeforeEach
	void configureTemplate() {
		this.retryTemplate.setBackOffPolicy(new FixedBackOff(Duration.ofMillis(10)));
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
		AtomicInteger invocationCount = new AtomicInteger();
		RuntimeException exception = new RuntimeException("Boom!");

		Retryable<String> retryable = new Retryable<>() {
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
	void retryWithFailingRetryableAndCustomRetryPolicy() {
		AtomicInteger invocationCount = new AtomicInteger();
		RuntimeException exception = new NumberFormatException();

		Retryable<String> retryable = new Retryable<>() {
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

		AtomicInteger retryCount = new AtomicInteger();
		// Custom RetryPolicy that only retries for a NumberFormatException and max 5 retry attempts.
		RetryPolicy retryPolicy = () -> throwable -> (retryCount.incrementAndGet() <= 5 && throwable instanceof NumberFormatException);
		retryTemplate.setRetryPolicy(retryPolicy);

		assertThat(invocationCount).hasValue(0);
		assertThat(retryCount).hasValue(0);
		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessage("Retry policy for operation 'always fails' exhausted; aborting execution")
				.withCause(exception);
		 // 6 = 1 initial invocation + 5 retry attempts
		assertThat(invocationCount).hasValue(6);
		assertThat(retryCount).hasValue(6);
	}

}

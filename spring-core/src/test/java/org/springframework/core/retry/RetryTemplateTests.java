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

import org.junit.jupiter.api.Test;

import org.springframework.core.retry.support.MaxRetryAttemptsPolicy;
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

	@Test
	void retryWithSuccess() throws Exception {
		Retryable<String> retryable = new Retryable<>() {

			int failure;

			@Override
			public String run() throws Exception {
				if (failure++ < 2) {
					throw new Exception("Error while invoking greeting service");
				}
				return "hello world";
			}

			@Override
			public String getName() {
				return "greeting service";
			}
		};

		retryTemplate.setBackOffPolicy(new FixedBackOff(100, Long.MAX_VALUE));

		assertThat(retryTemplate.execute(retryable)).isEqualTo("hello world");
	}

	@Test
	void retryWithFailure() {
		Exception exception = new Exception("Error while invoking greeting service");

		Retryable<String> retryable = new Retryable<>() {
			@Override
			public String run() throws Exception {
				throw exception;
			}

			@Override
			public String getName() {
				return "greeting service";
			}
		};

		retryTemplate.setBackOffPolicy(new FixedBackOff(100, Long.MAX_VALUE));

		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessage("Retry policy for operation 'greeting service' exhausted; aborting execution")
				.withCause(exception);
	}

	@Test
	void retrySpecificException() {

		@SuppressWarnings("serial")
		class TechnicalException extends Exception {
			public TechnicalException(String message) {
				super(message);
			}
		}

		TechnicalException technicalException = new TechnicalException("Error while invoking greeting service");

		Retryable<String> retryable = new Retryable<>() {
			@Override
			public String run() throws TechnicalException {
				throw technicalException;
			}

			@Override
			public String getName() {
				return "greeting service";
			}
		};

		MaxRetryAttemptsPolicy retryPolicy = new MaxRetryAttemptsPolicy() {
			@Override
			public RetryExecution start() {
				return new RetryExecution() {

					int retryAttempts;

					@Override
					public boolean shouldRetry(Throwable throwable) {
						return this.retryAttempts++ < 3 && throwable instanceof TechnicalException;
					}
				};
			}
		};
		retryTemplate.setRetryPolicy(retryPolicy);
		retryTemplate.setBackOffPolicy(new FixedBackOff(100, Long.MAX_VALUE));

		assertThatExceptionOfType(RetryException.class)
				.isThrownBy(() -> retryTemplate.execute(retryable))
				.withMessage("Retry policy for operation 'greeting service' exhausted; aborting execution")
				.withCause(technicalException);
	}

}

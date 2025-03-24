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

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import org.springframework.core.retry.support.MaxRetryAttemptsPolicy;
import org.springframework.util.backoff.FixedBackOff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RetryTemplate}.
 *
 * @author Mahmoud Ben Hassine
 */
class RetryTemplateTests {

	@Test
	void testRetryWithSuccess() throws Exception {
		// given
		RetryCallback<String> retryCallback = new RetryCallback<>() {
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
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new MaxRetryAttemptsPolicy());
		retryTemplate.setBackOffPolicy(new FixedBackOff());

		// when
		String result = retryTemplate.execute(retryCallback);

		// then
		assertThat(result).isEqualTo("hello world");
	}

	@Test
	void testRetryWithFailure() {
		// given
		Exception exception = new Exception("Error while invoking greeting service");
		RetryCallback<String> retryCallback = new RetryCallback<>() {
			@Override
			public String run() throws Exception {
				throw exception;
			}

			@Override
			public String getName() {
				return "greeting service";
			}
		};
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new MaxRetryAttemptsPolicy());
		retryTemplate.setBackOffPolicy(new FixedBackOff());

		// when
		ThrowingCallable throwingCallable = () -> retryTemplate.execute(retryCallback);

		// then
		assertThatThrownBy(throwingCallable)
				.isInstanceOf(RetryException.class)
				.hasMessage("Retry policy for callback 'greeting service' exhausted, aborting execution")
				.hasCause(exception);
	}

	@Test
	void testRetrySpecificException() {
		// given
		class TechnicalException extends Exception {
			@java.io.Serial
			private static final long serialVersionUID = 1L;
			public TechnicalException(String message) {
				super(message);
			}
		}
		final TechnicalException technicalException = new TechnicalException("Error while invoking greeting service");
		RetryCallback<String> retryCallback = new RetryCallback<>() {
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
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(retryPolicy);
		retryTemplate.setBackOffPolicy(new FixedBackOff());

		// when
		ThrowingCallable throwingCallable = () -> retryTemplate.execute(retryCallback);

		// then
		assertThatThrownBy(throwingCallable)
				.isInstanceOf(RetryException.class)
				.hasMessage("Retry policy for callback 'greeting service' exhausted, aborting execution")
				.hasCause(technicalException);
	}

}

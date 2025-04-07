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
import java.util.function.Predicate;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import org.springframework.core.retry.support.MaxAttemptsRetryPolicy;
import org.springframework.core.retry.support.backoff.FixedBackOffPolicy;

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
		retryTemplate.setRetryPolicy(new MaxAttemptsRetryPolicy(3));
		retryTemplate.setBackOffPolicy(new FixedBackOffPolicy(Duration.ofMillis(100)));

		// when
		String result = retryTemplate.execute(retryCallback);

		// then
		assertThat(result).isEqualTo("hello world");
	}

	@Test
	void testRetryWithFailure() {
		// given
		RetryCallback<String> retryCallback = new RetryCallback<>() {
			@Override
			public String run() throws Exception {
				throw new Exception("Error while invoking greeting service");
			}

			@Override
			public String getName() {
				return "greeting service";
			}
		};
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new MaxAttemptsRetryPolicy(3));
		retryTemplate.setBackOffPolicy(new FixedBackOffPolicy(Duration.ofMillis(100)));

		// when
		ThrowingCallable throwingCallable = () -> retryTemplate.execute(retryCallback);

		// then
		assertThatThrownBy(throwingCallable)
				.isInstanceOf(RetryException.class)
				.hasMessage("Retry callback 'greeting service' exceeded maximum retry attempts 3, aborting execution")
				.cause().isInstanceOf(Exception.class).hasMessage("Error while invoking greeting service");
	}

	@Test
	void testRetrySpecificException() {
		// given
		class TechnicalException extends Exception {
			public TechnicalException(String message) {
				super(message);
			}
		}
		RetryCallback<String> retryCallback = new RetryCallback<>() {
			@Override
			public String run() throws TechnicalException {
				throw new TechnicalException("Error while invoking greeting service");
			}

			@Override
			public String getName() {
				return "greeting service";
			}
		};
		MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy(3) {
			@Override
			public Predicate<Exception> retryOn() {
				return exception -> exception instanceof TechnicalException;
			}
		};
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(retryPolicy);
		retryTemplate.setBackOffPolicy(new FixedBackOffPolicy(Duration.ofMillis(100)));

		// when
		ThrowingCallable throwingCallable = () -> retryTemplate.execute(retryCallback);

		// then
		assertThatThrownBy(throwingCallable)
				.isInstanceOf(RetryException.class)
				.cause().isInstanceOf(TechnicalException.class)
				.hasMessage("Error while invoking greeting service");
	}

}

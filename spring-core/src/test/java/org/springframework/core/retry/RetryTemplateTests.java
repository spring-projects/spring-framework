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
import java.util.concurrent.Callable;

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
		// given some unreliable code
		Callable<String> callable = new Callable<>() {
			int failure;
			@Override
			public String call() throws Exception {
				if (failure++ < 2) {
					throw new Exception("Error while invoking service");
				}
				return "hello world";
			}
		};
		// and a configured retry template
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new MaxAttemptsRetryPolicy(3));
		retryTemplate.setBackOffPolicy(new FixedBackOffPolicy(Duration.ofMillis(100)));

		// when
		String result = retryTemplate.execute(callable);

		// then
		assertThat(result).isEqualTo("hello world");
	}

	@Test
	void testRetryWithFailure() {
		// given some unreliable code
		Callable<String> callable = () -> {
			throw new Exception("Error while invoking service");
		};
		// and a configured retry template
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new MaxAttemptsRetryPolicy(3));
		retryTemplate.setBackOffPolicy(new FixedBackOffPolicy(Duration.ofMillis(100)));

		// when
		ThrowingCallable throwingCallable = () -> retryTemplate.execute(callable);

		// then
		assertThatThrownBy(throwingCallable).hasMessage("Error while invoking service");
	}
}

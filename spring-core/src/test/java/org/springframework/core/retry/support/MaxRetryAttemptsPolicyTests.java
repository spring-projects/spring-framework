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

package org.springframework.core.retry.support;

import org.junit.jupiter.api.Test;

import org.springframework.core.retry.RetryExecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MaxRetryAttemptsPolicy} and its {@link RetryExecution}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 */
class MaxRetryAttemptsPolicyTests {

	@Test
	void defaultMaxRetryAttempts() {
		// given
		MaxRetryAttemptsPolicy retryPolicy = new MaxRetryAttemptsPolicy();
		Throwable throwable = mock();

		// when
		RetryExecution retryExecution = retryPolicy.start();

		// then
		assertThat(retryExecution.shouldRetry(throwable)).isTrue();
		assertThat(retryExecution.shouldRetry(throwable)).isTrue();
		assertThat(retryExecution.shouldRetry(throwable)).isTrue();

		assertThat(retryExecution.shouldRetry(throwable)).isFalse();
		assertThat(retryExecution.shouldRetry(throwable)).isFalse();
	}

	@Test
	void customMaxRetryAttempts() {
		// given
		MaxRetryAttemptsPolicy retryPolicy = new MaxRetryAttemptsPolicy(2);
		Throwable throwable = mock();

		// when
		RetryExecution retryExecution = retryPolicy.start();

		// then
		assertThat(retryExecution.shouldRetry(throwable)).isTrue();
		assertThat(retryExecution.shouldRetry(throwable)).isTrue();

		assertThat(retryExecution.shouldRetry(throwable)).isFalse();
		assertThat(retryExecution.shouldRetry(throwable)).isFalse();
	}

	@Test
	void invalidMaxRetryAttempts() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MaxRetryAttemptsPolicy(0))
				.withMessage("Max retry attempts must be greater than zero");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MaxRetryAttemptsPolicy(-1))
				.withMessage("Max retry attempts must be greater than zero");
	}

	@Test
	void toStringImplementations() {
		MaxRetryAttemptsPolicy policy1 = new MaxRetryAttemptsPolicy();
		MaxRetryAttemptsPolicy policy2 = new MaxRetryAttemptsPolicy(1);

		assertThat(policy1).asString().isEqualTo("MaxRetryAttemptsPolicy[maxRetryAttempts=3]");
		assertThat(policy2).asString().isEqualTo("MaxRetryAttemptsPolicy[maxRetryAttempts=1]");

		RetryExecution retryExecution = policy1.start();
		assertThat(retryExecution).asString()
				.isEqualTo("MaxRetryAttemptsPolicyExecution[retryAttempts=0, maxRetryAttempts=3]");

		assertThat(retryExecution.shouldRetry(mock())).isTrue();
		assertThat(retryExecution).asString()
				.isEqualTo("MaxRetryAttemptsPolicyExecution[retryAttempts=1, maxRetryAttempts=3]");

		assertThat(retryExecution.shouldRetry(mock())).isTrue();
		assertThat(retryExecution).asString()
				.isEqualTo("MaxRetryAttemptsPolicyExecution[retryAttempts=2, maxRetryAttempts=3]");

		assertThat(retryExecution.shouldRetry(mock())).isTrue();
		assertThat(retryExecution).asString()
				.isEqualTo("MaxRetryAttemptsPolicyExecution[retryAttempts=3, maxRetryAttempts=3]");

		assertThat(retryExecution.shouldRetry(mock())).isFalse();
		assertThat(retryExecution).asString()
				.isEqualTo("MaxRetryAttemptsPolicyExecution[retryAttempts=4, maxRetryAttempts=3]");

		assertThat(retryExecution.shouldRetry(mock())).isFalse();
		assertThat(retryExecution).asString()
				.isEqualTo("MaxRetryAttemptsPolicyExecution[retryAttempts=5, maxRetryAttempts=3]");
	}

}

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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.util.backoff.BackOffExecution.STOP;

/**
 * Max attempts {@link RetryPolicy} tests.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 */
class MaxAttemptsRetryPolicyTests {

	@Test
	void maxAttempts() {
		var retryPolicy = RetryPolicy.builder().maxAttempts(2).delay(Duration.ofMillis(1)).build();
		var backOffExecution = retryPolicy.getBackOff().start();
		var throwable = mock(Throwable.class);

		assertThat(retryPolicy.shouldRetry(throwable)).isTrue();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);
		assertThat(retryPolicy.shouldRetry(throwable)).isTrue();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);

		assertThat(retryPolicy.shouldRetry(throwable)).isTrue();
		assertThat(backOffExecution.nextBackOff()).isEqualTo(STOP);
		assertThat(retryPolicy.shouldRetry(throwable)).isTrue();
		assertThat(backOffExecution.nextBackOff()).isEqualTo(STOP);
	}

	@Test
	void maxAttemptsAndPredicate() {
		var retryPolicy = RetryPolicy.builder()
				.maxAttempts(4)
				.delay(Duration.ofMillis(1))
				.predicate(NumberFormatException.class::isInstance)
				.build();

		var backOffExecution = retryPolicy.getBackOff().start();

		// 4 retries
		assertThat(retryPolicy.shouldRetry(new NumberFormatException())).isTrue();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);
		assertThat(retryPolicy.shouldRetry(new IllegalStateException())).isFalse();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);
		assertThat(retryPolicy.shouldRetry(new IllegalStateException())).isFalse();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);
		assertThat(retryPolicy.shouldRetry(new CustomNumberFormatException())).isTrue();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);

		// After policy exhaustion
		assertThat(retryPolicy.shouldRetry(new NumberFormatException())).isTrue();
		assertThat(backOffExecution.nextBackOff()).isEqualTo(STOP);
		assertThat(retryPolicy.shouldRetry(new IllegalStateException())).isFalse();
		assertThat(backOffExecution.nextBackOff()).isEqualTo(STOP);
	}

	@Test
	void maxAttemptsWithIncludesAndExcludes() {
		var retryPolicy = RetryPolicy.builder()
				.maxAttempts(6)
				.includes(RuntimeException.class, IOException.class)
				.excludes(FileNotFoundException.class, CustomFileSystemException.class)
				.build();

		var backOffExecution = retryPolicy.getBackOff().start();

		// 6 retries
		assertThat(retryPolicy.shouldRetry(new IOException())).isTrue();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);
		assertThat(retryPolicy.shouldRetry(new RuntimeException())).isTrue();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);
		assertThat(retryPolicy.shouldRetry(new FileNotFoundException())).isFalse();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);
		assertThat(retryPolicy.shouldRetry(new FileSystemException("file"))).isTrue();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);
		assertThat(retryPolicy.shouldRetry(new CustomFileSystemException("file"))).isFalse();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);
		assertThat(retryPolicy.shouldRetry(new IOException())).isTrue();
		assertThat(backOffExecution.nextBackOff()).isGreaterThan(0);

		// After policy exhaustion
		assertThat(retryPolicy.shouldRetry(new IOException())).isTrue();
		assertThat(backOffExecution.nextBackOff()).isEqualTo(STOP);
	}


	@SuppressWarnings("serial")
	private static class CustomNumberFormatException extends NumberFormatException {
	}

	@SuppressWarnings("serial")
	private static class CustomFileSystemException extends FileSystemException {

		CustomFileSystemException(String file) {
			super(file);
		}
	}

}

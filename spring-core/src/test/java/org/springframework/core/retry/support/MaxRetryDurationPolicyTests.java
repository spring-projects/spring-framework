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

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MaxRetryDurationPolicy}.
 *
 * @author Mahmoud Ben Hassine
 */
class MaxRetryDurationPolicyTests {

	@Test
	void testMaxRetryDuration() throws InterruptedException {
		MaxRetryDurationPolicy retryPolicy = new MaxRetryDurationPolicy();
		retryPolicy.setMaxRetryDuration(Duration.of(1000, ChronoUnit.MILLIS));
		MaxRetryDurationPolicy.MaxRetryDurationPolicyExecution retryExecution = retryPolicy.start();

		assertThat(retryExecution.shouldRetry(Mockito.any(Exception.class))).isTrue();
		Thread.sleep(400);
		assertThat(retryExecution.shouldRetry(Mockito.any(Exception.class))).isTrue();
		Thread.sleep(400);
		assertThat(retryExecution.shouldRetry(Mockito.any(Exception.class))).isFalse();
	}

	@Test
	void testInvalidMaxRetryDuration() {
		MaxRetryDurationPolicy retryPolicy = new MaxRetryDurationPolicy();
		assertThatThrownBy(() -> retryPolicy.setMaxRetryDuration(Duration.ZERO))
				.hasMessage("Max retry duration must be positive");
	}
}

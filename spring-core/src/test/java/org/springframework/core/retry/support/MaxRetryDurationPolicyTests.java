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

import org.junit.jupiter.api.Test;

import org.springframework.core.retry.RetryExecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MaxRetryDurationPolicy} and its {@link RetryExecution}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 */
class MaxRetryDurationPolicyTests {

	@Test
	void invalidMaxRetryDuration() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MaxRetryDurationPolicy(Duration.ZERO))
				.withMessage("Max retry duration must be positive");
	}

	@Test
	void toStringImplementations() {
		MaxRetryDurationPolicy policy1 = new MaxRetryDurationPolicy();
		MaxRetryDurationPolicy policy2 = new MaxRetryDurationPolicy(Duration.ofSeconds(1));

		assertThat(policy1).asString().isEqualTo("MaxRetryDurationPolicy[maxRetryDuration=3000ms]");
		assertThat(policy2).asString().isEqualTo("MaxRetryDurationPolicy[maxRetryDuration=1000ms]");

		assertThat(policy1.start()).asString()
				.matches("MaxRetryDurationPolicyExecution\\[retryStartTime=.+, maxRetryDuration=3000ms\\]");
		assertThat(policy2.start()).asString()
				.matches("MaxRetryDurationPolicyExecution\\[retryStartTime=.+, maxRetryDuration=1000ms\\]");
	}

}

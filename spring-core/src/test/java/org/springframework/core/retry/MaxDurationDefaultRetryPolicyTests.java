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

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Max duration tests for {@link DefaultRetryPolicy} and its {@link RetryExecution}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 */
class MaxDurationDefaultRetryPolicyTests {

	@Test
	void invalidMaxDuration() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> RetryPolicy.withMaxDuration(Duration.ZERO))
				.withMessage("Max duration must be positive");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> RetryPolicy.withMaxDuration(ofSeconds(-1)))
				.withMessage("Max duration must be positive");
	}

	@Test
	void toStringImplementations() {
		var policy1 = RetryPolicy.withMaxDuration(ofSeconds(3));
		var policy2 = RetryPolicy.builder()
				.maxDuration(ofSeconds(1))
				.predicate(new NumberFormatExceptionMatcher())
				.build();

		assertThat(policy1).asString()
				.isEqualTo("DefaultRetryPolicy[maxDuration=3000ms]");
		assertThat(policy2).asString()
				.isEqualTo("DefaultRetryPolicy[maxDuration=1000ms, predicate=NumberFormatExceptionMatcher]");

		assertThat(policy1.start()).asString()
				.matches("DefaultRetryPolicyExecution\\[maxDuration=3000ms, retryStartTime=.+]");
		assertThat(policy2.start()).asString()
				.matches("DefaultRetryPolicyExecution\\[maxDuration=1000ms, retryStartTime=.+, predicate=NumberFormatExceptionMatcher]");
	}

}

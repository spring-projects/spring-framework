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

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.core.retry.RetryExecution;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PredicateRetryPolicy} and its {@link RetryExecution}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 */
class PredicateRetryPolicyTests {

	@Test
	void predicateRetryPolicy() {
		Predicate<Throwable> predicate = NumberFormatException.class::isInstance;
		PredicateRetryPolicy retryPolicy = new PredicateRetryPolicy(predicate);

		RetryExecution retryExecution = retryPolicy.start();

		assertThat(retryExecution.shouldRetry(new NumberFormatException())).isTrue();
		assertThat(retryExecution.shouldRetry(new IllegalStateException())).isFalse();
	}

	@Test
	void toStringImplementations() {
		PredicateRetryPolicy policy1 = new PredicateRetryPolicy(NumberFormatException.class::isInstance);
		PredicateRetryPolicy policy2 = new PredicateRetryPolicy(new NumberFormatExceptionMatcher());

		assertThat(policy1).asString().matches("PredicateRetryPolicy\\[predicate=PredicateRetryPolicyTests.+?Lambda.+?\\]");
		assertThat(policy2).asString().isEqualTo("PredicateRetryPolicy[predicate=NumberFormatExceptionMatcher]");

		assertThat(policy1.start()).asString()
				.matches("PredicateRetryPolicyExecution\\[predicate=PredicateRetryPolicyTests.+?Lambda.+?\\]");
		assertThat(policy2.start()).asString()
				.isEqualTo("PredicateRetryPolicyExecution[predicate=NumberFormatExceptionMatcher]");
	}


	private static class NumberFormatExceptionMatcher implements Predicate<Throwable> {

		@Override
		public boolean test(Throwable throwable) {
			return (throwable instanceof NumberFormatException);
		}
	}

}

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
 * Tests for {@link PredicateRetryPolicy}.
 *
 * @author Mahmoud Ben Hassine
 */
class PredicateRetryPolicyTests {

	@Test
	void predicateRetryPolicy() {
		// given
		class MyException extends Exception {
			@java.io.Serial
			private static final long serialVersionUID = 1L;
		}
		Predicate<Throwable> predicate = MyException.class::isInstance;
		PredicateRetryPolicy retryPolicy = new PredicateRetryPolicy(predicate);

		// when
		RetryExecution retryExecution = retryPolicy.start();

		// then
		assertThat(retryExecution.shouldRetry(new MyException())).isTrue();
		assertThat(retryExecution.shouldRetry(new IllegalStateException())).isFalse();
	}

}

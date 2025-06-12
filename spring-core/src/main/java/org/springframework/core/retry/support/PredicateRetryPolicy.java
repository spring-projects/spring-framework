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

import org.springframework.core.retry.RetryExecution;
import org.springframework.core.retry.RetryPolicy;

/**
 * A {@link RetryPolicy} based on a {@link Predicate}.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 */
public final class PredicateRetryPolicy implements RetryPolicy {

	private final Predicate<Throwable> predicate;


	/**
	 * Create a new {@code PredicateRetryPolicy} with the given {@link Predicate}.
	 * @param predicate the predicate to use for determining whether to retry an
	 * operation based on a given {@link Throwable}
	 */
	public PredicateRetryPolicy(Predicate<Throwable> predicate) {
		this.predicate = predicate;
	}


	@Override
	public RetryExecution start() {
		return new PredicateRetryPolicyExecution();
	}

	@Override
	public String toString() {
		return "PredicateRetryPolicy[predicate=%s]".formatted(this.predicate.getClass().getSimpleName());
	}


	/**
	 * A {@link RetryExecution} based on a {@link Predicate}.
	 */
	private class PredicateRetryPolicyExecution implements RetryExecution {

		@Override
		public boolean shouldRetry(Throwable throwable) {
			return PredicateRetryPolicy.this.predicate.test(throwable);
		}

		@Override
		public String toString() {
			return "PredicateRetryPolicyExecution[predicate=%s]"
					.formatted(PredicateRetryPolicy.this.predicate.getClass().getSimpleName());
		}

	}

}

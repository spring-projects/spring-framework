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

package org.springframework.aop.retry;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

/**
 * A specification for retry attempts on a given method, combining common
 * retry characteristics. This roughly matches the annotation attributes
 * on {@link org.springframework.aop.retry.annotation.Retryable}.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @param includes applicable exception types to attempt a retry for
 * @param excludes non-applicable exception types to avoid a retry for
 * @param predicate a predicate for filtering exceptions from applicable methods
 * @param maxAttempts the maximum number of retry attempts
 * @param delay the base delay after the initial invocation
 * @param jitter a jitter value for the next retry attempt
 * @param multiplier a multiplier for a delay for the next retry attempt
 * @param maxDelay the maximum delay for any retry attempt
 * @see AbstractRetryInterceptor#getRetrySpec
 * @see SimpleRetryInterceptor#SimpleRetryInterceptor(MethodRetrySpec)
 * @see org.springframework.aop.retry.annotation.Retryable
 */
public record MethodRetrySpec(
		Collection<Class<? extends Throwable>> includes,
		Collection<Class<? extends Throwable>> excludes,
		MethodRetryPredicate predicate,
		long maxAttempts,
		Duration delay,
		Duration jitter,
		double multiplier,
		Duration maxDelay) {

	public MethodRetrySpec(MethodRetryPredicate predicate, long maxAttempts, Duration delay) {
		this(predicate, maxAttempts, delay, Duration.ofMillis(0), 1.0, Duration.ofMillis(Long.MAX_VALUE));
	}

	public MethodRetrySpec(MethodRetryPredicate predicate, long maxAttempts, Duration delay,
			Duration jitter, double multiplier, Duration maxDelay) {

		this(Collections.emptyList(), Collections.emptyList(), predicate, maxAttempts, delay,
				jitter, multiplier, maxDelay);
	}


	MethodRetryPredicate combinedPredicate() {
		return (method, throwable) -> {
			if (!this.excludes.isEmpty()) {
				for (Class<? extends Throwable> exclude : this.excludes) {
					if (exclude.isInstance(throwable)) {
						return false;
					}
				}
			}
			if (!this.includes.isEmpty()) {
				boolean included = false;
				for (Class<? extends Throwable> include : this.includes) {
					if (include.isInstance(throwable)) {
						included = true;
						break;
					}
				}
				if (!included) {
					return false;
				}
			}
			return this.predicate.shouldRetry(method, throwable);
		};
	}

}

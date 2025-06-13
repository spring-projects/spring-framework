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
import java.time.LocalDateTime;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Default {@link RetryPolicy} created by {@link RetryPolicy.Builder}.
 *
 * @author Sam Brannen
 * @author Mahmoud Ben Hassine
 * @since 7.0
 */
class DefaultRetryPolicy implements RetryPolicy {

	private final int maxAttempts;

	private final @Nullable Duration maxDuration;

	private final Set<Class<? extends Throwable>> includes;

	private final Set<Class<? extends Throwable>> excludes;

	private final @Nullable Predicate<Throwable> predicate;


	DefaultRetryPolicy(int maxAttempts, @Nullable Duration maxDuration, Set<Class<? extends Throwable>> includes,
			Set<Class<? extends Throwable>> excludes, @Nullable Predicate<Throwable> predicate) {

		Assert.isTrue((maxAttempts > 0 || maxDuration != null), "Max attempts or max duration must be specified");

		this.maxAttempts = maxAttempts;
		this.maxDuration = maxDuration;
		this.includes = includes;
		this.excludes = excludes;
		this.predicate = predicate;
	}


	@Override
	public RetryExecution start() {
		return new DefaultRetryPolicyExecution();
	}

	@Override
	public String toString() {
		StringJoiner result = new StringJoiner(", ", "DefaultRetryPolicy[", "]");
		if (this.maxAttempts > 0) {
			result.add("maxAttempts=" + this.maxAttempts);
		}
		if (this.maxDuration != null) {
			result.add("maxDuration=" + this.maxDuration.toMillis() + "ms");
		}
		if (!this.includes.isEmpty()) {
			result.add("includes=" + names(this.includes));
		}
		if (!this.excludes.isEmpty()) {
			result.add("excludes=" + names(this.excludes));
		}
		if (this.predicate != null) {
			result.add("predicate=" + this.predicate.getClass().getSimpleName());
		}
		return result.toString();
	}


	private static String names(Set<Class<? extends Throwable>> types) {
		StringJoiner result = new StringJoiner(", ", "[", "]");
		for (Class<? extends Throwable> type : types) {
			String name = type.getCanonicalName();
			result.add(name != null? name : type.getName());
		}
		return result.toString();
	}


	/**
	 * {@link RetryExecution} for {@link DefaultRetryPolicy}.
	 */
	private class DefaultRetryPolicyExecution implements RetryExecution {

		private final LocalDateTime retryStartTime = LocalDateTime.now();

		private int retryCount;


		@Override
		public boolean shouldRetry(Throwable throwable) {
			if (DefaultRetryPolicy.this.maxAttempts > 0 &&
					this.retryCount++ >= DefaultRetryPolicy.this.maxAttempts) {
				return false;
			}
			if (DefaultRetryPolicy.this.maxDuration != null) {
				Duration retryDuration = Duration.between(this.retryStartTime, LocalDateTime.now());
				if (retryDuration.compareTo(DefaultRetryPolicy.this.maxDuration) > 0) {
					return false;
				}
			}
			if (!DefaultRetryPolicy.this.excludes.isEmpty()) {
				for (Class<? extends Throwable> excludedType : DefaultRetryPolicy.this.excludes) {
					if (excludedType.isInstance(throwable)) {
						return false;
					}
				}
			}
			if (!DefaultRetryPolicy.this.includes.isEmpty()) {
				boolean included = false;
				for (Class<? extends Throwable> includedType : DefaultRetryPolicy.this.includes) {
					if (includedType.isInstance(throwable)) {
						included = true;
						break;
					}
				}
				if (!included) {
					return false;
				}
			}
			return DefaultRetryPolicy.this.predicate == null || DefaultRetryPolicy.this.predicate.test(throwable);
		}

		@Override
		public String toString() {
			StringJoiner result = new StringJoiner(", ", "DefaultRetryPolicyExecution[", "]");
			if (DefaultRetryPolicy.this.maxAttempts > 0) {
				result.add("maxAttempts=" + DefaultRetryPolicy.this.maxAttempts);
				result.add("retryCount=" + this.retryCount);
			}
			if (DefaultRetryPolicy.this.maxDuration != null) {
				result.add("maxDuration=" + DefaultRetryPolicy.this.maxDuration.toMillis() + "ms");
				result.add("retryStartTime=" + this.retryStartTime);
			}
			if (!DefaultRetryPolicy.this.includes.isEmpty()) {
				result.add("includes=" + names(DefaultRetryPolicy.this.includes));
			}
			if (!DefaultRetryPolicy.this.excludes.isEmpty()) {
				result.add("excludes=" + names(DefaultRetryPolicy.this.excludes));
			}
			if (DefaultRetryPolicy.this.predicate != null) {
				result.add("predicate=" + DefaultRetryPolicy.this.predicate.getClass().getSimpleName());
			}
			return result.toString();
		}
	}

}

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
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ExceptionTypeFilter;
import org.springframework.util.backoff.BackOff;

/**
 * Default {@link RetryPolicy} created by {@link RetryPolicy.Builder}.
 *
 * @author Sam Brannen
 * @author Mahmoud Ben Hassine
 * @since 7.0
 */
class DefaultRetryPolicy implements RetryPolicy {

	private final Set<Class<? extends Throwable>> includes;

	private final Set<Class<? extends Throwable>> excludes;

	private final ExceptionTypeFilter exceptionFilter;

	private final @Nullable Predicate<Throwable> predicate;

	private final Duration timeout;

	private final BackOff backOff;


	DefaultRetryPolicy(Set<Class<? extends Throwable>> includes, Set<Class<? extends Throwable>> excludes,
			@Nullable Predicate<Throwable> predicate, Duration timeout, BackOff backOff) {

		this.includes = includes;
		this.excludes = excludes;
		this.exceptionFilter = new ExceptionTypeFilter(this.includes, this.excludes);
		this.predicate = predicate;
		this.timeout = timeout;
		this.backOff = backOff;
	}


	@Override
	public boolean shouldRetry(Throwable throwable) {
		return (this.exceptionFilter.match(throwable, true) &&
				(this.predicate == null || this.predicate.test(throwable)));
	}

	@Override
	public Duration getTimeout() {
		return this.timeout;
	}

	@Override
	public BackOff getBackOff() {
		return this.backOff;
	}

	@Override
	public String toString() {
		StringJoiner result = new StringJoiner(", ", "DefaultRetryPolicy[", "]");
		if (!this.includes.isEmpty()) {
			result.add("includes=" + names(this.includes));
		}
		if (!this.excludes.isEmpty()) {
			result.add("excludes=" + names(this.excludes));
		}
		if (this.predicate != null) {
			result.add("predicate=" + this.predicate.getClass().getSimpleName());
		}
		result.add("backOff=" + this.backOff);
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

}

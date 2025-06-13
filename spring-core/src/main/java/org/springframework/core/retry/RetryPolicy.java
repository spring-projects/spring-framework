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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Strategy interface to define a retry policy.
 *
 * <p>Also provides factory methods and a fluent builder API for creating retry
 * policies with common configurations. See {@link #withMaxAttempts(int)},
 * {@link #withMaxDuration(Duration)}, {@link #builder()}, and the configuration
 * options in {@link Builder} for details.
 *
 * @author Sam Brannen
 * @author Mahmoud Ben Hassine
 * @since 7.0
 * @see RetryExecution
 */
public interface RetryPolicy {

	/**
	 * Start a new execution for this retry policy.
	 * @return a new {@link RetryExecution}
	 */
	RetryExecution start();


	/**
	 * Create a {@link RetryPolicy} configured with a maximum number of retry attempts.
	 * @param maxAttempts the maximum number of retry attempts; must be greater than zero
	 * @see Builder#maxAttempts(int)
	 */
	static RetryPolicy withMaxAttempts(int maxAttempts) {
		return builder().maxAttempts(maxAttempts).build();
	}

	/**
	 * Create a {@link RetryPolicy} configured with a maximum retry {@link Duration}.
	 * @param maxDuration the maximum retry duration; must be positive
	 * @see Builder#maxDuration(Duration)
	 */
	static RetryPolicy withMaxDuration(Duration maxDuration) {
		return builder().maxDuration(maxDuration).build();
	}

	/**
	 * Create a {@link Builder} to configure a {@link RetryPolicy} with common
	 * configuration options.
	 */
	static Builder builder() {
		return new Builder();
	}


	/**
	 * Fluent API for configuring a {@link RetryPolicy} with common configuration
	 * options.
	 */
	final class Builder {

		private int maxAttempts;

		private @Nullable Duration maxDuration;

		private final Set<Class<? extends Throwable>> includes = new LinkedHashSet<>();

		private final Set<Class<? extends Throwable>> excludes = new LinkedHashSet<>();

		private @Nullable Predicate<Throwable> predicate;


		private Builder() {
			// internal constructor
		}


		/**
		 * Specify the maximum number of retry attempts.
		 * @param maxAttempts the maximum number of retry attempts; must be
		 * greater than zero
		 * @return this {@code Builder} instance for chained method invocations
		 */
		public Builder maxAttempts(int maxAttempts) {
			Assert.isTrue(maxAttempts > 0, "Max attempts must be greater than zero");
			this.maxAttempts = maxAttempts;
			return this;
		}

		/**
		 * Specify the maximum retry {@link Duration}.
		 * @param maxDuration the maximum retry duration; must be positive
		 * @return this {@code Builder} instance for chained method invocations
		 */
		public Builder maxDuration(Duration maxDuration) {
			Assert.isTrue(!maxDuration.isNegative() && !maxDuration.isZero(), "Max duration must be positive");
			this.maxDuration = maxDuration;
			return this;
		}

		/**
		 * Specify the types of exceptions for which the {@link RetryPolicy}
		 * should retry a failed operation.
		 * <p>This can be combined with {@link #excludes(Class...)} and
		 * {@link #predicate(Predicate)}.
		 * @param types the types of exceptions to include in the policy
		 * @return this {@code Builder} instance for chained method invocations
		 */
		@SafeVarargs // Making the method final allows us to use @SafeVarargs.
		@SuppressWarnings("varargs")
		public final Builder includes(Class<? extends Throwable>... types) {
			Collections.addAll(this.includes, types);
			return this;
		}

		/**
		 * Specify the types of exceptions for which the {@link RetryPolicy}
		 * should not retry a failed operation.
		 * <p>This can be combined with {@link #includes(Class...)} and
		 * {@link #predicate(Predicate)}.
		 * @param types the types of exceptions to exclude from the policy
		 * @return this {@code Builder} instance for chained method invocations
		 */
		@SafeVarargs // Making the method final allows us to use @SafeVarargs.
		@SuppressWarnings("varargs")
		public final Builder excludes(Class<? extends Throwable>... types) {
			Collections.addAll(this.excludes, types);
			return this;
		}

		/**
		 * Specify a custom {@link Predicate} that the {@link RetryPolicy} will
		 * use to determine whether to retry a failed operation based on a given
		 * {@link Throwable}.
		 * <p>If a predicate has already been configured, the supplied predicate
		 * will be {@linkplain Predicate#and(Predicate) combined} with the
		 * existing predicate.
		 * <p>This can be combined with {@link #includes(Class...)} and
		 * {@link #excludes(Class...)}.
		 * @param predicate a custom predicate
		 * @return this {@code Builder} instance for chained method invocations
		 */
		public Builder predicate(Predicate<Throwable> predicate) {
			this.predicate = (this.predicate != null ? this.predicate.and(predicate) : predicate);
			return this;
		}

		/**
		 * Build the {@link RetryPolicy} configured via this {@code Builder}.
		 */
		public RetryPolicy build() {
			return new DefaultRetryPolicy(this.maxAttempts, this.maxDuration,
					this.includes, this.excludes, this.predicate);
		}
	}

}

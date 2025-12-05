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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Strategy interface to define a retry policy.
 *
 * <p>Also provides factory methods and a fluent builder API for creating retry
 * policies with common configurations. See {@link #withDefaults()},
 * {@link #withMaxRetries(long)}, {@link #builder()}, and the configuration
 * options in {@link Builder} for details.
 *
 * @author Sam Brannen
 * @author Mahmoud Ben Hassine
 * @since 7.0
 * @see Retryable
 * @see RetryTemplate
 * @see BackOff
 */
public interface RetryPolicy {

	/**
	 * Specify if the {@link Retryable} operation should be retried based on the
	 * given throwable.
	 * @param throwable the exception that caused the operation to fail
	 * @return {@code true} if the operation should be retried, {@code false} otherwise
	 */
	boolean shouldRetry(Throwable throwable);

	/**
	 * Get the timeout to use for this retry policy.
	 * <p>The returned {@link Duration} represents the maximum amount of elapsed
	 * time allowed for the initial invocation and any subsequent retry attempts,
	 * including delays.
	 * <p>Defaults to {@link Duration#ZERO} which signals that no timeout should
	 * be applied.
	 * @return the timeout to apply
	 * @since 7.0.2
	 * @see Builder#timeout(Duration)
	 */
	default Duration getTimeout() {
		return Duration.ZERO;
	}

	/**
	 * Get the {@link BackOff} strategy to use for this retry policy.
	 * <p>Defaults to a fixed backoff of {@value Builder#DEFAULT_DELAY} milliseconds
	 * and maximum {@value Builder#DEFAULT_MAX_RETRIES} retries.
	 * <p>Note that {@code total attempts = 1 initial attempt + maxRetries attempts}.
	 * Thus, when {@code maxRetries} is set to 3, a retryable operation will be
	 * invoked at least once and at most 4 times.
	 * @return the {@code BackOff} strategy to use
	 * @see FixedBackOff
	 */
	default BackOff getBackOff() {
		return new FixedBackOff(Builder.DEFAULT_DELAY, Builder.DEFAULT_MAX_RETRIES);
	}


	/**
	 * Create a {@link RetryPolicy} with default configuration.
	 * <p>The returned policy applies to all exception types, uses a fixed backoff
	 * of {@value Builder#DEFAULT_DELAY} milliseconds, and supports maximum
	 * {@value Builder#DEFAULT_MAX_RETRIES} retries.
	 * <p>Note that {@code total attempts = 1 initial attempt + maxRetries attempts}.
	 * Thus, when {@code maxRetries} is set to 3, a retryable operation will be
	 * invoked at least once and at most 4 times.
	 * @see FixedBackOff
	 */
	static RetryPolicy withDefaults() {
		return throwable -> true;
	}

	/**
	 * Create a {@link RetryPolicy} configured with a maximum number of retry attempts.
	 * <p>Note that {@code total attempts = 1 initial attempt + maxRetries attempts}.
	 * Thus, if {@code maxRetries} is set to 4, a retryable operation will be invoked
	 * at least once and at most 5 times.
	 * <p>The returned policy applies to all exception types and uses a fixed backoff
	 * of {@value Builder#DEFAULT_DELAY} milliseconds.
	 * @param maxRetries the maximum number of retry attempts;
	 * must be positive (or zero for no retry)
	 * @see Builder#maxRetries(long)
	 * @see FixedBackOff
	 */
	static RetryPolicy withMaxRetries(long maxRetries) {
		assertMaxRetriesIsNotNegative(maxRetries);
		return builder().backOff(new FixedBackOff(Builder.DEFAULT_DELAY, maxRetries)).build();
	}

	/**
	 * Create a {@link Builder} to configure a {@link RetryPolicy} with common
	 * configuration options.
	 */
	static Builder builder() {
		return new Builder();
	}


	private static void assertMaxRetriesIsNotNegative(long maxRetries) {
		Assert.isTrue(maxRetries >= 0,
				() -> "Invalid maxRetries (%d): must be positive or zero for no retry.".formatted(maxRetries));
	}

	private static void assertIsNotNegative(String name, Duration duration) {
		Assert.isTrue(!duration.isNegative(),
				() -> "Invalid %s (%dms): must be greater than or equal to zero.".formatted(name, duration.toMillis()));
	}

	private static void assertIsPositive(String name, Duration duration) {
		Assert.isTrue((!duration.isNegative() && !duration.isZero()),
				() -> "Invalid %s (%dms): must be greater than zero.".formatted(name, duration.toMillis()));
	}


	/**
	 * Fluent API for configuring a {@link RetryPolicy} with common configuration
	 * options.
	 */
	final class Builder {

		/**
		 * The default {@linkplain #maxRetries(long) max retries}: {@value}.
		 */
		public static final long DEFAULT_MAX_RETRIES = 3;

		/**
		 * The default {@linkplain #delay(Duration) delay}: {@value} ms.
		 */
		public static final long DEFAULT_DELAY = 1000;

		/**
		 * The default {@linkplain #maxDelay(Duration) max delay}: {@value} ms.
		 * @see Long#MAX_VALUE
		 */
		public static final long DEFAULT_MAX_DELAY = Long.MAX_VALUE;

		/**
		 * The default {@linkplain #multiplier(double) multiplier}: {@value}.
		 */
		public static final double DEFAULT_MULTIPLIER = 1.0;


		private @Nullable BackOff backOff;

		private @Nullable Long maxRetries;

		private Duration timeout = Duration.ZERO;

		private @Nullable Duration delay;

		private @Nullable Duration jitter;

		private @Nullable Double multiplier;

		private @Nullable Duration maxDelay;

		private final Set<Class<? extends Throwable>> includes = new LinkedHashSet<>();

		private final Set<Class<? extends Throwable>> excludes = new LinkedHashSet<>();

		private @Nullable Predicate<Throwable> predicate;


		private Builder() {
			// internal constructor
		}


		/**
		 * Specify the {@link BackOff} strategy to use.
		 * <p>The supplied value will override any previously configured value.
		 * <p><strong>WARNING</strong>: If you configure a custom {@code BackOff}
		 * strategy, you should not configure any of the following:
		 * {@link #maxRetries(long) maxRetries}, {@link #delay(Duration) delay},
		 * {@link #jitter(Duration) jitter}, {@link #multiplier(double) multiplier},
		 * or {@link #maxDelay(Duration) maxDelay}.
		 * @param backOff the {@code BackOff} strategy
		 * @return this {@code Builder} instance for chained method invocations
		 */
		public Builder backOff(BackOff backOff) {
			Assert.notNull(backOff, "BackOff must not be null");
			this.backOff = backOff;
			return this;
		}

		/**
		 * Specify the maximum number of retry attempts.
		 * <p>Note that {@code total attempts = 1 initial attempt + maxRetries attempts}.
		 * Thus, if {@code maxRetries} is set to 4, a retryable operation will be
		 * invoked at least once and at most 5 times.
		 * <p>The default is {@value #DEFAULT_MAX_RETRIES}.
		 * <p>The supplied value will override any previously configured value.
		 * <p>You should not specify this configuration option if you have
		 * configured a custom {@link #backOff(BackOff) BackOff} strategy.
		 * @param maxRetries the maximum number of retry attempts;
		 * must be positive (or zero for no retry)
		 * @return this {@code Builder} instance for chained method invocations
		 */
		public Builder maxRetries(long maxRetries) {
			assertMaxRetriesIsNotNegative(maxRetries);
			this.maxRetries = maxRetries;
			return this;
		}

		/**
		 * Specify a timeout for the maximum amount of elapsed time allowed for
		 * the initial invocation and any subsequent retry attempts, including
		 * delays.
		 * <p>The default is {@link Duration#ZERO}, which signals that no timeout
		 * should be applied.
		 * <p>The supplied value will override any previously configured value.
		 * @param timeout the timeout, typically in milliseconds or seconds;
		 * must be greater than or equal to zero
		 * @return this {@code Builder} instance for chained method invocations
		 * @since 7.0.2
		 */
		public Builder timeout(Duration timeout) {
			assertIsNotNegative("timeout", timeout);
			this.timeout = timeout;
			return this;
		}

		/**
		 * Specify the base delay after the initial invocation.
		 * <p>If a {@linkplain #multiplier(double) multiplier} is specified, this
		 * serves as the initial delay to multiply from.
		 * <p>The default is {@value #DEFAULT_DELAY} milliseconds.
		 * <p>The supplied value will override any previously configured value.
		 * <p>You should not specify this configuration option if you have
		 * configured a custom {@link #backOff(BackOff) BackOff} strategy.
		 * @param delay the base delay, typically in milliseconds or seconds;
		 * must be greater than or equal to zero
		 * @return this {@code Builder} instance for chained method invocations
		 * @see #jitter(Duration)
		 * @see #multiplier(double)
		 * @see #maxDelay(Duration)
		 */
		public Builder delay(Duration delay) {
			assertIsNotNegative("delay", delay);
			this.delay = delay;
			return this;
		}

		/**
		 * Specify a jitter value for the base retry attempt, randomly subtracted
		 * or added to the calculated delay, resulting in a value between
		 * {@code delay - jitter} and {@code delay + jitter} but never below the
		 * {@linkplain #delay(Duration) base delay} or above the
		 * {@linkplain #maxDelay(Duration) max delay}.
		 * <p>If a {@linkplain #multiplier(double) multiplier} is specified, it
		 * is applied to the jitter value as well.
		 * <p>The default is no jitter.
		 * <p>The supplied value will override any previously configured value.
		 * <p>You should not specify this configuration option if you have
		 * configured a custom {@link #backOff(BackOff) BackOff} strategy.
		 * @param jitter the jitter value, typically in milliseconds; must be
		 * greater than or equal to zero
		 * @return this {@code Builder} instance for chained method invocations
		 * @see #delay(Duration)
		 * @see #multiplier(double)
		 * @see #maxDelay(Duration)
		 */
		public Builder jitter(Duration jitter) {
			assertIsNotNegative("jitter", jitter);
			this.jitter = jitter;
			return this;
		}

		/**
		 * Specify a multiplier for a delay for the next retry attempt, applied
		 * to the previous delay (starting with the initial
		 * {@linkplain #delay(Duration) delay}) as well as to the applicable
		 * {@linkplain #jitter(Duration) jitter} for each attempt.
		 * <p>The default is {@value Builder#DEFAULT_MULTIPLIER}, effectively
		 * resulting in a fixed delay.
		 * <p>The supplied value will override any previously configured value.
		 * <p>You should not specify this configuration option if you have
		 * configured a custom {@link #backOff(BackOff) BackOff} strategy.
		 * @param multiplier the multiplier value; must be greater than or equal to 1
		 * @return this {@code Builder} instance for chained method invocations
		 * @see #delay(Duration)
		 * @see #jitter(Duration)
		 * @see #maxDelay(Duration)
		 */
		public Builder multiplier(double multiplier) {
			Assert.isTrue(multiplier >= 1, () -> "Invalid multiplier '" + multiplier + "': " +
					"must be greater than or equal to 1. A multiplier of 1 is equivalent to a fixed delay.");
			this.multiplier = multiplier;
			return this;
		}

		/**
		 * Specify the maximum delay for any retry attempt, limiting how far
		 * {@linkplain #jitter(Duration) jitter} and the
		 * {@linkplain #multiplier(double) multiplier} can increase the
		 * {@linkplain #delay(Duration) delay}.
		 * <p>The default is unlimited.
		 * <p>The supplied value will override any previously configured value.
		 * <p>You should not specify this configuration option if you have
		 * configured a custom {@link #backOff(BackOff) BackOff} strategy.
		 * @param maxDelay the maximum delay; must be greater than zero
		 * @return this {@code Builder} instance for chained method invocations
		 * @see #delay(Duration)
		 * @see #jitter(Duration)
		 * @see #multiplier(double)
		 */
		public Builder maxDelay(Duration maxDelay) {
			assertIsPositive("maxDelay", maxDelay);
			this.maxDelay = maxDelay;
			return this;
		}

		/**
		 * Specify the types of exceptions for which the {@link RetryPolicy}
		 * should retry a failed operation.
		 * <p>Defaults to all exception types.
		 * <p>The supplied exception types will be matched against an exception
		 * thrown by a failed operation as well as nested
		 * {@linkplain Throwable#getCause() causes}.
		 * <p>If included exception types have already been configured, the supplied
		 * types will be added to the existing list of included types.
		 * <p>This can be combined with other {@code includes}, {@code excludes},
		 * and a custom {@code predicate}.
		 * @param types the types of exceptions to include in the policy
		 * @return this {@code Builder} instance for chained method invocations
		 * @see #includes(Collection)
		 * @see #excludes(Class...)
		 * @see #excludes(Collection)
		 * @see #predicate(Predicate)
		 */
		@SafeVarargs // Making the method final allows us to use @SafeVarargs.
		@SuppressWarnings("varargs")
		public final Builder includes(Class<? extends Throwable>... types) {
			Collections.addAll(this.includes, types);
			return this;
		}

		/**
		 * Specify the types of exceptions for which the {@link RetryPolicy}
		 * should retry a failed operation.
		 * <p>Defaults to all exception types.
		 * <p>The supplied exception types will be matched against an exception
		 * thrown by a failed operation as well as nested
		 * {@linkplain Throwable#getCause() causes}.
		 * <p>If included exception types have already been configured, the supplied
		 * types will be added to the existing list of included types.
		 * <p>This can be combined with other {@code includes}, {@code excludes},
		 * and a custom {@code predicate}.
		 * @param types the types of exceptions to include in the policy
		 * @return this {@code Builder} instance for chained method invocations
		 * @see #includes(Class...)
		 * @see #excludes(Class...)
		 * @see #excludes(Collection)
		 * @see #predicate(Predicate)
		 */
		public Builder includes(Collection<Class<? extends Throwable>> types) {
			this.includes.addAll(types);
			return this;
		}

		/**
		 * Specify the types of exceptions for which the {@link RetryPolicy}
		 * should not retry a failed operation.
		 * <p>The supplied exception types will be matched against an exception
		 * thrown by a failed operation as well as nested
		 * {@linkplain Throwable#getCause() causes}.
		 * <p>If excluded exception types have already been configured, the supplied
		 * types will be added to the existing list of excluded types.
		 * <p>This can be combined with {@code includes}, other {@code excludes},
		 * and a custom {@code predicate}.
		 * @param types the types of exceptions to exclude from the policy
		 * @return this {@code Builder} instance for chained method invocations
		 * @see #includes(Class...)
		 * @see #includes(Collection)
		 * @see #excludes(Collection)
		 * @see #predicate(Predicate)
		 */
		@SafeVarargs // Making the method final allows us to use @SafeVarargs.
		@SuppressWarnings("varargs")
		public final Builder excludes(Class<? extends Throwable>... types) {
			Collections.addAll(this.excludes, types);
			return this;
		}

		/**
		 * Specify the types of exceptions for which the {@link RetryPolicy}
		 * should not retry a failed operation.
		 * <p>The supplied exception types will be matched against an exception
		 * thrown by a failed operation as well as nested
		 * {@linkplain Throwable#getCause() causes}.
		 * <p>If excluded exception types have already been configured, the supplied
		 * types will be added to the existing list of excluded types.
		 * <p>This can be combined with {@code includes}, other {@code excludes},
		 * and a custom {@code predicate}.
		 * @param types the types of exceptions to exclude from the policy
		 * @return this {@code Builder} instance for chained method invocations
		 * @see #includes(Class...)
		 * @see #includes(Collection)
		 * @see #excludes(Class...)
		 * @see #predicate(Predicate)
		 */
		public Builder excludes(Collection<Class<? extends Throwable>> types) {
			this.excludes.addAll(types);
			return this;
		}

		/**
		 * Specify a custom {@link Predicate} that the {@link RetryPolicy} will
		 * use to determine whether to retry a failed operation based on a given
		 * {@link Throwable}.
		 * <p>If a predicate has already been configured, the supplied predicate
		 * will be {@linkplain Predicate#and(Predicate) combined} with the
		 * existing predicate.
		 * <p>This can be combined with {@code includes} and {@code excludes}.
		 * @param predicate a custom predicate
		 * @return this {@code Builder} instance for chained method invocations
		 * @see #includes(Class...)
		 * @see #includes(Collection)
		 * @see #excludes(Class...)
		 * @see #excludes(Collection)
		 */
		public Builder predicate(Predicate<Throwable> predicate) {
			this.predicate = (this.predicate != null ? this.predicate.and(predicate) : predicate);
			return this;
		}

		/**
		 * Build the configured {@link RetryPolicy}.
		 */
		public RetryPolicy build() {
			BackOff backOff = this.backOff;
			if (backOff != null) {
				boolean misconfigured = (this.maxRetries != null || this.delay != null || this.jitter != null ||
						this.multiplier != null || this.maxDelay != null);
				Assert.state(!misconfigured, """
						The following configuration options are not supported with a custom BackOff strategy: \
						maxRetries, delay, jitter, multiplier, or maxDelay.""");
			}
			else {
				ExponentialBackOff exponentialBackOff = new ExponentialBackOff();
				exponentialBackOff.setMaxAttempts(this.maxRetries != null ? this.maxRetries : DEFAULT_MAX_RETRIES);
				exponentialBackOff.setInitialInterval(this.delay != null ? this.delay.toMillis() : DEFAULT_DELAY);
				exponentialBackOff.setMaxInterval(this.maxDelay != null ? this.maxDelay.toMillis() : DEFAULT_MAX_DELAY);
				exponentialBackOff.setMultiplier(this.multiplier != null ? this.multiplier : DEFAULT_MULTIPLIER);
				if (this.jitter != null) {
					exponentialBackOff.setJitter(this.jitter.toMillis());
				}
				backOff = exponentialBackOff;
			}
			return new DefaultRetryPolicy(this.includes, this.excludes, this.predicate, this.timeout, backOff);
		}
	}

}

/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.scheduling.support;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A trigger for periodic task execution. The period may be applied as either
 * fixed-rate or fixed-delay, and an initial delay value may also be configured.
 * The default initial delay is 0, and the default behavior is fixed-delay
 * (i.e. the interval between successive executions is measured from each
 * <i>completion</i> time). To measure the interval between the
 * scheduled <i>start</i> time of each execution instead, set the
 * 'fixedRate' property to {@code true}.
 *
 * <p>Note that the TaskScheduler interface already defines methods for scheduling
 * tasks at fixed-rate or with fixed-delay. Both also support an optional value
 * for the initial delay. Those methods should be used directly whenever
 * possible. The value of this Trigger implementation is that it can be used
 * within components that rely on the Trigger abstraction. For example, it may
 * be convenient to allow periodic triggers, cron-based triggers, and even
 * custom Trigger implementations to be used interchangeably.
 *
 * @author Mark Fisher
 * @since 3.0
 */
public class PeriodicTrigger implements Trigger {

	private final Duration period;

	@Nullable
	private final ChronoUnit chronoUnit;

	@Nullable
	private volatile Duration initialDelay;

	private volatile boolean fixedRate;


	/**
	 * Create a trigger with the given period in milliseconds.
	 * @deprecated as of 6.0, in favor on {@link #PeriodicTrigger(Duration)}
	 */
	@Deprecated(since = "6.0")
	public PeriodicTrigger(long period) {
		this(period, null);
	}

	/**
	 * Create a trigger with the given period and time unit. The time unit will
	 * apply not only to the period but also to any 'initialDelay' value, if
	 * configured on this Trigger later via {@link #setInitialDelay(long)}.
	 * @deprecated as of 6.0, in favor on {@link #PeriodicTrigger(Duration)}
	 */
	@Deprecated(since = "6.0")
	public PeriodicTrigger(long period, @Nullable TimeUnit timeUnit) {
		this(toDuration(period, timeUnit), timeUnit);
	}

	private static Duration toDuration(long amount, @Nullable TimeUnit timeUnit) {
		if (timeUnit != null) {
			return Duration.of(amount, timeUnit.toChronoUnit());
		}
		else {
			return Duration.ofMillis(amount);
		}
	}

	/**
	 * Create a trigger with the given period as a duration.
	 * @since 6.0
	 */
	public PeriodicTrigger(Duration period) {
		this(period, null);
	}

	private PeriodicTrigger(Duration period, @Nullable TimeUnit timeUnit) {
		Assert.notNull(period, "Period must not be null");
		Assert.isTrue(!period.isNegative(), "Period must not be negative");
		this.period = period;
		if (timeUnit != null) {
			this.chronoUnit = timeUnit.toChronoUnit();
		}
		else {
			this.chronoUnit = null;
		}
	}


	/**
	 * Return this trigger's period.
	 * @since 5.0.2
	 * @deprecated as of 6.0, in favor on {@link #getPeriodDuration()}
	 */
	@Deprecated(since = "6.0")
	public long getPeriod() {
		if (this.chronoUnit != null) {
			return this.period.get(this.chronoUnit);
		}
		else {
			return this.period.toMillis();
		}
	}

	/**
	 * Return this trigger's period.
	 * @since 6.0
	 */
	public Duration getPeriodDuration() {
		return this.period;
	}

	/**
	 * Return this trigger's time unit (milliseconds by default).
	 * @since 5.0.2
	 * @deprecated as of 6.0, with no direct replacement
	 */
	@Deprecated(since = "6.0")
	public TimeUnit getTimeUnit() {
		if (this.chronoUnit != null) {
			return TimeUnit.of(this.chronoUnit);
		}
		else {
			return TimeUnit.MILLISECONDS;
		}
	}

	/**
	 * Specify the delay for the initial execution. It will be evaluated in
	 * terms of this trigger's {@link TimeUnit}. If no time unit was explicitly
	 * provided upon instantiation, the default is milliseconds.
	 * @deprecated as of 6.0, in favor of {@link #setInitialDelay(Duration)}
	 */
	@Deprecated(since = "6.0")
	public void setInitialDelay(long initialDelay) {
		if (this.chronoUnit != null) {
			this.initialDelay = Duration.of(initialDelay, this.chronoUnit);
		}
		else {
			this.initialDelay = Duration.ofMillis(initialDelay);
		}
	}

	/**
	 * Specify the delay for the initial execution.
	 * @since 6.0
	 */
	public void setInitialDelay(Duration initialDelay) {
		this.initialDelay = initialDelay;
	}

	/**
	 * Return the initial delay, or 0 if none.
	 * @since 5.0.2
	 * @deprecated as of 6.0, in favor on {@link #getInitialDelayDuration()}
	 */
	@Deprecated(since = "6.0")
	public long getInitialDelay() {
		Duration initialDelay = this.initialDelay;
		if (initialDelay != null) {
			if (this.chronoUnit != null) {
				return initialDelay.get(this.chronoUnit);
			}
			else {
				return initialDelay.toMillis();
			}
		}
		else {
			return 0;
		}
	}

	/**
	 * Return the initial delay, or {@code null} if none.
	 * @since 6.0
	 */
	@Nullable
	public Duration getInitialDelayDuration() {
		return this.initialDelay;
	}

	/**
	 * Specify whether the periodic interval should be measured between the
	 * scheduled start times rather than between actual completion times.
	 * The latter, "fixed delay" behavior, is the default.
	 */
	public void setFixedRate(boolean fixedRate) {
		this.fixedRate = fixedRate;
	}

	/**
	 * Return whether this trigger uses fixed rate ({@code true}) or
	 * fixed delay ({@code false}) behavior.
	 * @since 5.0.2
	 */
	public boolean isFixedRate() {
		return this.fixedRate;
	}


	/**
	 * Returns the time after which a task should run again.
	 */
	@Override
	public Instant nextExecution(TriggerContext triggerContext) {
		Instant lastExecution = triggerContext.lastScheduledExecution();
		Instant lastCompletion = triggerContext.lastCompletion();
		if (lastExecution == null || lastCompletion == null) {
			Instant instant = triggerContext.getClock().instant();
			Duration initialDelay = this.initialDelay;
			if (initialDelay == null) {
				return instant;
			}
			else {
				return instant.plus(initialDelay);
			}
		}
		if (this.fixedRate) {
			return lastExecution.plus(this.period);
		}
		return lastCompletion.plus(this.period);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof PeriodicTrigger that &&
				this.fixedRate == that.fixedRate &&
				this.period.equals(that.period) &&
				ObjectUtils.nullSafeEquals(this.initialDelay, that.initialDelay)));
	}

	@Override
	public int hashCode() {
		return this.period.hashCode();
	}

}

/*
 * Copyright 2002-2024 the original author or authors.
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;

/**
 * {@link Trigger} implementation for cron expressions. Wraps a
 * {@link CronExpression} which parses according to common crontab conventions.
 *
 * <p>Supports a Quartz day-of-month/week field with an L/# expression. Follows
 * common cron conventions in every other respect, including 0-6 for SUN-SAT
 * (plus 7 for SUN as well). Note that Quartz deviates from the day-of-week
 * convention in cron through 1-7 for SUN-SAT whereas Spring strictly follows
 * cron even in combination with the optional Quartz-specific L/# expressions.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 3.0
 * @see CronExpression
 */
public class CronTrigger implements Trigger {

	private final CronExpression expression;

	@Nullable
	private final ZoneId zoneId;


	/**
	 * Build a {@code CronTrigger} from the pattern provided in the default time zone.
	 * <p>This is equivalent to the {@link CronTrigger#forLenientExecution} factory
	 * method. Original trigger firings may be skipped if the previous task is still
	 * running; if this is not desirable, consider {@link CronTrigger#forFixedExecution}.
	 * @param expression a space-separated list of time fields, following cron
	 * expression conventions
	 * @see CronTrigger#forLenientExecution
	 * @see CronTrigger#forFixedExecution
	 */
	public CronTrigger(String expression) {
		this.expression = CronExpression.parse(expression);
		this.zoneId = null;
	}

	/**
	 * Build a {@code CronTrigger} from the pattern provided in the given time zone,
	 * with the same lenient execution as {@link CronTrigger#CronTrigger(String)}.
	 * <p>Note that such explicit time zone customization is usually not necessary,
	 * using {@link org.springframework.scheduling.TaskScheduler#getClock()} instead.
	 * @param expression a space-separated list of time fields, following cron
	 * expression conventions
	 * @param timeZone a time zone in which the trigger times will be generated
	 */
	public CronTrigger(String expression, TimeZone timeZone) {
		this.expression = CronExpression.parse(expression);
		Assert.notNull(timeZone, "TimeZone must not be null");
		this.zoneId = timeZone.toZoneId();
	}

	/**
	 * Build a {@code CronTrigger} from the pattern provided in the given time zone,
	 * with the same lenient execution as {@link CronTrigger#CronTrigger(String)}.
	 * <p>Note that such explicit time zone customization is usually not necessary,
	 * using {@link org.springframework.scheduling.TaskScheduler#getClock()} instead.
	 * @param expression a space-separated list of time fields, following cron
	 * expression conventions
	 * @param zoneId a time zone in which the trigger times will be generated
	 * @since 5.3
	 * @see CronExpression#parse(String)
	 */
	public CronTrigger(String expression, ZoneId zoneId) {
		this.expression = CronExpression.parse(expression);
		Assert.notNull(zoneId, "ZoneId must not be null");
		this.zoneId = zoneId;
	}


	/**
	 * Return the cron pattern that this trigger has been built with.
	 */
	public String getExpression() {
		return this.expression.toString();
	}


	/**
	 * Determine the next execution time according to the given trigger context.
	 * <p>Next execution times are calculated based on the
	 * {@linkplain TriggerContext#lastCompletion completion time} of the
	 * previous execution; therefore, overlapping executions won't occur.
	 */
	@Override
	@Nullable
	public Instant nextExecution(TriggerContext triggerContext) {
		Instant timestamp = determineLatestTimestamp(triggerContext);
		ZoneId zone = (this.zoneId != null ? this.zoneId : triggerContext.getClock().getZone());
		ZonedDateTime zonedTimestamp = ZonedDateTime.ofInstant(timestamp, zone);
		ZonedDateTime nextTimestamp = this.expression.next(zonedTimestamp);
		return (nextTimestamp != null ? nextTimestamp.toInstant() : null);
	}

	Instant determineLatestTimestamp(TriggerContext triggerContext) {
		Instant timestamp = triggerContext.lastCompletion();
		if (timestamp != null) {
			Instant scheduled = triggerContext.lastScheduledExecution();
			if (scheduled != null && timestamp.isBefore(scheduled)) {
				// Previous task apparently executed too early...
				// Let's simply use the last calculated execution time then,
				// in order to prevent accidental re-fires in the same second.
				timestamp = scheduled;
			}
		}
		else {
			timestamp = determineInitialTimestamp(triggerContext);
		}
		return timestamp;
	}

	Instant determineInitialTimestamp(TriggerContext triggerContext) {
		return triggerContext.getClock().instant();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof CronTrigger that &&
				this.expression.equals(that.expression)));
	}

	@Override
	public int hashCode() {
		return this.expression.hashCode();
	}

	@Override
	public String toString() {
		return this.expression.toString();
	}


	/**
	 * Create a {@link CronTrigger} for lenient execution, to be rescheduled
	 * after every task based on the completion time.
	 * <p>This variant does not make up for missed trigger firings if the
	 * associated task has taken too long. As a consequence, original trigger
	 * firings may be skipped if the previous task is still running.
	 * <p>This is equivalent to the regular {@link CronTrigger} constructor.
	 * Note that lenient execution is scheduler-dependent: it may skip trigger
	 * firings with long-running tasks on a thread pool while executing at
	 * {@link #forFixedExecution}-like precision with new threads per task.
	 * @param expression a space-separated list of time fields, following cron
	 * expression conventions
	 * @since 6.1.3
	 * @see #resumeLenientExecution
	 */
	public static CronTrigger forLenientExecution(String expression) {
		return new CronTrigger(expression);
	}

	/**
	 * Create a {@link CronTrigger} for lenient execution, to be rescheduled
	 * after every task based on the completion time.
	 * <p>This variant does not make up for missed trigger firings if the
	 * associated task has taken too long. As a consequence, original trigger
	 * firings may be skipped if the previous task is still running.
	 * @param expression a space-separated list of time fields, following cron
	 * expression conventions
	 * @param resumptionTimestamp the timestamp to resume from (the last-known
	 * completion timestamp), with the new trigger calculated from there and
	 * possibly immediately firing (but only once, every subsequent calculation
	 * will start from the completion time of that first resumed trigger)
	 * @since 6.1.3
	 * @see #forLenientExecution
	 */
	public static CronTrigger resumeLenientExecution(String expression, Instant resumptionTimestamp) {
		return new CronTrigger(expression) {
			@Override
			Instant determineInitialTimestamp(TriggerContext triggerContext) {
				return resumptionTimestamp;
			}
		};
	}

	/**
	 * Create a {@link CronTrigger} for fixed execution, to be rescheduled
	 * after every task based on the last scheduled time.
	 * <p>This variant makes up for missed trigger firings if the associated task
	 * has taken too long, scheduling a task for every original trigger firing.
	 * Such follow-up tasks may execute late but will never be skipped.
	 * <p>Immediate versus late execution in case of long-running tasks may
	 * be scheduler-dependent but the guarantee to never skip a task is portable.
	 * @param expression a space-separated list of time fields, following cron
	 * expression conventions
	 * @since 6.1.3
	 * @see #resumeFixedExecution
	 */
	public static CronTrigger forFixedExecution(String expression) {
		return new CronTrigger(expression) {
			@Override
			protected Instant determineLatestTimestamp(TriggerContext triggerContext) {
				Instant scheduled = triggerContext.lastScheduledExecution();
				return (scheduled != null ? scheduled : super.determineInitialTimestamp(triggerContext));
			}
		};
	}

	/**
	 * Create a {@link CronTrigger} for fixed execution, to be rescheduled
	 * after every task based on the last scheduled time.
	 * <p>This variant makes up for missed trigger firings if the associated task
	 * has taken too long, scheduling a task for every original trigger firing.
	 * Such follow-up tasks may execute late but will never be skipped.
	 * @param expression a space-separated list of time fields, following cron
	 * expression conventions
	 * @param resumptionTimestamp the timestamp to resume from (the last-known
	 * scheduled timestamp), with every trigger in-between immediately firing
	 * to make up for every execution that would have happened in the meantime
	 * @since 6.1.3
	 * @see #forFixedExecution
	 */
	public static CronTrigger resumeFixedExecution(String expression, Instant resumptionTimestamp) {
		return new CronTrigger(expression) {
			@Override
			protected Instant determineLatestTimestamp(TriggerContext triggerContext) {
				Instant scheduled = triggerContext.lastScheduledExecution();
				return (scheduled != null ? scheduled : super.determineLatestTimestamp(triggerContext));
			}
			@Override
			Instant determineInitialTimestamp(TriggerContext triggerContext) {
				return resumptionTimestamp;
			}
		};
	}

}

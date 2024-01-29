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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
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
	 * @param expression a space-separated list of time fields, following cron
	 * expression conventions
	 */
	public CronTrigger(String expression) {
		this.expression = CronExpression.parse(expression);
		this.zoneId = null;
	}

	/**
	 * Build a {@code CronTrigger} from the pattern provided in the given time zone.
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
	 * Build a {@code CronTrigger} from the pattern provided in the given time zone.
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
	 * {@linkplain TriggerContext#lastCompletionTime completion time} of the
	 * previous execution; therefore, overlapping executions won't occur.
	 */
	@Override
	public Date nextExecutionTime(TriggerContext triggerContext) {
		Date timestamp = triggerContext.lastCompletionTime();
		if (timestamp != null) {
			Date scheduled = triggerContext.lastScheduledExecutionTime();
			if (scheduled != null && timestamp.before(scheduled)) {
				// Previous task apparently executed too early...
				// Let's simply use the last calculated execution time then,
				// in order to prevent accidental re-fires in the same second.
				timestamp = scheduled;
			}
		}
		else {
			timestamp = new Date(triggerContext.getClock().millis());
		}
		ZoneId zone = (this.zoneId != null ? this.zoneId : triggerContext.getClock().getZone());
		ZonedDateTime zonedTimestamp = ZonedDateTime.ofInstant(timestamp.toInstant(), zone);
		ZonedDateTime nextTimestamp = this.expression.next(zonedTimestamp);
		return (nextTimestamp != null ? Date.from(nextTimestamp.toInstant()) : null);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof CronTrigger &&
				this.expression.equals(((CronTrigger) other).expression)));
	}

	@Override
	public int hashCode() {
		return this.expression.hashCode();
	}

	@Override
	public String toString() {
		return this.expression.toString();
	}

}

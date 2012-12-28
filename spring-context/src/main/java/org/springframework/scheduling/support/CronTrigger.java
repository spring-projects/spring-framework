/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scheduling.support;

import java.util.Date;
import java.util.TimeZone;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

/**
 * {@link Trigger} implementation for cron expressions.
 * Wraps a {@link CronSequenceGenerator}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see CronSequenceGenerator
 */
public class CronTrigger implements Trigger {

	private final CronSequenceGenerator sequenceGenerator;


	/**
	 * Build a {@link CronTrigger} from the pattern provided in the default time zone.
	 * @param cronExpression a space-separated list of time fields,
	 * following cron expression conventions
	 */
	public CronTrigger(String cronExpression) {
		this(cronExpression, TimeZone.getDefault());
	}

	/**
	 * Build a {@link CronTrigger} from the pattern provided.
	 * @param cronExpression a space-separated list of time fields,
	 * following cron expression conventions
	 * @param timeZone a time zone in which the trigger times will be generated
	 */
	public CronTrigger(String cronExpression, TimeZone timeZone) {
		this.sequenceGenerator = new CronSequenceGenerator(cronExpression, timeZone);
	}


	public Date nextExecutionTime(TriggerContext triggerContext) {
		Date date = triggerContext.lastCompletionTime();
		if (date != null) {
			Date scheduled = triggerContext.lastScheduledExecutionTime();
			if (scheduled != null && date.before(scheduled)) {
				// Previous task apparently executed too early...
				// Let's simply use the last calculated execution time then,
				// in order to prevent accidental re-fires in the same second.
				date = scheduled;
			}
		}
		else {
			date = new Date();
		}
		return this.sequenceGenerator.next(date);
	}

	public String getExpression() {
		return this.sequenceGenerator.getExpression();
	}

	@Override
	public boolean equals(Object obj) {
		return (this == obj || (obj instanceof CronTrigger &&
				this.sequenceGenerator.equals(((CronTrigger) obj).sequenceGenerator)));
	}

	@Override
	public int hashCode() {
		return this.sequenceGenerator.hashCode();
	}

	@Override
	public String toString() {
		return sequenceGenerator.toString();
	}

}

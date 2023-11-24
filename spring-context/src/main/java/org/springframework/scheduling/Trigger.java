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

package org.springframework.scheduling;

import java.time.Instant;
import java.util.Date;

import org.springframework.lang.Nullable;

/**
 * Common interface for trigger objects that determine the next execution time
 * of a task that they get associated with.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see TaskScheduler#schedule(Runnable, Trigger)
 * @see org.springframework.scheduling.support.CronTrigger
 */
public interface Trigger {

	/**
	 * Determine the next execution time according to the given trigger context.
	 * <p>The default implementation delegates to {@link #nextExecution(TriggerContext)}.
	 * @param triggerContext context object encapsulating last execution times
	 * and last completion time
	 * @return the next execution time as defined by the trigger,
	 * or {@code null} if the trigger won't fire anymore
	 * @deprecated as of 6.0, in favor of {@link #nextExecution(TriggerContext)}
	 */
	@Deprecated(since = "6.0")
	@Nullable
	default Date nextExecutionTime(TriggerContext triggerContext) {
		Instant instant = nextExecution(triggerContext);
		return (instant != null ? Date.from(instant) : null);
	}

	/**
	 * Determine the next execution time according to the given trigger context.
	 * @param triggerContext context object encapsulating last execution times
	 * and last completion time
	 * @return the next execution time as defined by the trigger,
	 * or {@code null} if the trigger won't fire anymore
	 * @since 6.0
	 */
	@Nullable
	Instant nextExecution(TriggerContext triggerContext);

}

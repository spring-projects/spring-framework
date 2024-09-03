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

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import org.springframework.lang.Nullable;

/**
 * Context object encapsulating last execution times and last completion time
 * of a given task.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface TriggerContext {

	/**
	 * Return the clock to use for trigger calculation.
	 * <p>Defaults to {@link Clock#systemDefaultZone()}.
	 * @since 5.3
	 * @see TaskScheduler#getClock()
	 */
	default Clock getClock() {
		return Clock.systemDefaultZone();
	}

	/**
	 * Return the last <i>scheduled</i> execution time of the task,
	 * or {@code null} if not scheduled before.
	 * <p>The default implementation delegates to {@link #lastScheduledExecution()}.
	 * @deprecated as of 6.0, in favor on {@link #lastScheduledExecution()}
	 */
	@Nullable
	@Deprecated(since = "6.0")
	default Date lastScheduledExecutionTime() {
		Instant instant = lastScheduledExecution();
		return instant != null ? Date.from(instant) : null;
	}

	/**
	 * Return the last <i>scheduled</i> execution time of the task,
	 * or {@code null} if not scheduled before.
	 * @since 6.0
	 */
	@Nullable
	Instant lastScheduledExecution();

	/**
	 * Return the last <i>actual</i> execution time of the task,
	 * or {@code null} if not scheduled before.
	 * <p>The default implementation delegates to {@link #lastActualExecution()}.
	 * @deprecated as of 6.0, in favor on {@link #lastActualExecution()}
	 */
	@Nullable
	@Deprecated(since = "6.0")
	default Date lastActualExecutionTime() {
		Instant instant = lastActualExecution();
		return instant != null ? Date.from(instant) : null;
	}

	/**
	 * Return the last <i>actual</i> execution time of the task,
	 * or {@code null} if not scheduled before.
	 * @since 6.0
	 */
	@Nullable
	Instant lastActualExecution();

	/**
	 * Return the last completion time of the task,
	 * or {@code null} if not scheduled before.
	 * <p>The default implementation delegates to {@link #lastCompletion()}.
	 * @deprecated as of 6.0, in favor on {@link #lastCompletion()}
	 */
	@Deprecated(since = "6.0")
	@Nullable
	default Date lastCompletionTime() {
		Instant instant = lastCompletion();
		return instant != null ? Date.from(instant) : null;
	}

	/**
	 * Return the last completion time of the task,
	 * or {@code null} if not scheduled before.
	 * @since 6.0
	 */
	@Nullable
	Instant lastCompletion();

}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import org.jspecify.annotations.Nullable;

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
	@Deprecated(since = "6.0")
	default @Nullable Date lastScheduledExecutionTime() {
		Instant instant = lastScheduledExecution();
		return (instant != null ? Date.from(instant) : null);
	}

	/**
	 * Return the last <i>scheduled</i> execution time of the task,
	 * or {@code null} if not scheduled before.
	 * @since 6.0
	 */
	@Nullable Instant lastScheduledExecution();

	/**
	 * Return the last <i>actual</i> execution time of the task,
	 * or {@code null} if not scheduled before.
	 * <p>The default implementation delegates to {@link #lastActualExecution()}.
	 * @deprecated as of 6.0, in favor on {@link #lastActualExecution()}
	 */
	@Deprecated(since = "6.0")
	default @Nullable Date lastActualExecutionTime() {
		Instant instant = lastActualExecution();
		return (instant != null ? Date.from(instant) : null);
	}

	/**
	 * Return the last <i>actual</i> execution time of the task,
	 * or {@code null} if not scheduled before.
	 * @since 6.0
	 */
	@Nullable Instant lastActualExecution();

	/**
	 * Return the last completion time of the task,
	 * or {@code null} if not scheduled before.
	 * <p>The default implementation delegates to {@link #lastCompletion()}.
	 * @deprecated as of 6.0, in favor on {@link #lastCompletion()}
	 */
	@Deprecated(since = "6.0")
	default @Nullable Date lastCompletionTime() {
		Instant instant = lastCompletion();
		return (instant != null ? Date.from(instant) : null);
	}

	/**
	 * Return the last completion time of the task,
	 * or {@code null} if not scheduled before.
	 * @since 6.0
	 */
	@Nullable Instant lastCompletion();

}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;

/**
 * Holder class defining a {@code Runnable} to be executed as a task, typically at a
 * scheduled time or interval. See subclass hierarchy for various scheduling approaches.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 3.2
 */
public class Task {

	private final Runnable runnable;

	private TaskExecutionOutcome lastExecutionOutcome;


	/**
	 * Create a new {@code Task}.
	 * @param runnable the underlying task to execute
	 */
	public Task(Runnable runnable) {
		Assert.notNull(runnable, "Runnable must not be null");
		this.runnable = new OutcomeTrackingRunnable(runnable);
		this.lastExecutionOutcome = TaskExecutionOutcome.create();
	}


	/**
	 * Return a {@link Runnable} that executes the underlying task.
	 * <p>Note, this does not necessarily return the {@link Task#Task(Runnable) original runnable}
	 * as it can be wrapped by the Framework for additional support.
	 */
	public Runnable getRunnable() {
		return this.runnable;
	}

	/**
	 * Return the outcome of the last task execution.
	 * @since 6.2
	 */
	public TaskExecutionOutcome getLastExecutionOutcome() {
		return this.lastExecutionOutcome;
	}

	@Override
	public String toString() {
		return this.runnable.toString();
	}


	private class OutcomeTrackingRunnable implements SchedulingAwareRunnable {

		private final Runnable runnable;

		public OutcomeTrackingRunnable(Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public void run() {
			try {
				Task.this.lastExecutionOutcome = Task.this.lastExecutionOutcome.start(Instant.now());
				this.runnable.run();
				Task.this.lastExecutionOutcome = Task.this.lastExecutionOutcome.success();
			}
			catch (Throwable exc) {
				Task.this.lastExecutionOutcome = Task.this.lastExecutionOutcome.failure(exc);
				throw exc;
			}
		}

		@Override
		public boolean isLongLived() {
			if (this.runnable instanceof SchedulingAwareRunnable sar) {
				return sar.isLongLived();
			}
			return SchedulingAwareRunnable.super.isLongLived();
		}

		@Override
		public @Nullable String getQualifier() {
			if (this.runnable instanceof SchedulingAwareRunnable sar) {
				return sar.getQualifier();
			}
			return SchedulingAwareRunnable.super.getQualifier();
		}

		@Override
		public String toString() {
			return this.runnable.toString();
		}
	}

}

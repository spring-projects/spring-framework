/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Outcome of a {@link Task} execution.
 *
 * @author Brian Clozel
 * @since 6.2
 * @param executionTime the instant when the task execution started, or
 * {@code null} if the task has not started
 * @param status the {@link Status} of the execution outcome
 * @param throwable the exception thrown from the task execution, if any
 */
public record TaskExecutionOutcome(@Nullable Instant executionTime, Status status, @Nullable Throwable throwable) {

	TaskExecutionOutcome start(Instant executionTime) {
		return new TaskExecutionOutcome(executionTime, Status.STARTED, null);
	}

	TaskExecutionOutcome success() {
		Assert.state(this.executionTime != null, "Task has not been started yet");
		return new TaskExecutionOutcome(this.executionTime, Status.SUCCESS, null);
	}

	TaskExecutionOutcome failure(Throwable throwable) {
		Assert.state(this.executionTime != null, "Task has not been started yet");
		return new TaskExecutionOutcome(this.executionTime, Status.ERROR, throwable);
	}

	static TaskExecutionOutcome create() {
		return new TaskExecutionOutcome(null, Status.NONE, null);
	}


	/**
	 * Status of the task execution outcome.
	 */
	public enum Status {

		/**
		 * The task has not been executed so far.
		 */
		NONE,

		/**
		 * The task execution has been started and is ongoing.
		 */
		STARTED,

		/**
		 * The task execution finished successfully.
		 */
		SUCCESS,

		/**
		 * The task execution finished with an error.
		 */
		ERROR

	}

}

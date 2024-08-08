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

package org.springframework.scheduling.config;

import java.time.Instant;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Outcome of a {@link Task} execution.
 * @param executionTime the instant when the task execution started, {@code null} if the task has not started.
 * @param status        the {@link Status} of the execution outcome.
 * @param throwable     the exception thrown from the task execution, if any.
 * @author Brian Clozel
 * @since 6.2
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

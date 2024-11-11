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
	 * Return the underlying task.
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


	private class OutcomeTrackingRunnable implements Runnable {

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
		public String toString() {
			return this.runnable.toString();
		}
	}

}

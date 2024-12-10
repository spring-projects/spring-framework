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

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Task}.
 *
 * @author Brian Clozel
 * @since 6.2
 */
class TaskTests {

	@Test
	void shouldRejectNullRunnable() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Task(null));
	}

	@Test
	void initialStateShouldBeUnknown() {
		TestRunnable testRunnable = new TestRunnable();
		Task task = new Task(testRunnable);
		assertThat(testRunnable.hasRun).isFalse();
		TaskExecutionOutcome executionOutcome = task.getLastExecutionOutcome();
		assertThat(executionOutcome.executionTime()).isNull();
		assertThat(executionOutcome.status()).isEqualTo(TaskExecutionOutcome.Status.NONE);
		assertThat(executionOutcome.throwable()).isNull();
	}

	@Test
	void stateShouldUpdateAfterRun() {
		TestRunnable testRunnable = new TestRunnable();
		Task task = new Task(testRunnable);
		task.getRunnable().run();

		assertThat(testRunnable.hasRun).isTrue();
		TaskExecutionOutcome executionOutcome = task.getLastExecutionOutcome();
		assertThat(executionOutcome.executionTime()).isInThePast();
		assertThat(executionOutcome.status()).isEqualTo(TaskExecutionOutcome.Status.SUCCESS);
		assertThat(executionOutcome.throwable()).isNull();
	}

	@Test
	void stateShouldUpdateAfterFailingRun() {
		FailingTestRunnable testRunnable = new FailingTestRunnable();
		Task task = new Task(testRunnable);
		assertThatIllegalStateException().isThrownBy(() -> task.getRunnable().run());

		assertThat(testRunnable.hasRun).isTrue();
		TaskExecutionOutcome executionOutcome = task.getLastExecutionOutcome();
		assertThat(executionOutcome.executionTime()).isInThePast();
		assertThat(executionOutcome.status()).isEqualTo(TaskExecutionOutcome.Status.ERROR);
		assertThat(executionOutcome.throwable()).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void shouldDelegateToSchedulingAwareRunnable() throws Exception {
		ScheduledMethodRunnable methodRunnable = new ScheduledMethodRunnable(new TestRunnable(),
				TestRunnable.class.getMethod("run"), "myScheduler", TestObservationRegistry::create);
		Task task = new Task(methodRunnable);

		assertThat(task.getRunnable()).isInstanceOf(SchedulingAwareRunnable.class);
		SchedulingAwareRunnable actual = (SchedulingAwareRunnable) task.getRunnable();
		assertThat(actual.getQualifier()).isEqualTo(methodRunnable.getQualifier());
		assertThat(actual.isLongLived()).isEqualTo(methodRunnable.isLongLived());
	}


	static class TestRunnable implements Runnable {

		boolean hasRun;

		@Override
		public void run() {
			try {
				Thread.sleep(1);
			}
			catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
			this.hasRun = true;
		}
	}


	static class FailingTestRunnable implements Runnable {

		boolean hasRun;

		@Override
		public void run() {
			try {
				Thread.sleep(1);
			}
			catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
			this.hasRun = true;
			throw new IllegalStateException("test exception");
		}
	}

}

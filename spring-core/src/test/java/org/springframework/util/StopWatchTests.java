/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.util;

import org.junit.jupiter.api.Test;

import org.springframework.util.StopWatch.TaskInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link StopWatch}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class StopWatchTests {

	private static final String ID = "myId";

	private static final String name1 = "Task 1";
	private static final String name2 = "Task 2";

	private static final long duration1 = 200;
	private static final long duration2 = 100;
	// private static final long fudgeFactor = 100;

	private final StopWatch stopWatch = new StopWatch(ID);


	@Test
	void failureToStartBeforeGettingTimings() {
		assertThatIllegalStateException().isThrownBy(stopWatch::getLastTaskTimeMillis);
	}

	@Test
	void failureToStartBeforeStop() {
		assertThatIllegalStateException().isThrownBy(stopWatch::stop);
	}

	@Test
	void rejectsStartTwice() {
		stopWatch.start();
		assertThat(stopWatch.isRunning()).isTrue();
		stopWatch.stop();
		assertThat(stopWatch.isRunning()).isFalse();

		stopWatch.start();
		assertThat(stopWatch.isRunning()).isTrue();
		assertThatIllegalStateException().isThrownBy(stopWatch::start);
	}

	@Test
	void validUsage() throws Exception {
		assertThat(stopWatch.isRunning()).isFalse();

		stopWatch.start(name1);
		Thread.sleep(duration1);
		assertThat(stopWatch.isRunning()).isTrue();
		assertThat(stopWatch.currentTaskName()).isEqualTo(name1);
		stopWatch.stop();
		assertThat(stopWatch.isRunning()).isFalse();

		/* Flaky StopWatch time assertions...
		assertThat(stopWatch.getLastTaskTimeNanos())
				.as("last task time in nanoseconds for task #1")
				.isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(duration1 - fudgeFactor))
				.isLessThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(duration1 + fudgeFactor));
		assertThat(stopWatch.getTotalTimeMillis())
				.as("total time in milliseconds for task #1")
				.isGreaterThanOrEqualTo(duration1 - fudgeFactor)
				.isLessThanOrEqualTo(duration1 + fudgeFactor);
		assertThat(stopWatch.getTotalTimeSeconds())
				.as("total time in seconds for task #1")
				.isGreaterThanOrEqualTo((duration1 - fudgeFactor) / 1000.0)
				.isLessThanOrEqualTo((duration1 + fudgeFactor) / 1000.0);
		*/

		stopWatch.start(name2);
		Thread.sleep(duration2);
		assertThat(stopWatch.isRunning()).isTrue();
		assertThat(stopWatch.currentTaskName()).isEqualTo(name2);
		stopWatch.stop();
		assertThat(stopWatch.isRunning()).isFalse();

		/* Flaky StopWatch time assertions...
		assertThat(stopWatch.getLastTaskTimeNanos())
				.as("last task time in nanoseconds for task #2")
				.isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(duration2))
				.isLessThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(duration2 + fudgeFactor));
		assertThat(stopWatch.getTotalTimeMillis())
				.as("total time in milliseconds for tasks #1 and #2")
				.isGreaterThanOrEqualTo(duration1 + duration2 - fudgeFactor)
				.isLessThanOrEqualTo(duration1 + duration2 + fudgeFactor);
		assertThat(stopWatch.getTotalTimeSeconds())
				.as("total time in seconds for task #2")
				.isGreaterThanOrEqualTo((duration1 + duration2 - fudgeFactor) / 1000.0)
				.isLessThanOrEqualTo((duration1 + duration2 + fudgeFactor) / 1000.0);
		*/

		assertThat(stopWatch.getTaskCount()).isEqualTo(2);
		assertThat(stopWatch.prettyPrint()).contains(name1, name2);
		assertThat(stopWatch.getTaskInfo()).extracting(TaskInfo::getTaskName).containsExactly(name1, name2);
		assertThat(stopWatch.toString()).contains(ID, name1, name2);
		assertThat(stopWatch.getId()).isEqualTo(ID);
	}

	@Test
	void validUsageDoesNotKeepTaskList() throws Exception {
		stopWatch.setKeepTaskList(false);

		stopWatch.start(name1);
		Thread.sleep(duration1);
		assertThat(stopWatch.currentTaskName()).isEqualTo(name1);
		stopWatch.stop();

		stopWatch.start(name2);
		Thread.sleep(duration2);
		assertThat(stopWatch.currentTaskName()).isEqualTo(name2);
		stopWatch.stop();

		assertThat(stopWatch.getTaskCount()).isEqualTo(2);
		assertThat(stopWatch.prettyPrint()).contains("No task info kept");
		assertThat(stopWatch.toString()).doesNotContain(name1, name2);
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(stopWatch::getTaskInfo)
				.withMessage("Task info is not being kept!");
	}

}

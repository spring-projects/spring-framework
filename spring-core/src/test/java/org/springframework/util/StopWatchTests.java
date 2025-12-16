/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.StopWatch.TaskInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link StopWatch}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@ExtendWith(MockitoExtension.class)
class StopWatchTests {

	private static final String ID = "myId";

	private static final String name1 = "Task 1";
	private static final String name2 = "Task 2";

	private StopWatch stopWatch;

	@Mock
	private StopWatch.NanoClock nanoClock;

	@BeforeEach
	void setUp() {
		stopWatch = new StopWatch(ID, nanoClock);
	}

	@Test
	void failureToStartBeforeGettingTimings() {
		assertThatIllegalStateException().isThrownBy(stopWatch::lastTaskInfo);
	}

	@Test
	void failureToStartBeforeStop() {
		assertThatIllegalStateException().isThrownBy(stopWatch::stop);
	}

	@Test
	void rejectsStartTwice() {
		when(nanoClock.nanoTime()).thenAnswer(invocation -> System.nanoTime());

		stopWatch.start();
		assertThat(stopWatch.isRunning()).isTrue();
		stopWatch.stop();
		assertThat(stopWatch.isRunning()).isFalse();

		stopWatch.start();
		assertThat(stopWatch.isRunning()).isTrue();
		assertThatIllegalStateException().isThrownBy(stopWatch::start);
	}

	@Test
	void validUsage() {
		when(nanoClock.nanoTime())
				.thenReturn(1_000_000_000L) // start task 1
				.thenReturn(2_000_000_000L) // stop task 1 (duration = 1 second)
				.thenReturn(3_000_000_000L) // start task 2
				.thenReturn(5_123_456_789L); // stop task 2 (duration = 2.123456789 seconds)

		assertThat(stopWatch.isRunning()).isFalse();

		stopWatch.start(name1);
		assertThat(stopWatch.isRunning()).isTrue();
		assertThat(stopWatch.currentTaskName()).isEqualTo(name1);
		stopWatch.stop();
		assertThat(stopWatch.isRunning()).isFalse();

		assertThat(stopWatch.lastTaskInfo().getTimeNanos())
				.as("last task time in nanoseconds for task #1")
				.isEqualTo(1_000_000_000L);
		assertThat(stopWatch.getTotalTimeMillis())
				.as("total time in milliseconds")
				.isEqualTo(1_000L);
		assertThat(stopWatch.getTotalTimeSeconds())
				.as("total time in seconds")
				.isEqualTo(1.0);

		stopWatch.start(name2);
		assertThat(stopWatch.isRunning()).isTrue();
		assertThat(stopWatch.currentTaskName()).isEqualTo(name2);
		stopWatch.stop();
		assertThat(stopWatch.isRunning()).isFalse();

		assertThat(stopWatch.lastTaskInfo().getTimeNanos())
				.as("last task time in nanoseconds for task #2")
				.isEqualTo(2_123_456_789L);
		assertThat(stopWatch.getTotalTimeMillis())
				.as("total time in milliseconds")
				.isEqualTo(3_123L);
		assertThat(stopWatch.getTotalTimeSeconds())
				.as("total time in seconds")
				.isEqualTo(3.123456789);

		assertThat(stopWatch.getTaskCount()).isEqualTo(2);
		assertThat(stopWatch.prettyPrint()).contains(name1, name2);
		assertThat(stopWatch.getTaskInfo()).extracting(TaskInfo::getTaskName).containsExactly(name1, name2);
		assertThat(stopWatch.toString()).contains(ID, name1, name2);
		assertThat(stopWatch.getId()).isEqualTo(ID);
	}

	@Test
	void validUsageDoesNotKeepTaskList() {
		when(nanoClock.nanoTime()).thenAnswer(invocation -> System.nanoTime());
		stopWatch.setKeepTaskList(false);

		stopWatch.start(name1);
		assertThat(stopWatch.currentTaskName()).isEqualTo(name1);
		stopWatch.stop();

		stopWatch.start(name2);
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

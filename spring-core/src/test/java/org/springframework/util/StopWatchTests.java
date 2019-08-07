/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.Test;

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
public class StopWatchTests {

	private static final String ID = "myId";

	private static final String name1 = "Task 1";
	private static final String name2 = "Task 2";

	private static final long duration1 = 200;
	private static final long duration2 = 50;
	private static final long fudgeFactor = 20;

	private final StopWatch stopWatch = new StopWatch(ID);


	@Test
	public void failureToStartBeforeGettingTimings() {
		assertThatIllegalStateException().isThrownBy(stopWatch::getLastTaskTimeMillis);
	}

	@Test
	public void failureToStartBeforeStop() {
		assertThatIllegalStateException().isThrownBy(stopWatch::stop);
	}

	@Test
	public void rejectsStartTwice() {
		stopWatch.start();
		assertThat(stopWatch.isRunning()).isTrue();
		stopWatch.stop();
		assertThat(stopWatch.isRunning()).isFalse();

		stopWatch.start();
		assertThat(stopWatch.isRunning()).isTrue();
		assertThatIllegalStateException().isThrownBy(stopWatch::start);
	}

	@Test
	public void validUsage() throws Exception {
		assertThat(stopWatch.isRunning()).isFalse();
		stopWatch.start(name1);
		Thread.sleep(duration1);
		assertThat(stopWatch.isRunning()).isTrue();
		assertThat(stopWatch.currentTaskName()).isEqualTo(name1);
		stopWatch.stop();
		assertThat(stopWatch.isRunning()).isFalse();

		assertThat(stopWatch.getTotalTimeMillis())
			.as("Unexpected timing " + stopWatch.getTotalTimeMillis())
			.isGreaterThanOrEqualTo(duration1);
		assertThat(stopWatch.getTotalTimeMillis())
			.as("Unexpected timing " + stopWatch.getTotalTimeMillis())
			.isLessThanOrEqualTo(duration1 + fudgeFactor);

		stopWatch.start(name2);
		Thread.sleep(duration2);
		stopWatch.stop();

		assertThat(stopWatch.getTotalTimeMillis())
			.as("Unexpected timing " + stopWatch.getTotalTimeMillis())
			.isGreaterThanOrEqualTo(duration1 + duration2);
		assertThat(stopWatch.getTotalTimeMillis())
			.as("Unexpected timing " + stopWatch.getTotalTimeMillis())
			.isLessThanOrEqualTo(duration1 + duration2 + fudgeFactor);

		assertThat(stopWatch.getTaskCount()).isEqualTo(2);
		assertThat(stopWatch.prettyPrint()).contains(name1, name2);
		assertThat(stopWatch.getTaskInfo()).extracting(TaskInfo::getTaskName).containsExactly(name1, name2);
		assertThat(stopWatch.toString()).contains(ID, name1, name2);
		assertThat(stopWatch.getId()).isEqualTo(ID);
	}

	@Test
	public void validUsageDoesNotKeepTaskList() throws Exception {
		stopWatch.setKeepTaskList(false);

		assertThat(stopWatch.isRunning()).isFalse();

		stopWatch.start(name1);
		Thread.sleep(duration1);
		assertThat(stopWatch.isRunning()).isTrue();
		stopWatch.stop();
		assertThat(stopWatch.isRunning()).isFalse();

		stopWatch.start(name2);
		Thread.sleep(duration2);
		assertThat(stopWatch.isRunning()).isTrue();
		stopWatch.stop();
		assertThat(stopWatch.isRunning()).isFalse();

		assertThat(stopWatch.getTaskCount()).isEqualTo(2);
		assertThat(stopWatch.prettyPrint()).contains("No task info kept");
		assertThat(stopWatch.toString()).doesNotContain(name1, name2);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(stopWatch::getTaskInfo);
	}

}

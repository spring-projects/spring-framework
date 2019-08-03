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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class StopWatchTests {

	private final StopWatch sw = new StopWatch();

	@Test
	public void validUsage() throws Exception {
		String id = "myId";
		StopWatch sw = new StopWatch(id);
		long int1 = 166L;
		long int2 = 45L;
		String name1 = "Task 1";
		String name2 = "Task 2";

		assertThat(sw.isRunning()).isFalse();
		sw.start(name1);
		Thread.sleep(int1);
		assertThat(sw.isRunning()).isTrue();
		assertThat(sw.currentTaskName()).isEqualTo(name1);
		sw.stop();

		// TODO are timings off in JUnit? Why do these assertions sometimes fail
		// under both Ant and Eclipse?

		// long fudgeFactor = 5L;
		// assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() >=
		// int1);
		// assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() <= int1
		// + fudgeFactor);
		sw.start(name2);
		Thread.sleep(int2);
		sw.stop();
		// assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() >= int1
		// + int2);
		// assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() <= int1
		// + int2 + fudgeFactor);

		assertThat(sw.getTaskCount() == 2).isTrue();
		String pp = sw.prettyPrint();
		assertThat(pp.contains(name1)).isTrue();
		assertThat(pp.contains(name2)).isTrue();

		StopWatch.TaskInfo[] tasks = sw.getTaskInfo();
		assertThat(tasks.length == 2).isTrue();
		assertThat(tasks[0].getTaskName().equals(name1)).isTrue();
		assertThat(tasks[1].getTaskName().equals(name2)).isTrue();

		String toString = sw.toString();
		assertThat(toString.contains(id)).isTrue();
		assertThat(toString.contains(name1)).isTrue();
		assertThat(toString.contains(name2)).isTrue();

		assertThat(sw.getId()).isEqualTo(id);
	}

	@Test
	public void validUsageNotKeepingTaskList() throws Exception {
		sw.setKeepTaskList(false);
		long int1 = 166L;
		long int2 = 45L;
		String name1 = "Task 1";
		String name2 = "Task 2";

		assertThat(sw.isRunning()).isFalse();
		sw.start(name1);
		Thread.sleep(int1);
		assertThat(sw.isRunning()).isTrue();
		sw.stop();

		// TODO are timings off in JUnit? Why do these assertions sometimes fail
		// under both Ant and Eclipse?

		// long fudgeFactor = 5L;
		// assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() >=
		// int1);
		// assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() <= int1
		// + fudgeFactor);
		sw.start(name2);
		Thread.sleep(int2);
		sw.stop();
		// assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() >= int1
		// + int2);
		// assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() <= int1
		// + int2 + fudgeFactor);

		assertThat(sw.getTaskCount() == 2).isTrue();
		String pp = sw.prettyPrint();
		assertThat(pp.contains("kept")).isTrue();

		String toString = sw.toString();
		assertThat(toString.contains(name1)).isFalse();
		assertThat(toString.contains(name2)).isFalse();

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
				sw::getTaskInfo);
	}

	@Test
	public void failureToStartBeforeGettingTimings() {
		assertThatIllegalStateException().isThrownBy(
				sw::getLastTaskTimeMillis);
	}

	@Test
	public void failureToStartBeforeStop() {
		assertThatIllegalStateException().isThrownBy(
				sw::stop);
	}

	@Test
	public void rejectsStartTwice() {
		sw.start("");
		sw.stop();
		sw.start("");
		assertThat(sw.isRunning()).isTrue();
		assertThatIllegalStateException().isThrownBy(() ->
				sw.start(""));
	}

}

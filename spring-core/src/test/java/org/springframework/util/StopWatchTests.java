/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class StopWatchTests {

	private final StopWatch sw = new StopWatch();

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void validUsage() throws Exception {
		String id = "myId";
		StopWatch sw = new StopWatch(id);
		long int1 = 166L;
		long int2 = 45L;
		String name1 = "Task 1";
		String name2 = "Task 2";

		assertFalse(sw.isRunning());
		sw.start(name1);
		Thread.sleep(int1);
		assertTrue(sw.isRunning());
		assertEquals(name1, sw.currentTaskName());
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

		assertTrue(sw.getTaskCount() == 2);
		String pp = sw.prettyPrint();
		assertTrue(pp.contains(name1));
		assertTrue(pp.contains(name2));

		StopWatch.TaskInfo[] tasks = sw.getTaskInfo();
		assertTrue(tasks.length == 2);
		assertTrue(tasks[0].getTaskName().equals(name1));
		assertTrue(tasks[1].getTaskName().equals(name2));

		String toString = sw.toString();
		assertTrue(toString.contains(id));
		assertTrue(toString.contains(name1));
		assertTrue(toString.contains(name2));

		assertEquals(id, sw.getId());
	}

	@Test
	public void validUsageNotKeepingTaskList() throws Exception {
		sw.setKeepTaskList(false);
		long int1 = 166L;
		long int2 = 45L;
		String name1 = "Task 1";
		String name2 = "Task 2";

		assertFalse(sw.isRunning());
		sw.start(name1);
		Thread.sleep(int1);
		assertTrue(sw.isRunning());
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

		assertTrue(sw.getTaskCount() == 2);
		String pp = sw.prettyPrint();
		assertTrue(pp.contains("kept"));

		String toString = sw.toString();
		assertFalse(toString.contains(name1));
		assertFalse(toString.contains(name2));

		exception.expect(UnsupportedOperationException.class);
		sw.getTaskInfo();
	}

	@Test
	public void failureToStartBeforeGettingTimings() {
		exception.expect(IllegalStateException.class);
		sw.getLastTaskTimeMillis();
	}

	@Test
	public void failureToStartBeforeStop() {
		exception.expect(IllegalStateException.class);
		sw.stop();
	}

	@Test
	public void rejectsStartTwice() {
		sw.start("");
		sw.stop();
		sw.start("");
		assertTrue(sw.isRunning());
		exception.expect(IllegalStateException.class);
		sw.start("");
	}

}


/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import junit.framework.TestCase;

/**
 * @author Rod Johnson
 */
public class StopWatchTests extends TestCase {

	/**
	 * Are timings off in JUnit?
	 */
	public void testValidUsage() throws Exception {
		StopWatch sw = new StopWatch();
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
		// long fudgeFactor = 5L;
		// under both Ant and Eclipse?

		//assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() >= int1);
		//assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() <= int1 + fudgeFactor);
		sw.start(name2);
		Thread.sleep(int2);
		sw.stop();
		//assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() >= int1 + int2);
		//assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() <= int1 + int2 + fudgeFactor);

		assertTrue(sw.getTaskCount() == 2);
		String pp = sw.prettyPrint();
		assertTrue(pp.indexOf(name1) != -1);
		assertTrue(pp.indexOf(name2) != -1);

		StopWatch.TaskInfo[] tasks = sw.getTaskInfo();
		assertTrue(tasks.length == 2);
		assertTrue(tasks[0].getTaskName().equals(name1));
		assertTrue(tasks[1].getTaskName().equals(name2));
		sw.toString();
	}

	public void testValidUsageNotKeepingTaskList() throws Exception {
		StopWatch sw = new StopWatch();
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
		//long fudgeFactor = 5L;
		// under both Ant and Eclipse?

		//assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() >= int1);
		//assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() <= int1 + fudgeFactor);
		sw.start(name2);
		Thread.sleep(int2);
		sw.stop();
		//assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() >= int1 + int2);
		//assertTrue("Unexpected timing " + sw.getTotalTime(), sw.getTotalTime() <= int1 + int2 + fudgeFactor);

		assertTrue(sw.getTaskCount() == 2);
		String pp = sw.prettyPrint();
		assertTrue(pp.indexOf("kept") != -1);
		sw.toString();

		try {
			sw.getTaskInfo();
			fail();
		}
		catch (UnsupportedOperationException ex) {
			// Ok
		}
	}

	public void testFailureToStartBeforeGettingTimings() {
		StopWatch sw = new StopWatch();
		try {
			sw.getLastTaskTimeMillis();
			fail("Can't get last interval if no tests run");
		}
		catch (IllegalStateException ex) {
			// Ok
		}
	}

	public void testFailureToStartBeforeStop() {
		StopWatch sw = new StopWatch();
		try {
			sw.stop();
			fail("Can't stop without starting");
		}
		catch (IllegalStateException ex) {
			// Ok
		}
	}

	public void testRejectsStartTwice() {
		StopWatch sw = new StopWatch();
		try {
			sw.start("");
			sw.stop();
			sw.start("");
			assertTrue(sw.isRunning());
			sw.start("");
			fail("Can't start twice");
		}
		catch (IllegalStateException ex) {
			// Ok
		}
	}

}

/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.context.i18n;

import java.util.TimeZone;

import junit.framework.TestCase;

/**
 * @author Nicholas Williams
 */
public class TimeZoneContextHolderTests extends TestCase {

	private static final String SYSTEM_ID = TimeZone.getDefault().getID();

	private static final String ALTERNATE_ID1 = SYSTEM_ID.equals("America/Chicago") ?
			"America/Los_Angeles" : "America/Chicago";

	private static final String ALTERNATE_ID2 = SYSTEM_ID.equals("America/New_York") ?
			"America/Anchorage" : "America/New_York";

	@Override
	protected void tearDown() throws Exception {
		TimeZoneContextHolder.resetTimeZoneContext();
		super.tearDown();
	}

	public void testSetTimeZoneImplicitlyNotInheritable() throws Throwable {
		TimeZoneContextHolder.setTimeZone(TimeZone.getTimeZone(ALTERNATE_ID1));

		assertNotNull("The time zone should not be null.",
				TimeZoneContextHolder.getTimeZone());
		assertEquals("The time zone is not correct.", ALTERNATE_ID1,
				TimeZoneContextHolder.getTimeZone().getID());

		runInSeparateThreadAndWait(new Runnable() {
			@Override
			public void run() {
				assertNotNull("The time zone should not be null.",
						TimeZoneContextHolder.getTimeZone());
				assertEquals("The time zone is not correct.", SYSTEM_ID,
						TimeZoneContextHolder.getTimeZone().getID());
				TimeZoneContextHolder.resetTimeZoneContext();
			}
		});

		assertNotNull("The time zone should not be null.",
				TimeZoneContextHolder.getTimeZone());
		assertEquals("The time zone is not correct.", ALTERNATE_ID1,
				TimeZoneContextHolder.getTimeZone().getID());
	}

	public void testSetTimeZoneExplicitlyNotInheritable() throws Throwable {
		TimeZoneContextHolder.setTimeZone(TimeZone.getTimeZone(ALTERNATE_ID2), false);

		assertNotNull("The time zone should not be null.",
				TimeZoneContextHolder.getTimeZone());
		assertEquals("The time zone is not correct.", ALTERNATE_ID2,
				TimeZoneContextHolder.getTimeZone().getID());

		runInSeparateThreadAndWait(new Runnable() {
			@Override
			public void run() {
				assertNotNull("The time zone should not be null.",
						TimeZoneContextHolder.getTimeZone());
				assertEquals("The time zone is not correct.", SYSTEM_ID,
						TimeZoneContextHolder.getTimeZone().getID());
				TimeZoneContextHolder.resetTimeZoneContext();
			}
		});

		assertNotNull("The time zone should not be null.",
				TimeZoneContextHolder.getTimeZone());
		assertEquals("The time zone is not correct.", ALTERNATE_ID2,
				TimeZoneContextHolder.getTimeZone().getID());
	}

	public void testSetTimeZoneInheritable() throws Throwable {
		TimeZoneContextHolder.setTimeZone(TimeZone.getTimeZone(ALTERNATE_ID1), true);

		assertNotNull("The time zone should not be null.",
				TimeZoneContextHolder.getTimeZone());
		assertEquals("The time zone is not correct.", ALTERNATE_ID1,
				TimeZoneContextHolder.getTimeZone().getID());

		runInSeparateThreadAndWait(new Runnable() {
			@Override
			public void run() {
				assertNotNull("The time zone should not be null.",
						TimeZoneContextHolder.getTimeZone());
				assertEquals("The time zone is not correct.", ALTERNATE_ID1,
						TimeZoneContextHolder.getTimeZone().getID());
				TimeZoneContextHolder.resetTimeZoneContext();
				assertNotNull("The time zone should not be null.",
						TimeZoneContextHolder.getTimeZone());
				assertEquals("The time zone is not correct.", SYSTEM_ID,
						TimeZoneContextHolder.getTimeZone().getID());
			}
		});

		assertNotNull("The time zone should not be null.",
				TimeZoneContextHolder.getTimeZone());
		assertEquals("The time zone is not correct.", ALTERNATE_ID1,
				TimeZoneContextHolder.getTimeZone().getID());
	}

	public void testSetTimeZoneNotInheritableChangeInOtherThread() throws Throwable {
		TimeZoneContextHolder.setTimeZone(TimeZone.getTimeZone(ALTERNATE_ID1), false);

		assertNotNull("The time zone should not be null.",
				TimeZoneContextHolder.getTimeZone());
		assertEquals("The time zone is not correct.", ALTERNATE_ID1,
				TimeZoneContextHolder.getTimeZone().getID());

		runInSeparateThreadAndWait(new Runnable() {
			@Override
			public void run() {
				assertNotNull("The time zone should not be null.",
						TimeZoneContextHolder.getTimeZone());
				assertEquals("The time zone is not correct.", SYSTEM_ID,
						TimeZoneContextHolder.getTimeZone().getID());

				TimeZoneContextHolder.setTimeZone(TimeZone.getTimeZone(ALTERNATE_ID2), true);
				assertNotNull("The time zone should not be null.",
						TimeZoneContextHolder.getTimeZone());
				assertEquals("The time zone is not correct.", ALTERNATE_ID2,
						TimeZoneContextHolder.getTimeZone().getID());
			}
		});

		assertNotNull("The time zone should not be null.",
				TimeZoneContextHolder.getTimeZone());
		assertEquals("The time zone is not correct.", ALTERNATE_ID1,
				TimeZoneContextHolder.getTimeZone().getID());
	}

	public void testSetTimeZoneInheritableChangeInOtherThread() throws Throwable {
		TimeZoneContextHolder.setTimeZone(TimeZone.getTimeZone(ALTERNATE_ID2), true);

		assertNotNull("The time zone should not be null.",
				TimeZoneContextHolder.getTimeZone());
		assertEquals("The time zone is not correct.", ALTERNATE_ID2,
				TimeZoneContextHolder.getTimeZone().getID());

		runInSeparateThreadAndWait(new Runnable() {
			@Override
			public void run() {
				assertNotNull("The time zone should not be null.",
						TimeZoneContextHolder.getTimeZone());
				assertEquals("The time zone is not correct.", ALTERNATE_ID2,
						TimeZoneContextHolder.getTimeZone().getID());

				TimeZoneContextHolder.setTimeZone(TimeZone.getTimeZone(ALTERNATE_ID1), true);
				assertNotNull("The time zone should not be null.",
						TimeZoneContextHolder.getTimeZone());
				assertEquals("The time zone is not correct.", ALTERNATE_ID1,
						TimeZoneContextHolder.getTimeZone().getID());
			}
		});

		assertNotNull("The time zone should not be null.",
				TimeZoneContextHolder.getTimeZone());
		assertEquals("The time zone is not correct.", ALTERNATE_ID2,
				TimeZoneContextHolder.getTimeZone().getID());
	}

	private static void runInSeparateThreadAndWait(Runnable runnable) throws Throwable {
		TestUncaughtExceptionHandler handler = new TestUncaughtExceptionHandler();

		Thread thread = new Thread(runnable);
		thread.setUncaughtExceptionHandler(handler);
		thread.start();

		int slept = 0;
		while (slept < 1000) {
			if (thread.getState() == Thread.State.TERMINATED) {
				handler.throwIfCaught();
				return;
			}
			Thread.sleep(50L);
			slept += 50;
		}

		fail("Waited too long for thread to stop.");
	}

	private static final class TestUncaughtExceptionHandler
			implements Thread.UncaughtExceptionHandler {

		private Throwable caught;

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			this.caught = e;
		}

		public void throwIfCaught() throws Throwable {
			if(this.caught != null)
				throw caught;
		}
	}

}

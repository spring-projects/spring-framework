/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.context.junit4;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.springframework.test.annotation.Timed;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Verifies proper handling of the following in conjunction with the
 * {@link SpringJUnit4ClassRunner}:
 * <ul>
 * <li>JUnit's {@link Test#timeout() @Test(timeout=...)}</li>
 * <li>Spring's {@link Timed @Timed}</li>
 * </ul>
 * 
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(JUnit4.class)
public class TimedSpringRunnerTests {

	@Test
	public void timedTests() throws Exception {
		Class<TimedSpringRunnerTestCase> testClass = TimedSpringRunnerTestCase.class;
		TrackingRunListener listener = new TrackingRunListener();
		RunNotifier notifier = new RunNotifier();
		notifier.addListener(listener);

		new SpringJUnit4ClassRunner(testClass).run(notifier);
		assertEquals("Verifying number of failures for test class [" + testClass + "].", 3,
			listener.getTestFailureCount());
		assertEquals("Verifying number of tests started for test class [" + testClass + "].", 5,
			listener.getTestStartedCount());
		assertEquals("Verifying number of tests finished for test class [" + testClass + "].", 5,
			listener.getTestFinishedCount());
	}


	@org.junit.Ignore // causing timeouts on cbeams' MBP
	@RunWith(SpringJUnit4ClassRunner.class)
	@TestExecutionListeners( {})
	public static final class TimedSpringRunnerTestCase {

		// Should Pass.
		@Test(timeout = 2000)
		public void testJUnitTimeoutWithNoOp() {
			/* no-op */
		}

		// Should Pass.
		@Test
		@Timed(millis = 2000)
		public void testSpringTimeoutWithNoOp() {
			/* no-op */
		}

		// Should Fail due to timeout.
		@Test(timeout = 200)
		public void testJUnitTimeoutWithOneSecondWait() throws Exception {
			Thread.sleep(1000);
		}

		// Should Fail due to timeout.
		@Test
		@Timed(millis = 200)
		public void testSpringTimeoutWithOneSecondWait() throws Exception {
			Thread.sleep(1000);
		}

		// Should Fail due to duplicate configuration.
		@Test(timeout = 200)
		@Timed(millis = 200)
		public void testSpringAndJUnitTimeout() {
			/* no-op */
		}
	}

}

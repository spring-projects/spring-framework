/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Verifies proper handling of the following in conjunction with the
 * {@link SpringJUnit4ClassRunner}:
 * <ul>
 * <li>JUnit's {@link Test#expected() &#064;Test(expected=...)}</li>
 * <li>Spring's {@link ExpectedException &#064;ExpectedException}</li>
 * </ul>
 * 
 * @author Sam Brannen
 * @since 3.0
 */
@SuppressWarnings("deprecation")
@RunWith(JUnit4.class)
public class ExpectedExceptionSpringRunnerTests {

	@Test
	public void expectedExceptions() throws Exception {
		Class<ExpectedExceptionSpringRunnerTestCase> testClass = ExpectedExceptionSpringRunnerTestCase.class;
		TrackingRunListener listener = new TrackingRunListener();
		RunNotifier notifier = new RunNotifier();
		notifier.addListener(listener);

		new SpringJUnit4ClassRunner(testClass).run(notifier);
		assertEquals("Verifying number of failures for test class [" + testClass + "].", 1,
			listener.getTestFailureCount());
		assertEquals("Verifying number of tests started for test class [" + testClass + "].", 3,
			listener.getTestStartedCount());
		assertEquals("Verifying number of tests finished for test class [" + testClass + "].", 3,
			listener.getTestFinishedCount());
	}


	@org.junit.Ignore
	@RunWith(SpringJUnit4ClassRunner.class)
	@TestExecutionListeners({})
	public static final class ExpectedExceptionSpringRunnerTestCase {

		// Should Pass.
		@Test(expected = IndexOutOfBoundsException.class)
		public void verifyJUnitExpectedException() {
			new ArrayList<Object>().get(1);
		}

		// Should Pass.
		@Test
		@ExpectedException(IndexOutOfBoundsException.class)
		public void verifySpringExpectedException() {
			new ArrayList<Object>().get(1);
		}

		// Should Fail due to duplicate configuration.
		@Test(expected = IllegalStateException.class)
		@ExpectedException(IllegalStateException.class)
		public void verifyJUnitAndSpringExpectedException() {
			new ArrayList<Object>().get(1);
		}

	}

}

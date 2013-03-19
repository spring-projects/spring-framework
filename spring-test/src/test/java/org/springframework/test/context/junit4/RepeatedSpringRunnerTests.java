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

package org.springframework.test.context.junit4;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Timed;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Verifies proper handling of the following in conjunction with the
 * {@link SpringJUnit4ClassRunner}:
 * <ul>
 * <li>Spring's {@link Repeat &#064;Repeat}</li>
 * <li>Spring's {@link Timed &#064;Timed}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(Parameterized.class)
public class RepeatedSpringRunnerTests {

	private static final AtomicInteger invocationCount = new AtomicInteger();

	private final Class<? extends AbstractRepeatedTestCase> testClass;

	private final int expectedFailureCount;

	private final int expectedTestStartedCount;

	private final int expectedTestFinishedCount;

	private final int expectedInvocationCount;


	public RepeatedSpringRunnerTests(Class<? extends AbstractRepeatedTestCase> testClass, int expectedFailureCount,
			int expectedTestStartedCount, int expectedTestFinishedCount, int expectedInvocationCount) {
		this.testClass = testClass;
		this.expectedFailureCount = expectedFailureCount;
		this.expectedTestStartedCount = expectedTestStartedCount;
		this.expectedTestFinishedCount = expectedTestFinishedCount;
		this.expectedInvocationCount = expectedInvocationCount;
	}

	@Parameters
	public static Collection<Object[]> repetitionData() {
		return Arrays.asList(new Object[][] {//
		//
			{ NonAnnotatedRepeatedTestCase.class, 0, 1, 1, 1 },//
			{ DefaultRepeatValueRepeatedTestCase.class, 0, 1, 1, 1 },//
			{ NegativeRepeatValueRepeatedTestCase.class, 0, 1, 1, 1 },//
			{ RepeatedFiveTimesRepeatedTestCase.class, 0, 1, 1, 5 },//
			{ TimedRepeatedTestCase.class, 3, 4, 4, (5 + 1 + 4 + 10) } //
		});
	}

	@Test
	public void assertRepetitions() throws Exception {
		TrackingRunListener listener = new TrackingRunListener();
		RunNotifier notifier = new RunNotifier();
		notifier.addListener(listener);
		invocationCount.set(0);

		new SpringJUnit4ClassRunner(this.testClass).run(notifier);
		assertEquals("Verifying number of failures for test class [" + this.testClass + "].",
			this.expectedFailureCount, listener.getTestFailureCount());
		assertEquals("Verifying number of tests started for test class [" + this.testClass + "].",
			this.expectedTestStartedCount, listener.getTestStartedCount());
		assertEquals("Verifying number of tests finished for test class [" + this.testClass + "].",
			this.expectedTestFinishedCount, listener.getTestFinishedCount());
		assertEquals("Verifying number of invocations for test class [" + this.testClass + "].",
			this.expectedInvocationCount, invocationCount.get());
	}


	@RunWith(SpringJUnit4ClassRunner.class)
	@TestExecutionListeners({})
	public abstract static class AbstractRepeatedTestCase {

		protected void incrementInvocationCount() throws IOException {
			invocationCount.incrementAndGet();
		}
	}

	public static final class NonAnnotatedRepeatedTestCase extends AbstractRepeatedTestCase {

		@Test
		@Timed(millis = 10000)
		public void nonAnnotated() throws Exception {
			incrementInvocationCount();
		}
	}

	public static final class DefaultRepeatValueRepeatedTestCase extends AbstractRepeatedTestCase {

		@Test
		@Repeat
		@Timed(millis = 10000)
		public void defaultRepeatValue() throws Exception {
			incrementInvocationCount();
		}
	}

	public static final class NegativeRepeatValueRepeatedTestCase extends AbstractRepeatedTestCase {

		@Test
		@Repeat(-5)
		@Timed(millis = 10000)
		public void negativeRepeatValue() throws Exception {
			incrementInvocationCount();
		}
	}

	public static final class RepeatedFiveTimesRepeatedTestCase extends AbstractRepeatedTestCase {

		@Test
		@Repeat(5)
		public void repeatedFiveTimes() throws Exception {
			incrementInvocationCount();
		}
	}

	/**
	 * Unit tests for claims raised in <a
	 * href="http://jira.springframework.org/browse/SPR-6011"
	 * target="_blank">SPR-6011</a>.
	 */
	@Ignore("TestCase classes are run manually by the enclosing test class")
	public static final class TimedRepeatedTestCase extends AbstractRepeatedTestCase {

		@Test
		@Timed(millis = 1000)
		@Repeat(5)
		public void repeatedFiveTimesButDoesNotExceedTimeout() throws Exception {
			incrementInvocationCount();
		}

		@Test
		@Timed(millis = 10)
		@Repeat(1)
		public void singleRepetitionExceedsTimeout() throws Exception {
			incrementInvocationCount();
			Thread.sleep(15);
		}

		@Test
		@Timed(millis = 20)
		@Repeat(4)
		public void firstRepetitionOfManyExceedsTimeout() throws Exception {
			incrementInvocationCount();
			Thread.sleep(25);
		}

		@Test
		@Timed(millis = 100)
		@Repeat(10)
		public void collectiveRepetitionsExceedTimeout() throws Exception {
			incrementInvocationCount();
			Thread.sleep(11);
		}
	}

}

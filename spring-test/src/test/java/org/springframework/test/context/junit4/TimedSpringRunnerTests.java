/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runners.JUnit4;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.annotation.Timed;
import org.springframework.test.context.TestExecutionListeners;

import static org.springframework.test.context.junit4.JUnitTestingUtils.runTestsAndAssertCounters;

/**
 * Verifies proper handling of the following in conjunction with the
 * {@link SpringRunner}:
 * <ul>
 * <li>JUnit's {@link Test#timeout() @Test(timeout=...)}</li>
 * <li>Spring's {@link Timed @Timed}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(JUnit4.class)
@SuppressWarnings("deprecation")
public class TimedSpringRunnerTests {

	protected Class<?> getTestCase() {
		return TimedSpringRunnerTestCase.class;
	}

	protected Class<? extends Runner> getRunnerClass() {
		return SpringRunner.class;
	}

	@Test
	public void timedTests() throws Exception {
		runTestsAndAssertCounters(getRunnerClass(), getTestCase(), 7, 5, 7, 0, 0);
	}


	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners({})
	public static class TimedSpringRunnerTestCase {

		// Should Pass.
		@Test(timeout = 2000)
		public void jUnitTimeoutWithNoOp() {
			/* no-op */
		}

		// Should Pass.
		@Test
		@Timed(millis = 2000)
		public void springTimeoutWithNoOp() {
			/* no-op */
		}

		// Should Fail due to timeout.
		@Test(timeout = 10)
		public void jUnitTimeoutWithSleep() throws Exception {
			Thread.sleep(200);
		}

		// Should Fail due to timeout.
		@Test
		@Timed(millis = 10)
		public void springTimeoutWithSleep() throws Exception {
			Thread.sleep(200);
		}

		// Should Fail due to timeout.
		@Test
		@MetaTimed
		public void springTimeoutWithSleepAndMetaAnnotation() throws Exception {
			Thread.sleep(200);
		}

		// Should Fail due to timeout.
		@Test
		@MetaTimedWithOverride(millis = 10)
		public void springTimeoutWithSleepAndMetaAnnotationAndOverride() throws Exception {
			Thread.sleep(200);
		}

		// Should Fail due to duplicate configuration.
		@Test(timeout = 200)
		@Timed(millis = 200)
		public void springAndJUnitTimeouts() {
			/* no-op */
		}
	}

	@Timed(millis = 10)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface MetaTimed {
	}

	@Timed(millis = 1000)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface MetaTimedWithOverride {
		@AliasFor(annotation = Timed.class)
		long millis() default 1000;
	}

}

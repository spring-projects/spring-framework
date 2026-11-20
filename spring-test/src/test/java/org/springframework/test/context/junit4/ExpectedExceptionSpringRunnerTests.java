/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4;

import java.util.ArrayList;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.springframework.test.context.TestExecutionListeners;

import static org.springframework.test.context.junit4.JUnitTestingUtils.runTestsAndAssertCounters;

/**
 * Verifies proper handling of JUnit's {@link Test#expected() @Test(expected = ...)}
 * support in conjunction with the {@link SpringRunner}.
 *
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(JUnit4.class)
@SuppressWarnings("deprecation")
public class ExpectedExceptionSpringRunnerTests {

	@Test
	public void expectedExceptions() throws Exception {
		runTestsAndAssertCounters(SpringRunner.class, ExpectedExceptionSpringRunnerTestCase.class, 1, 0, 1, 0, 0);
	}


	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners({})
	public static final class ExpectedExceptionSpringRunnerTestCase {

		// Should Pass.
		@Test(expected = IndexOutOfBoundsException.class)
		public void verifyJUnitExpectedException() {
			new ArrayList<>().get(1);
		}
	}

}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.Runner;
import org.junit.runners.JUnit4;

import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.TimedSpringRunnerTests;

import static org.assertj.core.api.Assertions.fail;

/**
 * This class is an extension of {@link TimedSpringRunnerTests}
 * that has been modified to use {@link SpringClassRule} and
 * {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@SuppressWarnings("deprecation")
public class TimedSpringRuleTests extends TimedSpringRunnerTests {

	// All tests are in superclass.

	@Override
	protected Class<?> getTestCase() {
		return TimedSpringRuleTestCase.class;
	}

	@Override
	protected Class<? extends Runner> getRunnerClass() {
		return JUnit4.class;
	}


	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners({})
	public static final class TimedSpringRuleTestCase extends TimedSpringRunnerTestCase {

		@ClassRule
		public static final SpringClassRule springClassRule = new SpringClassRule();

		@Rule
		public final SpringMethodRule springMethodRule = new SpringMethodRule();


		/**
		 * Overridden to always throw an exception, since Spring's Rule-based
		 * JUnit integration does not fail a test for duplicate configuration
		 * of timeouts.
		 */
		@Override
		public void springAndJUnitTimeouts() {
			fail("intentional failure to make tests in superclass pass");
		}

		// All other tests are in superclass.
	}

}

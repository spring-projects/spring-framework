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

package org.springframework.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.Runner;
import org.junit.runners.JUnit4;

import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.TimedSpringRunnerTests;

import static org.junit.Assert.*;

/**
 * This class is an extension of {@link TimedSpringRunnerTests}
 * that has been modified to use {@link SpringClassRule} and
 * {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
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

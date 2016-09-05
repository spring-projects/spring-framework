/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit4.concurrency;

import org.junit.Test;
import org.junit.experimental.ParallelComputer;

import org.springframework.test.context.hierarchies.web.DispatcherWacRootWacEarTests;
import org.springframework.test.context.junit4.InheritedConfigSpringJUnit4ClassRunnerAppCtxTests;
import org.springframework.test.context.junit4.MethodLevelTransactionalSpringRunnerTests;
import org.springframework.test.context.junit4.SpringJUnit47ClassRunnerRuleTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunnerAppCtxTests;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.junit4.TimedTransactionalSpringRunnerTests;
import org.springframework.test.context.junit4.rules.BasicAnnotationConfigWacSpringRuleTests;
import org.springframework.test.context.junit4.rules.ParameterizedSpringRuleTests;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.web.RequestAndSessionScopedBeansWacTests;
import org.springframework.test.context.web.socket.WebSocketServletServerContainerFactoryBeanTests;
import org.springframework.test.web.client.samples.SampleTests;
import org.springframework.test.web.servlet.samples.context.JavaConfigTests;
import org.springframework.test.web.servlet.samples.context.WebAppResourceTests;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;

import static org.springframework.test.context.junit4.JUnitTestingUtils.runTestsAndAssertCounters;

/**
 * Concurrency tests for the {@link SpringRunner}, {@link SpringClassRule}, and
 * {@link SpringMethodRule} that use JUnit 4's experimental {@link ParallelComputer}
 * to execute tests in parallel.
 *
 * <p>The tests executed by this test class come from a hand-picked collection of test
 * classes within the test suite that is intended to cover most categories of tests
 * that are currently supported by the TestContext Framework on JUnit 4.
 * 
 * <p>The chosen test classes intentionally do <em>not</em> include any classes that
 * fall under the following categories.
 *
 * <ul>
 * <li>tests that make use of Spring's {@code @DirtiesContext} support
 * <li>tests that make use of JUnit 4's {@code @FixMethodOrder} support
 * <li>tests that commit changes to the state of a shared in-memory database
 * </ul>
 *
 * <p><strong>NOTE</strong>: these tests only run if the {@link TestGroup#LONG_RUNNING
 * LONG_RUNNING} test group is enabled.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see org.springframework.test.context.TestContextConcurrencyTests
 */
public class SpringJUnit4ConcurrencyTests {

	// @formatter:off
	private static final Class<?>[] testClasses = new Class[] {
		// Basics
			/* 9 */ SpringJUnit4ClassRunnerAppCtxTests.class,
			/* 9 */ InheritedConfigSpringJUnit4ClassRunnerAppCtxTests.class,
			/* 2 */ SpringJUnit47ClassRunnerRuleTests.class,
			/* 2 */ ParameterizedSpringRuleTests.class,
		// Transactional
			/* 2 */ MethodLevelTransactionalSpringRunnerTests.class,
			/* 4 */ TimedTransactionalSpringRunnerTests.class,
		// Web and Scopes
			/* 1 */ DispatcherWacRootWacEarTests.class, /* 2 ignored */
			/* 3 */ BasicAnnotationConfigWacSpringRuleTests.class,
			/* 2 */ RequestAndSessionScopedBeansWacTests.class,
			/* 1 */ WebSocketServletServerContainerFactoryBeanTests.class,
		// Spring MVC Test
			/* 2 */ JavaConfigTests.class,
			/* 3 */ WebAppResourceTests.class,
			/* 4 */ SampleTests.class
	};
	// @formatter:on

	/**
	 * The number of tests in all {@link #testClasses}.
	 *
	 * <p>The current number of tests per test class is tracked as a comment
	 * before each class reference above. The following constant must therefore
	 * be the sum of those values.
	 *
	 * <p>This is admittedly fragile, but there's unfortunately not really a
	 * better way to count the number of tests without re-implementing JUnit 4's
	 * discovery algorithm. Plus, the presence of parameterized tests makes it
	 * even more difficult to count programmatically.
	 */
	private static final int TESTS = 44;
	private static final int FAILED = 0;
	private static final int IGNORED = 2;
	private static final int ABORTED = 0;


	@Test
	public void runAllTestsConcurrently() throws Exception {

		Assume.group(TestGroup.LONG_RUNNING);

		runTestsAndAssertCounters(new ParallelComputer(true, true), TESTS, FAILED, TESTS, IGNORED, ABORTED,
			testClasses);
	}

}

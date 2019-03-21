/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

import static org.junit.Assert.*;
import static org.springframework.test.context.junit4.JUnitTestingUtils.*;

/**
 * Integration tests which verify that '<i>before</i>' and '<i>after</i>'
 * methods of {@link TestExecutionListener TestExecutionListeners} as well as
 * {@code @BeforeTransaction} and {@code @AfterTransaction} methods can fail
 * tests run via the {@link SpringRunner} in a JUnit 4 environment.
 *
 * <p>See: <a href="https://jira.spring.io/browse/SPR-3960" target="_blank">SPR-3960</a>.
 *
 * <p>Indirectly, this class also verifies that all {@code TestExecutionListener}
 * lifecycle callbacks are called.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@RunWith(Parameterized.class)
public class FailingBeforeAndAfterMethodsSpringRunnerTests {

	protected final Class<?> clazz;


	@Parameters(name = "{0}")
	public static Object[] testCases() {
		return new Object[] {//
			AlwaysFailingBeforeTestClassTestCase.class.getSimpleName(),//
			AlwaysFailingAfterTestClassTestCase.class.getSimpleName(),//
			AlwaysFailingPrepareTestInstanceTestCase.class.getSimpleName(),//
			AlwaysFailingBeforeTestMethodTestCase.class.getSimpleName(),//
			AlwaysFailingBeforeTestExecutionTestCase.class.getSimpleName(), //
			AlwaysFailingAfterTestExecutionTestCase.class.getSimpleName(), //
			AlwaysFailingAfterTestMethodTestCase.class.getSimpleName(),//
			FailingBeforeTransactionTestCase.class.getSimpleName(),//
			FailingAfterTransactionTestCase.class.getSimpleName() //
		};
	}

	public FailingBeforeAndAfterMethodsSpringRunnerTests(String testClassName) throws Exception {
		this.clazz = ClassUtils.forName(getClass().getName() + "." + testClassName, getClass().getClassLoader());
	}

	protected Class<? extends Runner> getRunnerClass() {
		return SpringRunner.class;
	}

	@Test
	public void runTestAndAssertCounters() throws Exception {
		int expectedStartedCount = this.clazz.getSimpleName().startsWith("AlwaysFailingBeforeTestClass") ? 0 : 1;
		int expectedFinishedCount = this.clazz.getSimpleName().startsWith("AlwaysFailingBeforeTestClass") ? 0 : 1;

		runTestsAndAssertCounters(getRunnerClass(), this.clazz, expectedStartedCount, 1, expectedFinishedCount, 0, 0);
	}


	// -------------------------------------------------------------------

	protected static class AlwaysFailingBeforeTestClassTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestClass(TestContext testContext) {
			fail("always failing beforeTestClass()");
		}
	}

	protected static class AlwaysFailingAfterTestClassTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestClass(TestContext testContext) {
			fail("always failing afterTestClass()");
		}
	}

	protected static class AlwaysFailingPrepareTestInstanceTestExecutionListener implements TestExecutionListener {

		@Override
		public void prepareTestInstance(TestContext testContext) throws Exception {
			fail("always failing prepareTestInstance()");
		}
	}

	protected static class AlwaysFailingBeforeTestMethodTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestMethod(TestContext testContext) {
			fail("always failing beforeTestMethod()");
		}
	}

	protected static class AlwaysFailingBeforeTestExecutionTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestExecution(TestContext testContext) {
			fail("always failing beforeTestExecution()");
		}
	}

	protected static class AlwaysFailingAfterTestMethodTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestMethod(TestContext testContext) {
			fail("always failing afterTestMethod()");
		}
	}

	protected static class AlwaysFailingAfterTestExecutionTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestExecution(TestContext testContext) {
			fail("always failing afterTestExecution()");
		}
	}

	@RunWith(SpringRunner.class)
	public static abstract class BaseTestCase {

		@Test
		public void testNothing() {
		}
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingBeforeTestClassTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestClassTestCase extends BaseTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingAfterTestClassTestExecutionListener.class)
	public static class AlwaysFailingAfterTestClassTestCase extends BaseTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingPrepareTestInstanceTestExecutionListener.class)
	public static class AlwaysFailingPrepareTestInstanceTestCase extends BaseTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingBeforeTestMethodTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestMethodTestCase extends BaseTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingBeforeTestExecutionTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestExecutionTestCase extends BaseTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingAfterTestExecutionTestExecutionListener.class)
	public static class AlwaysFailingAfterTestExecutionTestCase extends BaseTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingAfterTestMethodTestExecutionListener.class)
	public static class AlwaysFailingAfterTestMethodTestCase extends BaseTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@RunWith(SpringRunner.class)
	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	@Transactional
	public static class FailingBeforeTransactionTestCase {

		@Test
		public void testNothing() {
		}

		@BeforeTransaction
		public void beforeTransaction() {
			fail("always failing beforeTransaction()");
		}
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@RunWith(SpringRunner.class)
	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	@Transactional
	public static class FailingAfterTransactionTestCase {

		@Test
		public void testNothing() {
		}

		@AfterTransaction
		public void afterTransaction() {
			fail("always failing afterTransaction()");
		}
	}

}

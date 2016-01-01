/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.test.context.testng.TrackingTestNGTestListener;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.util.ClassUtils;

import org.testng.TestNG;

import static org.junit.Assert.*;

/**
 * <p>
 * JUnit 4 based integration test for verifying that '<i>before</i>' and '<i>after</i>'
 * methods of {@link TestExecutionListener TestExecutionListeners} as well as
 * {@link BeforeTransaction &#064;BeforeTransaction} and
 * {@link AfterTransaction &#064;AfterTransaction} methods can fail a test in a
 * TestNG environment, as requested in <a
 * href="http://opensource.atlassian.com/projects/spring/browse/SPR-3960"
 * target="_blank">SPR-3960</a>.
 * </p>
 * <p>
 * Indirectly, this class also verifies that all {@link TestExecutionListener}
 * lifecycle callbacks are called.
 * </p>
 * <p>
 * As of Spring 3.0, this class also tests support for the new
 * {@link TestExecutionListener#beforeTestClass(TestContext) beforeTestClass()}
 * and {@link TestExecutionListener#afterTestClass(TestContext)
 * afterTestClass()} lifecycle callback methods.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 */
@RunWith(Parameterized.class)
public class FailingBeforeAndAfterMethodsTestNGTests {

	protected final Class<?> clazz;
	protected final int expectedTestStartCount;
	protected final int expectedTestSuccessCount;
	protected final int expectedFailureCount;
	protected final int expectedFailedConfigurationsCount;


	@Parameters(name = "{0}")
	public static Collection<Object[]> testData() {
		return Arrays.asList(new Object[][] {//
		//
			{ AlwaysFailingBeforeTestClassTestCase.class.getSimpleName(), 1, 0, 0, 1 },//
			{ AlwaysFailingAfterTestClassTestCase.class.getSimpleName(), 1, 1, 0, 1 },//
			{ AlwaysFailingPrepareTestInstanceTestCase.class.getSimpleName(), 1, 0, 0, 1 },//
			{ AlwaysFailingBeforeTestMethodTestCase.class.getSimpleName(), 1, 0, 0, 1 },//
			{ AlwaysFailingAfterTestMethodTestCase.class.getSimpleName(), 1, 1, 0, 1 },//
			{ FailingBeforeTransactionTestCase.class.getSimpleName(), 1, 0, 0, 1 },//
			{ FailingAfterTransactionTestCase.class.getSimpleName(), 1, 1, 0, 1 } //
		});
	}

	public FailingBeforeAndAfterMethodsTestNGTests(String testClassName, int expectedTestStartCount,
			int expectedTestSuccessCount, int expectedFailureCount, int expectedFailedConfigurationsCount) throws Exception {
		this.clazz = ClassUtils.forName(getClass().getName() + "." + testClassName, getClass().getClassLoader());
		this.expectedTestStartCount = expectedTestStartCount;
		this.expectedTestSuccessCount = expectedTestSuccessCount;
		this.expectedFailureCount = expectedFailureCount;
		this.expectedFailedConfigurationsCount = expectedFailedConfigurationsCount;
	}

	@Test
	public void runTestAndAssertCounters() throws Exception {
		final TrackingTestNGTestListener listener = new TrackingTestNGTestListener();
		final TestNG testNG = new TestNG();
		testNG.addListener(listener);
		testNG.setTestClasses(new Class<?>[] { this.clazz });
		testNG.setVerbose(0);
		testNG.run();

		assertEquals("Verifying number of test starts for test class [" + this.clazz + "].",
			this.expectedTestStartCount, listener.testStartCount);
		assertEquals("Verifying number of successful tests for test class [" + this.clazz + "].",
			this.expectedTestSuccessCount, listener.testSuccessCount);
		assertEquals("Verifying number of failures for test class [" + this.clazz + "].", this.expectedFailureCount,
			listener.testFailureCount);
		assertEquals("Verifying number of failed configurations for test class [" + this.clazz + "].",
			this.expectedFailedConfigurationsCount, listener.failedConfigurationsCount);
	}

	// -------------------------------------------------------------------

	static class AlwaysFailingBeforeTestClassTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void beforeTestClass(TestContext testContext) {
			org.testng.Assert.fail("always failing beforeTestClass()");
		}
	}

	static class AlwaysFailingAfterTestClassTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void afterTestClass(TestContext testContext) {
			org.testng.Assert.fail("always failing afterTestClass()");
		}
	}

	static class AlwaysFailingPrepareTestInstanceTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void prepareTestInstance(TestContext testContext) throws Exception {
			org.testng.Assert.fail("always failing prepareTestInstance()");
		}
	}

	static class AlwaysFailingBeforeTestMethodTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void beforeTestMethod(TestContext testContext) {
			org.testng.Assert.fail("always failing beforeTestMethod()");
		}
	}

	static class AlwaysFailingAfterTestMethodTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void afterTestMethod(TestContext testContext) {
			org.testng.Assert.fail("always failing afterTestMethod()");
		}
	}

	// -------------------------------------------------------------------

	@TestExecutionListeners(value = {}, inheritListeners = false)
	public static abstract class BaseTestCase extends AbstractTestNGSpringContextTests {

		@org.testng.annotations.Test
		public void testNothing() {
		}
	}

	@TestExecutionListeners(AlwaysFailingBeforeTestClassTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestClassTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingAfterTestClassTestExecutionListener.class)
	public static class AlwaysFailingAfterTestClassTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingPrepareTestInstanceTestExecutionListener.class)
	public static class AlwaysFailingPrepareTestInstanceTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingBeforeTestMethodTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestMethodTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingAfterTestMethodTestExecutionListener.class)
	public static class AlwaysFailingAfterTestMethodTestCase extends BaseTestCase {
	}

	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	public static class FailingBeforeTransactionTestCase extends AbstractTransactionalTestNGSpringContextTests {

		@org.testng.annotations.Test
		public void testNothing() {
		}

		@BeforeTransaction
		public void beforeTransaction() {
			org.testng.Assert.fail("always failing beforeTransaction()");
		}
	}

	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	public static class FailingAfterTransactionTestCase extends AbstractTransactionalTestNGSpringContextTests {

		@org.testng.annotations.Test
		public void testNothing() {
		}

		@AfterTransaction
		public void afterTransaction() {
			org.testng.Assert.fail("always failing afterTransaction()");
		}
	}

}

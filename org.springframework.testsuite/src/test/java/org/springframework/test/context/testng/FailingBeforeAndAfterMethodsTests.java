/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.test.context.testng;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.TestNG;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * <p>
 * JUnit 4 based unit test for verifying that '<em>before</em>' and '<em>after</em>'
 * methods of {@link TestExecutionListener TestExecutionListeners} as well as
 * {@link BeforeTransaction @BeforeTransaction} and
 * {@link AfterTransaction @AfterTransaction} methods can fail a test in a
 * TestNG environment, as requested in <a
 * href="http://opensource.atlassian.com/projects/spring/browse/SPR-3960"
 * target="_blank">SPR-3960</a>.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 */
@RunWith(Parameterized.class)
public class FailingBeforeAndAfterMethodsTests {

	protected final Class<?> clazz;
	protected final int expectedTestStartCount;
	protected final int expectedTestSuccessCount;
	protected final int expectedFailureCount;
	protected final int expectedFailedConfigurationsCount;


	public FailingBeforeAndAfterMethodsTests(final Class<?> clazz, final int expectedTestStartCount,
			final int expectedTestSuccessCount, final int expectedFailureCount,
			final int expectedFailedConfigurationsCount) {
		this.clazz = clazz;
		this.expectedTestStartCount = expectedTestStartCount;
		this.expectedTestSuccessCount = expectedTestSuccessCount;
		this.expectedFailureCount = expectedFailureCount;
		this.expectedFailedConfigurationsCount = expectedFailedConfigurationsCount;
	}

	@Parameters
	public static Collection<Object[]> testData() {
		return Arrays.asList(new Object[][] {

		{ AlwaysFailingBeforeTestMethodTestCase.class, 1, 0, 0, 1 },

		{ AlwaysFailingAfterTestMethodTestCase.class, 1, 1, 0, 1 },

		{ FailingBeforeTransactionalTestCase.class, 1, 0, 0, 1 },

		{ FailingAfterTransactionalTestCase.class, 1, 1, 0, 1 }

		});
	}

	@Test
	public void runTestAndAssertCounters() throws Exception {
		final FailureTrackingTestListener listener = new FailureTrackingTestListener();
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


	static class FailureTrackingTestListener implements ITestListener {

		int testStartCount = 0;
		int testSuccessCount = 0;
		int testFailureCount = 0;
		int failedConfigurationsCount = 0;


		public void onFinish(ITestContext testContext) {
			this.failedConfigurationsCount += testContext.getFailedConfigurations().size();
		}

		public void onStart(ITestContext testContext) {
		}

		public void onTestFailedButWithinSuccessPercentage(ITestResult testResult) {
		}

		public void onTestFailure(ITestResult testResult) {
			this.testFailureCount++;
		}

		public void onTestSkipped(ITestResult testResult) {
		}

		public void onTestStart(ITestResult testResult) {
			this.testStartCount++;
		}

		public void onTestSuccess(ITestResult testResult) {
			this.testSuccessCount++;
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


	@TestExecutionListeners(value = { AlwaysFailingBeforeTestMethodTestExecutionListener.class }, inheritListeners = false)
	public static class AlwaysFailingBeforeTestMethodTestCase extends AbstractTestNGSpringContextTests {

		@org.testng.annotations.Test
		public void testNothing() {
		}
	}


	@TestExecutionListeners(value = { AlwaysFailingAfterTestMethodTestExecutionListener.class }, inheritListeners = false)
	public static class AlwaysFailingAfterTestMethodTestCase extends AbstractTestNGSpringContextTests {

		@org.testng.annotations.Test
		public void testNothing() {
		}
	}


	@ContextConfiguration(locations = { "FailingBeforeAndAfterMethodsTests-context.xml" })
	public static class FailingBeforeTransactionalTestCase extends AbstractTransactionalTestNGSpringContextTests {

		@org.testng.annotations.Test
		public void testNothing() {
		}

		@BeforeTransaction
		public void beforeTransaction() {
			org.testng.Assert.fail("always failing beforeTransaction()");
		}
	}


	@ContextConfiguration(locations = { "FailingBeforeAndAfterMethodsTests-context.xml" })
	public static class FailingAfterTransactionalTestCase extends AbstractTransactionalTestNGSpringContextTests {

		@org.testng.annotations.Test
		public void testNothing() {
		}

		@AfterTransaction
		public void afterTransaction() {
			org.testng.Assert.fail("always failing afterTransaction()");
		}
	}

}

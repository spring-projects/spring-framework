/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context.junit38;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * <p>
 * JUnit 4 based integration test for verifying that '<em>before</em>' and '<em>after</em>'
 * methods of {@link TestExecutionListener TestExecutionListeners} as well as
 * {@link BeforeTransaction @BeforeTransaction} and
 * {@link AfterTransaction @AfterTransaction} methods can fail a test in a JUnit
 * 3.8 environment, as requested in <a
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


	public FailingBeforeAndAfterMethodsTests(final Class<?> clazz) {
		this.clazz = clazz;
	}

	@Parameters
	public static Collection<Object[]> testData() {
		return Arrays.asList(new Object[][] {

		{ AlwaysFailingBeforeTestMethodTestCase.class },

		{ AlwaysFailingAfterTestMethodTestCase.class },

		{ FailingBeforeTransactionalTestCase.class },

		{ FailingAfterTransactionalTestCase.class }

		});
	}

	@Test
	public void runTestAndAssertCounters() throws Exception {
		final String testName = "testNothing";
		final TestCase testCase = (TestCase) this.clazz.newInstance();
		testCase.setName(testName);
		TestResult testResult = testCase.run();
		assertEquals("Verifying number of errors for test method [" + testName + "] and class [" + this.clazz + "].",
			0, testResult.errorCount());
		assertEquals("Verifying number of failures for test method [" + testName + "] and class [" + this.clazz + "].",
			1, testResult.failureCount());
	}


	static class AlwaysFailingBeforeTestMethodTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		@SuppressWarnings("deprecation")
		public void beforeTestMethod(TestContext testContext) {
			junit.framework.Assert.fail("always failing beforeTestMethod()");
		}
	}

	static class AlwaysFailingAfterTestMethodTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		@SuppressWarnings("deprecation")
		public void afterTestMethod(TestContext testContext) {
			junit.framework.Assert.fail("always failing afterTestMethod()");
		}
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@SuppressWarnings("deprecation")
	@TestExecutionListeners(listeners = AlwaysFailingBeforeTestMethodTestExecutionListener.class, inheritListeners = false)
	public static class AlwaysFailingBeforeTestMethodTestCase extends AbstractJUnit38SpringContextTests {

		public void testNothing() {
		}
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@SuppressWarnings("deprecation")
	@TestExecutionListeners(listeners = AlwaysFailingAfterTestMethodTestExecutionListener.class, inheritListeners = false)
	public static class AlwaysFailingAfterTestMethodTestCase extends AbstractJUnit38SpringContextTests {

		public void testNothing() {
		}
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@SuppressWarnings("deprecation")
	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	public static class FailingBeforeTransactionalTestCase extends AbstractTransactionalJUnit38SpringContextTests {

		public void testNothing() {
		}

		@BeforeTransaction
		public void beforeTransaction() {
			fail("always failing beforeTransaction()");
		}
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@SuppressWarnings("deprecation")
	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	public static class FailingAfterTransactionalTestCase extends AbstractTransactionalJUnit38SpringContextTests {

		public void testNothing() {
		}

		@AfterTransaction
		public void afterTransaction() {
			fail("always failing afterTransaction()");
		}
	}

}

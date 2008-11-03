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

package org.springframework.test.context.junit4;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
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
 * JUnit 4 based unit test for verifying that '<em>before</em>' and '<em>after</em>'
 * methods of {@link TestExecutionListener TestExecutionListeners} as well as
 * {@link BeforeTransaction @BeforeTransaction} and
 * {@link AfterTransaction @AfterTransaction} methods can fail a test in a JUnit
 * 4.4 environment, as requested in <a
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
		final FailureTrackingRunListener failureTrackingRunListener = new FailureTrackingRunListener();
		final RunNotifier notifier = new RunNotifier();
		notifier.addListener(failureTrackingRunListener);

		new SpringJUnit4ClassRunner(this.clazz).run(notifier);
		assertEquals("Verifying number of failures for test class [" + this.clazz + "].", 1,
				failureTrackingRunListener.failureCount);
	}


	static class FailureTrackingRunListener extends RunListener {

		int failureCount = 0;


		public void testFailure(Failure failure) throws Exception {
			this.failureCount++;
		}
	}


	static class AlwaysFailingBeforeTestMethodTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void beforeTestMethod(TestContext testContext) {
			org.junit.Assert.fail("always failing beforeTestMethod()");
		}
	}


	static class AlwaysFailingAfterTestMethodTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void afterTestMethod(TestContext testContext) {
			org.junit.Assert.fail("always failing afterTestMethod()");
		}
	}


	@TestExecutionListeners(value = { AlwaysFailingBeforeTestMethodTestExecutionListener.class }, inheritListeners = false)
	public static class AlwaysFailingBeforeTestMethodTestCase extends AbstractJUnit4SpringContextTests {

		@Test
		public void testNothing() {
		}
	}


	@TestExecutionListeners(value = { AlwaysFailingAfterTestMethodTestExecutionListener.class }, inheritListeners = false)
	public static class AlwaysFailingAfterTestMethodTestCase extends AbstractJUnit4SpringContextTests {

		@Test
		public void testNothing() {
		}
	}


	@ContextConfiguration(locations = { "FailingBeforeAndAfterMethodsTests-context.xml" })
	public static class FailingBeforeTransactionalTestCase extends AbstractTransactionalJUnit4SpringContextTests {

		@Test
		public void testNothing() {
		}

		@BeforeTransaction
		public void beforeTransaction() {
			org.junit.Assert.fail("always failing beforeTransaction()");
		}
	}


	@ContextConfiguration(locations = { "FailingBeforeAndAfterMethodsTests-context.xml" })
	public static class FailingAfterTransactionalTestCase extends AbstractTransactionalJUnit4SpringContextTests {

		@Test
		public void testNothing() {
		}

		@AfterTransaction
		public void afterTransaction() {
			org.junit.Assert.fail("always failing afterTransaction()");
		}
	}

}

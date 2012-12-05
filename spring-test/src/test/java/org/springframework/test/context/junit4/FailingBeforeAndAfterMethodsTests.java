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

package org.springframework.test.context.junit4;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
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
 * JUnit 4 based integration test for verifying that '<i>before</i>' and '<i>after</i>'
 * methods of {@link TestExecutionListener TestExecutionListeners} as well as
 * {@link BeforeTransaction &#064;BeforeTransaction} and
 * {@link AfterTransaction &#064;AfterTransaction} methods can fail a test in a
 * JUnit 4.4 environment, as requested in <a
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
public class FailingBeforeAndAfterMethodsTests {

	protected final Class<?> clazz;


	public FailingBeforeAndAfterMethodsTests(final Class<?> clazz) {
		this.clazz = clazz;
	}

	@Parameters
	public static Collection<Object[]> testData() {
		return Arrays.asList(new Object[][] {//
		//
			{ AlwaysFailingBeforeTestClassTestCase.class },//
			{ AlwaysFailingAfterTestClassTestCase.class },//
			{ AlwaysFailingPrepareTestInstanceTestCase.class },//
			{ AlwaysFailingBeforeTestMethodTestCase.class },//
			{ AlwaysFailingAfterTestMethodTestCase.class },//
			{ FailingBeforeTransactionTestCase.class },//
			{ FailingAfterTransactionTestCase.class } //
		});
	}

	@Test
	public void runTestAndAssertCounters() throws Exception {
		final TrackingRunListener listener = new TrackingRunListener();
		final RunNotifier notifier = new RunNotifier();
		notifier.addListener(listener);

		new SpringJUnit4ClassRunner(this.clazz).run(notifier);
		assertEquals("Verifying number of failures for test class [" + this.clazz + "].", 1,
			listener.getTestFailureCount());
	}


	// -------------------------------------------------------------------

	static class AlwaysFailingBeforeTestClassTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void beforeTestClass(TestContext testContext) {
			fail("always failing beforeTestClass()");
		}
	}

	static class AlwaysFailingAfterTestClassTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void afterTestClass(TestContext testContext) {
			fail("always failing afterTestClass()");
		}
	}

	static class AlwaysFailingPrepareTestInstanceTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void prepareTestInstance(TestContext testContext) throws Exception {
			fail("always failing prepareTestInstance()");
		}
	}

	static class AlwaysFailingBeforeTestMethodTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void beforeTestMethod(TestContext testContext) {
			fail("always failing beforeTestMethod()");
		}
	}

	static class AlwaysFailingAfterTestMethodTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public void afterTestMethod(TestContext testContext) {
			fail("always failing afterTestMethod()");
		}
	}

	@RunWith(SpringJUnit4ClassRunner.class)
	@TestExecutionListeners({})
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
	@TestExecutionListeners(AlwaysFailingAfterTestMethodTestExecutionListener.class)
	public static class AlwaysFailingAfterTestMethodTestCase extends BaseTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	public static class FailingBeforeTransactionTestCase extends AbstractTransactionalJUnit4SpringContextTests {

		@Test
		public void testNothing() {
		}

		@BeforeTransaction
		public void beforeTransaction() {
			fail("always failing beforeTransaction()");
		}
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	public static class FailingAfterTransactionTestCase extends AbstractTransactionalJUnit4SpringContextTests {

		@Test
		public void testNothing() {
		}

		@AfterTransaction
		public void afterTransaction() {
			fail("always failing afterTransaction()");
		}
	}

}

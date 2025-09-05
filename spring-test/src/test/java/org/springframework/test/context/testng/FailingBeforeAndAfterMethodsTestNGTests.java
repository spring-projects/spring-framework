/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.testng;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testng.TestNG;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Integration tests which verify that '<i>before</i>' and '<i>after</i>'
 * methods of {@link TestExecutionListener TestExecutionListeners} as well as
 * {@code @BeforeTransaction} and {@code @AfterTransaction} methods can fail
 * tests in a TestNG environment.
 *
 * <p>See: <a href="https://jira.spring.io/browse/SPR-3960" target="_blank">SPR-3960</a>.
 *
 * <p>Indirectly, this class also verifies that all {@code TestExecutionListener}
 * lifecycle callbacks are called.
 *
 * @author Sam Brannen
 * @since 2.5
 */
class FailingBeforeAndAfterMethodsTestNGTests {

	@ParameterizedTest
	@MethodSource("testData")
	void runTestAndAssertCounters(Class<?> clazz, int expectedTestStartCount,
			int expectedTestSuccessCount, int expectedFailureCount, int expectedFailedConfigurationsCount) throws Exception {

		TrackingTestNGTestListener listener = new TrackingTestNGTestListener();
		TestNG testNG = new TestNG();
		testNG.addListener(listener);
		testNG.setTestClasses(new Class<?>[] {clazz});
		testNG.setVerbose(0);
		testNG.run();

		String name = clazz.getSimpleName();

		assertThat(listener.testStartCount).as("tests started for [" + name + "] ==> ").isEqualTo(expectedTestStartCount);
		assertThat(listener.testSuccessCount).as("successful tests for [" + name + "] ==> ").isEqualTo(expectedTestSuccessCount);
		assertThat(listener.testFailureCount).as("failed tests for [" + name + "] ==> ").isEqualTo(expectedFailureCount);
		assertThat(listener.failedConfigurationsCount).as("failed configurations for [" + name + "] ==> ").isEqualTo(expectedFailedConfigurationsCount);
	}

	static List<Arguments> testData() {
		return List.of(
			argumentSet("AlwaysFailingBeforeTestClass", AlwaysFailingBeforeTestClassTestCase.class, 1, 0, 0, 1),
			argumentSet("AlwaysFailingAfterTestClass", AlwaysFailingAfterTestClassTestCase.class, 1, 1, 0, 1),
			argumentSet("AlwaysFailingPrepareTestInstance", AlwaysFailingPrepareTestInstanceTestCase.class, 1, 0, 0, 1),
			argumentSet("AlwaysFailingBeforeTestMethod", AlwaysFailingBeforeTestMethodTestCase.class, 1, 0, 0, 1),
			argumentSet("AlwaysFailingBeforeTestExecution", AlwaysFailingBeforeTestExecutionTestCase.class, 1, 0, 1, 0),
			argumentSet("AlwaysFailingAfterTestExecution", AlwaysFailingAfterTestExecutionTestCase.class, 1, 0, 1, 0),
			argumentSet("AlwaysFailingAfterTestMethod", AlwaysFailingAfterTestMethodTestCase.class, 1, 1, 0, 1),
			argumentSet("FailingBeforeTransaction", FailingBeforeTransactionTestCase.class, 1, 0, 0, 1),
			argumentSet("FailingAfterTransaction", FailingAfterTransactionTestCase.class, 1, 1, 0, 1)
		);
	}


	static class AlwaysFailingBeforeTestClassTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestClass(TestContext testContext) {
			fail("always failing beforeTestClass()");
		}
	}

	static class AlwaysFailingAfterTestClassTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestClass(TestContext testContext) {
			fail("always failing afterTestClass()");
		}
	}

	static class AlwaysFailingPrepareTestInstanceTestExecutionListener implements TestExecutionListener {

		@Override
		public void prepareTestInstance(TestContext testContext) throws Exception {
			fail("always failing prepareTestInstance()");
		}
	}

	static class AlwaysFailingBeforeTestMethodTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestMethod(TestContext testContext) {
			fail("always failing beforeTestMethod()");
		}
	}

	static class AlwaysFailingBeforeTestExecutionTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestExecution(TestContext testContext) {
			fail("always failing beforeTestExecution()");
		}
	}

	static class AlwaysFailingAfterTestExecutionTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestExecution(TestContext testContext) {
			fail("always failing afterTestExecution()");
		}
	}

	static class AlwaysFailingAfterTestMethodTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestMethod(TestContext testContext) {
			fail("always failing afterTestMethod()");
		}
	}


	@TestExecutionListeners(inheritListeners = false)
	abstract static class BaseTestCase extends AbstractTestNGSpringContextTests {

		@org.testng.annotations.Test
		void testNothing() {
		}
	}

	@TestExecutionListeners(AlwaysFailingBeforeTestClassTestExecutionListener.class)
	static class AlwaysFailingBeforeTestClassTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingAfterTestClassTestExecutionListener.class)
	static class AlwaysFailingAfterTestClassTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingPrepareTestInstanceTestExecutionListener.class)
	static class AlwaysFailingPrepareTestInstanceTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingBeforeTestMethodTestExecutionListener.class)
	static class AlwaysFailingBeforeTestMethodTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingBeforeTestExecutionTestExecutionListener.class)
	static class AlwaysFailingBeforeTestExecutionTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingAfterTestExecutionTestExecutionListener.class)
	static class AlwaysFailingAfterTestExecutionTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingAfterTestMethodTestExecutionListener.class)
	static class AlwaysFailingAfterTestMethodTestCase extends BaseTestCase {
	}

	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	static class FailingBeforeTransactionTestCase extends AbstractTransactionalTestNGSpringContextTests {

		@org.testng.annotations.Test
		void testNothing() {
		}

		@BeforeTransaction
		void beforeTransaction() {
			fail("always failing beforeTransaction()");
		}
	}

	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	static class FailingAfterTransactionTestCase extends AbstractTransactionalTestNGSpringContextTests {

		@org.testng.annotations.Test
		void testNothing() {
		}

		@AfterTransaction
		void afterTransaction() {
			fail("always failing afterTransaction()");
		}
	}

}

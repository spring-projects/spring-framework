/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * JUnit Jupiter based unit tests for {@link TestContextManager}, which verify proper
 * support for <em>suppressed exceptions</em> thrown by {@link TestExecutionListener
 * TestExecutionListeners}.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see Throwable#getSuppressed()
 */
class TestContextManagerSuppressedExceptionsTests {

	@Test
	void afterTestExecution() {
		test("afterTestExecution", FailingAfterTestExecutionTestCase.class,
			(tcm, c, m) -> tcm.afterTestExecution(this, m, null));
	}

	@Test
	void afterTestMethod() {
		test("afterTestMethod", FailingAfterTestMethodTestCase.class,
			(tcm, c, m) -> tcm.afterTestMethod(this, m, null));
	}

	@Test
	void afterTestClass() {
		test("afterTestClass", FailingAfterTestClassTestCase.class, (tcm, c, m) -> tcm.afterTestClass());
	}

	private void test(String useCase, Class<?> testClass, Callback callback) {
		TestContextManager testContextManager = new TestContextManager(testClass);
		assertThat(testContextManager.getTestExecutionListeners().size()).as("Registered TestExecutionListeners").isEqualTo(2);

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> {
				Method testMethod = getClass().getMethod("toString");
				callback.invoke(testContextManager, testClass, testMethod);
				fail("should have thrown an AssertionError");
			}).satisfies(ex -> {
				// 'after' callbacks are reversed, so 2 comes before 1.
				assertThat(ex.getMessage()).isEqualTo(useCase + "-2");
				Throwable[] suppressed = ex.getSuppressed();
				assertThat(suppressed).hasSize(1);
				assertThat(suppressed[0].getMessage()).isEqualTo(useCase + "-1");
			});
	}


	// -------------------------------------------------------------------

	@FunctionalInterface
	private interface Callback {

		void invoke(TestContextManager tcm, Class<?> clazz, Method method) throws Exception;
	}

	private static class FailingAfterTestClassListener1 implements TestExecutionListener {

		@Override
		public void afterTestClass(TestContext testContext) {
			fail("afterTestClass-1");
		}
	}

	private static class FailingAfterTestClassListener2 implements TestExecutionListener {

		@Override
		public void afterTestClass(TestContext testContext) {
			fail("afterTestClass-2");
		}
	}

	private static class FailingAfterTestMethodListener1 implements TestExecutionListener {

		@Override
		public void afterTestMethod(TestContext testContext) {
			fail("afterTestMethod-1");
		}
	}

	private static class FailingAfterTestMethodListener2 implements TestExecutionListener {

		@Override
		public void afterTestMethod(TestContext testContext) {
			fail("afterTestMethod-2");
		}
	}

	private static class FailingAfterTestExecutionListener1 implements TestExecutionListener {

		@Override
		public void afterTestExecution(TestContext testContext) {
			fail("afterTestExecution-1");
		}
	}

	private static class FailingAfterTestExecutionListener2 implements TestExecutionListener {

		@Override
		public void afterTestExecution(TestContext testContext) {
			fail("afterTestExecution-2");
		}
	}

	@TestExecutionListeners({ FailingAfterTestExecutionListener1.class, FailingAfterTestExecutionListener2.class })
	private static class FailingAfterTestExecutionTestCase {
	}

	@TestExecutionListeners({ FailingAfterTestMethodListener1.class, FailingAfterTestMethodListener2.class })
	private static class FailingAfterTestMethodTestCase {
	}

	@TestExecutionListeners({ FailingAfterTestClassListener1.class, FailingAfterTestClassListener2.class })
	private static class FailingAfterTestClassTestCase {
	}

}

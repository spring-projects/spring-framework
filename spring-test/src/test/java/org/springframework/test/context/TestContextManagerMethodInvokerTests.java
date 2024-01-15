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

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.MethodInvoker.DEFAULT_INVOKER;

/**
 * JUnit Jupiter based unit test for {@link TestContextManager}, which verifies proper
 * {@link MethodInvoker} behavior.
 *
 * @author Sam Brannen
 * @since 6.1
 */
class TestContextManagerMethodInvokerTests {

	private static final MethodInvoker customMethodInvoker = (method, target) -> null;

	private final TestContextManager testContextManager = new TestContextManager(TestCase.class);

	private final Method testMethod = ReflectionUtils.findMethod(TestCase.class, "testMethod");


	@Test
	void methodInvokerState() throws Exception {
		assertThat(testContextManager.getTestExecutionListeners()).singleElement().isInstanceOf(DemoExecutionListener.class);

		assertMethodInvokerState(testContextManager::beforeTestClass);

		assertMethodInvokerState(() -> testContextManager.prepareTestInstance(this));

		assertMethodInvokerState(() -> testContextManager.beforeTestMethod(this, testMethod));

		assertMethodInvokerState(() -> testContextManager.beforeTestExecution(this, testMethod));

		assertMethodInvokerState(() -> testContextManager.afterTestExecution(this, testMethod, null));

		assertMethodInvokerState(() -> testContextManager.afterTestMethod(this, testMethod, null));

		assertMethodInvokerState(testContextManager::afterTestClass);
	}

	private void assertMethodInvokerState(Executable callback) throws Exception {
		testContextManager.getTestContext().setMethodInvoker(customMethodInvoker);
		callback.execute();
		assertThat(testContextManager.getTestContext().getMethodInvoker()).isSameAs(DEFAULT_INVOKER);
	}


	@TestExecutionListeners(DemoExecutionListener.class)
	private static class TestCase {

		@SuppressWarnings("unused")
		void testMethod() {
		}
	}

	private static class DemoExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestClass(TestContext testContext) {
			assertThat(testContext.getMethodInvoker()).isSameAs(customMethodInvoker);
		}

		@Override
		public void prepareTestInstance(TestContext testContext) {
			assertThat(testContext.getMethodInvoker()).isSameAs(customMethodInvoker);
		}

		@Override
		public void beforeTestMethod(TestContext testContext) {
			assertThat(testContext.getMethodInvoker()).isSameAs(customMethodInvoker);
		}

		@Override
		public void beforeTestExecution(TestContext testContext) {
			assertThat(testContext.getMethodInvoker()).isSameAs(customMethodInvoker);
		}

		@Override
		public void afterTestExecution(TestContext testContext) {
			assertThat(testContext.getMethodInvoker()).isSameAs(customMethodInvoker);
		}

		@Override
		public void afterTestMethod(TestContext testContext) {
			assertThat(testContext.getMethodInvoker()).isSameAs(customMethodInvoker);
		}

		@Override
		public void afterTestClass(TestContext testContext) {
			assertThat(testContext.getMethodInvoker()).isSameAs(customMethodInvoker);
		}

	}

	private interface Executable {

		void execute() throws Exception;

	}

}

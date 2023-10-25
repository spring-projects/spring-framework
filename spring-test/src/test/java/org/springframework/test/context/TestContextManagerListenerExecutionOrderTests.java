/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit Jupiter based unit test for {@link TestContextManager}, which verifies proper
 * <em>execution order</em> of registered {@link TestExecutionListener
 * TestExecutionListeners}.
 *
 * @author Sam Brannen
 * @since 2.5
 */
class TestContextManagerListenerExecutionOrderTests {

	private static final List<String> executionOrder = new ArrayList<>();

	private final TestContextManager testContextManager = new TestContextManager(TestCase.class);

	private final Method testMethod = ReflectionUtils.findMethod(TestCase.class, "testMethod");


	@Test
	void listenerExecutionOrder() throws Exception {
		// @formatter:off
		assertThat(this.testContextManager.getTestExecutionListeners()).as("Registered TestExecutionListeners").hasSize(3);

		this.testContextManager.beforeTestMethod(this, this.testMethod);
		assertExecutionOrder("beforeTestMethod",
			"beforeTestMethod-1",
				"beforeTestMethod-2",
					"beforeTestMethod-3"
		);

		this.testContextManager.beforeTestExecution(this, this.testMethod);
		assertExecutionOrder("beforeTestExecution",
			"beforeTestMethod-1",
				"beforeTestMethod-2",
					"beforeTestMethod-3",
						"beforeTestExecution-1",
							"beforeTestExecution-2",
								"beforeTestExecution-3"
		);

		this.testContextManager.afterTestExecution(this, this.testMethod, null);
		assertExecutionOrder("afterTestExecution",
			"beforeTestMethod-1",
				"beforeTestMethod-2",
					"beforeTestMethod-3",
						"beforeTestExecution-1",
							"beforeTestExecution-2",
								"beforeTestExecution-3",
								"afterTestExecution-3",
							"afterTestExecution-2",
						"afterTestExecution-1"
		);

		this.testContextManager.afterTestMethod(this, this.testMethod, null);
		assertExecutionOrder("afterTestMethod",
			"beforeTestMethod-1",
				"beforeTestMethod-2",
					"beforeTestMethod-3",
						"beforeTestExecution-1",
							"beforeTestExecution-2",
								"beforeTestExecution-3",
								"afterTestExecution-3",
							"afterTestExecution-2",
						"afterTestExecution-1",
					"afterTestMethod-3",
				"afterTestMethod-2",
			"afterTestMethod-1"
		);
		// @formatter:on
	}

	private static void assertExecutionOrder(String usageContext, String... expectedBeforeTestMethodCalls) {
		assertThat(executionOrder).as("execution order (" + usageContext + ") ==>").containsExactly(expectedBeforeTestMethodCalls);
	}


	@TestExecutionListeners({ FirstTel.class, SecondTel.class, ThirdTel.class })
	private static class TestCase {

		@SuppressWarnings("unused")
		void testMethod() {
		}
	}

	private abstract static class NamedTestExecutionListener implements TestExecutionListener {

		private final String name;


		NamedTestExecutionListener(String name) {
			this.name = name;
		}

		@Override
		public void beforeTestMethod(TestContext testContext) {
			executionOrder.add("beforeTestMethod-" + this.name);
		}

		@Override
		public void beforeTestExecution(TestContext testContext) {
			executionOrder.add("beforeTestExecution-" + this.name);
		}

		@Override
		public void afterTestExecution(TestContext testContext) {
			executionOrder.add("afterTestExecution-" + this.name);
		}

		@Override
		public void afterTestMethod(TestContext testContext) {
			executionOrder.add("afterTestMethod-" + this.name);
		}
	}

	private static class FirstTel extends NamedTestExecutionListener {

		public FirstTel() {
			super("1");
		}
	}

	private static class SecondTel extends NamedTestExecutionListener {

		public SecondTel() {
			super("2");
		}
	}

	private static class ThirdTel extends NamedTestExecutionListener {

		public ThirdTel() {
			super("3");
		}
	}

}

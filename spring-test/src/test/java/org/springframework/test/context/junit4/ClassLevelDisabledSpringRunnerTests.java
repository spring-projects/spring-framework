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

package org.springframework.test.context.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.aot.DisabledInAotMode;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@RunWith(SpringRunner.class)
@TestExecutionListeners(ClassLevelDisabledSpringRunnerTests.CustomTestExecutionListener.class)
@IfProfileValue(name = "ClassLevelDisabledSpringRunnerTests.profile_value.name", value = "enigmaX")
// Since Spring test's AOT processing support does not evaluate @IfProfileValue,
// this test class simply is not supported for AOT processing.
@DisabledInAotMode
public class ClassLevelDisabledSpringRunnerTests {

	@Test
	public void testIfProfileValueDisabled() {
		fail("The body of a disabled test should never be executed!");
	}


	public static class CustomTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestClass(TestContext testContext) throws Exception {
			fail("A listener method for a disabled test should never be executed!");
		}

		@Override
		public void prepareTestInstance(TestContext testContext) throws Exception {
			fail("A listener method for a disabled test should never be executed!");
		}

		@Override
		public void beforeTestMethod(TestContext testContext) throws Exception {
			fail("A listener method for a disabled test should never be executed!");
		}

		@Override
		public void afterTestMethod(TestContext testContext) throws Exception {
			fail("A listener method for a disabled test should never be executed!");
		}

		@Override
		public void afterTestClass(TestContext testContext) throws Exception {
			fail("A listener method for a disabled test should never be executed!");
		}
	}
}

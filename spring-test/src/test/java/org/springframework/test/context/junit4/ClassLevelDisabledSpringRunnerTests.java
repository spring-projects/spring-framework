/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
@DisabledInAotMode("@IfProfileValue is not supported for AOT processing")
@SuppressWarnings("deprecation")
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

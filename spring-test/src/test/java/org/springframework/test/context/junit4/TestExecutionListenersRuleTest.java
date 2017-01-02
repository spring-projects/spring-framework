/*
 * Copyright 2004-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;


/**
 * Verifies that test execution listeners work correctly for rule based
 * JUnit 4 tests.
 * 
 * @author Philippe Marschall
 * @since 3.2.2
 */
@TestExecutionListeners(TestExecutionListenersRuleTest.InnerTestExecutionListener.class)
public class TestExecutionListenersRuleTest {
	
	@ClassRule
	public static final SpringJUnitClassRule CLASS_RULE = new SpringJUnitClassRule();
	
	@Rule
	public MethodRule methodRule = new SpringJUnitMethodRule(CLASS_RULE);
	
	private static int beforeTestClassRun = 0;
	private static int prepareTestInstanceRun = 0;
	private static int beforeTestMethodRun = 0;
	private static int afterTestMethodRun = 0;
	private static int afterTestClassRun = 0;
	
	@BeforeClass
	public static void verifyBeforeClass() {
		assertEquals("beforeTestClassRun", 1, beforeTestClassRun);
		assertEquals("prepareTestInstanceRun", 0, prepareTestInstanceRun);
		assertEquals("beforeTestMethodRun", 0, beforeTestMethodRun);
		assertEquals("afterTestMethodRun", 0, afterTestMethodRun);
		assertEquals("afterTestClassRun", 0, afterTestClassRun);
	}
	
	@Test
	public void verifyTest() {
		assertEquals("beforeTestClassRun", 1, beforeTestClassRun);
		assertEquals("prepareTestInstanceRun", 1, prepareTestInstanceRun);
		assertEquals("beforeTestMethodRun", 1, beforeTestMethodRun);
		assertEquals("afterTestMethodRun", 0, afterTestMethodRun);
		assertEquals("afterTestClassRun", 0, afterTestClassRun);
	}
	
	@AfterClass
	public static void verifyAfterClass() {
		assertEquals("beforeTestClassRun", 1, beforeTestClassRun);
		assertEquals("prepareTestInstanceRun", 1, prepareTestInstanceRun);
		assertEquals("beforeTestMethodRun", 1, beforeTestMethodRun);
		assertEquals("afterTestMethodRun", 1, afterTestMethodRun);
		assertEquals("afterTestClassRun", 0, afterTestClassRun);
	}
	
	
	static class InnerTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestClass(TestContext testContext) throws Exception {
			beforeTestClassRun += 1;
		}

		@Override
		public void prepareTestInstance(TestContext testContext) throws Exception {
			prepareTestInstanceRun += 1;
		}

		@Override
		public void beforeTestMethod(TestContext testContext) throws Exception {
			beforeTestMethodRun += 1;
		}

		@Override
		public void afterTestMethod(TestContext testContext) throws Exception {
			afterTestMethodRun += 1;
		}

		@Override
		public void afterTestClass(TestContext testContext) throws Exception {
			afterTestClassRun += 1;
		}
		
	}

}

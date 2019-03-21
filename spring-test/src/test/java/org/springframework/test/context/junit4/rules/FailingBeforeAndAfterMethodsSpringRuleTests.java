/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.FailingBeforeAndAfterMethodsSpringRunnerTests;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

/**
 * This class is an extension of {@link FailingBeforeAndAfterMethodsSpringRunnerTests}
 * that has been modified to use {@link SpringClassRule} and
 * {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
public class FailingBeforeAndAfterMethodsSpringRuleTests extends FailingBeforeAndAfterMethodsSpringRunnerTests {

	@Parameters(name = "{0}")
	public static Object[] testData() {
		return new Object[] {//
			AlwaysFailingBeforeTestClassSpringRuleTestCase.class.getSimpleName(),//
			AlwaysFailingAfterTestClassSpringRuleTestCase.class.getSimpleName(),//
			AlwaysFailingPrepareTestInstanceSpringRuleTestCase.class.getSimpleName(),//
			AlwaysFailingBeforeTestMethodSpringRuleTestCase.class.getSimpleName(),//
			AlwaysFailingAfterTestMethodSpringRuleTestCase.class.getSimpleName(),//
			FailingBeforeTransactionSpringRuleTestCase.class.getSimpleName(),//
			FailingAfterTransactionSpringRuleTestCase.class.getSimpleName() //
		};
	}

	public FailingBeforeAndAfterMethodsSpringRuleTests(String testClassName) throws Exception {
		super(testClassName);
	}

	@Override
	protected Class<? extends Runner> getRunnerClass() {
		return JUnit4.class;
	}

	// All tests are in superclass.

	@RunWith(JUnit4.class)
	public static abstract class BaseSpringRuleTestCase {

		@ClassRule
		public static final SpringClassRule springClassRule = new SpringClassRule();

		@Rule
		public final SpringMethodRule springMethodRule = new SpringMethodRule();


		@Test
		public void testNothing() {
		}
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingBeforeTestClassTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestClassSpringRuleTestCase extends BaseSpringRuleTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingAfterTestClassTestExecutionListener.class)
	public static class AlwaysFailingAfterTestClassSpringRuleTestCase extends BaseSpringRuleTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingPrepareTestInstanceTestExecutionListener.class)
	public static class AlwaysFailingPrepareTestInstanceSpringRuleTestCase extends BaseSpringRuleTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingBeforeTestMethodTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestMethodSpringRuleTestCase extends BaseSpringRuleTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingAfterTestMethodTestExecutionListener.class)
	public static class AlwaysFailingAfterTestMethodSpringRuleTestCase extends BaseSpringRuleTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@RunWith(JUnit4.class)
	@ContextConfiguration("../FailingBeforeAndAfterMethodsTests-context.xml")
	@Transactional
	public static class FailingBeforeTransactionSpringRuleTestCase {

		@ClassRule
		public static final SpringClassRule springClassRule = new SpringClassRule();

		@Rule
		public final SpringMethodRule springMethodRule = new SpringMethodRule();


		@Test
		public void testNothing() {
		}

		@BeforeTransaction
		public void beforeTransaction() {
			fail("always failing beforeTransaction()");
		}
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@RunWith(JUnit4.class)
	@ContextConfiguration("../FailingBeforeAndAfterMethodsTests-context.xml")
	@Transactional
	public static class FailingAfterTransactionSpringRuleTestCase {

		@ClassRule
		public static final SpringClassRule springClassRule = new SpringClassRule();

		@Rule
		public final SpringMethodRule springMethodRule = new SpringMethodRule();


		@Test
		public void testNothing() {
		}

		@AfterTransaction
		public void afterTransaction() {
			fail("always failing afterTransaction()");
		}
	}

}

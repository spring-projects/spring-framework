/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.test.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * JUnit 4 based unit test for {@link TestContextManager}, which verifies proper
 * <em>execution order</em> of registered {@link TestExecutionListener
 * TestExecutionListeners}.
 *
 * @author Sam Brannen
 * @since 2.5
 */
public class TestContextManagerTests {

	private static final String FIRST = "veni";
	private static final String SECOND = "vidi";
	private static final String THIRD = "vici";

	private static final List<String> afterTestMethodCalls = new ArrayList<String>();
	private static final List<String> beforeTestMethodCalls = new ArrayList<String>();

	protected static final Log logger = LogFactory.getLog(TestContextManagerTests.class);

	private final TestContextManager testContextManager = new TestContextManager(ExampleTestCase.class);


	private Method getTestMethod() throws NoSuchMethodException {
		return ExampleTestCase.class.getDeclaredMethod("exampleTestMethod", (Class<?>[]) null);
	}

	/**
	 * Asserts the <em>execution order</em> of 'before' and 'after' test method
	 * calls on {@link TestExecutionListener listeners} registered for the
	 * configured {@link TestContextManager}.
	 *
	 * @see #beforeTestMethodCalls
	 * @see #afterTestMethodCalls
	 */
	private static void assertExecutionOrder(List<String> expectedBeforeTestMethodCalls,
			List<String> expectedAfterTestMethodCalls, final String usageContext) {

		if (expectedBeforeTestMethodCalls == null) {
			expectedBeforeTestMethodCalls = new ArrayList<String>();
		}
		if (expectedAfterTestMethodCalls == null) {
			expectedAfterTestMethodCalls = new ArrayList<String>();
		}

		if (logger.isDebugEnabled()) {
			for (String listenerName : beforeTestMethodCalls) {
				logger.debug("'before' listener [" + listenerName + "] (" + usageContext + ").");
			}
			for (String listenerName : afterTestMethodCalls) {
				logger.debug("'after' listener [" + listenerName + "] (" + usageContext + ").");
			}
		}

		assertTrue("Verifying execution order of 'before' listeners' (" + usageContext + ").",
			expectedBeforeTestMethodCalls.equals(beforeTestMethodCalls));
		assertTrue("Verifying execution order of 'after' listeners' (" + usageContext + ").",
			expectedAfterTestMethodCalls.equals(afterTestMethodCalls));
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		beforeTestMethodCalls.clear();
		afterTestMethodCalls.clear();
		assertExecutionOrder(null, null, "BeforeClass");
	}

	/**
	 * Verifies the expected {@link TestExecutionListener}
	 * <em>execution order</em> after all test methods have completed.
	 */
	@AfterClass
	public static void verifyListenerExecutionOrderAfterClass() throws Exception {
		assertExecutionOrder(Arrays.<String> asList(FIRST, SECOND, THIRD),
			Arrays.<String> asList(THIRD, SECOND, FIRST), "AfterClass");
	}

	@Before
	public void setUpTestContextManager() throws Throwable {
		assertEquals("Verifying the number of registered TestExecutionListeners.", 3,
			this.testContextManager.getTestExecutionListeners().size());

		this.testContextManager.beforeTestMethod(new ExampleTestCase(), getTestMethod());
	}

	/**
	 * Verifies the expected {@link TestExecutionListener}
	 * <em>execution order</em> within a test method.
	 *
	 * @see #verifyListenerExecutionOrderAfterClass()
	 */
	@Test
	public void verifyListenerExecutionOrderWithinTestMethod() {
		assertExecutionOrder(Arrays.<String> asList(FIRST, SECOND, THIRD), null, "Test");
	}

	@After
	public void tearDownTestContextManager() throws Throwable {
		this.testContextManager.afterTestMethod(new ExampleTestCase(), getTestMethod(), null);
	}


	@TestExecutionListeners({ FirstTel.class, SecondTel.class, ThirdTel.class })
	private static class ExampleTestCase {

		@SuppressWarnings("unused")
		public void exampleTestMethod() {
			assertTrue(true);
		}
	}

	private static class NamedTestExecutionListener extends AbstractTestExecutionListener {

		private final String name;


		public NamedTestExecutionListener(final String name) {
			this.name = name;
		}

		@Override
		public void beforeTestMethod(final TestContext testContext) {
			beforeTestMethodCalls.add(this.name);
		}

		@Override
		public void afterTestMethod(final TestContext testContext) {
			afterTestMethodCalls.add(this.name);
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("name", this.name).toString();
		}
	}

	private static class FirstTel extends NamedTestExecutionListener {

		public FirstTel() {
			super(FIRST);
		}
	}

	private static class SecondTel extends NamedTestExecutionListener {

		public SecondTel() {
			super(SECOND);
		}
	}

	private static class ThirdTel extends NamedTestExecutionListener {

		public ThirdTel() {
			super(THIRD);
		}
	}

}

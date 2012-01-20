/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.test.context.junit38;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueSource;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.annotation.SystemProfileValueSource;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Verifies proper handling of {@link IfProfileValue &#064;IfProfileValue} and
 * {@link ProfileValueSourceConfiguration &#064;ProfileValueSourceConfiguration}
 * in conjunction with {@link AbstractJUnit38SpringContextTests}.
 * 
 * @author Sam Brannen
 * @since 2.5
 */
public class ProfileValueJUnit38SpringContextTests extends TestCase {

	private static final String EMPTY = "testIfProfileValueEmpty";
	private static final String DISABLED_VIA_WRONG_NAME = "testIfProfileValueDisabledViaWrongName";
	private static final String DISABLED_VIA_WRONG_VALUE = "testIfProfileValueDisabledViaWrongValue";
	private static final String ENABLED_VIA_MULTIPLE_VALUES = "testIfProfileValueEnabledViaMultipleValues";
	private static final String ENABLED_VIA_SINGLE_VALUE = "testIfProfileValueEnabledViaSingleValue";
	private static final String NOT_CONFIGURED = "testIfProfileValueNotConfigured";

	private static final String NAME = "ProfileValueAnnotationAwareTransactionalTests.profile_value.name";
	private static final String VALUE = "enigma";

	private final Map<String, Integer> expectedInvocationCounts = new HashMap<String, Integer>();


	public ProfileValueJUnit38SpringContextTests() {
		System.setProperty(NAME, VALUE);
	}

	@Override
	protected void setUp() throws Exception {
		this.expectedInvocationCounts.put(EMPTY, 0);
		this.expectedInvocationCounts.put(DISABLED_VIA_WRONG_NAME, 0);
		this.expectedInvocationCounts.put(DISABLED_VIA_WRONG_VALUE, 0);
		this.expectedInvocationCounts.put(ENABLED_VIA_SINGLE_VALUE, 1);
		this.expectedInvocationCounts.put(ENABLED_VIA_MULTIPLE_VALUES, 1);
		this.expectedInvocationCounts.put(NOT_CONFIGURED, 1);
	}

	private void configureDisabledClassExpectations() {
		this.expectedInvocationCounts.put(ENABLED_VIA_SINGLE_VALUE, 0);
		this.expectedInvocationCounts.put(ENABLED_VIA_MULTIPLE_VALUES, 0);
		this.expectedInvocationCounts.put(NOT_CONFIGURED, 0);
	}

	private void runTestAndAssertCounters(Class<? extends DefaultProfileValueSourceTestCase> testCaseType,
			String testName, int expectedInvocationCount, int expectedErrorCount, int expectedFailureCount)
			throws Exception {

		DefaultProfileValueSourceTestCase testCase = testCaseType.newInstance();
		testCase.setName(testName);
		TestResult testResult = testCase.run();
		assertEquals("Verifying number of invocations for test method [" + testName + "].", expectedInvocationCount,
			testCase.invocationCount);
		assertEquals("Verifying number of errors for test method [" + testName + "].", expectedErrorCount,
			testResult.errorCount());
		assertEquals("Verifying number of failures for test method [" + testName + "].", expectedFailureCount,
			testResult.failureCount());
	}

	private void runTests(final Class<? extends DefaultProfileValueSourceTestCase> testCaseType) throws Exception {
		runTestAndAssertCounters(testCaseType, EMPTY, expectedInvocationCounts.get(EMPTY), 0, 0);
		runTestAndAssertCounters(testCaseType, DISABLED_VIA_WRONG_NAME,
			expectedInvocationCounts.get(DISABLED_VIA_WRONG_NAME), 0, 0);
		runTestAndAssertCounters(testCaseType, DISABLED_VIA_WRONG_VALUE,
			expectedInvocationCounts.get(DISABLED_VIA_WRONG_VALUE), 0, 0);
		runTestAndAssertCounters(testCaseType, ENABLED_VIA_SINGLE_VALUE,
			expectedInvocationCounts.get(ENABLED_VIA_SINGLE_VALUE), 0, 0);
		runTestAndAssertCounters(testCaseType, ENABLED_VIA_MULTIPLE_VALUES,
			expectedInvocationCounts.get(ENABLED_VIA_MULTIPLE_VALUES), 0, 0);
		runTestAndAssertCounters(testCaseType, NOT_CONFIGURED, expectedInvocationCounts.get(NOT_CONFIGURED), 0, 0);
	}

	public void testDefaultProfileValueSource() throws Exception {
		assertEquals("Verifying the type of the configured ProfileValueSource.", SystemProfileValueSource.class,
			new DefaultProfileValueSourceTestCase().getProfileValueSource().getClass());
		runTests(DefaultProfileValueSourceTestCase.class);
	}

	public void testHardCodedProfileValueSource() throws Exception {
		assertEquals("Verifying the type of the configured ProfileValueSource.", HardCodedProfileValueSource.class,
			new HardCodedProfileValueSourceTestCase().getProfileValueSource().getClass());
		runTests(HardCodedProfileValueSourceTestCase.class);
	}

	public void testClassLevelIfProfileValueEnabledSingleValue() throws Exception {
		runTests(ClassLevelIfProfileValueEnabledSingleValueTestCase.class);
	}

	public void testClassLevelIfProfileValueDisabledSingleValue() throws Exception {
		configureDisabledClassExpectations();
		runTests(ClassLevelIfProfileValueDisabledSingleValueTestCase.class);
	}

	public void testClassLevelIfProfileValueEnabledMultiValue() throws Exception {
		runTests(ClassLevelIfProfileValueEnabledMultiValueTestCase.class);
	}

	public void testClassLevelIfProfileValueDisabledMultiValue() throws Exception {
		configureDisabledClassExpectations();
		runTests(ClassLevelIfProfileValueDisabledMultiValueTestCase.class);
	}


	// -------------------------------------------------------------------

	/**
	 * Note that {@link TestExecutionListeners @TestExecutionListeners} is
	 * explicitly configured with an empty list, thus disabling all default
	 * listeners.
	 */
	@SuppressWarnings("deprecation")
	@TestExecutionListeners(listeners = {}, inheritListeners = false)
	public static class DefaultProfileValueSourceTestCase extends AbstractJUnit38SpringContextTests {

		int invocationCount = 0;


		public ProfileValueSource getProfileValueSource() {
			return super.profileValueSource;
		}

		@IfProfileValue(name = NAME, value = "")
		public void testIfProfileValueEmpty() {
			this.invocationCount++;
			fail("An empty profile value should throw an IllegalArgumentException.");
		}

		@IfProfileValue(name = NAME + "X", value = VALUE)
		public void testIfProfileValueDisabledViaWrongName() {
			this.invocationCount++;
			fail("The body of a disabled test should never be executed!");
		}

		@IfProfileValue(name = NAME, value = VALUE + "X")
		public void testIfProfileValueDisabledViaWrongValue() {
			this.invocationCount++;
			fail("The body of a disabled test should never be executed!");
		}

		@IfProfileValue(name = NAME, value = VALUE)
		public void testIfProfileValueEnabledViaSingleValue() {
			this.invocationCount++;
		}

		@IfProfileValue(name = NAME, values = { "foo", VALUE, "bar" })
		public void testIfProfileValueEnabledViaMultipleValues() {
			this.invocationCount++;
		}

		public void testIfProfileValueNotConfigured() {
			this.invocationCount++;
		}
	}

	@ProfileValueSourceConfiguration(HardCodedProfileValueSource.class)
	public static class HardCodedProfileValueSourceTestCase extends DefaultProfileValueSourceTestCase {
	}

	public static class HardCodedProfileValueSource implements ProfileValueSource {

		public String get(final String key) {
			return (key.equals(NAME) ? VALUE : null);
		}
	}

	@IfProfileValue(name = NAME, value = VALUE)
	public static class ClassLevelIfProfileValueEnabledSingleValueTestCase extends DefaultProfileValueSourceTestCase {
	}

	@IfProfileValue(name = NAME, value = VALUE + "X")
	public static class ClassLevelIfProfileValueDisabledSingleValueTestCase extends DefaultProfileValueSourceTestCase {
	}

	@IfProfileValue(name = NAME, values = { "foo", VALUE, "bar" })
	public static class ClassLevelIfProfileValueEnabledMultiValueTestCase extends DefaultProfileValueSourceTestCase {
	}

	@IfProfileValue(name = NAME, values = { "foo", "bar", "baz" })
	public static class ClassLevelIfProfileValueDisabledMultiValueTestCase extends DefaultProfileValueSourceTestCase {
	}

}

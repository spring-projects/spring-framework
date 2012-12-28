/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.annotation;

import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * Verifies proper handling of {@link IfProfileValue @IfProfileValue} and
 * {@link ProfileValueSourceConfiguration @ProfileValueSourceConfiguration} in
 * conjunction with {@link AbstractAnnotationAwareTransactionalTests}.
 *
 * @author Sam Brannen
 * @since 2.5
 */
public class ProfileValueAnnotationAwareTransactionalTests extends TestCase {

	private static final String NAME = "ProfileValueAnnotationAwareTransactionalTests.profile_value.name";

	private static final String VALUE = "enigma";


	public ProfileValueAnnotationAwareTransactionalTests() {
		System.setProperty(NAME, VALUE);
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

	private void runTests(Class<? extends DefaultProfileValueSourceTestCase> testCaseType) throws Exception {
		runTestAndAssertCounters(testCaseType, "testIfProfileValueEmpty", 0, 0, 0);
		runTestAndAssertCounters(testCaseType, "testIfProfileValueDisabledViaWrongName", 0, 0, 0);
		runTestAndAssertCounters(testCaseType, "testIfProfileValueDisabledViaWrongValue", 0, 0, 0);
		runTestAndAssertCounters(testCaseType, "testIfProfileValueEnabledViaSingleValue", 1, 0, 0);
		runTestAndAssertCounters(testCaseType, "testIfProfileValueEnabledViaMultipleValues", 1, 0, 0);
		runTestAndAssertCounters(testCaseType, "testIfProfileValueNotConfigured", 1, 0, 0);
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


	@SuppressWarnings("deprecation")
	public static class DefaultProfileValueSourceTestCase extends AbstractAnnotationAwareTransactionalTests {

		int invocationCount = 0;


		public DefaultProfileValueSourceTestCase() {
		}

		public ProfileValueSource getProfileValueSource() {
			return super.profileValueSource;
		}

		@Override
		protected String getConfigPath() {
			return "ProfileValueAnnotationAwareTransactionalTests-context.xml";
		}

		@NotTransactional
		@IfProfileValue(name = NAME)
		public void testIfProfileValueEmpty() {
			this.invocationCount++;
			fail("The body of a disabled test should never be executed!");
		}

		@NotTransactional
		@IfProfileValue(name = NAME + "X", value = VALUE)
		public void testIfProfileValueDisabledViaWrongName() {
			this.invocationCount++;
			fail("The body of a disabled test should never be executed!");
		}

		@NotTransactional
		@IfProfileValue(name = NAME, value = VALUE + "X")
		public void testIfProfileValueDisabledViaWrongValue() {
			this.invocationCount++;
			fail("The body of a disabled test should never be executed!");
		}

		@NotTransactional
		@IfProfileValue(name = NAME, value = VALUE)
		public void testIfProfileValueEnabledViaSingleValue() {
			this.invocationCount++;
		}

		@NotTransactional
		@IfProfileValue(name = NAME, values = { "foo", VALUE, "bar" })
		public void testIfProfileValueEnabledViaMultipleValues() {
			this.invocationCount++;
		}

		@NotTransactional
		public void testIfProfileValueNotConfigured() {
			this.invocationCount++;
		}
	}

	@ProfileValueSourceConfiguration(HardCodedProfileValueSource.class)
	public static class HardCodedProfileValueSourceTestCase extends DefaultProfileValueSourceTestCase {
	}

	public static class HardCodedProfileValueSource implements ProfileValueSource {

		public String get(String key) {
			return (key.equals(NAME) ? VALUE : null);
		}
	}

}

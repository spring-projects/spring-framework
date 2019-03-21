/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ProfileValueUtils}.
 *
 * @author Sam Brannen
 * @since 3.0
 */
public class ProfileValueUtilsTests {

	private static final String NON_ANNOTATED_METHOD = "nonAnnotatedMethod";
	private static final String ENABLED_ANNOTATED_METHOD = "enabledAnnotatedMethod";
	private static final String DISABLED_ANNOTATED_METHOD = "disabledAnnotatedMethod";

	private static final String NAME = "ProfileValueUtilsTests.profile_value.name";
	private static final String VALUE = "enigma";


	@BeforeClass
	public static void setProfileValue() {
		System.setProperty(NAME, VALUE);
	}

	private void assertClassIsEnabled(Class<?> testClass) throws Exception {
		assertTrue("Test class [" + testClass + "] should be enabled.",
			ProfileValueUtils.isTestEnabledInThisEnvironment(testClass));
	}

	private void assertClassIsDisabled(Class<?> testClass) throws Exception {
		assertFalse("Test class [" + testClass + "] should be disbled.",
			ProfileValueUtils.isTestEnabledInThisEnvironment(testClass));
	}

	private void assertMethodIsEnabled(String methodName, Class<?> testClass) throws Exception {
		Method testMethod = testClass.getMethod(methodName);
		assertTrue("Test method [" + testMethod + "] should be enabled.",
			ProfileValueUtils.isTestEnabledInThisEnvironment(testMethod, testClass));
	}

	private void assertMethodIsDisabled(String methodName, Class<?> testClass) throws Exception {
		Method testMethod = testClass.getMethod(methodName);
		assertFalse("Test method [" + testMethod + "] should be disabled.",
			ProfileValueUtils.isTestEnabledInThisEnvironment(testMethod, testClass));
	}

	private void assertMethodIsEnabled(ProfileValueSource profileValueSource, String methodName, Class<?> testClass)
			throws Exception {
		Method testMethod = testClass.getMethod(methodName);
		assertTrue("Test method [" + testMethod + "] should be enabled for ProfileValueSource [" + profileValueSource
				+ "].", ProfileValueUtils.isTestEnabledInThisEnvironment(profileValueSource, testMethod, testClass));
	}

	private void assertMethodIsDisabled(ProfileValueSource profileValueSource, String methodName, Class<?> testClass)
			throws Exception {
		Method testMethod = testClass.getMethod(methodName);
		assertFalse("Test method [" + testMethod + "] should be disabled for ProfileValueSource [" + profileValueSource
				+ "].", ProfileValueUtils.isTestEnabledInThisEnvironment(profileValueSource, testMethod, testClass));
	}

	// -------------------------------------------------------------------

	@Test
	public void isTestEnabledInThisEnvironmentForProvidedClass() throws Exception {
		assertClassIsEnabled(NonAnnotated.class);
		assertClassIsEnabled(EnabledAnnotatedSingleValue.class);
		assertClassIsEnabled(EnabledAnnotatedMultiValue.class);
		assertClassIsEnabled(MetaEnabledClass.class);
		assertClassIsEnabled(MetaEnabledWithCustomProfileValueSourceClass.class);
		assertClassIsEnabled(EnabledWithCustomProfileValueSourceOnTestInterface.class);

		assertClassIsDisabled(DisabledAnnotatedSingleValue.class);
		assertClassIsDisabled(DisabledAnnotatedSingleValueOnTestInterface.class);
		assertClassIsDisabled(DisabledAnnotatedMultiValue.class);
		assertClassIsDisabled(MetaDisabledClass.class);
		assertClassIsDisabled(MetaDisabledWithCustomProfileValueSourceClass.class);
	}

	@Test
	public void isTestEnabledInThisEnvironmentForProvidedMethodAndClass() throws Exception {
		assertMethodIsEnabled(NON_ANNOTATED_METHOD, NonAnnotated.class);

		assertMethodIsEnabled(NON_ANNOTATED_METHOD, EnabledAnnotatedSingleValue.class);
		assertMethodIsEnabled(ENABLED_ANNOTATED_METHOD, EnabledAnnotatedSingleValue.class);
		assertMethodIsDisabled(DISABLED_ANNOTATED_METHOD, EnabledAnnotatedSingleValue.class);


		assertMethodIsEnabled(NON_ANNOTATED_METHOD, MetaEnabledAnnotatedSingleValue.class);
		assertMethodIsEnabled(ENABLED_ANNOTATED_METHOD, MetaEnabledAnnotatedSingleValue.class);
		assertMethodIsDisabled(DISABLED_ANNOTATED_METHOD, MetaEnabledAnnotatedSingleValue.class);

		assertMethodIsEnabled(NON_ANNOTATED_METHOD, EnabledAnnotatedMultiValue.class);
		assertMethodIsEnabled(ENABLED_ANNOTATED_METHOD, EnabledAnnotatedMultiValue.class);
		assertMethodIsDisabled(DISABLED_ANNOTATED_METHOD, EnabledAnnotatedMultiValue.class);

		assertMethodIsDisabled(NON_ANNOTATED_METHOD, DisabledAnnotatedSingleValue.class);
		assertMethodIsDisabled(ENABLED_ANNOTATED_METHOD, DisabledAnnotatedSingleValue.class);
		assertMethodIsDisabled(DISABLED_ANNOTATED_METHOD, DisabledAnnotatedSingleValue.class);

		assertMethodIsDisabled(NON_ANNOTATED_METHOD, DisabledAnnotatedSingleValueOnTestInterface.class);

		assertMethodIsDisabled(NON_ANNOTATED_METHOD, MetaDisabledAnnotatedSingleValue.class);
		assertMethodIsDisabled(ENABLED_ANNOTATED_METHOD, MetaDisabledAnnotatedSingleValue.class);
		assertMethodIsDisabled(DISABLED_ANNOTATED_METHOD, MetaDisabledAnnotatedSingleValue.class);

		assertMethodIsDisabled(NON_ANNOTATED_METHOD, DisabledAnnotatedMultiValue.class);
		assertMethodIsDisabled(ENABLED_ANNOTATED_METHOD, DisabledAnnotatedMultiValue.class);
		assertMethodIsDisabled(DISABLED_ANNOTATED_METHOD, DisabledAnnotatedMultiValue.class);
	}

	@Test
	public void isTestEnabledInThisEnvironmentForProvidedProfileValueSourceMethodAndClass() throws Exception {

		ProfileValueSource profileValueSource = SystemProfileValueSource.getInstance();

		assertMethodIsEnabled(profileValueSource, NON_ANNOTATED_METHOD, NonAnnotated.class);

		assertMethodIsEnabled(profileValueSource, NON_ANNOTATED_METHOD, EnabledAnnotatedSingleValue.class);
		assertMethodIsEnabled(profileValueSource, ENABLED_ANNOTATED_METHOD, EnabledAnnotatedSingleValue.class);
		assertMethodIsDisabled(profileValueSource, DISABLED_ANNOTATED_METHOD, EnabledAnnotatedSingleValue.class);

		assertMethodIsEnabled(profileValueSource, NON_ANNOTATED_METHOD, EnabledAnnotatedMultiValue.class);
		assertMethodIsEnabled(profileValueSource, ENABLED_ANNOTATED_METHOD, EnabledAnnotatedMultiValue.class);
		assertMethodIsDisabled(profileValueSource, DISABLED_ANNOTATED_METHOD, EnabledAnnotatedMultiValue.class);

		assertMethodIsDisabled(profileValueSource, NON_ANNOTATED_METHOD, DisabledAnnotatedSingleValue.class);
		assertMethodIsDisabled(profileValueSource, ENABLED_ANNOTATED_METHOD, DisabledAnnotatedSingleValue.class);
		assertMethodIsDisabled(profileValueSource, DISABLED_ANNOTATED_METHOD, DisabledAnnotatedSingleValue.class);

		assertMethodIsDisabled(profileValueSource, NON_ANNOTATED_METHOD, DisabledAnnotatedMultiValue.class);
		assertMethodIsDisabled(profileValueSource, ENABLED_ANNOTATED_METHOD, DisabledAnnotatedMultiValue.class);
		assertMethodIsDisabled(profileValueSource, DISABLED_ANNOTATED_METHOD, DisabledAnnotatedMultiValue.class);
	}


	// -------------------------------------------------------------------

	@SuppressWarnings("unused")
	private static class NonAnnotated {

		public void nonAnnotatedMethod() {
		}
	}

	@SuppressWarnings("unused")
	@IfProfileValue(name = NAME, value = VALUE)
	private static class EnabledAnnotatedSingleValue {

		public void nonAnnotatedMethod() {
		}

		@IfProfileValue(name = NAME, value = VALUE)
		public void enabledAnnotatedMethod() {
		}

		@IfProfileValue(name = NAME, value = VALUE + "X")
		public void disabledAnnotatedMethod() {
		}
	}

	@IfProfileValue(name = NAME, value = VALUE + "X")
	private interface IfProfileValueTestInterface {
	}

	@SuppressWarnings("unused")
	private static class DisabledAnnotatedSingleValueOnTestInterface implements IfProfileValueTestInterface {

		public void nonAnnotatedMethod() {
		}
	}

	@SuppressWarnings("unused")
	@IfProfileValue(name = NAME, values = { "foo", VALUE, "bar" })
	private static class EnabledAnnotatedMultiValue {

		public void nonAnnotatedMethod() {
		}

		@IfProfileValue(name = NAME, value = VALUE)
		public void enabledAnnotatedMethod() {
		}

		@IfProfileValue(name = NAME, value = VALUE + "X")
		public void disabledAnnotatedMethod() {
		}
	}

	@SuppressWarnings("unused")
	@IfProfileValue(name = NAME, value = VALUE + "X")
	private static class DisabledAnnotatedSingleValue {

		public void nonAnnotatedMethod() {
		}

		@IfProfileValue(name = NAME, value = VALUE)
		public void enabledAnnotatedMethod() {
		}

		@IfProfileValue(name = NAME, value = VALUE + "X")
		public void disabledAnnotatedMethod() {
		}
	}

	@SuppressWarnings("unused")
	@IfProfileValue(name = NAME, values = { "foo", "bar" })
	private static class DisabledAnnotatedMultiValue {

		public void nonAnnotatedMethod() {
		}

		@IfProfileValue(name = NAME, value = VALUE)
		public void enabledAnnotatedMethod() {
		}

		@IfProfileValue(name = NAME, value = VALUE + "X")
		public void disabledAnnotatedMethod() {
		}
	}

	@IfProfileValue(name = NAME, value = VALUE)
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaEnabled {
	}

	@IfProfileValue(name = NAME, value = VALUE + "X")
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaDisabled {
	}

	@MetaEnabled
	private static class MetaEnabledClass {
	}

	@MetaDisabled
	private static class MetaDisabledClass {
	}

	@SuppressWarnings("unused")
	@MetaEnabled
	private static class MetaEnabledAnnotatedSingleValue {

		public void nonAnnotatedMethod() {
		}

		@MetaEnabled
		public void enabledAnnotatedMethod() {
		}

		@MetaDisabled
		public void disabledAnnotatedMethod() {
		}
	}

	@SuppressWarnings("unused")
	@MetaDisabled
	private static class MetaDisabledAnnotatedSingleValue {

		public void nonAnnotatedMethod() {
		}

		@MetaEnabled
		public void enabledAnnotatedMethod() {
		}

		@MetaDisabled
		public void disabledAnnotatedMethod() {
		}
	}

	public static class HardCodedProfileValueSource implements ProfileValueSource {

		@Override
		public String get(final String key) {
			return (key.equals(NAME) ? "42" : null);
		}
	}

	@ProfileValueSourceConfiguration(HardCodedProfileValueSource.class)
	@IfProfileValue(name = NAME, value = "42")
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaEnabledWithCustomProfileValueSource {
	}

	@ProfileValueSourceConfiguration(HardCodedProfileValueSource.class)
	@IfProfileValue(name = NAME, value = "13")
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaDisabledWithCustomProfileValueSource {
	}

	@MetaEnabledWithCustomProfileValueSource
	private static class MetaEnabledWithCustomProfileValueSourceClass {
	}

	@MetaDisabledWithCustomProfileValueSource
	private static class MetaDisabledWithCustomProfileValueSourceClass {
	}

	@ProfileValueSourceConfiguration(HardCodedProfileValueSource.class)
	private interface CustomProfileValueSourceTestInterface {
	}

	@IfProfileValue(name = NAME, value = "42")
	private static class EnabledWithCustomProfileValueSourceOnTestInterface
			implements CustomProfileValueSourceTestInterface {
	}

}

/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.support;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.SpringProperties;
import org.springframework.test.context.TestConstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;
import static org.springframework.test.context.TestConstructor.AutowireMode.ANNOTATED;

/**
 * Unit tests for {@link TestConstructorUtils}.
 *
 * @author Sam Brannen
 * @since 5.2
 */
class TestConstructorUtilsTests {

	@AfterEach
	void clearGlobalFlag() {
		setGlobalFlag(null);
	}

	@Test
	void notAutowirable() throws Exception {
		assertNotAutowirable(NotAutowirableTestCase.class);
	}

	@Test
	void autowiredAnnotation() throws Exception {
		assertAutowirable(AutowiredAnnotationTestCase.class);
	}

	@Test
	void testConstructorAnnotation() throws Exception {
		assertAutowirable(TestConstructorAnnotationTestCase.class);
	}

	@Test
	void automaticallyAutowired() throws Exception {
		setGlobalFlag();
		assertAutowirable(AutomaticallyAutowiredTestCase.class);
	}

	@Test
	void automaticallyAutowiredButOverriddenLocally() throws Exception {
		setGlobalFlag();
		assertNotAutowirable(TestConstructorAnnotationOverridesGlobalFlagTestCase.class);
	}

	@Test
	void globalFlagVariations() throws Exception {
		Class<?> testClass = AutomaticallyAutowiredTestCase.class;

		setGlobalFlag(ALL.name());
		assertAutowirable(testClass);

		setGlobalFlag(ALL.name().toLowerCase());
		assertAutowirable(testClass);

		setGlobalFlag("\t" + ALL.name().toLowerCase() + "   ");
		assertAutowirable(testClass);

		setGlobalFlag("bogus");
		assertNotAutowirable(testClass);

		setGlobalFlag("        ");
		assertNotAutowirable(testClass);
	}

	private void assertAutowirable(Class<?> testClass) throws NoSuchMethodException {
		Constructor<?> constructor = testClass.getDeclaredConstructor();
		assertThat(TestConstructorUtils.isAutowirableConstructor(constructor, testClass)).isTrue();
	}

	private void assertNotAutowirable(Class<?> testClass) throws NoSuchMethodException {
		Constructor<?> constructor = testClass.getDeclaredConstructor();
		assertThat(TestConstructorUtils.isAutowirableConstructor(constructor, testClass)).isFalse();
	}

	private void setGlobalFlag() {
		setGlobalFlag(ALL.name());
	}

	private void setGlobalFlag(String flag) {
		SpringProperties.setProperty(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME, flag);
	}


	static class NotAutowirableTestCase {
	}

	// The following declaration simply verifies that @Autowired on the constructor takes
	// precedence.
	@TestConstructor(autowireMode = ANNOTATED)
	static class AutowiredAnnotationTestCase {

		@Autowired
		AutowiredAnnotationTestCase() {
		}
	}

	@TestConstructor(autowireMode = ALL)
	static class TestConstructorAnnotationTestCase {
	}

	static class AutomaticallyAutowiredTestCase {
	}

	@TestConstructor(autowireMode = ANNOTATED)
	static class TestConstructorAnnotationOverridesGlobalFlagTestCase {
	}

}

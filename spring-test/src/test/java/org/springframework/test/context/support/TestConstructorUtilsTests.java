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

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestConstructor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TestConstructorUtils}.
 *
 * @author Sam Brannen
 * @since 5.2
 */
public class TestConstructorUtilsTests {

	@After
	public void clearGlobalFlag() {
		System.clearProperty(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_PROPERTY_NAME);
	}

	@Test
	public void notAutowirable() throws Exception {
		assertNotAutowirable(NotAutowirableTestCase.class);
	}

	@Test
	public void autowiredAnnotation() throws Exception {
		assertAutowirable(AutowiredAnnotationTestCase.class);
	}

	@Test
	public void testConstructorAnnotation() throws Exception {
		assertAutowirable(TestConstructorAnnotationTestCase.class);
	}

	@Test
	public void automaticallyAutowired() throws Exception {
		setGlobalFlag();
		assertAutowirable(AutomaticallyAutowiredTestCase.class);
	}

	@Test
	public void automaticallyAutowiredButOverriddenLocally() throws Exception {
		setGlobalFlag();
		assertNotAutowirable(TestConstructorAnnotationOverridesGlobalFlagTestCase.class);
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
		System.setProperty(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_PROPERTY_NAME, "true");
	}


	static class NotAutowirableTestCase {
	}

	@TestConstructor(autowire = false)
	static class AutowiredAnnotationTestCase {

		@Autowired
		AutowiredAnnotationTestCase() {
		}
	}

	@TestConstructor(autowire = true)
	static class TestConstructorAnnotationTestCase {
	}

	static class AutomaticallyAutowiredTestCase {
	}

	@TestConstructor(autowire = false)
	static class TestConstructorAnnotationOverridesGlobalFlagTestCase {
	}

}

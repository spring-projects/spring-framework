/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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
 * Tests for {@link TestConstructorUtils}.
 *
 * @author Sam Brannen
 * @since 5.2
 */
class TestConstructorUtilsTests {

	private static final PropertyProvider propertyProvider = name -> null;


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
	void constructorAnnotation() throws Exception {
		assertAutowirable(TestConstructorAnnotationTestCase.class);
	}

	@Test
	void constructorAsMetaAnnotation() throws Exception {
		assertAutowirable(TestConstructorAsMetaAnnotationTestCase.class);
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
		assertThat(TestConstructorUtils.isAutowirableConstructor(constructor, propertyProvider)).isTrue();
	}

	private void assertNotAutowirable(Class<?> testClass) throws NoSuchMethodException {
		Constructor<?> constructor = testClass.getDeclaredConstructor();
		assertThat(TestConstructorUtils.isAutowirableConstructor(constructor, propertyProvider)).isFalse();
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

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@TestConstructor(autowireMode = ALL)
	@interface AutowireConstructor {
	}

	@AutowireConstructor
	static class TestConstructorAsMetaAnnotationTestCase {
	}

	static class AutomaticallyAutowiredTestCase {
	}

	@TestConstructor(autowireMode = ANNOTATED)
	static class TestConstructorAnnotationOverridesGlobalFlagTestCase {
	}

}

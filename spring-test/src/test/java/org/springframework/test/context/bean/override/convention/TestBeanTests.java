/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.bean.override.convention;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.bean.override.BeanOverrideContextCustomizerTestUtils;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link TestBean @TestBean}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public class TestBeanTests {

	@Test
	void cannotOverrideBeanByNameWithNoSuchBeanName() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("anotherBean", String.class, () -> "example");
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByNameLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to replace bean: there is no bean with name 'beanToOverride' \
						and type java.lang.String (as required by field 'FailureByNameLookup.example').""");
	}

	@Test
	void cannotOverrideBeanByNameWithBeanOfWrongType() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("beanToOverride", Integer.class, () -> 42);
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByNameLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to replace bean: there is no bean with name 'beanToOverride' \
						and type java.lang.String (as required by field 'FailureByNameLookup.example').""");
	}

	@Test
	void cannotOverrideBeanByTypeWithNoSuchBeanType() {
		GenericApplicationContext context = new GenericApplicationContext();
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByTypeLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to override bean: there are no beans of \
						type %s (as required by field '%s.example').""",
						String.class.getName(), FailureByTypeLookup.class.getSimpleName());
	}

	@Test
	void cannotOverrideBeanByTypeWithTooManyBeansOfThatType() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("bean1", String.class, () -> "example1");
		context.registerBean("bean2", String.class, () -> "example2");
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByTypeLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to select a bean to override: found 2 beans of type java.lang.String \
						(as required by field 'FailureByTypeLookup.example'): %s""", List.of("bean1", "bean2"));
	}

	@Test
	void contextCustomizerCannotBeCreatedWithMissingOverrideMethod() {
		GenericApplicationContext context = new GenericApplicationContext();
		assertThatIllegalStateException()
				.isThrownBy(() -> BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(
						FailureMissingDefaultOverrideMethod.class, context))
				.withMessage("No static method found named example() or beanToOverride() in %s with return type %s",
						FailureMissingDefaultOverrideMethod.class.getName(), String.class.getName());
	}

	@Test
	void contextCustomizerCannotBeCreatedWithMissingExplicitOverrideMethod() {
		GenericApplicationContext context = new GenericApplicationContext();
		assertThatIllegalStateException()
				.isThrownBy(() -> BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(
						FailureMissingExplicitOverrideMethod.class, context))
				.withMessage("No static method found named createExample() in %s with return type %s",
						FailureMissingExplicitOverrideMethod.class.getName(), String.class.getName());
	}

	@Test
	void contextCustomizerCannotBeCreatedWithFieldInParentAndMissingOverrideMethod() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("beanToOverride", String.class, () -> "example");
		assertThatIllegalStateException()
				.isThrownBy(() -> BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(
						FailureOverrideInParentWithoutFactoryMethod.class, context))
				.withMessage("No static method found named beanToOverride() in %s with return type %s",
						AbstractByNameLookup.class.getName(), String.class.getName());
	}

	@Test
	void contextCustomizerCannotBeCreatedWithCompetingOverrideMethods() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("bean", String.class, () -> "example");
		assertThatIllegalStateException()
				.isThrownBy(() -> BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(
						FailureCompetingOverrideMethods.class, context))
				.withMessage("Found 2 competing static methods named example() or beanToOverride() in %s with return type %s",
						FailureCompetingOverrideMethods.class.getName(), String.class.getName());
	}


	static class FailureByNameLookup {

		@TestBean(name = "beanToOverride", enforceOverride = true)
		private String example;

		static String example() {
			throw new IllegalStateException("Should not be called");
		}
	}

	static class FailureByTypeLookup {

		@TestBean(enforceOverride = true)
		private String example;

		static String example() {
			throw new IllegalStateException("Should not be called");
		}
	}

	static class FailureMissingDefaultOverrideMethod {

		@TestBean(name = "beanToOverride")
		private String example;

		// No example() or beanToOverride() method
	}

	static class FailureMissingExplicitOverrideMethod {

		@TestBean(methodName = "createExample")
		private String example;

		// NO createExample() method
	}

	abstract static class AbstractByNameLookup {

		@TestBean
		String beanToOverride;

		// No beanToOverride() method
	}

	static class FailureOverrideInParentWithoutFactoryMethod extends AbstractByNameLookup {
	}

	abstract static class AbstractCompetingMethods {

		static String example() {
			throw new IllegalStateException("Should not be called");
		}
	}

	static class FailureCompetingOverrideMethods extends AbstractCompetingMethods {

		@TestBean(name = "beanToOverride")
		String example;

		static String beanToOverride() {
			throw new IllegalStateException("Should not be called");
		}
	}

}

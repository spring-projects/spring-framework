/*
 * Copyright 2002-2024 the original author or authors.
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
 * Tests for {@link TestBean}.
 *
 * @author Stephane Nicoll
 */
public class TestBeanTests {

	@Test
	void contextCustomizerCannotBeCreatedWithNoSuchBeanName() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("anotherBean", String.class, () -> "example");
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByNameLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to override bean 'beanToOverride': there is no bean definition \
						to replace with that name of type java.lang.String""");
	}

	@Test
	void contextCustomizerCannotBeCreatedWithNoSuchBeanType() {
		GenericApplicationContext context = new GenericApplicationContext();
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByTypeLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to override bean: no bean definitions of \
						type %s (as required by annotated field '%s.example')""".formatted(
						String.class.getName(), FailureByTypeLookup.class.getSimpleName()));
	}

	@Test
	void contextCustomizerCannotBeCreatedWithTooManyBeansOfThatType() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("bean1", String.class, () -> "example1");
		context.registerBean("bean2", String.class, () -> "example2");
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByTypeLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to select a bean definition to override: found 2 bean definitions \
						of type %s (as required by annotated field '%s.example'): %s""".formatted(
						String.class.getName(), FailureByTypeLookup.class.getSimpleName(), List.of("bean1", "bean2")));
	}

	@Test
	void contextCustomizerCannotBeCreatedWithBeanOfWrongType() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("beanToOverride", Integer.class, () -> 42);
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByNameLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to override bean 'beanToOverride': there is no bean definition \
						to replace with that name of type %s""".formatted(
						String.class.getName()));
	}

	@Test
	void contextCustomizerCannotBeCreatedWithMissingOverrideMethod() {
		GenericApplicationContext context = new GenericApplicationContext();
		assertThatIllegalStateException()
				.isThrownBy(() -> BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(
						FailureMissingDefaultOverrideMethod.class, context))
				.withMessage("No static method found named example() or beanToOverride() in %s with return type %s"
						.formatted(FailureMissingDefaultOverrideMethod.class.getName(), String.class.getName()));
	}

	@Test
	void contextCustomizerCannotBeCreatedWithMissingExplicitOverrideMethod() {
		GenericApplicationContext context = new GenericApplicationContext();
		assertThatIllegalStateException()
				.isThrownBy(() -> BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(
						FailureMissingExplicitOverrideMethod.class, context))
				.withMessage("No static method found named createExample() in %s with return type %s"
						.formatted(FailureMissingExplicitOverrideMethod.class.getName(), String.class.getName()));
	}

	@Test
	void contextCustomizerCannotBeCreatedWithFieldInParentAndMissingOverrideMethod() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("beanToOverride", String.class, () -> "example");
		assertThatIllegalStateException()
				.isThrownBy(() -> BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(
						FailureOverrideInParentWithoutFactoryMethod.class, context))
				.withMessage("No static method found named beanToOverride() in %s with return type %s"
						.formatted(FailureOverrideInParentWithoutFactoryMethod.class.getName(), String.class.getName()));
	}

	@Test
	void contextCustomizerCannotBeCreatedWitCompetingOverrideMethods() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("bean", String.class, () -> "example");
		assertThatIllegalStateException()
				.isThrownBy(() -> BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(
						FailureCompetingOverrideMethods.class, context))
				.withMessage("Found 2 competing static methods named example() or beanToOverride() in %s with return type %s"
						.formatted(FailureCompetingOverrideMethods.class.getName(), String.class.getName()));
	}

	static class FailureByTypeLookup {

		@TestBean
		private String example;

		private static String example() {
			throw new IllegalStateException("Should not be called");
		}
	}

	static class FailureByNameLookup {

		@TestBean(name = "beanToOverride")
		private String example;

		private static String example() {
			throw new IllegalStateException("Should not be called");
		}
	}

	static class FailureMissingDefaultOverrideMethod {

		@TestBean(name = "beanToOverride")
		private String example;

		// Expected static String example() { ... }
		// or static String beanToOverride() { ... }

	}

	static class FailureMissingExplicitOverrideMethod {

		@TestBean(methodName = "createExample")
		private String example;

		// Expected static String createExample() { ... }

	}

	abstract static class AbstractByNameLookup {

		@TestBean(methodName = "beanToOverride")
		protected String beanToOverride;

	}

	static class FailureOverrideInParentWithoutFactoryMethod extends AbstractByNameLookup {

		// No beanToOverride() method

	}

	abstract static class AbstractCompetingMethods {

		@TestBean(name = "beanToOverride")
		protected String example;

		static String example() {
			throw new IllegalStateException("Should not be called");
		}
	}

	static class FailureCompetingOverrideMethods extends AbstractCompetingMethods {

		static String beanToOverride() {
			throw new IllegalStateException("Should not be called");
		}

	}

}

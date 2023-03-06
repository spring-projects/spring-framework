/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.validation.beanvalidation;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BeanValidationBeanRegistrationAotProcessor}.
 *
 * @author Sebastien Deleuze
 */
class BeanValidationBeanRegistrationAotProcessorTests {

	private final BeanValidationBeanRegistrationAotProcessor processor = new BeanValidationBeanRegistrationAotProcessor();

	private final GenerationContext generationContext = new TestGenerationContext();

	@Test
	void shouldSkipNonAnnotatedType() {
		process(EmptyClass.class);
		assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
	}

	@Test
	void shouldProcessMethodParameterLevelConstraint() {
		process(MethodParameterLevelConstraint.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(ExistsValidator.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.generationContext.getRuntimeHints());
	}

	@Test
	void shouldProcessConstructorParameterLevelConstraint() {
		process(ConstructorParameterLevelConstraint.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(ExistsValidator.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.generationContext.getRuntimeHints());
	}

	@Test
	void shouldProcessPropertyLevelConstraint() {
		process(PropertyLevelConstraint.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(ExistsValidator.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.generationContext.getRuntimeHints());
	}

	private void process(Class<?> beanClass) {
		BeanRegistrationAotContribution contribution = createContribution(beanClass);
		if (contribution != null) {
			contribution.applyTo(this.generationContext, mock());
		}
	}

	@Nullable
	private BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
		return this.processor.processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
	}

	private static class EmptyClass { }

	@Constraint(validatedBy = { ExistsValidator.class })
	@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
	@Retention(RUNTIME)
	@Repeatable(Exists.List.class)
	@interface Exists {

		String message() default "Does not exist";

		Class<?>[] groups() default { };

		Class<? extends Payload>[] payload() default { };

		@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
		@Retention(RUNTIME)
		@Documented
		@interface List {
			Exists[] value();
		}
	}

	static class ExistsValidator implements ConstraintValidator<Exists, String> {

		@Override
		public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
			return true;
		}
	}

	static class MethodParameterLevelConstraint {

		@SuppressWarnings("unused")
		public String hello(@Exists String name) {
			return "Hello " + name;
		}

	}

	@SuppressWarnings("unused")
	static class ConstructorParameterLevelConstraint {

		private final String name;

		public ConstructorParameterLevelConstraint(@Exists String name) {
			this.name = name;
		}

		public String hello() {
			return "Hello " + this.name;
		}

	}

	@SuppressWarnings("unused")
	static class PropertyLevelConstraint {

		@Exists
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}

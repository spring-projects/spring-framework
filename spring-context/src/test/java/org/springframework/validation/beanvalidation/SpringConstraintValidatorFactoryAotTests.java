/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import jakarta.annotation.Resource;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Payload;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.AotDetector;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.test.tools.TestCompiler;

import static java.lang.annotation.ElementType.FIELD;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringConstraintValidatorFactory} when running with
 * AOT-generated artifacts.
 *
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34079">gh-34079</a>
 */
class SpringConstraintValidatorFactoryAotTests {

	@AfterEach
	void clearAotProperty() {
		System.clearProperty(AotDetector.AOT_ENABLED);
	}


	@Test
	void injectsDependenciesIntoConstraintValidatorWithAotGeneratedContext() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(applicationContext);
		applicationContext.registerBeanDefinition("testComponent",
				new RootBeanDefinition(TestComponent.class));
		applicationContext.registerBeanDefinition("validator",
				new RootBeanDefinition(LocalValidatorFactoryBean.class));
		applicationContext.registerBeanDefinition("pojo", new RootBeanDefinition(Pojo.class));

		TestGenerationContext generationContext = new TestGenerationContext();
		new ApplicationContextAotGenerator().processAheadOfTime(applicationContext, generationContext);
		generationContext.writeGeneratedContent();

		TestCompiler.forSystem().with(generationContext).compile(compiled -> {
			System.setProperty(AotDetector.AOT_ENABLED, "true");
			@SuppressWarnings("unchecked")
			ApplicationContextInitializer<GenericApplicationContext> initializer =
					compiled.getInstance(ApplicationContextInitializer.class);
			GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
			initializer.initialize(freshApplicationContext);
			freshApplicationContext.refresh();
			try {
				Validator validator = freshApplicationContext.getBean(Validator.class);
				Set<ConstraintViolation<Pojo>> violations = validator.validate(new Pojo("valid"));
				assertThat(violations).isEmpty();
				violations = validator.validate(new Pojo("invalid"));
				assertThat(violations).hasSize(1);
			}
			finally {
				freshApplicationContext.close();
			}
		});
	}

	@Test
	void injectsDependenciesWhenAotEnabledAndNoInjectionProcessorRegistered() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testComponent", new TestComponent());
		System.setProperty(AotDetector.AOT_ENABLED, "true");

		ValidValueValidator validator = new SpringConstraintValidatorFactory(beanFactory)
				.getInstance(ValidValueValidator.class);
		assertThat(validator.autowiredComponent).isNotNull();
		assertThat(validator.resourceComponent).isNotNull();
		assertThat(validator.setterComponent).isNotNull();
	}

	@Test
	void doesNotInjectDependenciesWithoutAot() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testComponent", new TestComponent());

		ValidValueValidator validator = new SpringConstraintValidatorFactory(beanFactory)
				.getInstance(ValidValueValidator.class);
		assertThat(validator.autowiredComponent).isNull();
		assertThat(validator.resourceComponent).isNull();
		assertThat(validator.setterComponent).isNull();
	}


	public static class TestComponent {

		public boolean isValid(String value) {
			return "valid".equals(value);
		}
	}


	@Constraint(validatedBy = ValidValueValidator.class)
	@Target(FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ValidValue {

		String message() default "Invalid value";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};
	}


	public static class ValidValueValidator implements ConstraintValidator<ValidValue, String> {

		@Autowired
		TestComponent autowiredComponent;

		@Resource
		TestComponent resourceComponent;

		TestComponent setterComponent;

		@Resource
		public void setTestComponent(TestComponent testComponent) {
			this.setterComponent = testComponent;
		}

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			return (this.autowiredComponent.isValid(value) && this.resourceComponent.isValid(value) &&
					this.setterComponent.isValid(value));
		}
	}


	public static class Pojo {

		@ValidValue
		private final String value;

		public Pojo() {
			this("valid");
		}

		public Pojo(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}
	}

}

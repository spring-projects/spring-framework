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

package org.springframework.test.context.bean.override;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.example.ExampleBeanOverrideAnnotation;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.FailingExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link BeanOverrideBeanFactoryPostProcessor} combined with a
 * {@link BeanOverrideRegistrar}.
 *
 * @author Simon Baslé
 */
class BeanOverrideBeanFactoryPostProcessorTests {

	@Test
	void canReplaceExistingBeanDefinitions() {
		AnnotationConfigApplicationContext context = createContext(ReplaceBeans.class);
		context.register(ReplaceBeans.class);
		context.registerBean("explicit", ExampleService.class, () -> new RealExampleService("unexpected"));
		context.registerBean("implicitName", ExampleService.class, () -> new RealExampleService("unexpected"));

		context.refresh();

		assertThat(context.getBean("explicit")).isSameAs(OVERRIDE_SERVICE);
		assertThat(context.getBean("implicitName")).isSameAs(OVERRIDE_SERVICE);
	}

	@Test
	void cannotReplaceIfNoBeanMatching() {
		AnnotationConfigApplicationContext context = createContext(ReplaceBeans.class);
		context.register(ReplaceBeans.class);
		//note we don't register any original bean here

		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("Unable to override bean 'explicit'; there is no bean definition " +
						"to replace with that name of type org.springframework.test.context.bean.override.example.ExampleService");
	}

	@Test
	void canReplaceExistingBeanDefinitionsWithCreateReplaceStrategy() {
		AnnotationConfigApplicationContext context = createContext(CreateIfOriginalIsMissingBean.class);
		context.register(CreateIfOriginalIsMissingBean.class);
		context.registerBean("explicit", ExampleService.class, () -> new RealExampleService("unexpected"));
		context.registerBean("implicitName", ExampleService.class, () -> new RealExampleService("unexpected"));

		context.refresh();

		assertThat(context.getBean("explicit")).isSameAs(OVERRIDE_SERVICE);
		assertThat(context.getBean("implicitName")).isSameAs(OVERRIDE_SERVICE);
	}

	@Test
	void canCreateIfOriginalMissingWithCreateReplaceStrategy() {
		AnnotationConfigApplicationContext context = createContext(CreateIfOriginalIsMissingBean.class);
		context.register(CreateIfOriginalIsMissingBean.class);
		//note we don't register original beans here

		context.refresh();

		assertThat(context.getBean("explicit")).isSameAs(OVERRIDE_SERVICE);
		assertThat(context.getBean("implicitName")).isSameAs(OVERRIDE_SERVICE);
	}

	@Test
	void canOverrideBeanProducedByFactoryBeanWithClassObjectTypeAttribute() {
		AnnotationConfigApplicationContext context = createContext(OverriddenFactoryBean.class);
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(TestFactoryBean.class);
		factoryBeanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, SomeInterface.class);
		context.registerBeanDefinition("beanToBeOverridden", factoryBeanDefinition);
		context.register(OverriddenFactoryBean.class);

		context.refresh();

		assertThat(context.getBean("beanToBeOverridden")).isSameAs(OVERRIDE);
	}

	@Test
	void canOverrideBeanProducedByFactoryBeanWithResolvableTypeObjectTypeAttribute() {
		AnnotationConfigApplicationContext context = createContext(OverriddenFactoryBean.class);
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(TestFactoryBean.class);
		ResolvableType objectType = ResolvableType.forClass(SomeInterface.class);
		factoryBeanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, objectType);
		context.registerBeanDefinition("beanToBeOverridden", factoryBeanDefinition);
		context.register(OverriddenFactoryBean.class);

		context.refresh();

		assertThat(context.getBean("beanToBeOverridden")).isSameAs(OVERRIDE);
	}

	@Test
	void postProcessorShouldNotTriggerEarlyInitialization() {
		AnnotationConfigApplicationContext context = createContext(EagerInitBean.class);

		context.register(FactoryBeanRegisteringPostProcessor.class);
		context.register(EarlyBeanInitializationDetector.class);
		context.register(EagerInitBean.class);

		assertThatNoException().isThrownBy(context::refresh);
	}

	@Test
	void allowReplaceDefinitionWhenSingletonDefinitionPresent() {
		AnnotationConfigApplicationContext context = createContext(SingletonBean.class);
		RootBeanDefinition definition = new RootBeanDefinition(String.class, () -> "ORIGINAL");
		definition.setScope(BeanDefinition.SCOPE_SINGLETON);
		context.registerBeanDefinition("singleton", definition);
		context.register(SingletonBean.class);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.isSingleton("singleton")).as("isSingleton").isTrue();
		assertThat(context.getBean("singleton")).as("overridden").isEqualTo("USED THIS");
	}

	@Test
	void copyDefinitionPrimaryFallbackAndScope() {
		AnnotationConfigApplicationContext context = createContext(SingletonBean.class);
		context.getBeanFactory().registerScope("customScope", new SimpleThreadScope());
		RootBeanDefinition definition = new RootBeanDefinition(String.class, () -> "ORIGINAL");
		definition.setScope("customScope");
		definition.setPrimary(true);
		definition.setFallback(true);
		context.registerBeanDefinition("singleton", definition);
		context.register(SingletonBean.class);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.getBeanDefinition("singleton"))
				.isNotSameAs(definition)
				.matches(BeanDefinition::isPrimary, "isPrimary")
				.matches(BeanDefinition::isFallback, "isFallback")
				.satisfies(d -> assertThat(d.getScope()).isEqualTo("customScope"))
				.matches(Predicate.not(BeanDefinition::isSingleton), "!isSingleton")
				.matches(Predicate.not(BeanDefinition::isPrototype), "!isPrototype");
	}

	@Test
	void createDefinitionShouldSetQualifierElement() {
		AnnotationConfigApplicationContext context = createContext(QualifiedBean.class);
		context.registerBeanDefinition("singleton", new RootBeanDefinition(String.class, () -> "ORIGINAL"));
		context.register(QualifiedBean.class);

		assertThatNoException().isThrownBy(context::refresh);

		assertThat(context.getBeanDefinition("singleton"))
				.isInstanceOfSatisfying(RootBeanDefinition.class, this::isTheValueField);
	}


	private void isTheValueField(RootBeanDefinition def) {
		assertThat(def.getQualifiedElement()).isInstanceOfSatisfying(Field.class, field -> {
					assertThat(field.getDeclaringClass()).isEqualTo(QualifiedBean.class);
					assertThat(field.getName()).as("annotated field name")
							.isEqualTo("value");
				});
	}

	private AnnotationConfigApplicationContext createContext(Class<?>... classes) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanOverrideContextCustomizer.registerInfrastructure(context, Set.of(classes));
		return context;
	}


	/*
		Classes to parse and register with the bean post processor
		-----
		Note that some of these are both a @Configuration class and bean override field holder.
		This is for this test convenience, as typically the bean override annotated fields
		should not be in configuration classes but rather in test case classes
		(where a TestExecutionListener automatically discovers and parses them).
	 */

	static final SomeInterface OVERRIDE = new SomeImplementation();

	static final ExampleService OVERRIDE_SERVICE = new FailingExampleService();

	static class ReplaceBeans {

		@ExampleBeanOverrideAnnotation(value = "useThis", beanName = "explicit")
		private ExampleService explicitName;

		@ExampleBeanOverrideAnnotation(value = "useThis")
		private ExampleService implicitName;

		static ExampleService useThis() {
			return OVERRIDE_SERVICE;
		}
	}

	static class CreateIfOriginalIsMissingBean {

		@ExampleBeanOverrideAnnotation(value = "useThis", createIfMissing = true, beanName = "explicit")
		private ExampleService explicitName;

		@ExampleBeanOverrideAnnotation(value = "useThis", createIfMissing = true)
		private ExampleService implicitName;

		static ExampleService useThis() {
			return OVERRIDE_SERVICE;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class OverriddenFactoryBean {

		@ExampleBeanOverrideAnnotation(value = "fOverride", beanName = "beanToBeOverridden")
		SomeInterface f;

		static SomeInterface fOverride() {
			return OVERRIDE;
		}

		@Bean
		TestFactoryBean testFactoryBean() {
			return new TestFactoryBean();
		}
	}

	static class EagerInitBean {

		@ExampleBeanOverrideAnnotation(value = "useThis", createIfMissing = true)
		private ExampleService service;

		static ExampleService useThis() {
			return OVERRIDE_SERVICE;
		}
	}

	static class SingletonBean {

		@ExampleBeanOverrideAnnotation(beanName = "singleton",
				value = "useThis", createIfMissing = false)
		private String value;

		static String useThis() {
			return "USED THIS";
		}
	}

	static class QualifiedBean {

		@Qualifier("preferThis")
		@ExampleBeanOverrideAnnotation(beanName = "singleton",
				value = "useThis", createIfMissing = false)
		private String value;

		static String useThis() {
			return "USED THIS";
		}
	}

	static class TestFactoryBean implements FactoryBean<Object> {

		@Override
		public Object getObject() {
			return new SomeImplementation();
		}

		@Override
		public Class<?> getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

	static class FactoryBeanRegisteringPostProcessor implements BeanFactoryPostProcessor, Ordered {

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			RootBeanDefinition beanDefinition = new RootBeanDefinition(TestFactoryBean.class);
			((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("test", beanDefinition);
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}
	}

	static class EarlyBeanInitializationDetector implements BeanFactoryPostProcessor {

		@Override
		@SuppressWarnings("unchecked")
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			Map<String, BeanWrapper> cache = (Map<String, BeanWrapper>) ReflectionTestUtils.getField(beanFactory,
					"factoryBeanInstanceCache");
			Assert.isTrue(cache.isEmpty(), "Early initialization of factory bean triggered.");
		}
	}

	interface SomeInterface {
	}

	static class SomeImplementation implements SomeInterface {
	}

}

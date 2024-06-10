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
import java.util.LinkedHashSet;
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
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BeanOverrideBeanFactoryPostProcessor} combined with a
 * {@link BeanOverrideRegistrar}.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 */
class BeanOverrideBeanFactoryPostProcessorTests {

	@Test
	void replaceBeanByNameWithMatchingBeanDefinition() {
		AnnotationConfigApplicationContext context = createContext(CaseByName.class);
		context.registerBean("descriptionBean", String.class, () -> "Original");
		context.refresh();

		assertThat(context.getBean("descriptionBean")).isEqualTo("overridden");
	}

	@Test
	void replaceBeanByNameWithoutMatchingBeanDefinitionFails() {
		AnnotationConfigApplicationContext context = createContext(CaseByName.class);

		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("Unable to override bean 'descriptionBean': there is no bean definition " +
						"to replace with that name of type java.lang.String");
	}

	@Test
	void replaceBeanByNameWithMatchingBeanDefinitionAndWrongTypeFails() {
		AnnotationConfigApplicationContext context = createContext(CaseByName.class);
		context.registerBean("descriptionBean", Integer.class, () -> -1);

		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("Unable to override bean 'descriptionBean': there is no bean definition " +
						"to replace with that name of type java.lang.String");
	}

	@Test
	void replaceBeanByNameCanOverrideBeanProducedByFactoryBeanWithClassObjectTypeAttribute() {
		AnnotationConfigApplicationContext context = prepareContextWithFactoryBean(CharSequence.class);
		context.refresh();

		assertThat(context.getBean("beanToBeOverridden")).isEqualTo("overridden");
	}

	@Test
	void replaceBeanByNameCanOverrideBeanProducedByFactoryBeanWithResolvableTypeObjectTypeAttribute() {
		AnnotationConfigApplicationContext context = prepareContextWithFactoryBean(ResolvableType.forClass(CharSequence.class));
		context.refresh();

		assertThat(context.getBean("beanToBeOverridden")).isEqualTo("overridden");
	}

	private AnnotationConfigApplicationContext prepareContextWithFactoryBean(Object objectTypeAttribute) {
		AnnotationConfigApplicationContext context = createContext(CaseOverrideBeanProducedByFactoryBean.class);
		context.registerBean("testFactoryBean", TestFactoryBean.class, TestFactoryBean::new);
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(TestFactoryBean.class);
		factoryBeanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, objectTypeAttribute);
		context.registerBeanDefinition("beanToBeOverridden", factoryBeanDefinition);
		return context;
	}

	@Test
	void replaceBeanByTypeWithSingleMatchingBean() {
		AnnotationConfigApplicationContext context = createContext(CaseByType.class);
		context.registerBean("someInteger", Integer.class, () -> 1);
		context.refresh();

		assertThat(context.getBean("someInteger")).isEqualTo(42);
	}

	@Test
	void replaceBeanByTypeWithoutMatchingBeanFails() {
		AnnotationConfigApplicationContext context = createContext(CaseByType.class);

		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("Unable to override bean: no bean definitions of type java.lang.Integer " +
						"(as required by annotated field 'CaseByType.counter')");
	}

	@Test
	void replaceBeanByTypeWithMultipleMatchesAndNoQualifierFails() {
		AnnotationConfigApplicationContext context = createContext(CaseByType.class);
		context.registerBean("someInteger", Integer.class, () -> 1);
		context.registerBean("anotherInteger", Integer.class, () -> 2);

		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("Unable to select a bean definition to override: found 2 bean definitions " +
						"of type java.lang.Integer (as required by annotated field 'CaseByType.counter'): " +
						"[someInteger, anotherInteger]");
	}

	@Test
	void replaceBeanByTypeWithMultipleMatchesAndFieldNameQualifierMatches() {
		AnnotationConfigApplicationContext context = createContext(CaseByType.class);
		context.registerBean("counter", Integer.class, () -> 1);
		context.registerBean("someInteger", Integer.class, () -> 2);
		context.refresh();

		assertThat(context.getBean("counter")).isSameAs(42);
	}

	@Test
	void createOrReplaceBeanByNameWithMatchingBeanDefinition() {
		AnnotationConfigApplicationContext context = createContext(CaseByNameWithReplaceOrCreateStrategy.class);
		context.registerBean("descriptionBean", String.class, () -> "Original");
		context.refresh();

		assertThat(context.getBean("descriptionBean")).isEqualTo("overridden");
	}

	@Test
	void createOrReplaceBeanByNameWithoutMatchingDefinitionCreatesBeanDefinition() {
		AnnotationConfigApplicationContext context = createContext(CaseByNameWithReplaceOrCreateStrategy.class);
		context.refresh();

		assertThat(context.getBean("descriptionBean")).isEqualTo("overridden");
	}

	@Test
	void createOrReplaceBeanByTypeWithMatchingBean() {
		AnnotationConfigApplicationContext context = createContext(CaseByTypeWithReplaceOrCreateStrategy.class);
		context.registerBean("someBean", String.class, () -> "Original");
		context.refresh();

		assertThat(context.getBean("someBean")).isEqualTo("overridden");
	}

	@Test
	void createOrReplaceBeanByTypeWithoutMatchingDefinitionCreatesBeanDefinition() {
		AnnotationConfigApplicationContext context = createContext(CaseByTypeWithReplaceOrCreateStrategy.class);
		context.refresh();

		String generatedBeanName = "java.lang.String#0";
		assertThat(context.getBeanDefinitionNames()).contains(generatedBeanName);
		assertThat(context.getBean(generatedBeanName)).isEqualTo("overridden");
	}

	@Test
	void postProcessorShouldNotTriggerEarlyInitialization() {
		AnnotationConfigApplicationContext context = createContext(CaseByTypeWithReplaceOrCreateStrategy.class);

		context.register(FactoryBeanRegisteringPostProcessor.class);
		context.register(EarlyBeanInitializationDetector.class);

		assertThatNoException().isThrownBy(context::refresh);
	}

	@Test
	void allowReplaceDefinitionWhenSingletonDefinitionPresent() {
		AnnotationConfigApplicationContext context = createContext(CaseByName.class);
		RootBeanDefinition definition = new RootBeanDefinition(String.class, () -> "ORIGINAL");
		definition.setScope(BeanDefinition.SCOPE_SINGLETON);
		context.registerBeanDefinition("descriptionBean", definition);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.isSingleton("descriptionBean")).as("isSingleton").isTrue();
		assertThat(context.getBean("descriptionBean")).isEqualTo("overridden");
	}

	@Test
	void copyDefinitionPrimaryFallbackAndScope() {
		AnnotationConfigApplicationContext context = createContext(CaseByName.class);
		context.getBeanFactory().registerScope("customScope", new SimpleThreadScope());
		RootBeanDefinition definition = new RootBeanDefinition(String.class, () -> "ORIGINAL");
		definition.setScope("customScope");
		definition.setPrimary(true);
		definition.setFallback(true);
		context.registerBeanDefinition("descriptionBean", definition);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.getBeanDefinition("descriptionBean"))
				.isNotSameAs(definition)
				.matches(BeanDefinition::isPrimary, "isPrimary")
				.matches(BeanDefinition::isFallback, "isFallback")
				.satisfies(d -> assertThat(d.getScope()).isEqualTo("customScope"))
				.matches(Predicate.not(BeanDefinition::isSingleton), "!isSingleton")
				.matches(Predicate.not(BeanDefinition::isPrototype), "!isPrototype");
	}

	@Test
	void createDefinitionShouldSetQualifierElement() {
		AnnotationConfigApplicationContext context = createContext(CaseByNameWithQualifier.class);
		context.registerBeanDefinition("descriptionBean", new RootBeanDefinition(String.class, () -> "ORIGINAL"));

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.getBeanDefinition("descriptionBean"))
				.isInstanceOfSatisfying(RootBeanDefinition.class, this::isTheValueField);
	}


	private void isTheValueField(RootBeanDefinition def) {
		assertThat(def.getQualifiedElement()).isInstanceOfSatisfying(Field.class, field -> {
					assertThat(field.getDeclaringClass()).isEqualTo(CaseByNameWithQualifier.class);
					assertThat(field.getName()).as("annotated field name")
							.isEqualTo("description");
				});
	}

	private AnnotationConfigApplicationContext createContext(Class<?> testClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		Set<OverrideMetadata> metadata = new LinkedHashSet<>(OverrideMetadata.forTestClass(testClass));
		new BeanOverrideContextCustomizer(metadata).customizeContext(context, mock(MergedContextConfiguration.class));
		return context;
	}


	static class CaseByName {

		@DummyBean(beanName = "descriptionBean")
		private String description;

	}

	static class CaseByType {

		@DummyBean
		private Integer counter;

	}

	static class CaseByNameWithReplaceOrCreateStrategy {

		@DummyBean(beanName = "descriptionBean", strategy = BeanOverrideStrategy.REPLACE_OR_CREATE_DEFINITION)
		private String description;

	}

	static class CaseByTypeWithReplaceOrCreateStrategy {

		@DummyBean(strategy = BeanOverrideStrategy.REPLACE_OR_CREATE_DEFINITION)
		private String description;

	}

	static class CaseByNameAndByTypeWithReplaceOrCreateStrategy {

		@DummyBean(beanName = "descriptionBean", strategy = BeanOverrideStrategy.REPLACE_OR_CREATE_DEFINITION)
		private String description;

		@DummyBean(strategy = BeanOverrideStrategy.REPLACE_OR_CREATE_DEFINITION)
		private Integer counter;

	}

	static class CaseOverrideBeanProducedByFactoryBean {

		@DummyBean(beanName = "beanToBeOverridden")
		CharSequence description;

	}

	static class CaseByNameWithQualifier {

		@Qualifier("preferThis")
		@DummyBean(beanName = "descriptionBean")
		private String description;

	}

	static class TestFactoryBean implements FactoryBean<Object> {

		@Override
		public Object getObject() {
			return "test";
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

}

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

package org.springframework.test.context.bean.override;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
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
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BeanOverrideBeanFactoryPostProcessor} combined with a
 * {@link BeanOverrideRegistry}.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class BeanOverrideBeanFactoryPostProcessorTests {

	@Test
	void beanNameWithFactoryBeanPrefixIsRejected() {
		AnnotationConfigApplicationContext context = createContext(FactoryBeanPrefixTestCase.class);

		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
					Unable to override bean '&messageService' for field 'FactoryBeanPrefixTestCase.messageService': \
					a FactoryBean cannot be overridden. To override the bean created by the FactoryBean, remove the \
					'&' prefix.""");
	}

	@Test
	void replaceBeanByNameWithMatchingBeanDefinition() {
		AnnotationConfigApplicationContext context = createContext(ByNameTestCase.class);
		context.registerBean("descriptionBean", String.class, () -> "Original");
		context.refresh();

		assertThat(context.getBean("descriptionBean")).isEqualTo("overridden");
	}

	@Test
	void replaceBeanByNameWithoutMatchingBeanDefinitionFails() {
		AnnotationConfigApplicationContext context = createContext(ByNameTestCase.class);

		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to replace bean: there is no bean with name 'descriptionBean' \
						and type java.lang.String (as required by field 'ByNameTestCase.description'). \
						If the bean is defined in a @Bean method, make sure the return type is the most \
						specific type possible (for example, the concrete implementation type).""");
	}

	@Test
	void replaceBeanByNameWithMatchingBeanDefinitionAndWrongTypeFails() {
		AnnotationConfigApplicationContext context = createContext(ByNameTestCase.class);
		context.registerBean("descriptionBean", Integer.class, () -> -1);

		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to replace bean: there is no bean with name 'descriptionBean' \
						and type java.lang.String (as required by field 'ByNameTestCase.description'). \
						If the bean is defined in a @Bean method, make sure the return type is the most \
						specific type possible (for example, the concrete implementation type).""");
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
		AnnotationConfigApplicationContext context = createContext(OverrideBeanProducedByFactoryBeanTestCase.class);
		// Register a TestFactoryBean that will not be overridden
		context.registerBean("testFactoryBean", TestFactoryBean.class, TestFactoryBean::new);
		// Register another TestFactoryBean that will be overridden
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(TestFactoryBean.class);
		factoryBeanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, objectTypeAttribute);
		context.registerBeanDefinition("beanToBeOverridden", factoryBeanDefinition);
		return context;
	}

	@Test
	void replaceBeanByTypeWithSingleMatchingBean() {
		AnnotationConfigApplicationContext context = createContext(ByTypeTestCase.class);
		context.registerBean("someInteger", Integer.class, () -> 1);
		context.refresh();

		assertThat(context.getBean("someInteger")).isEqualTo(42);
	}

	@Test
	void replaceBeanByTypeWithoutMatchingBeanFails() {
		AnnotationConfigApplicationContext context = createContext(ByTypeTestCase.class);

		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to override bean: there are no beans of type java.lang.Integer \
						(as required by field 'ByTypeTestCase.counter'). \
						If the bean is defined in a @Bean method, make sure the return type is the most \
						specific type possible (for example, the concrete implementation type).""");
	}

	@Test
	void replaceBeanByTypeWithMultipleCandidatesAndNoQualifierFails() {
		AnnotationConfigApplicationContext context = createContext(ByTypeTestCase.class);
		context.registerBean("someInteger", Integer.class, () -> 1);
		context.registerBean("anotherInteger", Integer.class, () -> 2);

		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to select a bean to override: found 2 beans of type java.lang.Integer \
						(as required by field 'ByTypeTestCase.counter'): %s""",
						List.of("someInteger", "anotherInteger"));
	}

	@Test
	void replaceBeanByTypeWithMultipleCandidatesAndFieldNameAsFallbackQualifier() {
		AnnotationConfigApplicationContext context = createContext(ByTypeTestCase.class);
		context.registerBean("counter", Integer.class, () -> 1);
		context.registerBean("someInteger", Integer.class, () -> 2);
		context.refresh();

		assertThat(context.getBean("counter")).isSameAs(42);
	}

	@Test  // gh-33819
	void replaceBeanByTypeWithMultipleCandidatesAndOnePrimary() {
		AnnotationConfigApplicationContext context = createContext(TestBeanByTypeTestCase.class);
		context.registerBean("description1", String.class, () -> "one");
		RootBeanDefinition beanDefinition2 = new RootBeanDefinition(String.class);
		beanDefinition2.getConstructorArgumentValues().addIndexedArgumentValue(0, "two");
		beanDefinition2.setPrimary(true);
		context.registerBeanDefinition("description2", beanDefinition2);
		context.refresh();

		assertThat(context.getBean("description1", String.class)).isEqualTo("one");
		assertThat(context.getBean("description2", String.class)).isEqualTo("overridden");
		assertThat(context.getBean(String.class)).isEqualTo("overridden");
	}

	@Test  // gh-33819
	void replaceBeanByTypeWithMultipleCandidatesAndMultiplePrimaryBeansFails() {
		AnnotationConfigApplicationContext context = createContext(TestBeanByTypeTestCase.class);

		RootBeanDefinition beanDefinition1 = new RootBeanDefinition(String.class);
		beanDefinition1.getConstructorArgumentValues().addIndexedArgumentValue(0, "one");
		beanDefinition1.setPrimary(true);
		context.registerBeanDefinition("description1", beanDefinition1);

		RootBeanDefinition beanDefinition2 = new RootBeanDefinition(String.class);
		beanDefinition2.getConstructorArgumentValues().addIndexedArgumentValue(0, "two");
		beanDefinition2.setPrimary(true);
		context.registerBeanDefinition("description2", beanDefinition2);

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
				.isThrownBy(context::refresh)
				.withMessage("No qualifying bean of type 'java.lang.String' available: " +
						"more than one 'primary' bean found among candidates: [description1, description2]");
	}

	@Test
	void createOrReplaceBeanByNameWithMatchingBeanDefinition() {
		AnnotationConfigApplicationContext context = createContext(ByNameWithReplaceOrCreateStrategyTestCase.class);
		context.registerBean("descriptionBean", String.class, () -> "Original");
		context.refresh();

		assertThat(context.getBean("descriptionBean")).isEqualTo("overridden");
	}

	@Test
	void createOrReplaceBeanByNameWithoutMatchingDefinitionCreatesBeanDefinition() {
		AnnotationConfigApplicationContext context = createContext(ByNameWithReplaceOrCreateStrategyTestCase.class);
		context.refresh();

		assertThat(context.getBean("descriptionBean")).isEqualTo("overridden");
	}

	@Test
	void createOrReplaceBeanByTypeWithMatchingBean() {
		AnnotationConfigApplicationContext context = createContext(ByTypeWithReplaceOrCreateStrategyTestCase.class);
		context.registerBean("someBean", String.class, () -> "Original");
		context.refresh();

		assertThat(context.getBean("someBean")).isEqualTo("overridden");
	}

	@Test
	void createOrReplaceBeanByTypeWithoutMatchingDefinitionCreatesBeanDefinition() {
		AnnotationConfigApplicationContext context = createContext(ByTypeWithReplaceOrCreateStrategyTestCase.class);
		context.refresh();

		String generatedBeanName = "java.lang.String#0";
		assertThat(context.getBeanDefinitionNames()).contains(generatedBeanName);
		assertThat(context.getBean(generatedBeanName)).isEqualTo("overridden");
	}

	@Test
	void postProcessorShouldNotTriggerEarlyInitialization() {
		AnnotationConfigApplicationContext context = createContext(ByTypeWithReplaceOrCreateStrategyTestCase.class);

		context.register(FactoryBeanRegisteringPostProcessor.class);
		context.register(EarlyBeanInitializationDetector.class);

		assertThatNoException().isThrownBy(context::refresh);
	}

	@Test
	void replaceBeanByNameWithMatchingBeanDefinitionWithExplicitSingletonScope() {
		AnnotationConfigApplicationContext context = createContext(ByNameTestCase.class);
		RootBeanDefinition definition = new RootBeanDefinition(String.class, () -> "ORIGINAL");
		definition.setScope(BeanDefinition.SCOPE_SINGLETON);
		context.registerBeanDefinition("descriptionBean", definition);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.isSingleton("descriptionBean")).as("isSingleton").isTrue();
		assertThat(context.getBean("descriptionBean")).isEqualTo("overridden");
	}

	@Test
	void replaceBeanByNameWithMatchingBeanDefinitionForClassBasedSingletonFactoryBean() {
		String beanName = "descriptionBean";
		AnnotationConfigApplicationContext context = createContext(ByNameTestCase.class);
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(SingletonStringFactoryBean.class);
		context.registerBeanDefinition(beanName, factoryBeanDefinition);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.isSingleton(beanName)).as("isSingleton").isTrue();
		assertThat(context.getBean(beanName)).isEqualTo("overridden");
	}

	@Test  // gh-33800
	void replaceBeanByNameWithMatchingBeanDefinitionForClassBasedNonSingletonFactoryBean() {
		String beanName = "descriptionBean";
		AnnotationConfigApplicationContext context = createContext(ByNameTestCase.class);
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(NonSingletonStringFactoryBean.class);
		context.registerBeanDefinition(beanName, factoryBeanDefinition);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.isSingleton(beanName)).as("isSingleton").isTrue();
		assertThat(context.getBean(beanName)).isEqualTo("overridden");
	}

	@Test
	void replaceBeanByNameWithMatchingBeanDefinitionForInterfaceBasedSingletonFactoryBean() {
		String beanName = "messageServiceBean";
		AnnotationConfigApplicationContext context = createContext(MessageServiceTestCase.class);
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(SingletonMessageServiceFactoryBean.class);
		context.registerBeanDefinition(beanName, factoryBeanDefinition);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.isSingleton(beanName)).as("isSingleton").isTrue();
		assertThat(context.getBean(beanName, MessageService.class).getMessage()).isEqualTo("overridden");
	}

	@Test  // gh-33800
	void replaceBeanByNameWithMatchingBeanDefinitionForInterfaceBasedNonSingletonFactoryBean() {
		String beanName = "messageServiceBean";
		AnnotationConfigApplicationContext context = createContext(MessageServiceTestCase.class);
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(NonSingletonMessageServiceFactoryBean.class);
		context.registerBeanDefinition(beanName, factoryBeanDefinition);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.isSingleton(beanName)).as("isSingleton").isTrue();
		assertThat(context.getBean(beanName, MessageService.class).getMessage()).isEqualTo("overridden");
	}

	@Test
	void replaceBeanByNameWithMatchingBeanDefinitionWithPrototypeScope() {
		String beanName = "descriptionBean";

		AnnotationConfigApplicationContext context = createContext(ByNameTestCase.class);
		RootBeanDefinition definition = new RootBeanDefinition(String.class, () -> "ORIGINAL");
		definition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		context.registerBeanDefinition(beanName, definition);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.isSingleton(beanName)).as("isSingleton").isTrue();
		assertThat(context.getBean(beanName, String.class)).isEqualTo("overridden");
	}

	@Test
	void replaceBeanByNameWithMatchingBeanDefinitionWithCustomScope() {
		String beanName = "descriptionBean";
		String scope = "customScope";

		AnnotationConfigApplicationContext context = createContext(ByNameTestCase.class);
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		beanFactory.registerScope(scope, new SimpleThreadScope());
		RootBeanDefinition definition = new RootBeanDefinition(String.class, () -> "ORIGINAL");
		definition.setScope(scope);
		context.registerBeanDefinition(beanName, definition);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.isSingleton(beanName)).as("isSingleton").isTrue();
		assertThat(context.getBean(beanName, String.class)).isEqualTo("overridden");
	}

	@Test
	void replaceBeanByNameWithMatchingBeanDefinitionForPrototypeScopedFactoryBean() {
		String beanName = "messageServiceBean";
		AnnotationConfigApplicationContext context = createContext(MessageServiceTestCase.class);
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(SingletonMessageServiceFactoryBean.class);
		factoryBeanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		context.registerBeanDefinition(beanName, factoryBeanDefinition);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.isSingleton(beanName)).as("isSingleton").isTrue();
		assertThat(context.getBean(beanName, MessageService.class).getMessage()).isEqualTo("overridden");
	}

	@Test
	void replaceBeanByNameWithMatchingBeanDefinitionRetainsPrimaryAndFallbackFlags() {
		AnnotationConfigApplicationContext context = createContext(ByNameTestCase.class);
		RootBeanDefinition definition = new RootBeanDefinition(String.class, () -> "ORIGINAL");
		definition.setPrimary(true);
		definition.setFallback(true);
		context.registerBeanDefinition("descriptionBean", definition);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.getBeanDefinition("descriptionBean"))
				.matches(BeanDefinition::isPrimary, "isPrimary")
				.matches(BeanDefinition::isFallback, "isFallback")
				.satisfies(d -> assertThat(d.getScope()).isEqualTo(""))
				.matches(BeanDefinition::isSingleton, "isSingleton")
				.matches(Predicate.not(BeanDefinition::isPrototype), "!isPrototype");
	}

	@Test
	void qualifiedElementIsSetToBeanOverrideFieldForNonexistentBeanDefinition() {
		AnnotationConfigApplicationContext context = createContext(TestBeanByNameTestCase.class);

		assertThatNoException().isThrownBy(context::refresh);
		assertThat(context.getBeanDefinition("descriptionBean"))
				.isInstanceOfSatisfying(RootBeanDefinition.class, this::qualifiedElementIsField);
	}


	private void qualifiedElementIsField(RootBeanDefinition def) {
		assertThat(def.getQualifiedElement()).isInstanceOfSatisfying(Field.class,
			field -> {
				assertThat(field.getDeclaringClass()).isEqualTo(TestBeanByNameTestCase.class);
				assertThat(field.getName()).as("annotated field name").isEqualTo("description");
			});
	}

	private static AnnotationConfigApplicationContext createContext(Class<?> testClass) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		Set<BeanOverrideHandler> handlers = new LinkedHashSet<>(BeanOverrideTestUtils.findHandlers(testClass));
		new BeanOverrideContextCustomizer(handlers).customizeContext(context, mock(MergedContextConfiguration.class));
		return context;
	}


	@FunctionalInterface
	interface MessageService {
		String getMessage();
	}

	static class FactoryBeanPrefixTestCase {

		@DummyBean(beanName = "&messageService")
		MessageService messageService;

	}

	static class ByNameTestCase {

		@DummyBean(beanName = "descriptionBean")
		private String description;

	}

	static class ByTypeTestCase {

		@DummyBean
		private Integer counter;

	}

	static class ByNameWithReplaceOrCreateStrategyTestCase {

		@DummyBean(beanName = "descriptionBean", strategy = BeanOverrideStrategy.REPLACE_OR_CREATE)
		private String description;

	}

	static class ByTypeWithReplaceOrCreateStrategyTestCase {

		@DummyBean(strategy = BeanOverrideStrategy.REPLACE_OR_CREATE)
		private String description;

	}

	static class ByNameAndByTypeWithReplaceOrCreateStrategyTestCase {

		@DummyBean(beanName = "descriptionBean", strategy = BeanOverrideStrategy.REPLACE_OR_CREATE)
		private String description;

		@DummyBean(strategy = BeanOverrideStrategy.REPLACE_OR_CREATE)
		private Integer counter;

	}

	static class OverrideBeanProducedByFactoryBeanTestCase {

		@DummyBean(beanName = "beanToBeOverridden")
		CharSequence description;

	}

	static class TestBeanByNameTestCase {

		@TestBean(name = "descriptionBean")
		String description;

		static String descriptionBean() {
			return "overridden";
		}
	}

	static class TestBeanByTypeTestCase {

		@TestBean
		String description;

		static String description() {
			return "overridden";
		}
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

	static class SingletonStringFactoryBean implements FactoryBean<String> {

		@Override
		public String getObject() {
			return "test";
		}

		@Override
		public Class<?> getObjectType() {
			return String.class;
		}
	}

	static class NonSingletonStringFactoryBean extends SingletonStringFactoryBean {

		@Override
		public boolean isSingleton() {
			return false;
		}
	}

	static class SingletonMessageServiceFactoryBean implements FactoryBean<MessageService> {

		@Override
		public MessageService getObject() {
			return () -> "test";
		}

		@Override
		public Class<?> getObjectType() {
			return MessageService.class;
		}
	}

	static class NonSingletonMessageServiceFactoryBean extends SingletonMessageServiceFactoryBean {

		@Override
		public boolean isSingleton() {
			return false;
		}
	}

	static class MessageServiceTestCase {

		@TestBean(name = "messageServiceBean")
		MessageService messageService;

		static MessageService messageService() {
			return () -> "overridden";
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

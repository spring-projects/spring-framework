/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.OverridingClassLoader;
import org.springframework.core.ResolvableType;
import org.springframework.core.SpringProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the bean type index in {@link DefaultListableBeanFactory}: every
 * by-type lookup must return the same bean names in the same order as a factory
 * that checks all bean definitions.
 *
 * @author Nazar Shuliak
 * @see BeanTypeIndex
 */
class DefaultListableBeanFactoryTypeIndexTests {

	private static final List<Class<?>> LOOKUP_TYPES = List.of(Service.class, ServiceImpl.class,
			SubService.class, Extra.class, ServiceWithExtra.class, Unrelated.class, Product.class,
			FactoryBean.class, ProductFactoryBean.class, Object.class, int.class, Integer.class,
			Number.class, Object[].class, CharSequence[].class, String[].class, Serializable.class);


	private final DefaultListableBeanFactory plainFactory = createFactoryWithoutTypeIndex();

	private final DefaultListableBeanFactory indexedFactory = new DefaultListableBeanFactory();


	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void lookupsBeforeSingletonsCreated(boolean includeNonSingletons) {
		forBothFactories(this::registerScenario);
		forBothFactories(DefaultListableBeanFactory::freezeConfiguration);

		assertSameLookups(includeNonSingletons);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void lookupsAfterSingletonsCreated(boolean includeNonSingletons) {
		forBothFactories(this::registerScenario);
		forBothFactories(DefaultListableBeanFactory::freezeConfiguration);
		forBothFactories(DefaultListableBeanFactory::preInstantiateSingletons);

		assertSameLookups(includeNonSingletons);
	}

	@Test
	void lookupForTypeOnlyExposedBySingletonInstance() {
		forBothFactories(this::registerScenario);
		forBothFactories(DefaultListableBeanFactory::freezeConfiguration);
		assertSameLookups(true);

		forBothFactories(DefaultListableBeanFactory::preInstantiateSingletons);

		assertThat(indexedFactory.getBeanNamesForType(Extra.class)).containsExactly("interfaceTyped");
		assertSameLookups(true);
	}

	@Test
	void lookupAfterBeanDefinitionRegisteredInFrozenConfiguration() {
		forBothFactories(this::registerScenario);
		forBothFactories(DefaultListableBeanFactory::freezeConfiguration);
		assertSameLookups(true);

		forBothFactories(factory -> factory.registerBeanDefinition("late", new RootBeanDefinition(SubService.class)));

		assertThat(indexedFactory.getBeanNamesForType(SubService.class)).contains("late");
		assertSameLookups(true);
	}

	@Test
	void lookupAfterSingletonRegisteredInFrozenConfiguration() {
		forBothFactories(this::registerScenario);
		forBothFactories(DefaultListableBeanFactory::freezeConfiguration);
		assertSameLookups(true);

		forBothFactories(factory -> factory.registerSingleton("manual", new ServiceWithExtra()));

		assertThat(indexedFactory.getBeanNamesForType(Extra.class)).contains("manual");
		assertSameLookups(true);
	}

	@Test
	void lookupForPrototypeWithPredictedTypeChangedAfterCreation() {
		forBothFactories(factory -> {
			registerScenario(factory);
			factory.addBeanPostProcessor(new PrototypeTypePredictingPostProcessor());
			factory.freezeConfiguration();
			factory.getBeanNamesForType(Unrelated.class);
			factory.getBean("prototype");
		});

		assertThat(indexedFactory.getBeanNamesForType(Extra.class)).contains("prototype");
		assertSameLookups(true);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void lookupForTypeOnlyExposedByEarlyReference(boolean indexBuiltBeforeCreation) {
		forBothFactories(factory -> {
			RootBeanDefinition first = new RootBeanDefinition(CircularFirst.class);
			first.getPropertyValues().add("second", new RuntimeBeanReference("second"));
			factory.registerBeanDefinition("first", first);
			RootBeanDefinition second = new RootBeanDefinition(CircularSecond.class);
			second.getPropertyValues().add("first", new RuntimeBeanReference("first"));
			factory.registerBeanDefinition("second", second);
			factory.addBeanPostProcessor(new EarlyReferenceIntroducingPostProcessor());
			factory.freezeConfiguration();
			if (indexBuiltBeforeCreation) {
				factory.getBeanNamesForType(Unrelated.class);
			}
			factory.preInstantiateSingletons();
		});

		CircularSecond indexedSecond = indexedFactory.getBean("second", CircularSecond.class);
		CircularSecond plainSecond = plainFactory.getBean("second", CircularSecond.class);
		assertThat(indexedSecond.introducedNames).containsExactly("first");
		assertThat(indexedSecond.introducedNames).isEqualTo(plainSecond.introducedNames);
		assertThat(indexedFactory.getBeanNamesForType(Introduced.class)).containsExactly("first");
		assertThat(plainFactory.getBeanNamesForType(Introduced.class)).containsExactly("first");
	}

	@Test
	void lookupChecksOnlyCandidateBeanDefinitions() {
		TypeMatchCountingBeanFactory factory = new TypeMatchCountingBeanFactory();
		registerUnrelatedBeans(factory);
		factory.registerBeanDefinition("service", new RootBeanDefinition(ServiceImpl.class));
		factory.freezeConfiguration();

		assertThat(factory.getBeanNamesForType(Service.class)).containsExactly("service");
		assertThat(factory.typeMatchCount).hasValue(1);
	}

	@Test
	void lookupChecksAllBeanDefinitionsWhenTypeIndexIgnored() {
		SpringProperties.setFlag(DefaultListableBeanFactory.IGNORE_TYPE_INDEX_PROPERTY_NAME);
		TypeMatchCountingBeanFactory factory;
		try {
			factory = new TypeMatchCountingBeanFactory();
		}
		finally {
			SpringProperties.setProperty(DefaultListableBeanFactory.IGNORE_TYPE_INDEX_PROPERTY_NAME, null);
		}
		registerUnrelatedBeans(factory);
		factory.registerBeanDefinition("service", new RootBeanDefinition(ServiceImpl.class));
		factory.freezeConfiguration();

		assertThat(factory.getBeanNamesForType(Service.class)).containsExactly("service");
		assertThat(factory.typeMatchCount).hasValue(101);
	}

	@Test
	void lookupChecksAllBeanDefinitionsWithTempClassLoader() {
		TypeMatchCountingBeanFactory factory = new TypeMatchCountingBeanFactory();
		factory.setTempClassLoader(new OverridingClassLoader(getClass().getClassLoader()));
		registerUnrelatedBeans(factory);
		factory.registerBeanDefinition("service", new RootBeanDefinition(ServiceImpl.class));
		factory.freezeConfiguration();

		assertThat(factory.getBeanNamesForType(Service.class)).containsExactly("service");
		assertThat(factory.typeMatchCount).hasValue(101);
	}


	private void assertSameLookups(boolean includeNonSingletons) {
		for (Class<?> type : LOOKUP_TYPES) {
			assertThat(indexedFactory.getBeanNamesForType(type, includeNonSingletons, true))
					.as(type.getName())
					.containsExactly(plainFactory.getBeanNamesForType(type, includeNonSingletons, true));
		}
	}

	private void forBothFactories(Consumer<DefaultListableBeanFactory> action) {
		action.accept(plainFactory);
		action.accept(indexedFactory);
	}

	private void registerScenario(DefaultListableBeanFactory factory) {
		factory.registerBeanDefinition("service", new RootBeanDefinition(ServiceImpl.class));
		factory.registerBeanDefinition("subService", new RootBeanDefinition(SubService.class));
		RootBeanDefinition prototype = new RootBeanDefinition(ServiceImpl.class);
		prototype.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		factory.registerBeanDefinition("prototype", prototype);
		RootBeanDefinition lazy = new RootBeanDefinition(SubService.class);
		lazy.setLazyInit(true);
		factory.registerBeanDefinition("lazy", lazy);
		RootBeanDefinition abstractDefinition = new RootBeanDefinition(ServiceImpl.class);
		abstractDefinition.setAbstract(true);
		factory.registerBeanDefinition("abstract", abstractDefinition);
		factory.registerBeanDefinition("factory", new RootBeanDefinition(ProductFactoryBean.class));
		RootBeanDefinition interfaceTyped = new RootBeanDefinition(Service.class, ServiceWithExtra::new);
		interfaceTyped.setTargetType(Service.class);
		factory.registerBeanDefinition("interfaceTyped", interfaceTyped);
		factory.registerBeanDefinition("unrelated", new RootBeanDefinition(Unrelated.class));
		RootBeanDefinition primitive = new RootBeanDefinition();
		primitive.setTargetType(int.class);
		primitive.setInstanceSupplier(() -> 42);
		factory.registerBeanDefinition("primitive", primitive);
		RootBeanDefinition array = new RootBeanDefinition();
		array.setTargetType(String[].class);
		array.setInstanceSupplier(() -> new String[] {"value"});
		factory.registerBeanDefinition("array", array);
		factory.registerAlias("service", "serviceAlias");
		factory.registerSingleton("manualSingleton", new Unrelated());
	}

	private static void registerUnrelatedBeans(DefaultListableBeanFactory factory) {
		for (int i = 0; i < 100; i++) {
			factory.registerBeanDefinition("unrelated" + i, new RootBeanDefinition(Unrelated.class));
		}
	}

	private static DefaultListableBeanFactory createFactoryWithoutTypeIndex() {
		SpringProperties.setFlag(DefaultListableBeanFactory.IGNORE_TYPE_INDEX_PROPERTY_NAME);
		try {
			return new DefaultListableBeanFactory();
		}
		finally {
			SpringProperties.setProperty(DefaultListableBeanFactory.IGNORE_TYPE_INDEX_PROPERTY_NAME, null);
		}
	}


	interface Service {
	}

	interface Extra {
	}

	interface Introduced {
	}

	static class ServiceImpl implements Service {
	}

	static class SubService extends ServiceImpl {
	}

	static class ServiceWithExtra implements Service, Extra {
	}

	static class Unrelated {
	}

	static class Product {
	}

	static class ProductFactoryBean implements FactoryBean<Product> {

		@Override
		public Product getObject() {
			return new Product();
		}

		@Override
		public Class<?> getObjectType() {
			return Product.class;
		}
	}

	static class CircularFirst {

		@SuppressWarnings("unused")
		private Object second;

		public void setSecond(Object second) {
			this.second = second;
		}
	}

	static class CircularSecond implements BeanFactoryAware, InitializingBean {

		@SuppressWarnings("unused")
		private Object first;

		private @Nullable BeanFactory beanFactory;

		String @Nullable [] introducedNames;

		public void setFirst(Object first) {
			this.first = first;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public void afterPropertiesSet() {
			// The "first" bean is still in creation, only exposed as an early reference.
			this.introducedNames = ((DefaultListableBeanFactory) this.beanFactory).getBeanNamesForType(Introduced.class);
		}
	}

	/**
	 * Exposes an early reference to the "first" bean that implements an interface
	 * that the bean class itself does not implement, similar to an introduction
	 * applied by an auto-proxy creator.
	 */
	static class EarlyReferenceIntroducingPostProcessor implements SmartInstantiationAwareBeanPostProcessor {

		@Override
		public Object getEarlyBeanReference(Object bean, String beanName) {
			return ("first".equals(beanName) ? new IntroducingWrapper(bean) : bean);
		}
	}

	record IntroducingWrapper(Object target) implements Introduced {
	}

	/**
	 * Predicts a different type for the "prototype" bean once an instance has been
	 * created, similar to an auto-proxy creator exposing the proxy type.
	 */
	static class PrototypeTypePredictingPostProcessor implements SmartInstantiationAwareBeanPostProcessor {

		private boolean prototypeCreated;

		@Override
		public @Nullable Class<?> predictBeanType(Class<?> beanClass, String beanName) {
			return ("prototype".equals(beanName) && this.prototypeCreated ? ServiceWithExtra.class : null);
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if ("prototype".equals(beanName)) {
				this.prototypeCreated = true;
			}
			return bean;
		}
	}

	@SuppressWarnings("serial")
	static class TypeMatchCountingBeanFactory extends DefaultListableBeanFactory {

		final AtomicInteger typeMatchCount = new AtomicInteger();

		@Override
		protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit) {
			this.typeMatchCount.incrementAndGet();
			return super.isTypeMatch(name, typeToMatch, allowFactoryBeanInit);
		}
	}

}

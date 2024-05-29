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

package org.springframework.beans.factory;

import java.io.Closeable;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.annotation.Priority;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.AutowiredPropertyMarker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ConstructorDependenciesBean;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.testfixture.beans.DependenciesBean;
import org.springframework.beans.testfixture.beans.DerivedTestBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.NestedTestBean;
import org.springframework.beans.testfixture.beans.SideEffectBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.factory.DummyFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests properties population and autowire behavior.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Sam Brannen
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class DefaultListableBeanFactoryTests {

	private final DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();


	@Test
	void unreferencedSingletonWasInstantiated() {
		KnowsIfInstantiated.clearInstantiationRecord();
		Properties p = new Properties();
		p.setProperty("x1.(class)", KnowsIfInstantiated.class.getName());
		assertThat(KnowsIfInstantiated.wasInstantiated()).as("singleton not instantiated").isFalse();
		registerBeanDefinitions(p);
		lbf.preInstantiateSingletons();
		assertThat(KnowsIfInstantiated.wasInstantiated()).as("singleton was instantiated").isTrue();
	}

	@Test
	void lazyInitialization() {
		KnowsIfInstantiated.clearInstantiationRecord();
		Properties p = new Properties();
		p.setProperty("x1.(class)", KnowsIfInstantiated.class.getName());
		p.setProperty("x1.(lazy-init)", "true");
		assertThat(KnowsIfInstantiated.wasInstantiated()).as("singleton not instantiated").isFalse();
		registerBeanDefinitions(p);
		assertThat(KnowsIfInstantiated.wasInstantiated()).as("singleton not instantiated").isFalse();
		lbf.preInstantiateSingletons();

		assertThat(KnowsIfInstantiated.wasInstantiated()).as("singleton not instantiated").isFalse();
		lbf.getBean("x1");
		assertThat(KnowsIfInstantiated.wasInstantiated()).as("singleton was instantiated").isTrue();
	}

	@Test
	void factoryBeanDidNotCreatePrototype() {
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.singleton", "false");
		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isFalse();
		registerBeanDefinitions(p);
		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isFalse();
		assertThat(lbf.getType("x1")).isEqualTo(TestBean.class);
		lbf.preInstantiateSingletons();

		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isFalse();
		lbf.getBean("x1");
		assertThat(lbf.getType("x1")).isEqualTo(TestBean.class);
		assertThat(lbf.containsBean("x1")).isTrue();
		assertThat(lbf.containsBean("&x1")).isTrue();
		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype was instantiated").isTrue();
	}

	@Test
	void prototypeFactoryBeanIgnoredByNonEagerTypeMatching() {
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.(singleton)", "false");
		p.setProperty("x1.singleton", "false");
		registerBeanDefinitions(p);

		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isFalse();
		assertBeanNamesForType(TestBean.class, false, false);
		assertThat(lbf.getBeanNamesForAnnotation(SuppressWarnings.class)).isEmpty();

		assertThat(lbf.containsSingleton("x1")).isFalse();
		assertThat(lbf.containsBean("x1")).isTrue();
		assertThat(lbf.containsBean("&x1")).isTrue();
		assertThat(lbf.isSingleton("x1")).isFalse();
		assertThat(lbf.isSingleton("&x1")).isFalse();
		assertThat(lbf.isPrototype("x1")).isTrue();
		assertThat(lbf.isPrototype("&x1")).isTrue();
		assertThat(lbf.isTypeMatch("x1", TestBean.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", TestBean.class)).isFalse();
		assertThat(lbf.isTypeMatch("&x1", DummyFactory.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", ResolvableType.forClass(DummyFactory.class))).isTrue();
		assertThat(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, Object.class))).isTrue();
		assertThat(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, String.class))).isFalse();
		assertThat(lbf.getType("x1")).isEqualTo(TestBean.class);
		assertThat(lbf.getType("&x1")).isEqualTo(DummyFactory.class);
		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isFalse();
	}

	@Test
	void singletonFactoryBeanIgnoredByNonEagerTypeMatching() {
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.(singleton)", "false");
		p.setProperty("x1.singleton", "true");
		registerBeanDefinitions(p);

		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isFalse();
		assertBeanNamesForType(TestBean.class, false, false);
		assertThat(lbf.getBeanNamesForAnnotation(SuppressWarnings.class)).isEmpty();

		assertThat(lbf.containsSingleton("x1")).isFalse();
		assertThat(lbf.containsBean("x1")).isTrue();
		assertThat(lbf.containsBean("&x1")).isTrue();
		assertThat(lbf.isSingleton("x1")).isFalse();
		assertThat(lbf.isSingleton("&x1")).isFalse();
		assertThat(lbf.isPrototype("x1")).isTrue();
		assertThat(lbf.isPrototype("&x1")).isTrue();
		assertThat(lbf.isTypeMatch("x1", TestBean.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", TestBean.class)).isFalse();
		assertThat(lbf.isTypeMatch("&x1", DummyFactory.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", ResolvableType.forClass(DummyFactory.class))).isTrue();
		assertThat(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, Object.class))).isTrue();
		assertThat(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, String.class))).isFalse();
		assertThat(lbf.getType("x1")).isEqualTo(TestBean.class);
		assertThat(lbf.getType("&x1")).isEqualTo(DummyFactory.class);
		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isFalse();
	}

	@Test
	void nonInitializedFactoryBeanIgnoredByNonEagerTypeMatching() {
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.singleton", "false");
		registerBeanDefinitions(p);

		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isFalse();
		assertBeanNamesForType(TestBean.class, false, false);
		assertThat(lbf.getBeanNamesForAnnotation(SuppressWarnings.class)).isEmpty();

		assertThat(lbf.containsSingleton("x1")).isFalse();
		assertThat(lbf.containsBean("x1")).isTrue();
		assertThat(lbf.containsBean("&x1")).isTrue();
		assertThat(lbf.isSingleton("x1")).isFalse();
		assertThat(lbf.isSingleton("&x1")).isTrue();
		assertThat(lbf.isPrototype("x1")).isTrue();
		assertThat(lbf.isPrototype("&x1")).isFalse();
		assertThat(lbf.isTypeMatch("x1", TestBean.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", TestBean.class)).isFalse();
		assertThat(lbf.isTypeMatch("&x1", DummyFactory.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", ResolvableType.forClass(DummyFactory.class))).isTrue();
		assertThat(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, Object.class))).isTrue();
		assertThat(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, String.class))).isFalse();
		assertThat(lbf.getType("x1")).isEqualTo(TestBean.class);
		assertThat(lbf.getType("&x1")).isEqualTo(DummyFactory.class);
		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isFalse();
	}

	@Test
	void initializedFactoryBeanFoundByNonEagerTypeMatching() {
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.singleton", "false");
		registerBeanDefinitions(p);
		lbf.preInstantiateSingletons();

		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isFalse();
		assertBeanNamesForType(TestBean.class, true, false, "x1");
		assertThat(lbf.containsSingleton("x1")).isTrue();
		assertThat(lbf.containsBean("x1")).isTrue();
		assertThat(lbf.containsBean("&x1")).isTrue();
		assertThat(lbf.containsLocalBean("x1")).isTrue();
		assertThat(lbf.containsLocalBean("&x1")).isTrue();
		assertThat(lbf.isSingleton("x1")).isFalse();
		assertThat(lbf.isSingleton("&x1")).isTrue();
		assertThat(lbf.isPrototype("x1")).isTrue();
		assertThat(lbf.isPrototype("&x1")).isFalse();
		assertThat(lbf.isTypeMatch("x1", TestBean.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", TestBean.class)).isFalse();
		assertThat(lbf.isTypeMatch("&x1", DummyFactory.class)).isTrue();
		assertThat(lbf.isTypeMatch("x1", Object.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", Object.class)).isTrue();
		assertThat(lbf.getType("x1")).isEqualTo(TestBean.class);
		assertThat(lbf.getType("&x1")).isEqualTo(DummyFactory.class);
		assertThat(DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isFalse();

		lbf.registerAlias("x1", "x2");
		assertThat(lbf.containsBean("x2")).isTrue();
		assertThat(lbf.containsBean("&x2")).isTrue();
		assertThat(lbf.containsLocalBean("x2")).isTrue();
		assertThat(lbf.containsLocalBean("&x2")).isTrue();
		assertThat(lbf.isSingleton("x2")).isFalse();
		assertThat(lbf.isSingleton("&x2")).isTrue();
		assertThat(lbf.isPrototype("x2")).isTrue();
		assertThat(lbf.isPrototype("&x2")).isFalse();
		assertThat(lbf.isTypeMatch("x2", TestBean.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x2", TestBean.class)).isFalse();
		assertThat(lbf.isTypeMatch("&x2", DummyFactory.class)).isTrue();
		assertThat(lbf.isTypeMatch("x2", Object.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x2", Object.class)).isTrue();
		assertThat(lbf.getType("x2")).isEqualTo(TestBean.class);
		assertThat(lbf.getType("&x2")).isEqualTo(DummyFactory.class);
		assertThat(lbf.getAliases("x1")).containsExactly("x2");
		assertThat(lbf.getAliases("&x1")).containsExactly("&x2");
		assertThat(lbf.getAliases("x2")).containsExactly("x1");
		assertThat(lbf.getAliases("&x2")).containsExactly("&x1");
	}

	@Test
	void staticFactoryMethodFoundByNonEagerTypeMatching() {
		RootBeanDefinition rbd = new RootBeanDefinition(TestBeanFactory.class);
		rbd.setFactoryMethodName("createTestBean");
		lbf.registerBeanDefinition("x1", rbd);

		TestBeanFactory.initialized = false;
		assertBeanNamesForType(TestBean.class, true, false, "x1");
		assertThat(lbf.containsSingleton("x1")).isFalse();
		assertThat(lbf.containsBean("x1")).isTrue();
		assertThat(lbf.containsBean("&x1")).isFalse();
		assertThat(lbf.isSingleton("x1")).isTrue();
		assertThat(lbf.isSingleton("&x1")).isFalse();
		assertThat(lbf.isPrototype("x1")).isFalse();
		assertThat(lbf.isPrototype("&x1")).isFalse();
		assertThat(lbf.isTypeMatch("x1", TestBean.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", TestBean.class)).isFalse();
		assertThat(lbf.getType("x1")).isEqualTo(TestBean.class);
		assertThat(lbf.getType("&x1")).isNull();
		assertThat(TestBeanFactory.initialized).isFalse();
	}

	@Test
	void staticPrototypeFactoryMethodFoundByNonEagerTypeMatching() {
		RootBeanDefinition rbd = new RootBeanDefinition(TestBeanFactory.class);
		rbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		rbd.setFactoryMethodName("createTestBean");
		lbf.registerBeanDefinition("x1", rbd);

		TestBeanFactory.initialized = false;
		assertBeanNamesForType(TestBean.class, true, false, "x1");
		assertThat(lbf.containsSingleton("x1")).isFalse();
		assertThat(lbf.containsBean("x1")).isTrue();
		assertThat(lbf.containsBean("&x1")).isFalse();
		assertThat(lbf.isSingleton("x1")).isFalse();
		assertThat(lbf.isSingleton("&x1")).isFalse();
		assertThat(lbf.isPrototype("x1")).isTrue();
		assertThat(lbf.isPrototype("&x1")).isFalse();
		assertThat(lbf.isTypeMatch("x1", TestBean.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", TestBean.class)).isFalse();
		assertThat(lbf.getType("x1")).isEqualTo(TestBean.class);
		assertThat(lbf.getType("&x1")).isNull();
		assertThat(TestBeanFactory.initialized).isFalse();
	}

	@Test
	void nonStaticFactoryMethodFoundByNonEagerTypeMatching() {
		RootBeanDefinition factoryBd = new RootBeanDefinition(TestBeanFactory.class);
		lbf.registerBeanDefinition("factory", factoryBd);
		RootBeanDefinition rbd = new RootBeanDefinition(TestBeanFactory.class);
		rbd.setFactoryBeanName("factory");
		rbd.setFactoryMethodName("createTestBeanNonStatic");
		lbf.registerBeanDefinition("x1", rbd);

		TestBeanFactory.initialized = false;
		assertBeanNamesForType(TestBean.class, true, false, "x1");
		assertThat(lbf.containsSingleton("x1")).isFalse();
		assertThat(lbf.containsBean("x1")).isTrue();
		assertThat(lbf.containsBean("&x1")).isFalse();
		assertThat(lbf.isSingleton("x1")).isTrue();
		assertThat(lbf.isSingleton("&x1")).isFalse();
		assertThat(lbf.isPrototype("x1")).isFalse();
		assertThat(lbf.isPrototype("&x1")).isFalse();
		assertThat(lbf.isTypeMatch("x1", TestBean.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", TestBean.class)).isFalse();
		assertThat(lbf.getType("x1")).isEqualTo(TestBean.class);
		assertThat(lbf.getType("&x1")).isNull();
		assertThat(TestBeanFactory.initialized).isFalse();
	}

	@Test
	void nonStaticPrototypeFactoryMethodFoundByNonEagerTypeMatching() {
		RootBeanDefinition factoryBd = new RootBeanDefinition(TestBeanFactory.class);
		lbf.registerBeanDefinition("factory", factoryBd);
		RootBeanDefinition rbd = new RootBeanDefinition();
		rbd.setFactoryBeanName("factory");
		rbd.setFactoryMethodName("createTestBeanNonStatic");
		rbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("x1", rbd);

		TestBeanFactory.initialized = false;
		assertBeanNamesForType(TestBean.class, true, false, "x1");
		assertThat(lbf.containsSingleton("x1")).isFalse();
		assertThat(lbf.containsBean("x1")).isTrue();
		assertThat(lbf.containsBean("&x1")).isFalse();
		assertThat(lbf.containsLocalBean("x1")).isTrue();
		assertThat(lbf.containsLocalBean("&x1")).isFalse();
		assertThat(lbf.isSingleton("x1")).isFalse();
		assertThat(lbf.isSingleton("&x1")).isFalse();
		assertThat(lbf.isPrototype("x1")).isTrue();
		assertThat(lbf.isPrototype("&x1")).isFalse();
		assertThat(lbf.isTypeMatch("x1", TestBean.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", TestBean.class)).isFalse();
		assertThat(lbf.isTypeMatch("x1", Object.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x1", Object.class)).isFalse();
		assertThat(lbf.getType("x1")).isEqualTo(TestBean.class);
		assertThat(lbf.getType("&x1")).isNull();
		assertThat(TestBeanFactory.initialized).isFalse();

		lbf.registerAlias("x1", "x2");
		assertThat(lbf.containsBean("x2")).isTrue();
		assertThat(lbf.containsBean("&x2")).isFalse();
		assertThat(lbf.containsLocalBean("x2")).isTrue();
		assertThat(lbf.containsLocalBean("&x2")).isFalse();
		assertThat(lbf.isSingleton("x2")).isFalse();
		assertThat(lbf.isSingleton("&x2")).isFalse();
		assertThat(lbf.isPrototype("x2")).isTrue();
		assertThat(lbf.isPrototype("&x2")).isFalse();
		assertThat(lbf.isTypeMatch("x2", TestBean.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x2", TestBean.class)).isFalse();
		assertThat(lbf.isTypeMatch("x2", Object.class)).isTrue();
		assertThat(lbf.isTypeMatch("&x2", Object.class)).isFalse();
		assertThat(lbf.getType("x2")).isEqualTo(TestBean.class);
		assertThat(lbf.getType("&x2")).isNull();
		assertThat(lbf.getAliases("x1")).containsExactly("x2");
		assertThat(lbf.getAliases("&x1")).containsExactly("&x2");
		assertThat(lbf.getAliases("x2")).containsExactly("x1");
		assertThat(lbf.getAliases("&x2")).containsExactly("&x1");
	}

	@Test
	void empty() {
		assertThat(lbf.getBeanDefinitionNames()).as("No beans defined --> array != null").isNotNull();
		assertThat(lbf.getBeanDefinitionNames()).as("No beans defined after no arg constructor").isEmpty();
		assertThat(lbf.getBeanDefinitionCount()).as("No beans defined after no arg constructor").isEqualTo(0);
	}

	@Test
	void emptyPropertiesPopulation() {
		Properties p = new Properties();
		registerBeanDefinitions(p);

		assertThat(lbf.getBeanDefinitionCount()).as("No beans defined after ignorable invalid").isEqualTo(0);
	}

	@Test
	void harmlessIgnorableRubbish() {
		Properties p = new Properties();
		p.setProperty("foo", "bar");
		p.setProperty("qwert", "er");
		registerBeanDefinitions(p, "test");

		assertThat(lbf.getBeanDefinitionCount()).as("No beans defined after harmless ignorable rubbish").isEqualTo(0);
	}

	@Test
	void propertiesPopulationWithNullPrefix() {
		Properties p = new Properties();
		p.setProperty("test.(class)", TestBean.class.getName());
		p.setProperty("test.name", "Tony");
		p.setProperty("test.age", "48");
		int count = registerBeanDefinitions(p);

		assertThat(count).as("1 beans registered, not " + count).isEqualTo(1);
		testPropertiesPopulation(lbf);
	}

	@Test
	void propertiesPopulationWithPrefix() {
		String PREFIX = "beans.";
		Properties p = new Properties();
		p.setProperty(PREFIX + "test.(class)", TestBean.class.getName());
		p.setProperty(PREFIX + "test.name", "Tony");
		p.setProperty(PREFIX + "test.age", "0x30");
		int count = registerBeanDefinitions(p, PREFIX);

		assertThat(count).as("1 beans registered, not " + count).isEqualTo(1);
		testPropertiesPopulation(lbf);
	}

	private void testPropertiesPopulation(ListableBeanFactory lbf) {
		assertThat(lbf.getBeanDefinitionCount() == 1).as("1 beans defined").isTrue();
		String[] names = lbf.getBeanDefinitionNames();
		assertThat(names != lbf.getBeanDefinitionNames()).isTrue();
		assertThat(names.length == 1).as("Array length == 1").isTrue();
		assertThat(names[0].equals("test")).as("0th element == test").isTrue();

		TestBean tb = (TestBean) lbf.getBean("test");
		assertThat(tb != null).as("Test is non null").isTrue();
		assertThat("Tony".equals(tb.getName())).as("Test bean name is Tony").isTrue();
		assertThat(tb.getAge() == 48).as("Test bean age is 48").isTrue();
	}

	@Test
	void simpleReference() {
		String PREFIX = "beans.";
		Properties p = new Properties();

		p.setProperty(PREFIX + "rod.(class)", TestBean.class.getName());
		p.setProperty(PREFIX + "rod.name", "Rod");

		p.setProperty(PREFIX + "kerry.(class)", TestBean.class.getName());
		p.setProperty(PREFIX + "kerry.name", "Kerry");
		p.setProperty(PREFIX + "kerry.age", "35");
		p.setProperty(PREFIX + "kerry.spouse(ref)", "rod");

		int count = registerBeanDefinitions(p, PREFIX);
		assertThat(count).as("2 beans registered, not " + count).isEqualTo(2);

		TestBean kerry = lbf.getBean("kerry", TestBean.class);
		assertThat(kerry.getName()).as("Kerry name is Kerry").isEqualTo("Kerry");
		ITestBean spouse = kerry.getSpouse();
		assertThat(spouse).as("Kerry spouse is non null").isNotNull();
		assertThat(spouse.getName()).as("Kerry spouse name is Rod").isEqualTo("Rod");
	}

	@Test
	void propertiesWithDotsInKey() {
		Properties p = new Properties();

		p.setProperty("tb.(class)", TestBean.class.getName());
		p.setProperty("tb.someMap[my.key]", "my.value");

		int count = registerBeanDefinitions(p);
		assertThat(count).as("1 beans registered, not " + count).isEqualTo(1);
		assertThat(lbf.getBeanDefinitionCount()).isEqualTo(1);

		TestBean tb = lbf.getBean("tb", TestBean.class);
		assertThat(tb.getSomeMap().get("my.key")).isEqualTo("my.value");
	}

	@Test
	void unresolvedReference() {
		String PREFIX = "beans.";
		Properties p = new Properties();

		p.setProperty(PREFIX + "kerry.(class)", TestBean.class.getName());
		p.setProperty(PREFIX + "kerry.name", "Kerry");
		p.setProperty(PREFIX + "kerry.age", "35");
		p.setProperty(PREFIX + "kerry.spouse(ref)", "rod");

		registerBeanDefinitions(p, PREFIX);

		assertThatExceptionOfType(BeansException.class).as("unresolved reference").isThrownBy(() ->
				lbf.getBean("kerry"));
	}

	@Test
	void selfReference() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("spouse", new RuntimeBeanReference("self"));
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("self", bd);

		TestBean self = (TestBean) lbf.getBean("self");
		assertThat(self.getSpouse()).isEqualTo(self);
	}

	@Test
	void referenceByName() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("doctor", new RuntimeBeanReference("doc"));
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("self", bd);
		lbf.registerBeanDefinition("doc", new RootBeanDefinition(NestedTestBean.class));

		TestBean self = (TestBean) lbf.getBean("self");
		assertThat(self.getDoctor()).isEqualTo(lbf.getBean("doc"));
	}

	@Test
	void referenceByType() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("doctor", new RuntimeBeanReference(NestedTestBean.class));
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("self", bd);
		lbf.registerBeanDefinition("doc", new RootBeanDefinition(NestedTestBean.class));

		TestBean self = (TestBean) lbf.getBean("self");
		assertThat(self.getDoctor()).isEqualTo(lbf.getBean("doc"));
	}

	@Test
	void referenceByAutowire() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("doctor", AutowiredPropertyMarker.INSTANCE);
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("self", bd);
		lbf.registerBeanDefinition("doc", new RootBeanDefinition(NestedTestBean.class));

		TestBean self = (TestBean) lbf.getBean("self");
		assertThat(self.getDoctor()).isEqualTo(lbf.getBean("doc"));
	}

	@Test
	void arrayReferenceByName() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("stringArray", new RuntimeBeanReference("string"));
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("self", bd);
		lbf.registerSingleton("string", "A");

		TestBean self = (TestBean) lbf.getBean("self");
		assertThat(self.getStringArray()).containsExactly("A");
	}

	@Test
	void arrayReferenceByType() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("stringArray", new RuntimeBeanReference(String.class));
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("self", bd);
		lbf.registerSingleton("string", "A");

		TestBean self = (TestBean) lbf.getBean("self");
		assertThat(self.getStringArray()).containsExactly("A");
	}

	@Test
	void arrayReferenceByAutowire() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("stringArray", AutowiredPropertyMarker.INSTANCE);
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("self", bd);
		lbf.registerSingleton("string1", "A");
		lbf.registerSingleton("string2", "B");

		TestBean self = (TestBean) lbf.getBean("self");
		assertThat(self.getStringArray()).containsOnly("A","B");
	}

	@Test
	void possibleMatches() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("ag", "foobar");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("tb", bd);

		assertThatExceptionOfType(BeanCreationException.class).as("invalid property")
				.isThrownBy(() -> lbf.getBean("tb"))
				.withCauseInstanceOf(NotWritablePropertyException.class)
				.satisfies(ex -> {
					NotWritablePropertyException cause = (NotWritablePropertyException) ex.getCause();
					assertThat(cause.getPossibleMatches()).containsExactly("age");
				});
	}

	@Test
	void prototype() {
		Properties p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.age", "35");
		registerBeanDefinitions(p);
		TestBean kerry1 = (TestBean) lbf.getBean("kerry");
		TestBean kerry2 = (TestBean) lbf.getBean("kerry");
		assertThat(kerry1).as("Non null").isNotNull();
		assertThat(kerry1).as("Singletons equal").isSameAs(kerry2);

		p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.(scope)", BeanDefinition.SCOPE_PROTOTYPE);
		p.setProperty("kerry.age", "35");
		registerBeanDefinitions(p);
		kerry1 = (TestBean) lbf.getBean("kerry");
		kerry2 = (TestBean) lbf.getBean("kerry");
		assertThat(kerry1).as("Non null").isNotNull();
		assertThat(kerry1).as("Prototypes NOT equal").isNotSameAs(kerry2);

		p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.(scope)", "singleton");
		p.setProperty("kerry.age", "35");
		registerBeanDefinitions(p);
		kerry1 = (TestBean) lbf.getBean("kerry");
		kerry2 = (TestBean) lbf.getBean("kerry");
		assertThat(kerry1).as("Non null").isNotNull();
		assertThat(kerry1).as("Specified singletons equal").isSameAs(kerry2);
	}

	@Test
	void prototypeCircleLeadsToException() {
		Properties p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.(singleton)", "false");
		p.setProperty("kerry.age", "35");
		p.setProperty("kerry.spouse", "*rod");
		p.setProperty("rod.(class)", TestBean.class.getName());
		p.setProperty("rod.(singleton)", "false");
		p.setProperty("rod.age", "34");
		p.setProperty("rod.spouse", "*kerry");
		registerBeanDefinitions(p);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> lbf.getBean("kerry"))
				.satisfies(ex -> assertThat(ex.contains(BeanCurrentlyInCreationException.class)).isTrue());
	}

	@Test
	void prototypeExtendsPrototype() {
		Properties p = new Properties();
		p.setProperty("wife.(class)", TestBean.class.getName());
		p.setProperty("wife.name", "kerry");

		p.setProperty("kerry.(parent)", "wife");
		p.setProperty("kerry.age", "35");
		registerBeanDefinitions(p);
		TestBean kerry1 = (TestBean) lbf.getBean("kerry");
		TestBean kerry2 = (TestBean) lbf.getBean("kerry");
		assertThat(kerry1.getName()).isEqualTo("kerry");
		assertThat(kerry1).as("Non null").isNotNull();
		assertThat(kerry1).as("Singletons equal").isSameAs(kerry2);

		p = new Properties();
		p.setProperty("wife.(class)", TestBean.class.getName());
		p.setProperty("wife.name", "kerry");
		p.setProperty("wife.(singleton)", "false");
		p.setProperty("kerry.(parent)", "wife");
		p.setProperty("kerry.(singleton)", "false");
		p.setProperty("kerry.age", "35");
		registerBeanDefinitions(p);
		assertThat(lbf.isSingleton("kerry")).isFalse();
		kerry1 = (TestBean) lbf.getBean("kerry");
		kerry2 = (TestBean) lbf.getBean("kerry");
		assertThat(kerry1).as("Non null").isNotNull();
		assertThat(kerry1).as("Prototypes NOT equal").isNotSameAs(kerry2);

		p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.(singleton)", "true");
		p.setProperty("kerry.age", "35");
		registerBeanDefinitions(p);
		kerry1 = (TestBean) lbf.getBean("kerry");
		kerry2 = (TestBean) lbf.getBean("kerry");
		assertThat(kerry1).as("Non null").isNotNull();
		assertThat(kerry1).as("Specified singletons equal").isSameAs(kerry2);
	}

	@Test
	void canReferenceParentBeanFromChildViaAlias() {
		final String EXPECTED_NAME = "Juergen";
		final int EXPECTED_AGE = 41;

		RootBeanDefinition parentDefinition = new RootBeanDefinition(TestBean.class);
		parentDefinition.setAbstract(true);
		parentDefinition.getPropertyValues().add("name", EXPECTED_NAME);
		parentDefinition.getPropertyValues().add("age", EXPECTED_AGE);

		ChildBeanDefinition childDefinition = new ChildBeanDefinition("alias");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("parent", parentDefinition);
		factory.registerBeanDefinition("child", childDefinition);
		factory.registerAlias("parent", "alias");

		TestBean child = factory.getBean("child", TestBean.class);
		assertThat(child.getName()).isEqualTo(EXPECTED_NAME);
		assertThat(child.getAge()).isEqualTo(EXPECTED_AGE);
		BeanDefinition mergedBeanDefinition1 = factory.getMergedBeanDefinition("child");
		BeanDefinition mergedBeanDefinition2 = factory.getMergedBeanDefinition("child");

		assertThat(mergedBeanDefinition1).as("Use cached merged bean definition").isSameAs(mergedBeanDefinition2);
	}

	@Test
	void hintAtPossibleDuplicateArgumentsInParentAndChildWhenMixingIndexAndNamed() {
		final String EXPECTED_NAME = "Juergen";
		final int EXPECTED_AGE = 41;

		RootBeanDefinition parentDefinition = new RootBeanDefinition(TestBean.class);
		parentDefinition.setAbstract(true);
		parentDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, EXPECTED_NAME);

		ChildBeanDefinition childDefinition = new ChildBeanDefinition("parent");
		childDefinition.getConstructorArgumentValues().addGenericArgumentValue(new ConstructorArgumentValues.ValueHolder(EXPECTED_NAME, null, "name"));
		childDefinition.getConstructorArgumentValues().addGenericArgumentValue(new ConstructorArgumentValues.ValueHolder(EXPECTED_AGE, null, "age"));

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("parent", parentDefinition);
		factory.registerBeanDefinition("child", childDefinition);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> factory.getBean("child", TestBean.class))
				.withMessage("Error creating bean with name 'child': Could not resolve matching constructor on bean class " +
						"[org.springframework.beans.testfixture.beans.TestBean] (hint: specify index/type/name arguments " +
						"for simple parameters to avoid type ambiguities. " +
						"You should also check the consistency of arguments when mixing indexed and named arguments, " +
						"especially in case of bean definition inheritance)");
	}

	@Test
	void getTypeWorksAfterParentChildMerging() {
		RootBeanDefinition parentDefinition = new RootBeanDefinition(TestBean.class);
		ChildBeanDefinition childDefinition = new ChildBeanDefinition("parent", DerivedTestBean.class, null, null);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("parent", parentDefinition);
		factory.registerBeanDefinition("child", childDefinition);
		factory.freezeConfiguration();

		assertThat(factory.getType("parent")).isEqualTo(TestBean.class);
		assertThat(factory.getType("child")).isEqualTo(DerivedTestBean.class);
	}

	@Test
	void mergedBeanDefinitionChangesRetainedAfterFreezeConfiguration() {
		RootBeanDefinition parentDefinition = new RootBeanDefinition(Object.class);
		ChildBeanDefinition childDefinition = new ChildBeanDefinition("parent");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("parent", parentDefinition);
		factory.registerBeanDefinition("child", childDefinition);

		assertThat(factory.getType("parent")).isEqualTo(Object.class);
		assertThat(factory.getType("child")).isEqualTo(Object.class);
		((RootBeanDefinition) factory.getBeanDefinition("parent")).setBeanClass(TestBean.class);

		factory.freezeConfiguration();

		assertThat(factory.getType("parent")).isEqualTo(TestBean.class);
		assertThat(factory.getType("child")).isEqualTo(TestBean.class);
		((RootBeanDefinition) factory.getMergedBeanDefinition("child")).setBeanClass(DerivedTestBean.class);

		assertThat(factory.getBean("parent")).isInstanceOf(TestBean.class);
		assertThat(factory.getBean("child")).isInstanceOf(DerivedTestBean.class);
	}

	@Test
	void aliasCircle() {
		lbf.setAllowBeanDefinitionOverriding(true);
		lbf.registerAlias("test", "test2");
		lbf.registerAlias("test2", "test3");

		assertThatIllegalStateException().isThrownBy(() ->
				lbf.registerAlias("test3", "test2"));

		assertThatIllegalStateException().isThrownBy(() ->
				lbf.registerAlias("test3", "test"));

		lbf.registerAlias("test", "test3");
	}

	@Test
	void aliasChaining() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("test", "testAlias");
		lbf.registerAlias("testAlias", "testAlias2");
		lbf.registerAlias("testAlias2", "testAlias3");

		Object bean = lbf.getBean("test");
		assertThat(lbf.getBean("testAlias")).isSameAs(bean);
		assertThat(lbf.getBean("testAlias2")).isSameAs(bean);
		assertThat(lbf.getBean("testAlias3")).isSameAs(bean);
	}

	@Test
	void beanDefinitionOverriding() {
		lbf.setAllowBeanDefinitionOverriding(true);
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("otherTest", "test2");
		lbf.registerAlias("test", "test2");
		lbf.registerAlias("test", "testX");
		lbf.registerBeanDefinition("testX", new RootBeanDefinition(TestBean.class));

		assertThat(lbf.getBean("test")).isInstanceOf(NestedTestBean.class);
		assertThat(lbf.getBean("test2")).isInstanceOf(NestedTestBean.class);
		assertThat(lbf.getBean("testX")).isInstanceOf(TestBean.class);
	}

	@Test
	void beanDefinitionOverridingNotAllowed() {
		lbf.setAllowBeanDefinitionOverriding(false);
		BeanDefinition oldDef = new RootBeanDefinition(TestBean.class);
		BeanDefinition newDef = new RootBeanDefinition(NestedTestBean.class);
		lbf.registerBeanDefinition("test", oldDef);
		lbf.registerAlias("test", "testX");

		assertThatExceptionOfType(BeanDefinitionOverrideException.class)
				.isThrownBy(() -> lbf.registerBeanDefinition("test", newDef))
				.satisfies(ex -> {
					assertThat(ex.getBeanName()).isEqualTo("test");
					assertThat(ex.getBeanDefinition()).isEqualTo(newDef);
					assertThat(ex.getExistingDefinition()).isEqualTo(oldDef);
				});

		assertThatExceptionOfType(BeanDefinitionOverrideException.class)
				.isThrownBy(() -> lbf.registerBeanDefinition("testX", newDef))
				.satisfies(ex -> {
					assertThat(ex.getBeanName()).isEqualTo("testX");
					assertThat(ex.getBeanDefinition()).isEqualTo(newDef);
					assertThat(ex.getExistingDefinition()).isEqualTo(oldDef);
				});
	}

	@Test
	void beanDefinitionOverridingWithAlias() {
		lbf.setAllowBeanDefinitionOverriding(true);
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.registerAlias("test", "testAlias");
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("test", "testAlias");

		assertThat(lbf.getBean("test")).isInstanceOf(NestedTestBean.class);
		assertThat(lbf.getBean("testAlias")).isInstanceOf(NestedTestBean.class);
	}

	@Test
	void beanDefinitionOverridingWithConstructorArgumentMismatch() {
		lbf.setAllowBeanDefinitionOverriding(true);
		RootBeanDefinition bd1 = new RootBeanDefinition(NestedTestBean.class);
		bd1.getConstructorArgumentValues().addIndexedArgumentValue(1, "value1");
		lbf.registerBeanDefinition("test", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(NestedTestBean.class);
		bd2.getConstructorArgumentValues().addIndexedArgumentValue(0, "value0");
		lbf.registerBeanDefinition("test", bd2);

		assertThat(lbf.getBean("test")).isInstanceOf(NestedTestBean.class);
		assertThat(lbf.getBean("test", NestedTestBean.class).getCompany()).isEqualTo("value0");
	}

	@Test
	void beanDefinitionRemoval() {
		lbf.setAllowBeanDefinitionOverriding(false);
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.registerAlias("test", "test2");
		lbf.preInstantiateSingletons();
		assertThat(lbf.getBean("test")).isInstanceOf(TestBean.class);
		assertThat(lbf.getBean("test2")).isInstanceOf(TestBean.class);
		lbf.removeBeanDefinition("test");
		lbf.removeAlias("test2");
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("test", "test2");

		assertThat(lbf.getBean("test")).isInstanceOf(NestedTestBean.class);
		assertThat(lbf.getBean("test2")).isInstanceOf(NestedTestBean.class);
	}

	@Test // gh-23542
	void concurrentBeanDefinitionRemoval() {
		final int MAX = 200;
		lbf.setAllowBeanDefinitionOverriding(false);

		// Register the bean definitions before invoking preInstantiateSingletons()
		// to simulate realistic usage of an ApplicationContext; otherwise, the bean
		// factory thinks it's an "empty" factory which causes this test to fail in
		// an unrealistic manner.
		IntStream.range(0, MAX).forEach(this::registerTestBean);
		lbf.preInstantiateSingletons();

		// This test is considered successful if the following does not result in an exception.
		IntStream.range(0, MAX).parallel().forEach(this::removeTestBean);
	}

	private void registerTestBean(int i) {
		String name = "test" + i;
		lbf.registerBeanDefinition(name, new RootBeanDefinition(TestBean.class));
	}

	private void removeTestBean(int i) {
		String name = "test" + i;
		lbf.removeBeanDefinition(name);
	}

	@Test
	void beanReferenceWithNewSyntax() {
		Properties p = new Properties();
		p.setProperty("r.(class)", TestBean.class.getName());
		p.setProperty("r.name", "rod");
		p.setProperty("k.(class)", TestBean.class.getName());
		p.setProperty("k.name", "kerry");
		p.setProperty("k.spouse", "*r");
		registerBeanDefinitions(p);

		TestBean k = (TestBean) lbf.getBean("k");
		TestBean r = (TestBean) lbf.getBean("r");
		assertThat(k.getSpouse()).isSameAs(r);
	}

	@Test
	void canEscapeBeanReferenceSyntax() {
		String name = "*name";
		Properties p = new Properties();
		p.setProperty("r.(class)", TestBean.class.getName());
		p.setProperty("r.name", "*" + name);
		registerBeanDefinitions(p);

		TestBean r = (TestBean) lbf.getBean("r");
		assertThat(r.getName()).isEqualTo(name);
	}

	@Test
	void customEditor() {
		lbf.addPropertyEditorRegistrar(registry -> {
				NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
				registry.registerCustomEditor(Float.class, new CustomNumberEditor(Float.class, nf, true));
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", "1,1");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("testBean", bd);

		TestBean testBean = (TestBean) lbf.getBean("testBean");
		assertThat(testBean.getMyFloat()).isEqualTo(1.1f);
	}

	@Test
	void customConverter() {
		GenericConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(String.class, Float.class, source -> {
			try {
				NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
				return nf.parse(source).floatValue();
			}
			catch (ParseException ex) {
				throw new IllegalArgumentException(ex);
			}
		});
		lbf.setConversionService(conversionService);

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", "1,1");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("testBean", bd);

		TestBean testBean = (TestBean) lbf.getBean("testBean");
		assertThat(testBean.getMyFloat()).isEqualTo(1.1f);
	}

	@Test
	void customEditorWithBeanReference() {
		lbf.addPropertyEditorRegistrar(registry -> {
			NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
			registry.registerCustomEditor(Float.class, new CustomNumberEditor(Float.class, nf, true));
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", new RuntimeBeanReference("myFloat"));
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("testBean", bd);
		lbf.registerSingleton("myFloat", "1,1");

		TestBean testBean = (TestBean) lbf.getBean("testBean");
		assertThat(testBean.getMyFloat()).isEqualTo(1.1f);
	}

	@Test
	void customTypeConverter() {
		NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
		lbf.setTypeConverter(new CustomTypeConverter(nf));
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", "1,1");
		ConstructorArgumentValues cav = new ConstructorArgumentValues();
		cav.addIndexedArgumentValue(0, "myName");
		cav.addIndexedArgumentValue(1, "myAge");
		lbf.registerBeanDefinition("testBean", new RootBeanDefinition(TestBean.class, cav, pvs));

		TestBean testBean = (TestBean) lbf.getBean("testBean");
		assertThat(testBean.getName()).isEqualTo("myName");
		assertThat(testBean.getAge()).isEqualTo(5);
		assertThat(testBean.getMyFloat()).isEqualTo(1.1f);
	}

	@Test
	void customTypeConverterWithBeanReference() {
		NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
		lbf.setTypeConverter(new CustomTypeConverter(nf));
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", new RuntimeBeanReference("myFloat"));
		ConstructorArgumentValues cav = new ConstructorArgumentValues();
		cav.addIndexedArgumentValue(0, "myName");
		cav.addIndexedArgumentValue(1, "myAge");
		lbf.registerBeanDefinition("testBean", new RootBeanDefinition(TestBean.class, cav, pvs));
		lbf.registerSingleton("myFloat", "1,1");

		TestBean testBean = (TestBean) lbf.getBean("testBean");
		assertThat(testBean.getName()).isEqualTo("myName");
		assertThat(testBean.getAge()).isEqualTo(5);
		assertThat(testBean.getMyFloat()).isEqualTo(1.1f);
	}

	@Test
	void registerExistingSingletonWithReference() {
		Properties p = new Properties();
		p.setProperty("test.(class)", TestBean.class.getName());
		p.setProperty("test.name", "Tony");
		p.setProperty("test.age", "48");
		p.setProperty("test.spouse(ref)", "singletonObject");
		registerBeanDefinitions(p);
		Object singletonObject = new TestBean();
		lbf.registerSingleton("singletonObject", singletonObject);

		assertThat(lbf.isSingleton("singletonObject")).isTrue();
		assertThat(lbf.getType("singletonObject")).isEqualTo(TestBean.class);
		TestBean test = (TestBean) lbf.getBean("test");
		assertThat(lbf.getBean("singletonObject")).isEqualTo(singletonObject);
		assertThat(test.getSpouse()).isEqualTo(singletonObject);

		Map<?, ?> beansOfType = lbf.getBeansOfType(TestBean.class, false, true);
		assertThat(beansOfType).hasSize(2);
		assertThat(beansOfType.containsValue(test)).isTrue();
		assertThat(beansOfType.containsValue(singletonObject)).isTrue();

		beansOfType = lbf.getBeansOfType(null, false, true);
		assertThat(beansOfType).hasSize(2);

		Iterator<String> beanNames = lbf.getBeanNamesIterator();
		assertThat(beanNames.next()).isEqualTo("test");
		assertThat(beanNames.next()).isEqualTo("singletonObject");
		assertThat(beanNames.hasNext()).isFalse();

		assertThat(lbf.containsSingleton("test")).isTrue();
		assertThat(lbf.containsSingleton("singletonObject")).isTrue();
		assertThat(lbf.containsBeanDefinition("test")).isTrue();
		assertThat(lbf.containsBeanDefinition("singletonObject")).isFalse();
	}

	@Test
	void registerExistingSingletonWithNameOverriding() {
		Properties p = new Properties();
		p.setProperty("test.(class)", TestBean.class.getName());
		p.setProperty("test.name", "Tony");
		p.setProperty("test.age", "48");
		p.setProperty("test.spouse(ref)", "singletonObject");
		registerBeanDefinitions(p);
		lbf.registerBeanDefinition("singletonObject", new RootBeanDefinition(PropertiesFactoryBean.class));
		Object singletonObject = new TestBean();
		lbf.registerSingleton("singletonObject", singletonObject);
		lbf.preInstantiateSingletons();

		assertThat(lbf.isSingleton("singletonObject")).isTrue();
		assertThat(lbf.getType("singletonObject")).isEqualTo(TestBean.class);
		TestBean test = (TestBean) lbf.getBean("test");
		assertThat(lbf.getBean("singletonObject")).isEqualTo(singletonObject);
		assertThat(test.getSpouse()).isEqualTo(singletonObject);

		Map<?, ?> beansOfType = lbf.getBeansOfType(TestBean.class, false, true);
		assertThat(beansOfType).hasSize(2);
		assertThat(beansOfType.containsValue(test)).isTrue();
		assertThat(beansOfType.containsValue(singletonObject)).isTrue();

		beansOfType = lbf.getBeansOfType(null, false, true);

		Iterator<String> beanNames = lbf.getBeanNamesIterator();
		assertThat(beanNames.next()).isEqualTo("test");
		assertThat(beanNames.next()).isEqualTo("singletonObject");
		assertThat(beanNames.hasNext()).isFalse();
		assertThat(beansOfType).hasSize(2);

		assertThat(lbf.containsSingleton("test")).isTrue();
		assertThat(lbf.containsSingleton("singletonObject")).isTrue();
		assertThat(lbf.containsBeanDefinition("test")).isTrue();
		assertThat(lbf.containsBeanDefinition("singletonObject")).isTrue();
	}

	@Test
	void registerExistingSingletonWithAutowire() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Tony");
		pvs.add("age", "48");
		RootBeanDefinition bd = new RootBeanDefinition(DependenciesBean.class);
		bd.setPropertyValues(pvs);
		bd.setDependencyCheck(RootBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		lbf.registerBeanDefinition("test", bd);
		Object singletonObject = new TestBean();
		lbf.registerSingleton("singletonObject", singletonObject);

		assertThat(lbf.containsBean("singletonObject")).isTrue();
		assertThat(lbf.isSingleton("singletonObject")).isTrue();
		assertThat(lbf.getType("singletonObject")).isEqualTo(TestBean.class);
		assertThat(lbf.getAliases("singletonObject")).isEmpty();
		DependenciesBean test = (DependenciesBean) lbf.getBean("test");
		assertThat(lbf.getBean("singletonObject")).isEqualTo(singletonObject);
		assertThat(test.getSpouse()).isEqualTo(singletonObject);
	}

	@Test
	void registerExistingSingletonWithAlreadyBound() {
		Object singletonObject = new TestBean();
		lbf.registerSingleton("singletonObject", singletonObject);
		assertThatIllegalStateException().isThrownBy(() ->
				lbf.registerSingleton("singletonObject", singletonObject));
	}

	@Test
	void reregisterBeanDefinition() {
		lbf.setAllowBeanDefinitionOverriding(true);
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("testBean", bd1);
		assertThat(lbf.getBean("testBean")).isInstanceOf(TestBean.class);

		RootBeanDefinition bd2 = new RootBeanDefinition(NestedTestBean.class);
		bd2.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("testBean", bd2);
		assertThat(lbf.getBean("testBean")).isInstanceOf(NestedTestBean.class);
	}

	@Test
	void arrayPropertyWithAutowiring() throws MalformedURLException {
		lbf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		lbf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		lbf.registerBeanDefinition("arrayBean", rbd);

		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");
		assertThat(ab.getResourceArray()[0]).isEqualTo(new UrlResource("http://localhost:8080"));
		assertThat(ab.getResourceArray()[1]).isEqualTo(new UrlResource("http://localhost:9090"));
	}

	@Test
	void arrayPropertyWithOptionalAutowiring() {
		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		lbf.registerBeanDefinition("arrayBean", rbd);

		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");
		assertThat(ab.getResourceArray()).isNull();
	}

	@Test
	void arrayConstructorWithAutowiring() {
		lbf.registerSingleton("integer1",4);
		lbf.registerSingleton("integer2", 5);

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("arrayBean", rbd);

		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");
		assertThat(ab.getIntegerArray()[0]).isEqualTo(4);
		assertThat(ab.getIntegerArray()[1]).isEqualTo(5);
	}

	@Test
	void arrayConstructorWithOptionalAutowiring() {
		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("arrayBean", rbd);

		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");
		assertThat(ab.getIntegerArray()).isNull();
	}

	@Test
	void doubleArrayConstructorWithAutowiring() throws MalformedURLException {
		lbf.registerSingleton("integer1", 4);
		lbf.registerSingleton("integer2", 5);
		lbf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		lbf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("arrayBean", rbd);

		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");
		assertThat(ab.getIntegerArray()[0]).isEqualTo(4);
		assertThat(ab.getIntegerArray()[1]).isEqualTo(5);
		assertThat(ab.getResourceArray()[0]).isEqualTo(new UrlResource("http://localhost:8080"));
		assertThat(ab.getResourceArray()[1]).isEqualTo(new UrlResource("http://localhost:9090"));
	}

	@Test
	void doubleArrayConstructorWithOptionalAutowiring() throws MalformedURLException {
		lbf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		lbf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("arrayBean", rbd);

		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");
		assertThat(ab.getIntegerArray()).isNull();
		assertThat(ab.getResourceArray()).isNull();
	}

	@Test
	void expressionInStringArray() {
		BeanExpressionResolver beanExpressionResolver = mock();
		given(beanExpressionResolver.evaluate(eq("#{foo}"), any(BeanExpressionContext.class)))
				.willReturn("classpath:/org/springframework/beans/factory/xml/util.properties");
		lbf.setBeanExpressionResolver(beanExpressionResolver);

		RootBeanDefinition rbd = new RootBeanDefinition(PropertiesFactoryBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("locations", new String[]{"#{foo}"});
		rbd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("myProperties", rbd);

		Properties properties = (Properties) lbf.getBean("myProperties");
		assertThat(properties.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	void withOverloadedSetters() {
		lbf.setAllowBeanDefinitionOverriding(true);
		RootBeanDefinition rbd = new RootBeanDefinition(SetterOverload.class);
		rbd.getPropertyValues().add("object", "a String");
		lbf.registerBeanDefinition("overloaded", rbd);
		assertThat(lbf.getBean(SetterOverload.class).getObject()).isEqualTo("a String");

		rbd = new RootBeanDefinition(SetterOverload.class);
		rbd.getPropertyValues().add("object", 1000);
		lbf.registerBeanDefinition("overloaded", rbd);
		assertThat(lbf.getBean(SetterOverload.class).getObject()).isEqualTo("1000");

		rbd = new RootBeanDefinition(SetterOverload.class);
		rbd.getPropertyValues().add("value", 1000);
		lbf.registerBeanDefinition("overloaded", rbd);
		assertThat(lbf.getBean(SetterOverload.class).getObject()).isEqualTo("1000i");

		rbd = new RootBeanDefinition(SetterOverload.class);
		rbd.getPropertyValues().add("value", Duration.ofSeconds(1000));
		lbf.registerBeanDefinition("overloaded", rbd);
		assertThat(lbf.getBean(SetterOverload.class).getObject()).isEqualTo("1000s");

		rbd = new RootBeanDefinition(SetterOverload.class);
		rbd.getPropertyValues().add("value", "1000");
		lbf.registerBeanDefinition("overloaded", rbd);
		assertThat(lbf.getBean(SetterOverload.class).getObject()).isEqualTo("1000i");
	}

	@Test
	void autowireWithNoDependencies() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("rod", bd);
		assertThat(lbf.getBeanDefinitionCount()).isEqualTo(1);
		Object registered = lbf.autowire(NoDependencies.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		assertThat(lbf.getBeanDefinitionCount()).isEqualTo(1);
		assertThat(registered).isInstanceOf(NoDependencies.class);
	}

	@Test
	void autowireWithSatisfiedJavaBeanDependency() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("rod", bd);
		assertThat(lbf.getBeanDefinitionCount()).isEqualTo(1);

		// Depends on age, name and spouse (TestBean)
		Object registered = lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		assertThat(lbf.getBeanDefinitionCount()).isEqualTo(1);
		DependenciesBean kerry = (DependenciesBean) registered;
		TestBean rod = (TestBean) lbf.getBean("rod");
		assertThat(kerry.getSpouse()).isSameAs(rod);
	}

	@Test
	void autowireWithSatisfiedConstructorDependency() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("rod", bd);
		assertThat(lbf.getBeanDefinitionCount()).isEqualTo(1);

		Object registered = lbf.autowire(ConstructorDependency.class, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
		assertThat(lbf.getBeanDefinitionCount()).isEqualTo(1);
		ConstructorDependency kerry = (ConstructorDependency) registered;
		TestBean rod = (TestBean) lbf.getBean("rod");
		assertThat(kerry.spouse).isSameAs(rod);
	}

	@Test
	void autowireWithTwoMatchesForConstructorDependency() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("rod", bd);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("rod2", bd2);
		lbf.setParameterNameDiscoverer(new DefaultParameterNameDiscoverer());

		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> lbf.autowire(ConstructorDependency.class, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false))
				.withMessageContaining("rod")
				.withMessageContaining("rod2");
	}

	@Test
	void autowireWithUnsatisfiedConstructorDependency() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("name", "Rod"));
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("rod", bd);

		assertThat(lbf.getBeanDefinitionCount()).isEqualTo(1);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				lbf.autowire(UnsatisfiedConstructorDependency.class, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, true));
	}

	@Test
	void autowireConstructor() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spouse", bd);
		ConstructorDependenciesBean bean = (ConstructorDependenciesBean)
				lbf.autowire(ConstructorDependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, true);

		Object spouse = lbf.getBean("spouse");
		assertThat(bean.getSpouse1()).isSameAs(spouse);
		assertThat(BeanFactoryUtils.beanOfType(lbf, TestBean.class)).isSameAs(spouse);
	}

	@Test
	void autowireBeanByName() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spouse", bd);
		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);

		TestBean spouse = (TestBean) lbf.getBean("spouse");
		assertThat(bean.getSpouse()).isEqualTo(spouse);
		assertThat(BeanFactoryUtils.beanOfType(lbf, TestBean.class)).isSameAs(spouse);
	}

	@Test
	void autowireBeanByNameWithDependencyCheck() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spous", bd);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true));
	}

	@Test
	void autowireBeanByNameWithNoDependencyCheck() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spous", bd);
		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);

		assertThat(bean.getSpouse()).isNull();
	}

	@Test
	void autowirePreferredConstructors() {
		lbf.registerBeanDefinition("spouse1", new RootBeanDefinition(TestBean.class));
		lbf.registerBeanDefinition("spouse2", new RootBeanDefinition(TestBean.class));
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependenciesBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("bean", bd);
		lbf.setParameterNameDiscoverer(new DefaultParameterNameDiscoverer());

		ConstructorDependenciesBean bean = lbf.getBean(ConstructorDependenciesBean.class);
		Object spouse1 = lbf.getBean("spouse1");
		Object spouse2 = lbf.getBean("spouse2");
		assertThat(bean.getSpouse1()).isSameAs(spouse1);
		assertThat(bean.getSpouse2()).isSameAs(spouse2);
	}

	@Test
	void autowirePreferredConstructorsFromAttribute() {
		lbf.registerBeanDefinition("spouse1", new RootBeanDefinition(TestBean.class));
		lbf.registerBeanDefinition("spouse2", new RootBeanDefinition(TestBean.class));
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(ConstructorDependenciesBean.class);
		bd.setAttribute(GenericBeanDefinition.PREFERRED_CONSTRUCTORS_ATTRIBUTE,
				ConstructorDependenciesBean.class.getConstructors());
		lbf.registerBeanDefinition("bean", bd);
		lbf.setParameterNameDiscoverer(new DefaultParameterNameDiscoverer());

		ConstructorDependenciesBean bean = lbf.getBean(ConstructorDependenciesBean.class);
		Object spouse1 = lbf.getBean("spouse1");
		Object spouse2 = lbf.getBean("spouse2");
		assertThat(bean.getSpouse1()).isSameAs(spouse1);
		assertThat(bean.getSpouse2()).isSameAs(spouse2);
	}

	@Test
	void autowirePreferredConstructorFromAttribute() throws Exception {
		lbf.registerBeanDefinition("spouse1", new RootBeanDefinition(TestBean.class));
		lbf.registerBeanDefinition("spouse2", new RootBeanDefinition(TestBean.class));
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(ConstructorDependenciesBean.class);
		bd.setAttribute(GenericBeanDefinition.PREFERRED_CONSTRUCTORS_ATTRIBUTE,
				ConstructorDependenciesBean.class.getConstructor(TestBean.class));
		lbf.registerBeanDefinition("bean", bd);
		lbf.setParameterNameDiscoverer(new DefaultParameterNameDiscoverer());

		ConstructorDependenciesBean bean = lbf.getBean(ConstructorDependenciesBean.class);
		Object spouse = lbf.getBean("spouse1");
		assertThat(bean.getSpouse1()).isSameAs(spouse);
		assertThat(bean.getSpouse2()).isNull();
	}

	@Test
	void orderFromAttribute() {
		GenericBeanDefinition bd1 = new GenericBeanDefinition();
		bd1.setBeanClass(TestBean.class);
		bd1.setPropertyValues(new MutablePropertyValues(List.of(new PropertyValue("name", "lowest"))));
		bd1.setAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE, Ordered.LOWEST_PRECEDENCE);
		lbf.registerBeanDefinition("bean1", bd1);
		GenericBeanDefinition bd2 = new GenericBeanDefinition();
		bd2.setBeanClass(TestBean.class);
		bd2.setPropertyValues(new MutablePropertyValues(List.of(new PropertyValue("name", "highest"))));
		bd2.setAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE, Ordered.HIGHEST_PRECEDENCE);
		lbf.registerBeanDefinition("bean2", bd2);
		assertThat(lbf.getBeanProvider(TestBean.class).orderedStream().map(TestBean::getName))
				.containsExactly("highest", "lowest");
	}

	@Test
	void orderFromAttributeOverrideAnnotation() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition rbd1 = new RootBeanDefinition(LowestPrecedenceTestBeanFactoryBean.class);
		rbd1.setAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE, Ordered.HIGHEST_PRECEDENCE);
		lbf.registerBeanDefinition("lowestPrecedenceFactory", rbd1);
		RootBeanDefinition rbd2 = new RootBeanDefinition(HighestPrecedenceTestBeanFactoryBean.class);
		rbd2.setAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE, Ordered.LOWEST_PRECEDENCE);
		lbf.registerBeanDefinition("highestPrecedenceFactory", rbd2);
		GenericBeanDefinition bd1 = new GenericBeanDefinition();
		bd1.setFactoryBeanName("highestPrecedenceFactory");
		lbf.registerBeanDefinition("bean1", bd1);
		GenericBeanDefinition bd2 = new GenericBeanDefinition();
		bd2.setFactoryBeanName("lowestPrecedenceFactory");
		lbf.registerBeanDefinition("bean2", bd2);
		assertThat(lbf.getBeanProvider(TestBean.class).orderedStream().map(TestBean::getName))
				.containsExactly("fromLowestPrecedenceTestBeanFactoryBean", "fromHighestPrecedenceTestBeanFactoryBean");
	}

	@Test
	void invalidOrderAttribute() {
		GenericBeanDefinition bd1 = new GenericBeanDefinition();
		bd1.setBeanClass(TestBean.class);
		bd1.setAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE, Boolean.TRUE);
		lbf.registerBeanDefinition("bean1", bd1);
		GenericBeanDefinition bd2 = new GenericBeanDefinition();
		bd2.setBeanClass(TestBean.class);
		lbf.registerBeanDefinition("bean", bd2);
		assertThatIllegalStateException()
				.isThrownBy(() -> lbf.getBeanProvider(TestBean.class).orderedStream().collect(Collectors.toList()))
				.withMessageContaining("Invalid value type for attribute");
	}

	@Test
	void dependsOnCycle() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setDependsOn("tb2");
		lbf.registerBeanDefinition("tb1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setDependsOn("tb1");
		lbf.registerBeanDefinition("tb2", bd2);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> lbf.preInstantiateSingletons())
				.withMessageContaining("Circular")
				.withMessageContaining("'tb2'")
				.withMessageContaining("'tb1'");
	}

	@Test
	void implicitDependsOnCycle() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setDependsOn("tb2");
		lbf.registerBeanDefinition("tb1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setDependsOn("tb3");
		lbf.registerBeanDefinition("tb2", bd2);
		RootBeanDefinition bd3 = new RootBeanDefinition(TestBean.class);
		bd3.setDependsOn("tb1");
		lbf.registerBeanDefinition("tb3", bd3);

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(lbf::preInstantiateSingletons)
				.withMessageContaining("Circular")
				.withMessageContaining("'tb3'")
				.withMessageContaining("'tb1'");
	}

	@Test
	void getBeanByTypeWithNoneFound() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(TestBean.class));
	}

	@Test
	void getBeanByTypeWithLateRegistration() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(TestBean.class));

		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("bd1", bd1);

		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName()).isEqualTo("bd1");
	}

	@Test
	void getBeanByTypeWithLateRegistrationAgainstFrozen() {
		lbf.freezeConfiguration();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(TestBean.class));

		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("bd1", bd1);

		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName()).isEqualTo("bd1");
	}

	@Test
	void getBeanByTypeDefinedInParent() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		parent.registerBeanDefinition("bd1", bd1);
		lbf.setParentBeanFactory(parent);

		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName()).isEqualTo("bd1");
	}

	@Test
	void getBeanByTypeWithAmbiguity() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(TestBean.class));
	}

	@Test
	void getBeanByTypeWithPrimary() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setLazyInit(true);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName()).isEqualTo("bd2");
		assertThat(lbf.containsSingleton("bd1")).isFalse();
	}

	@Test
	@SuppressWarnings("rawtypes")
	void getFactoryBeanByTypeWithPrimary() {
		RootBeanDefinition bd1 = new RootBeanDefinition(NullTestBeanFactoryBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(NullTestBeanFactoryBean.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		NullTestBeanFactoryBean factoryBeanByType = lbf.getBean(NullTestBeanFactoryBean.class);
		NullTestBeanFactoryBean bd1FactoryBean = (NullTestBeanFactoryBean)lbf.getBean("&bd1");
		NullTestBeanFactoryBean bd2FactoryBean = (NullTestBeanFactoryBean)lbf.getBean("&bd2");
		assertThat(factoryBeanByType).isNotNull();
		assertThat(bd1FactoryBean).isNotNull();
		assertThat(bd2FactoryBean).isNotNull();
		assertThat(bd1FactoryBean).isNotEqualTo(factoryBeanByType);
		assertThat(bd2FactoryBean).isEqualTo(factoryBeanByType);
	}

	@Test
	void getBeanByTypeWithMultiplePrimary() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setPrimary(true);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
				.isThrownBy(() -> lbf.getBean(TestBean.class))
				.withMessageContaining("more than one 'primary'");
	}

	@Test
	void getBeanByTypeWithPriority() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd1 = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(LowPriorityTestBean.class);
		RootBeanDefinition bd3 = new RootBeanDefinition(NullTestBeanFactoryBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);
		lbf.registerBeanDefinition("bd3", bd3);
		lbf.preInstantiateSingletons();

		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName()).isEqualTo("bd1");
	}

	@Test
	void mapInjectionWithPriority() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd1 = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(LowPriorityTestBean.class);
		RootBeanDefinition bd3 = new RootBeanDefinition(NullTestBeanFactoryBean.class);
		RootBeanDefinition bd4 = new RootBeanDefinition(TestBeanRecipient.class, RootBeanDefinition.AUTOWIRE_CONSTRUCTOR, false);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);
		lbf.registerBeanDefinition("bd3", bd3);
		lbf.registerBeanDefinition("bd4", bd4);
		lbf.preInstantiateSingletons();

		TestBean bean = lbf.getBean(TestBeanRecipient.class).testBean;
		assertThat(bean.getBeanName()).isEqualTo("bd1");
	}

	@Test
	void getBeanByTypeWithMultiplePriority() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd1 = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(HighPriorityTestBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
				.isThrownBy(() -> lbf.getBean(TestBean.class))
				.withMessageContaining("Multiple beans found with the same priority")
				.withMessageContaining("5"); // conflicting priority
	}

	@Test
	void getBeanByTypeWithPriorityAndNullInstance() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd1 = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(NullTestBeanFactoryBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName()).isEqualTo("bd1");
	}

	@Test
	void getBeanByTypePrimaryHasPrecedenceOverPriority() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd1 = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName()).isEqualTo("bd2");
	}

	@Test
	void getBeanByTypeFiltersOutNonAutowireCandidates() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition na1 = new RootBeanDefinition(TestBean.class);
		na1.setAutowireCandidate(false);

		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("na1", na1);
		TestBean actual = lbf.getBean(TestBean.class);  // na1 was filtered
		assertThat(actual).isSameAs(lbf.getBean("bd1", TestBean.class));

		lbf.registerBeanDefinition("bd2", bd2);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(TestBean.class));
	}

	@Test
	void getBeanByTypeWithNullRequiredType() {
		assertThatIllegalArgumentException().isThrownBy(() -> lbf.getBean((Class<?>) null));
	}

	@Test
	void getBeanProviderByTypeWithNullRequiredType() {
		assertThatIllegalArgumentException().isThrownBy(() -> lbf.getBeanProvider((Class<?>) null));
	}

	@Test
	void resolveNamedBeanByTypeWithNullRequiredType() {
		assertThatIllegalArgumentException().isThrownBy(() -> lbf.resolveNamedBean((Class<?>) null));
	}

	@Test
	void getBeanByTypeInstanceWithNoneFound() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(ConstructorDependency.class));
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(ConstructorDependency.class, 42));

		ObjectProvider<ConstructorDependency> provider = lbf.getBeanProvider(ConstructorDependency.class);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(
				provider::getObject);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				provider.getObject(42));
		assertThat(provider.getIfAvailable()).isNull();
		assertThat(provider.getIfUnique()).isNull();
	}

	@Test
	void getBeanByTypeInstanceDefinedInParent() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		parent.registerBeanDefinition("bd1", bd1);
		lbf.setParentBeanFactory(parent);

		ConstructorDependency bean = lbf.getBean(ConstructorDependency.class);
		assertThat(bean.beanName).isEqualTo("bd1");
		assertThat(bean.spouseAge).isEqualTo(99);
		bean = lbf.getBean(ConstructorDependency.class, 42);
		assertThat(bean.beanName).isEqualTo("bd1");
		assertThat(bean.spouseAge).isEqualTo(42);

		ObjectProvider<ConstructorDependency> provider = lbf.getBeanProvider(ConstructorDependency.class);
		bean = provider.getObject();
		assertThat(bean.beanName).isEqualTo("bd1");
		assertThat(bean.spouseAge).isEqualTo(99);
		bean = provider.getObject(42);
		assertThat(bean.beanName).isEqualTo("bd1");
		assertThat(bean.spouseAge).isEqualTo(42);
		bean = provider.getIfAvailable();
		assertThat(bean.beanName).isEqualTo("bd1");
		assertThat(bean.spouseAge).isEqualTo(99);
		bean = provider.getIfUnique();
		assertThat(bean.beanName).isEqualTo("bd1");
		assertThat(bean.spouseAge).isEqualTo(99);
	}

	@Test
	void getBeanByTypeInstanceWithAmbiguity() {
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		RootBeanDefinition bd2 = new RootBeanDefinition(ConstructorDependency.class);
		bd2.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bd2.getConstructorArgumentValues().addGenericArgumentValue("43");
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(ConstructorDependency.class));
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(ConstructorDependency.class, 42));
		ObjectProvider<ConstructorDependency> provider = lbf.getBeanProvider(ConstructorDependency.class);
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(
				provider::getObject);
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(() ->
				provider.getObject(42));
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(
				provider::getIfAvailable);
		assertThat(provider.getIfUnique()).isNull();

		Set<Object> resolved = new HashSet<>();
		for (ConstructorDependency instance : provider) {
			resolved.add(instance);
		}
		assertThat(resolved).hasSize(2);
		assertThat(resolved).contains(lbf.getBean("bd1"));
		assertThat(resolved).contains(lbf.getBean("bd2"));

		resolved = new HashSet<>();
		provider.forEach(resolved::add);
		assertThat(resolved).hasSize(2);
		assertThat(resolved).contains(lbf.getBean("bd1"));
		assertThat(resolved).contains(lbf.getBean("bd2"));

		resolved = provider.stream().collect(Collectors.toSet());
		assertThat(resolved).hasSize(2);
		assertThat(resolved).contains(lbf.getBean("bd1"));
		assertThat(resolved).contains(lbf.getBean("bd2"));
	}

	@Test
	void getBeanByTypeInstanceWithPrimary() {
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		RootBeanDefinition bd2 = createConstructorDependencyBeanDefinition(43);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		ConstructorDependency bean = lbf.getBean(ConstructorDependency.class);
		assertThat(bean.beanName).isEqualTo("bd2");
		assertThat(bean.spouseAge).isEqualTo(43);
		bean = lbf.getBean(ConstructorDependency.class, 42);
		assertThat(bean.beanName).isEqualTo("bd2");
		assertThat(bean.spouseAge).isEqualTo(42);

		ObjectProvider<ConstructorDependency> provider = lbf.getBeanProvider(ConstructorDependency.class);
		bean = provider.getObject();
		assertThat(bean.beanName).isEqualTo("bd2");
		assertThat(bean.spouseAge).isEqualTo(43);
		bean = provider.getObject(42);
		assertThat(bean.beanName).isEqualTo("bd2");
		assertThat(bean.spouseAge).isEqualTo(42);
		bean = provider.getIfAvailable();
		assertThat(bean.beanName).isEqualTo("bd2");
		assertThat(bean.spouseAge).isEqualTo(43);
		bean = provider.getIfUnique();
		assertThat(bean.beanName).isEqualTo("bd2");
		assertThat(bean.spouseAge).isEqualTo(43);

		Set<Object> resolved = new HashSet<>();
		for (ConstructorDependency instance : provider) {
			resolved.add(instance);
		}
		assertThat(resolved).hasSize(2);
		assertThat(resolved).contains(lbf.getBean("bd1"));
		assertThat(resolved).contains(lbf.getBean("bd2"));

		resolved = new HashSet<>();
		provider.forEach(resolved::add);
		assertThat(resolved).hasSize(2);
		assertThat(resolved).contains(lbf.getBean("bd1"));
		assertThat(resolved).contains(lbf.getBean("bd2"));

		resolved = provider.stream().collect(Collectors.toSet());
		assertThat(resolved).hasSize(2);
		assertThat(resolved).contains(lbf.getBean("bd1"));
		assertThat(resolved).contains(lbf.getBean("bd2"));
	}

	@Test
	void getBeanByTypeInstanceWithMultiplePrimary() {
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		RootBeanDefinition bd2 = createConstructorDependencyBeanDefinition(43);
		bd1.setPrimary(true);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
				.isThrownBy(() -> lbf.getBean(ConstructorDependency.class, 42))
				.withMessageContaining("more than one 'primary'");
	}

	@Test
	void getBeanByTypeInstanceFiltersOutNonAutowireCandidates() {
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		RootBeanDefinition bd2 = createConstructorDependencyBeanDefinition(43);
		RootBeanDefinition na1 = createConstructorDependencyBeanDefinition(21);
		na1.setAutowireCandidate(false);

		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("na1", na1);
		ConstructorDependency actual = lbf.getBean(ConstructorDependency.class, 42);  // na1 was filtered
		assertThat(actual.beanName).isEqualTo("bd1");

		lbf.registerBeanDefinition("bd2", bd2);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(TestBean.class, 67));
	}

	@Test
	void getBeanByTypeInstanceWithConstructorIgnoresInstanceSupplier() {
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		bd1.setInstanceSupplier(() -> new ConstructorDependency(new TestBean("test")));
		lbf.registerBeanDefinition("bd1", bd1);

		ConstructorDependency defaultInstance = lbf.getBean(ConstructorDependency.class);
		assertThat(defaultInstance.beanName).isEqualTo("bd1");
		assertThat(defaultInstance.spouseAge).isEqualTo(0);

		ConstructorDependency argsInstance = lbf.getBean(ConstructorDependency.class, 42);
		assertThat(argsInstance.beanName).isEqualTo("bd1");
		assertThat(argsInstance.spouseAge).isEqualTo(42);
	}

	@Test
	void getBeanByTypeInstanceWithFactoryMethodIgnoresInstanceSupplier() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bd1.setFactoryBeanName("config");
		bd1.setFactoryMethodName("create");
		bd1.setInstanceSupplier(() -> new TestBean("test"));
		lbf.registerBeanDefinition("config", new RootBeanDefinition(BeanWithFactoryMethod.class));
		lbf.registerBeanDefinition("bd1", bd1);

		TestBean defaultInstance = lbf.getBean(TestBean.class);
		assertThat(defaultInstance.getBeanName()).isEqualTo("bd1");
		assertThat(defaultInstance.getName()).isEqualTo("test");
		assertThat(defaultInstance.getAge()).isEqualTo(0);

		TestBean argsInstance = lbf.getBean(TestBean.class, "another", 42);
		assertThat(argsInstance.getBeanName()).isEqualTo("bd1");
		assertThat(argsInstance.getName()).isEqualTo("another");
		assertThat(argsInstance.getAge()).isEqualTo(42);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void beanProviderSerialization() throws Exception {
		lbf.setSerializationId("test");
		ObjectProvider<ConstructorDependency> provider = lbf.getBeanProvider(ConstructorDependency.class);
		ObjectProvider deserialized = SerializationTestUtils.serializeAndDeserialize(provider);

		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(
				deserialized::getObject);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				deserialized.getObject(42));
		assertThat(deserialized.getIfAvailable()).isNull();
		assertThat(deserialized.getIfUnique()).isNull();
	}

	@Test
	void getBeanWithArgsNotCreatedForFactoryBeanChecking() {
		RootBeanDefinition bd1 = new RootBeanDefinition(ConstructorDependency.class);
		bd1.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("bd1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(ConstructorDependencyFactoryBean.class);
		bd2.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("bd2", bd2);

		ConstructorDependency bean = lbf.getBean(ConstructorDependency.class, 42);
		assertThat(bean.beanName).isEqualTo("bd1");
		assertThat(bean.spouseAge).isEqualTo(42);

		assertThat(lbf.getBeanNamesForType(ConstructorDependency.class)).hasSize(1);
		assertThat(lbf.getBeanNamesForType(ConstructorDependencyFactoryBean.class)).hasSize(1);
		assertThat(lbf.getBeanNamesForType(ResolvableType.forClassWithGenerics(FactoryBean.class, Object.class))).hasSize(1);
		assertThat(lbf.getBeanNamesForType(ResolvableType.forClassWithGenerics(FactoryBean.class, String.class))).isEmpty();
		assertThat(lbf.getBeanNamesForType(ResolvableType.forClassWithGenerics(FactoryBean.class, Object.class), true, true)).hasSize(1);
		assertThat(lbf.getBeanNamesForType(ResolvableType.forClassWithGenerics(FactoryBean.class, String.class), true, true)).isEmpty();
	}

	private RootBeanDefinition createConstructorDependencyBeanDefinition(int age) {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependency.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bd.getConstructorArgumentValues().addGenericArgumentValue(age);
		return bd;
	}

	@Test
	void autowireBeanByType() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("test", bd);
		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);

		TestBean test = (TestBean) lbf.getBean("test");
		assertThat(bean.getSpouse()).isEqualTo(test);
	}

	/**
	 * Verifies that a dependency on a {@link FactoryBean} can be autowired
	 * <em>by type</em>, specifically addressing the JIRA issue raised in <a
	 * href="https://opensource.atlassian.com/projects/spring/browse/SPR-4040"
	 * target="_blank">SPR-4040</a>.
	 */
	@Test
	void autowireBeanWithFactoryBeanByType() {
		RootBeanDefinition bd = new RootBeanDefinition(LazyInitFactory.class);
		lbf.registerBeanDefinition("factoryBean", bd);

		LazyInitFactory factoryBean = (LazyInitFactory) lbf.getBean("&factoryBean");
		assertThat(factoryBean).as("The FactoryBean should have been registered.").isNotNull();

		FactoryBeanDependentBean bean = (FactoryBeanDependentBean) lbf.autowire(FactoryBeanDependentBean.class,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		assertThat(bean.getFactoryBean()).as("The FactoryBeanDependentBean should have been autowired 'by type' with the LazyInitFactory.").isEqualTo(factoryBean);
	}

	@Test
	void autowireBeanWithFactoryBeanByTypeWithPrimary() {
		RootBeanDefinition bd1 = new RootBeanDefinition(LazyInitFactory.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(LazyInitFactory.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		LazyInitFactory bd1FactoryBean = (LazyInitFactory) lbf.getBean("&bd1");
		LazyInitFactory bd2FactoryBean = (LazyInitFactory) lbf.getBean("&bd2");
		assertThat(bd1FactoryBean).isNotNull();
		assertThat(bd2FactoryBean).isNotNull();

		FactoryBeanDependentBean bean = (FactoryBeanDependentBean) lbf.autowire(FactoryBeanDependentBean.class,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		assertThat(bean.getFactoryBean()).isNotEqualTo(bd1FactoryBean);
		assertThat(bean.getFactoryBean()).isEqualTo(bd2FactoryBean);
	}

	@Test
	void getTypeForAbstractFactoryBean() {
		RootBeanDefinition bd = new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class);
		bd.setAbstract(true);
		lbf.registerBeanDefinition("factoryBean", bd);
		assertThat(lbf.getType("factoryBean")).isNull();
	}

	@Test
	void getBeanNamesForTypeBeforeFactoryBeanCreation() {
		FactoryBeanThatShouldntBeCalled.instantiated = false;
		lbf.registerBeanDefinition("factoryBean", new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class));
		assertThat(lbf.containsSingleton("factoryBean")).isFalse();
		assertThat(FactoryBeanThatShouldntBeCalled.instantiated).isFalse();

		assertBeanNamesForType(Runnable.class, false, false, "&factoryBean");
		assertBeanNamesForType(Callable.class, false, false, "&factoryBean");
		assertBeanNamesForType(RepositoryFactoryInformation.class, false, false, "&factoryBean");
		assertBeanNamesForType(FactoryBean.class, false, false, "&factoryBean");
	}

	@Test
	void getBeanNamesForTypeAfterFactoryBeanCreation() {
		FactoryBeanThatShouldntBeCalled.instantiated = false;
		lbf.registerBeanDefinition("factoryBean", new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class));
		lbf.getBean("&factoryBean");
		assertThat(FactoryBeanThatShouldntBeCalled.instantiated).isTrue();
		assertThat(lbf.containsSingleton("factoryBean")).isTrue();

		assertBeanNamesForType(Runnable.class, false, false, "&factoryBean");
		assertBeanNamesForType(Callable.class, false, false, "&factoryBean");
		assertBeanNamesForType(RepositoryFactoryInformation.class, false, false, "&factoryBean");
		assertBeanNamesForType(FactoryBean.class, false, false, "&factoryBean");
	}

	@Test  // gh-28616
	void getBeanNamesForTypeWithPrototypeScopedFactoryBean() {
		FactoryBeanThatShouldntBeCalled.instantiated = false;
		RootBeanDefinition beanDefinition = new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class);
		beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("factoryBean", beanDefinition);
		assertThat(FactoryBeanThatShouldntBeCalled.instantiated).isFalse();
		assertThat(lbf.containsSingleton("factoryBean")).isFalse();

		// We should not find any beans of the following types if the FactoryBean itself is prototype-scoped.
		assertBeanNamesForType(Runnable.class, false, false);
		assertBeanNamesForType(Callable.class, false, false);
		assertBeanNamesForType(RepositoryFactoryInformation.class, false, false);
		assertBeanNamesForType(FactoryBean.class, false, false);
	}

	@Test  // gh-30987
	void getBeanNamesForTypeWithFactoryBeanDefinedAsTargetType() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestRepositoryFactoryBean.class);
		beanDefinition.setTargetType(ResolvableType.forClassWithGenerics(TestRepositoryFactoryBean.class,
				CityRepository.class, Object.class, Object.class));
		lbf.registerBeanDefinition("factoryBean", beanDefinition);
		assertBeanNamesForType(TestRepositoryFactoryBean.class, true, false, "&factoryBean");
		assertBeanNamesForType(CityRepository.class, true, false, "factoryBean");
	}

	/**
	 * Verifies that a dependency on a {@link FactoryBean} can <strong>not</strong>
	 * be autowired <em>by name</em>, as &amp; is an illegal character in
	 * Java method names. In other words, you can't name a method
	 * {@code set&amp;FactoryBean(...)}.
	 */
	@Test
	void autowireBeanWithFactoryBeanByName() {
		RootBeanDefinition bd = new RootBeanDefinition(LazyInitFactory.class);
		lbf.registerBeanDefinition("factoryBean", bd);
		LazyInitFactory factoryBean = (LazyInitFactory) lbf.getBean("&factoryBean");

		assertThat(factoryBean).as("The FactoryBean should have been registered.").isNotNull();
		assertThatExceptionOfType(TypeMismatchException.class).isThrownBy(() ->
				lbf.autowire(FactoryBeanDependentBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true));
	}

	@Test
	void autowireBeanByTypeWithTwoMatches() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true))
				.withMessageContaining("test")
				.withMessageContaining("spouse");
	}

	@Test
	void autowireBeanByTypeWithDependencyCheck() {
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true));
	}

	@Test
	void autowireBeanByTypeWithNoDependencyCheck() {
		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		assertThat(bean.getSpouse()).isNull();
	}

	@Test
	void autowireBeanByTypeWithTwoMatchesAndOnePrimary() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPrimary(true);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);

		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		assertThat(bean.getSpouse()).isEqualTo(lbf.getBean("test"));
	}

	@Test
	void autowireBeanByTypeWithTwoPrimaryCandidates() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPrimary(true);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true))
				.withCauseExactlyInstanceOf(NoUniqueBeanDefinitionException.class);
	}

	@Test
	void autowireBeanByTypeWithTwoPrimaryCandidatesInOneAncestor() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPrimary(true);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPrimary(true);
		parent.registerBeanDefinition("test", bd);
		parent.registerBeanDefinition("spouse", bd2);
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory(parent);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true))
				.withCauseExactlyInstanceOf(NoUniqueBeanDefinitionException.class);
	}

	@Test
	void autowireBeanByTypeWithTwoPrimaryFactoryBeans(){
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition bd1 = new RootBeanDefinition(LazyInitFactory.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(LazyInitFactory.class);
		bd1.setPrimary(true);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> lbf.autowire(FactoryBeanDependentBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true))
				.withCauseExactlyInstanceOf(NoUniqueBeanDefinitionException.class);
	}

	@Test
	void autowireBeanByTypeWithTwoMatchesAndPriority() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(LowPriorityTestBean.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);

		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		assertThat(bean.getSpouse()).isEqualTo(lbf.getBean("test"));
	}

	@Test
	void autowireBeanByTypeWithIdenticalPriorityCandidates() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(HighPriorityTestBean.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class)
				.isThrownBy(() -> lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true))
				.withCauseExactlyInstanceOf(NoUniqueBeanDefinitionException.class)
				.withMessageContaining("5");
	}

	@Test
	void autowireBeanByTypePrimaryTakesPrecedenceOverPriority() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);

		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		assertThat(bean.getSpouse()).isEqualTo(lbf.getBean("spouse"));
	}

	@Test
	void beanProviderWithParentBeanFactoryDetectsOrder() {
		DefaultListableBeanFactory parentBf = new DefaultListableBeanFactory();
		parentBf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		parentBf.registerBeanDefinition("regular", new RootBeanDefinition(TestBean.class));
		parentBf.registerBeanDefinition("test", new RootBeanDefinition(HighPriorityTestBean.class));
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		lbf.setParentBeanFactory(parentBf);
		lbf.registerBeanDefinition("low", new RootBeanDefinition(LowPriorityTestBean.class));

		Stream<Class<?>> orderedTypes = lbf.getBeanProvider(TestBean.class).orderedStream().map(Object::getClass);
		assertThat(orderedTypes).containsExactly(HighPriorityTestBean.class, LowPriorityTestBean.class, TestBean.class);
	}

	@Test  // gh-28374
	void beanProviderWithParentBeanFactoryAndMixedOrder() {
		DefaultListableBeanFactory parentBf = new DefaultListableBeanFactory();
		parentBf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		lbf.setParentBeanFactory(parentBf);

		lbf.registerSingleton("plainTestBean", new TestBean());

		RootBeanDefinition bd1 = new RootBeanDefinition(PriorityTestBeanFactory.class);
		bd1.setFactoryMethodName("lowPriorityTestBean");
		lbf.registerBeanDefinition("lowPriorityTestBean", bd1);

		RootBeanDefinition bd2 = new RootBeanDefinition(PriorityTestBeanFactory.class);
		bd2.setFactoryMethodName("highPriorityTestBean");
		parentBf.registerBeanDefinition("highPriorityTestBean", bd2);

		ObjectProvider<TestBean> testBeanProvider = lbf.getBeanProvider(ResolvableType.forClass(TestBean.class));
		List<TestBean> resolved = testBeanProvider.orderedStream().toList();
		assertThat(resolved).containsExactly(
				lbf.getBean("highPriorityTestBean", TestBean.class),
				lbf.getBean("lowPriorityTestBean", TestBean.class),
				lbf.getBean("plainTestBean", TestBean.class));
	}

	@Test
	void autowireExistingBeanByName() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spouse", bd);
		DependenciesBean existingBean = new DependenciesBean();
		lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);

		TestBean spouse = (TestBean) lbf.getBean("spouse");
		assertThat(spouse).isEqualTo(existingBean.getSpouse());
		assertThat(BeanFactoryUtils.beanOfType(lbf, TestBean.class)).isSameAs(spouse);
	}

	@Test
	void autowireExistingBeanByNameWithDependencyCheck() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spous", bd);
		DependenciesBean existingBean = new DependenciesBean();

		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true));
	}

	@Test
	void autowireExistingBeanByNameWithNoDependencyCheck() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spous", bd);
		DependenciesBean existingBean = new DependenciesBean();
		lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);

		assertThat(existingBean.getSpouse()).isNull();
	}

	@Test
	void autowireExistingBeanByType() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("test", bd);
		DependenciesBean existingBean = new DependenciesBean();
		lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);

		TestBean test = (TestBean) lbf.getBean("test");
		assertThat(test).isEqualTo(existingBean.getSpouse());
	}

	@Test
	void autowireExistingBeanByTypeWithDependencyCheck() {
		DependenciesBean existingBean = new DependenciesBean();
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true));
	}

	@Test
	void autowireExistingBeanByTypeWithNoDependencyCheck() {
		DependenciesBean existingBean = new DependenciesBean();
		lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		assertThat(existingBean.getSpouse()).isNull();
	}

	@Test
	void invalidAutowireMode() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				lbf.autowireBeanProperties(new TestBean(), AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false));
	}

	@Test
	void applyBeanPropertyValues() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("age", "99");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("test", bd);
		TestBean tb = new TestBean();
		assertThat(tb.getAge()).isEqualTo(0);
		lbf.applyBeanPropertyValues(tb, "test");
		assertThat(tb.getAge()).isEqualTo(99);
	}

	@Test
	void applyBeanPropertyValuesWithIncompleteDefinition() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("age", "99");
		RootBeanDefinition bd = new RootBeanDefinition();
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("test", bd);
		TestBean tb = new TestBean();
		assertThat(tb.getAge()).isEqualTo(0);
		lbf.applyBeanPropertyValues(tb, "test");
		assertThat(tb.getAge()).isEqualTo(99);
		assertThat(tb.getBeanFactory()).isNull();
		assertThat(tb.getSpouse()).isNull();
	}

	@Test
	void createBean() {
		TestBean tb = lbf.createBean(TestBean.class);
		assertThat(tb.getBeanFactory()).isSameAs(lbf);
		lbf.destroyBean(tb);
	}

	@Test
	void createBeanWithDisposableBean() {
		DerivedTestBean tb = lbf.createBean(DerivedTestBean.class);
		assertThat(tb.getBeanFactory()).isSameAs(lbf);
		lbf.destroyBean(tb);
		assertThat(tb.wasDestroyed()).isTrue();
	}

	@Test
	void createBeanWithNonDefaultConstructor() {
		lbf.registerBeanDefinition("otherTestBean", new RootBeanDefinition(TestBean.class));
		TestBeanRecipient tb = lbf.createBean(TestBeanRecipient.class);
		assertThat(lbf.containsSingleton("otherTestBean")).isTrue();
		assertThat(tb.testBean).isEqualTo(lbf.getBean("otherTestBean"));
		lbf.destroyBean(tb);
	}

	@Test
	void createBeanWithPreferredDefaultConstructor() {
		lbf.registerBeanDefinition("otherTestBean", new RootBeanDefinition(TestBean.class));
		TestBean tb = lbf.createBean(TestBean.class);
		assertThat(lbf.containsSingleton("otherTestBean")).isFalse();
		lbf.destroyBean(tb);
	}

	@Test
	void configureBean() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("age", "99");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("test", bd);

		TestBean tb = new TestBean();
		assertThat(tb.getAge()).isEqualTo(0);
		lbf.configureBean(tb, "test");
		assertThat(tb.getAge()).isEqualTo(99);
		assertThat(tb.getBeanFactory()).isSameAs(lbf);
		assertThat(tb.getSpouse()).isNull();
	}

	@Test
	void configureBeanWithAutowiring() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spouse", bd);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("age", "99");
		RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
		tbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_NAME);
		lbf.registerBeanDefinition("test", tbd);

		TestBean tb = new TestBean();
		lbf.configureBean(tb, "test");
		assertThat(tb.getBeanFactory()).isSameAs(lbf);
		TestBean spouse = (TestBean) lbf.getBean("spouse");
		assertThat(tb.getSpouse()).isEqualTo(spouse);
	}

	@Test
	void extensiveCircularReference() {
		for (int i = 0; i < 1000; i++) {
			MutablePropertyValues pvs = new MutablePropertyValues();
			pvs.addPropertyValue(new PropertyValue("spouse", new RuntimeBeanReference("bean" + (i < 99 ? i + 1 : 0))));
			RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
			bd.setPropertyValues(pvs);
			lbf.registerBeanDefinition("bean" + i, bd);
		}
		lbf.preInstantiateSingletons();

		for (int i = 0; i < 1000; i++) {
			TestBean bean = (TestBean) lbf.getBean("bean" + i);
			TestBean otherBean = (TestBean) lbf.getBean("bean" + (i < 99 ? i + 1 : 0));
			assertThat(bean.getSpouse()).isSameAs(otherBean);
		}
	}

	@Test
	void circularReferenceThroughAutowiring() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(lbf::preInstantiateSingletons);
	}

	@Test
	void circularReferenceThroughFactoryBeanAutowiring() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyFactoryBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(lbf::preInstantiateSingletons);
	}

	@Test
	void circularReferenceThroughFactoryBeanTypeCheck() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyFactoryBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() -> lbf.getBeansOfType(String.class));
	}

	@Test
	void avoidCircularReferenceThroughAutowiring() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyFactoryBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);
		RootBeanDefinition bd2 = new RootBeanDefinition(String.class);
		bd2.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("string", bd2);
		lbf.preInstantiateSingletons();
	}

	@Test
	void constructorDependencyWithClassResolution() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyWithClassResolution.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("java.lang.String");
		lbf.registerBeanDefinition("test", bd);
		lbf.preInstantiateSingletons();
	}

	@Test
	void constructorDependencyWithUnresolvableClass() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyWithClassResolution.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("java.lang.Strin");
		lbf.registerBeanDefinition("test", bd);

		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(lbf::preInstantiateSingletons);
	}

	@Test
	void beanDefinitionWithInterface() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(ITestBean.class));

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> lbf.getBean("test"))
				.withMessageContaining("interface")
				.satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("test"));
	}

	@Test
	void beanDefinitionWithAbstractClass() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(AbstractBeanFactory.class));

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> lbf.getBean("test"))
				.withMessageContaining("abstract")
				.satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("test"));
	}

	@Test
	void prototypeFactoryBeanNotEagerlyCalled() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class));
		assertThatNoException().isThrownBy(lbf::preInstantiateSingletons);
	}

	@Test
	void prototypeFactoryBeanNotEagerlyCalledInCaseOfBeanClassName() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class.getName(), null, null));
		assertThatNoException().isThrownBy(lbf::preInstantiateSingletons);
	}

	@Test
	void lazyInitFlag() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setLazyInit(true);
		factory.registerBeanDefinition("tb1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setLazyInit(false);
		factory.registerBeanDefinition("tb2", bd2);
		factory.registerBeanDefinition("tb3", new RootBeanDefinition(TestBean.class));

		assertThat(((AbstractBeanDefinition) factory.getMergedBeanDefinition("tb1")).getLazyInit()).isTrue();
		assertThat(((AbstractBeanDefinition) factory.getMergedBeanDefinition("tb2")).getLazyInit()).isFalse();
		assertThat(((AbstractBeanDefinition) factory.getMergedBeanDefinition("tb3")).getLazyInit()).isNull();

		factory.preInstantiateSingletons();
		assertThat(factory.containsSingleton("tb1")).isFalse();
		assertThat(factory.containsSingleton("tb2")).isTrue();
		assertThat(factory.containsSingleton("tb3")).isTrue();
	}

	@Test
	void lazyInitFactory() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(LazyInitFactory.class));
		lbf.preInstantiateSingletons();

		LazyInitFactory factory = (LazyInitFactory) lbf.getBean("&test");
		assertThat(factory.initialized).isFalse();
	}

	@Test
	void smartInitFactory() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(EagerInitFactory.class));
		lbf.preInstantiateSingletons();

		EagerInitFactory factory = (EagerInitFactory) lbf.getBean("&test");
		assertThat(factory.initialized).isTrue();
	}

	@Test
	void prototypeStringCreatedRepeatedly() {
		RootBeanDefinition stringDef = new RootBeanDefinition(String.class);
		stringDef.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		stringDef.getConstructorArgumentValues().addGenericArgumentValue(new TypedStringValue("value"));
		lbf.registerBeanDefinition("string", stringDef);

		String val1 = lbf.getBean("string", String.class);
		String val2 = lbf.getBean("string", String.class);
		assertThat(val1).isEqualTo("value");
		assertThat(val2).isEqualTo("value");
		assertThat(val2).isNotSameAs(val1);
	}

	@Test
	void prototypeWithArrayConversionForConstructor() {
		List<String> list = ManagedList.of("myName", "myBeanName");
		RootBeanDefinition bd = new RootBeanDefinition(DerivedTestBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bd.getConstructorArgumentValues().addGenericArgumentValue(list);
		lbf.registerBeanDefinition("test", bd);

		DerivedTestBean tb = (DerivedTestBean) lbf.getBean("test");
		assertThat(tb.getName()).isEqualTo("myName");
		assertThat(tb.getBeanName()).isEqualTo("myBeanName");

		DerivedTestBean tb2 = (DerivedTestBean) lbf.getBean("test");
		assertThat(tb).isNotSameAs(tb2);
		assertThat(tb2.getName()).isEqualTo("myName");
		assertThat(tb2.getBeanName()).isEqualTo("myBeanName");
	}

	@Test
	void prototypeWithArrayConversionForFactoryMethod() {
		List<String> list = ManagedList.of("myName", "myBeanName");
		RootBeanDefinition bd = new RootBeanDefinition(DerivedTestBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bd.setFactoryMethodName("create");
		bd.getConstructorArgumentValues().addGenericArgumentValue(list);
		lbf.registerBeanDefinition("test", bd);

		DerivedTestBean tb = (DerivedTestBean) lbf.getBean("test");
		assertThat(tb.getName()).isEqualTo("myName");
		assertThat(tb.getBeanName()).isEqualTo("myBeanName");

		DerivedTestBean tb2 = (DerivedTestBean) lbf.getBean("test");
		assertThat(tb).isNotSameAs(tb2);
		assertThat(tb2.getName()).isEqualTo("myName");
		assertThat(tb2.getBeanName()).isEqualTo("myBeanName");
	}

	@Test
	void multipleInitAndDestroyMethods() {
		RootBeanDefinition bd = new RootBeanDefinition(BeanWithInitAndDestroyMethods.class);
		bd.setInitMethodNames("init1", "init2");
		bd.setDestroyMethodNames("destroy2", "destroy1");
		lbf.registerBeanDefinition("test", bd);

		BeanWithInitAndDestroyMethods bean = lbf.getBean("test", BeanWithInitAndDestroyMethods.class);
		assertThat(bean.initMethods).containsExactly("init", "init1", "init2");
		assertThat(bean.destroyMethods).isEmpty();
		lbf.destroySingletons();
		assertThat(bean.destroyMethods).containsExactly("destroy", "destroy2", "destroy1");
	}

	@Test
	void beanPostProcessorWithWrappedObjectAndDisposableBean() {
		RootBeanDefinition bd = new RootBeanDefinition(BeanWithDisposableBean.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return new TestBean();
			}
		});

		BeanWithDisposableBean.closed = false;
		lbf.preInstantiateSingletons();
		lbf.destroySingletons();
		assertThat(BeanWithDisposableBean.closed).as("Destroy method invoked").isTrue();
	}

	@Test
	void beanPostProcessorWithWrappedObjectAndCloseable() {
		RootBeanDefinition bd = new RootBeanDefinition(BeanWithCloseable.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return new TestBean();
			}
		});

		BeanWithDisposableBean.closed = false;
		lbf.preInstantiateSingletons();
		lbf.destroySingletons();
		assertThat(BeanWithCloseable.closed).as("Destroy method invoked").isTrue();
	}

	@Test
	void beanPostProcessorWithWrappedObjectAndDestroyMethod() {
		RootBeanDefinition bd = new RootBeanDefinition(BeanWithDestroyMethod.class);
		bd.setDestroyMethodName("close");
		lbf.registerBeanDefinition("test", bd);
		lbf.addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return new TestBean();
			}
		});

		BeanWithDestroyMethod.closeCount = 0;
		lbf.preInstantiateSingletons();
		lbf.destroySingletons();
		assertThat(BeanWithDestroyMethod.closeCount).as("Destroy methods invoked").isEqualTo(1);
	}

	@Test
	void destroyMethodOnInnerBean() {
		RootBeanDefinition innerBd = new RootBeanDefinition(BeanWithDestroyMethod.class);
		innerBd.setDestroyMethodName("close");
		RootBeanDefinition bd = new RootBeanDefinition(BeanWithDestroyMethod.class);
		bd.setDestroyMethodName("close");
		bd.getPropertyValues().add("inner", innerBd);
		lbf.registerBeanDefinition("test", bd);
		BeanWithDestroyMethod.closeCount = 0;
		lbf.preInstantiateSingletons();
		lbf.destroySingletons();
		assertThat(BeanWithDestroyMethod.closeCount).as("Destroy methods invoked").isEqualTo(2);
	}

	@Test
	void destroyMethodOnInnerBeanAsPrototype() {
		RootBeanDefinition innerBd = new RootBeanDefinition(BeanWithDestroyMethod.class);
		innerBd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		innerBd.setDestroyMethodName("close");
		RootBeanDefinition bd = new RootBeanDefinition(BeanWithDestroyMethod.class);
		bd.setDestroyMethodName("close");
		bd.getPropertyValues().add("inner", innerBd);
		lbf.registerBeanDefinition("test", bd);
		BeanWithDestroyMethod.closeCount = 0;
		lbf.preInstantiateSingletons();
		lbf.destroySingletons();
		assertThat(BeanWithDestroyMethod.closeCount).as("Destroy methods invoked").isEqualTo(1);
	}

	@Test
	void findTypeOfSingletonFactoryMethodOnBeanInstance() {
		findTypeOfPrototypeFactoryMethodOnBeanInstance(true);
	}

	@Test
	void findTypeOfPrototypeFactoryMethodOnBeanInstance() {
		findTypeOfPrototypeFactoryMethodOnBeanInstance(false);
	}

	/**
	 * @param singleton whether the bean created from the factory method on
	 * the bean instance should be a singleton or prototype. This flag is
	 * used to allow checking of the new ability in 1.2.4 to determine the type
	 * of a prototype created from invoking a factory method on a bean instance
	 * in the factory.
	 */
	private void findTypeOfPrototypeFactoryMethodOnBeanInstance(boolean singleton) {
		String expectedNameFromProperties = "tony";
		String expectedNameFromArgs = "gordon";

		RootBeanDefinition instanceFactoryDefinition = new RootBeanDefinition(BeanWithFactoryMethod.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", expectedNameFromProperties);
		instanceFactoryDefinition.setPropertyValues(pvs);
		lbf.registerBeanDefinition("factoryBeanInstance", instanceFactoryDefinition);

		RootBeanDefinition factoryMethodDefinitionWithProperties = new RootBeanDefinition();
		factoryMethodDefinitionWithProperties.setFactoryBeanName("factoryBeanInstance");
		factoryMethodDefinitionWithProperties.setFactoryMethodName("create");
		if (!singleton) {
			factoryMethodDefinitionWithProperties.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		}
		lbf.registerBeanDefinition("fmWithProperties", factoryMethodDefinitionWithProperties);

		RootBeanDefinition factoryMethodDefinitionGeneric = new RootBeanDefinition();
		factoryMethodDefinitionGeneric.setFactoryBeanName("factoryBeanInstance");
		factoryMethodDefinitionGeneric.setFactoryMethodName("createGeneric");
		if (!singleton) {
			factoryMethodDefinitionGeneric.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		}
		lbf.registerBeanDefinition("fmGeneric", factoryMethodDefinitionGeneric);

		RootBeanDefinition factoryMethodDefinitionWithArgs = new RootBeanDefinition();
		factoryMethodDefinitionWithArgs.setFactoryBeanName("factoryBeanInstance");
		factoryMethodDefinitionWithArgs.setFactoryMethodName("createWithArgs");
		ConstructorArgumentValues cvals = new ConstructorArgumentValues();
		cvals.addGenericArgumentValue(expectedNameFromArgs);
		factoryMethodDefinitionWithArgs.setConstructorArgumentValues(cvals);
		if (!singleton) {
			factoryMethodDefinitionWithArgs.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		}
		lbf.registerBeanDefinition("fmWithArgs", factoryMethodDefinitionWithArgs);

		assertThat(lbf.getBeanDefinitionCount()).isEqualTo(4);
		assertBeanNamesForType(TestBean.class, true, true, "fmWithProperties", "fmWithArgs");

		TestBean tb = (TestBean) lbf.getBean("fmWithProperties");
		TestBean second = (TestBean) lbf.getBean("fmWithProperties");
		if (singleton) {
			assertThat(second).isSameAs(tb);
		}
		else {
			assertThat(second).isNotSameAs(tb);
		}
		assertThat(tb.getName()).isEqualTo(expectedNameFromProperties);

		tb = (TestBean) lbf.getBean("fmGeneric");
		second = (TestBean) lbf.getBean("fmGeneric");
		if (singleton) {
			assertThat(second).isSameAs(tb);
		}
		else {
			assertThat(second).isNotSameAs(tb);
		}
		assertThat(tb.getName()).isEqualTo(expectedNameFromProperties);

		TestBean tb2 = (TestBean) lbf.getBean("fmWithArgs");
		second = (TestBean) lbf.getBean("fmWithArgs");
		if (singleton) {
			assertThat(second).isSameAs(tb2);
		}
		else {
			assertThat(second).isNotSameAs(tb2);
		}
		assertThat(tb2.getName()).isEqualTo(expectedNameFromArgs);
	}

	@Test
	void scopingBeanToUnregisteredScopeResultsInAnException() {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
		beanDefinition.setScope("he put himself so low could hardly look me in the face");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("testBean", beanDefinition);
		assertThatIllegalStateException().isThrownBy(() ->
				factory.getBean("testBean"));
	}

	@Test
	void explicitScopeInheritanceForChildBeanDefinitions() {
		String theChildScope = "bonanza!";

		RootBeanDefinition parent = new RootBeanDefinition();
		parent.setScope(BeanDefinition.SCOPE_PROTOTYPE);

		AbstractBeanDefinition child = BeanDefinitionBuilder.childBeanDefinition("parent").getBeanDefinition();
		child.setBeanClass(TestBean.class);
		child.setScope(theChildScope);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("parent", parent);
		factory.registerBeanDefinition("child", child);

		AbstractBeanDefinition def = (AbstractBeanDefinition) factory.getBeanDefinition("child");
		assertThat(def.getScope()).as("Child 'scope' not overriding parent scope (it must).").isEqualTo(theChildScope);
	}

	@Test
	void scopeInheritanceForChildBeanDefinitions() {
		String theParentScope = "bonanza!";

		RootBeanDefinition parent = new RootBeanDefinition();
		parent.setScope(theParentScope);

		AbstractBeanDefinition child = new ChildBeanDefinition("parent");
		child.setBeanClass(TestBean.class);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("parent", parent);
		factory.registerBeanDefinition("child", child);

		BeanDefinition def = factory.getMergedBeanDefinition("child");
		assertThat(def.getScope()).as("Child 'scope' not inherited").isEqualTo(theParentScope);
	}

	@Test
	void fieldSettingWithInstantiationAwarePostProcessorNoShortCircuit() {
		doTestFieldSettingWithInstantiationAwarePostProcessor(false);
	}

	@Test
	void fieldSettingWithInstantiationAwarePostProcessorWithShortCircuit() {
		doTestFieldSettingWithInstantiationAwarePostProcessor(true);
	}

	private void doTestFieldSettingWithInstantiationAwarePostProcessor(final boolean skipPropertyPopulation) {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		int ageSetByPropertyValue = 27;
		bd.getPropertyValues().addPropertyValue(new PropertyValue("age", ageSetByPropertyValue));
		lbf.registerBeanDefinition("test", bd);
		final String nameSetOnField = "nameSetOnField";
		lbf.addBeanPostProcessor(new InstantiationAwareBeanPostProcessor() {
			@Override
			public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
				TestBean tb = (TestBean) bean;
				try {
					Field f = TestBean.class.getDeclaredField("name");
					f.setAccessible(true);
					f.set(tb, nameSetOnField);
					return !skipPropertyPopulation;
				}
				catch (Exception ex) {
					throw new AssertionError("Unexpected exception", ex);
				}
			}
		});
		lbf.preInstantiateSingletons();

		TestBean tb = (TestBean) lbf.getBean("test");
		assertThat(tb.getName()).as("Name was set on field by IAPP").isEqualTo(nameSetOnField);
		if (!skipPropertyPopulation) {
			assertThat(tb.getAge()).as("Property value still set").isEqualTo(ageSetByPropertyValue);
		}
		else {
			assertThat(tb.getAge()).as("Property value was NOT set and still has default value").isEqualTo(0);
		}
	}

	@Test
	void containsBeanReturnsTrueEvenForAbstractBeanDefinition() {
		lbf.registerBeanDefinition("abs", BeanDefinitionBuilder
				.rootBeanDefinition(TestBean.class).setAbstract(true).getBeanDefinition());
		assertThat(lbf.containsBean("abs")).isTrue();
		assertThat(lbf.containsBean("bogus")).isFalse();
	}

	@Test
	void resolveEmbeddedValue() {
		StringValueResolver r1 = mock();
		StringValueResolver r2 = mock();
		StringValueResolver r3 = mock();
		lbf.addEmbeddedValueResolver(r1);
		lbf.addEmbeddedValueResolver(r2);
		lbf.addEmbeddedValueResolver(r3);
		given(r1.resolveStringValue("A")).willReturn("B");
		given(r2.resolveStringValue("B")).willReturn(null);
		given(r3.resolveStringValue(isNull())).willThrow(new IllegalArgumentException());

		lbf.resolveEmbeddedValue("A");

		verify(r1).resolveStringValue("A");
		verify(r2).resolveStringValue("B");
		verify(r3, never()).resolveStringValue(isNull());
	}

	@Test
	void populatedJavaUtilOptionalBean() {
		RootBeanDefinition bd = new RootBeanDefinition(Optional.class);
		bd.setFactoryMethodName("of");
		bd.getConstructorArgumentValues().addGenericArgumentValue("CONTENT");
		lbf.registerBeanDefinition("optionalBean", bd);

		assertThat((Optional<?>) lbf.getBean(Optional.class)).isEqualTo(Optional.of("CONTENT"));
	}

	@Test
	void emptyJavaUtilOptionalBean() {
		RootBeanDefinition bd = new RootBeanDefinition(Optional.class);
		bd.setFactoryMethodName("empty");
		lbf.registerBeanDefinition("optionalBean", bd);

		assertThat((Optional<?>) lbf.getBean(Optional.class)).isEmpty();
	}

	@Test
	void nonPublicEnum() {
		RootBeanDefinition bd = new RootBeanDefinition(NonPublicEnumHolder.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("VALUE_1");
		lbf.registerBeanDefinition("holderBean", bd);

		NonPublicEnumHolder holder = (NonPublicEnumHolder) lbf.getBean("holderBean");
		assertThat(holder.getNonPublicEnum()).isEqualTo(NonPublicEnum.VALUE_1);
	}


	private int registerBeanDefinitions(Properties p) {
		return registerBeanDefinitions(p, null);
	}

	@SuppressWarnings("deprecation")
	private int registerBeanDefinitions(Properties p, @Nullable String prefix) {
		for (String beanName : lbf.getBeanDefinitionNames()) {
			lbf.removeBeanDefinition(beanName);
		}
		return (new org.springframework.beans.factory.support.PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p, prefix);
	}

	private void assertBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit, String... names) {
		if (names.length == 0) {
			assertThat(lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit))
				.as("bean names for type " + type.getName())
				.isEmpty();
		}
		else {
			assertThat(lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit))
				.as("bean names for type " + type.getName())
				.containsExactly(names);
		}
	}


	public static class NoDependencies {

		private NoDependencies() {
		}
	}


	public static class ConstructorDependency implements BeanNameAware {

		public TestBean spouse;

		public int spouseAge;

		private String beanName;

		public ConstructorDependency(TestBean spouse) {
			this.spouse = spouse;
		}

		public ConstructorDependency(int spouseAge) {
			this.spouseAge = spouseAge;
		}

		@SuppressWarnings("unused")
		private ConstructorDependency(TestBean spouse, TestBean otherSpouse) {
			throw new IllegalArgumentException("Should never be called");
		}

		@Override
		public void setBeanName(String name) {
			this.beanName = name;
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ConstructorDependency that = (ConstructorDependency) o;
			return spouseAge == that.spouseAge &&
					Objects.equals(spouse, that.spouse) &&
					Objects.equals(beanName, that.beanName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(spouse, spouseAge, beanName);
		}
	}


	public static class UnsatisfiedConstructorDependency {

		public UnsatisfiedConstructorDependency(TestBean t, SideEffectBean b) {
		}
	}


	public static class ConstructorDependencyBean {

		public ConstructorDependencyBean(ConstructorDependencyBean dependency) {
		}
	}


	public static class ConstructorDependencyFactoryBean implements FactoryBean<Object> {

		public ConstructorDependencyFactoryBean(String dependency) {
		}

		@Override
		public Object getObject() {
			return "test";
		}

		@Override
		public Class<?> getObjectType() {
			return String.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}


	public static class ConstructorDependencyWithClassResolution {

		public ConstructorDependencyWithClassResolution(Class<?> clazz) {
		}

		public ConstructorDependencyWithClassResolution() {
		}
	}


	static class BeanWithInitAndDestroyMethods implements InitializingBean, DisposableBean {

		final List<String> initMethods = new ArrayList<>();
		final List<String> destroyMethods = new ArrayList<>();

		@Override
		public void afterPropertiesSet() {
			initMethods.add("init");
		}

		void init1() {
			initMethods.add("init1");
		}

		void init2() {
			initMethods.add("init2");
		}

		@Override
		public void destroy() {
			destroyMethods.add("destroy");
		}

		void destroy1() {
			destroyMethods.add("destroy1");
		}

		void destroy2() {
			destroyMethods.add("destroy2");
		}
	}


	public static class BeanWithDisposableBean implements DisposableBean {

		static boolean closed;

		@Override
		public void destroy() {
			closed = true;
		}
	}


	public static class BeanWithCloseable implements Closeable {

		static boolean closed;

		@Override
		public void close() {
			closed = true;
		}
	}


	public abstract static class BaseClassWithDestroyMethod {

		public abstract BaseClassWithDestroyMethod close();
	}


	public static class BeanWithDestroyMethod extends BaseClassWithDestroyMethod {

		static int closeCount = 0;

		@SuppressWarnings("unused")
		private BeanWithDestroyMethod inner;

		public void setInner(BeanWithDestroyMethod inner) {
			this.inner = inner;
		}

		@Override
		public BeanWithDestroyMethod close() {
			closeCount++;
			return this;
		}
	}


	public static class BeanWithFactoryMethod {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		public TestBean create() {
			TestBean tb = new TestBean();
			tb.setName(this.name);
			return tb;
		}

		public TestBean create(String name, int age) {
			return new TestBean(name, age);
		}

		public TestBean createWithArgs(String arg) {
			TestBean tb = new TestBean();
			tb.setName(arg);
			return tb;
		}

		public Object createGeneric() {
			return create();
		}
	}


	public interface Repository<T, ID extends Serializable> {
	}


	public interface RepositoryFactoryInformation<T, ID extends Serializable> {
	}


	public abstract static class RepositoryFactoryBeanSupport<T extends Repository<S, ID>, S, ID extends Serializable>
			implements RepositoryFactoryInformation<S, ID>, FactoryBean<T> {
	}


	public static class FactoryBeanThatShouldntBeCalled<T extends Repository<S, ID>, S, ID extends Serializable>
			extends RepositoryFactoryBeanSupport<T, S, ID> implements Runnable, Callable<T> {

		static boolean instantiated = false;

		{
			instantiated = true;
		}

		@Override
		public T getObject() {
			throw new IllegalStateException();
		}

		@Override
		public Class<?> getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}

		@Override
		public void run() {
			throw new IllegalStateException();
		}

		@Override
		public T call() {
			throw new IllegalStateException();
		}
	}


	public static class TestRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
			extends RepositoryFactoryBeanSupport<T, S, ID> {

		@Override
		public T getObject() {
			throw new IllegalArgumentException("Should not be called");
		}

		@Override
		public Class<?> getObjectType() {
			throw new IllegalArgumentException("Should not be called");
		}
	}


	public record City(String name) {}

	public static class CityRepository implements Repository<City, Long> {}


	public static class LazyInitFactory implements FactoryBean<Object> {

		public boolean initialized = false;

		@Override
		public Object getObject() {
			this.initialized = true;
			return "";
		}

		@Override
		public Class<?> getObjectType() {
			return String.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}


	public static class EagerInitFactory implements SmartFactoryBean<Object> {

		public boolean initialized = false;

		@Override
		public Object getObject() {
			this.initialized = true;
			return "";
		}

		@Override
		public Class<?> getObjectType() {
			return String.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public boolean isPrototype() {
			return false;
		}

		@Override
		public boolean isEagerInit() {
			return true;
		}
	}


	public static class TestBeanFactory {

		public static boolean initialized = false;

		public TestBeanFactory() {
			initialized = true;
		}

		public static TestBean createTestBean() {
			return new TestBean();
		}

		public TestBean createTestBeanNonStatic() {
			return new TestBean();
		}
	}


	public static class ArrayBean {

		private Integer[] integerArray;

		private Resource[] resourceArray;

		public ArrayBean() {
		}

		public ArrayBean(Integer[] integerArray) {
			this.integerArray = integerArray;
		}

		public ArrayBean(Integer[] integerArray, Resource[] resourceArray) {
			this.integerArray = integerArray;
			this.resourceArray = resourceArray;
		}

		public Integer[] getIntegerArray() {
			return this.integerArray;
		}

		public void setResourceArray(Resource[] resourceArray) {
			this.resourceArray = resourceArray;
		}

		public Resource[] getResourceArray() {
			return this.resourceArray;
		}
	}


	public static class SetterOverload {

		public String value;

		public void setObject(Integer length) {
			this.value = length + "i";
		}

		public void setObject(String object) {
			this.value = object;
		}

		public String getObject() {
			return this.value;
		}

		public void setValue(Duration duration) {
			this.value = duration.getSeconds() + "s";
		}

		public void setValue(int length) {
			this.value = length + "i";
		}
	}


	/**
	 * Bean with a dependency on a {@link FactoryBean}.
	 */
	@SuppressWarnings("unused")
	private static class FactoryBeanDependentBean {

		private FactoryBean<?> factoryBean;

		public final FactoryBean<?> getFactoryBean() {
			return this.factoryBean;
		}

		public final void setFactoryBean(final FactoryBean<?> factoryBean) {
			this.factoryBean = factoryBean;
		}
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static class CustomTypeConverter implements TypeConverter {

		private final NumberFormat numberFormat;

		public CustomTypeConverter(NumberFormat numberFormat) {
			this.numberFormat = numberFormat;
		}

		@Override
		public Object convertIfNecessary(Object value, @Nullable Class requiredType) {
			if (value instanceof String text && Float.class.isAssignableFrom(requiredType)) {
				try {
					return this.numberFormat.parse(text).floatValue();
				}
				catch (ParseException ex) {
					throw new TypeMismatchException(value, requiredType, ex);
				}
			}
			else if (value instanceof String && int.class.isAssignableFrom(requiredType)) {
				return 5;
			}
			else {
				return value;
			}
		}

		@Override
		public Object convertIfNecessary(Object value, @Nullable Class requiredType, @Nullable MethodParameter methodParam) {
			return convertIfNecessary(value, requiredType);
		}

		@Override
		public Object convertIfNecessary(Object value, @Nullable Class requiredType, @Nullable Field field) {
			return convertIfNecessary(value, requiredType);
		}
	}


	@SuppressWarnings("unused")
	private static class KnowsIfInstantiated {

		private static boolean instantiated;

		public static void clearInstantiationRecord() {
			instantiated = false;
		}

		public static boolean wasInstantiated() {
			return instantiated;
		}

		public KnowsIfInstantiated() {
			instantiated = true;
		}
	}


	@Priority(5)
	private static class HighPriorityTestBean extends TestBean {
	}


	@Priority(500)
	private static class LowPriorityTestBean extends TestBean {
	}


	static class PriorityTestBeanFactory {

		public static LowPriorityTestBean lowPriorityTestBean() {
			return new LowPriorityTestBean();
		}

		public static HighPriorityTestBean highPriorityTestBean() {
			return new HighPriorityTestBean();
		}
	}


	private static class NullTestBeanFactoryBean<T> implements FactoryBean<TestBean> {

		@Override
		public TestBean getObject() {
			return null;
		}

		@Override
		public Class<?> getObjectType() {
			return TestBean.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}


	private static class TestBeanRecipient {

		public TestBean testBean;

		@SuppressWarnings("unused")
		public TestBeanRecipient(TestBean testBean) {
			this.testBean = testBean;
		}
	}


	enum NonPublicEnum {

		VALUE_1, VALUE_2
	}


	static class NonPublicEnumHolder {

		final NonPublicEnum nonPublicEnum;

		public NonPublicEnumHolder(NonPublicEnum nonPublicEnum) {
			this.nonPublicEnum = nonPublicEnum;
		}

		public NonPublicEnum getNonPublicEnum() {
			return nonPublicEnum;
		}
	}


	@Order
	private static class LowestPrecedenceTestBeanFactoryBean implements FactoryBean<TestBean> {

		@Override
		public TestBean getObject() {
			return new TestBean("fromLowestPrecedenceTestBeanFactoryBean");
		}

		@Override
		public Class<?> getObjectType() {
			return TestBean.class;
		}
	}


	@Order(Ordered.HIGHEST_PRECEDENCE)
	private static class HighestPrecedenceTestBeanFactoryBean implements FactoryBean<TestBean> {

		@Override
		public TestBean getObject() {
			return new TestBean("fromHighestPrecedenceTestBeanFactoryBean");
		}

		@Override
		public Class<?> getObjectType() {
			return TestBean.class;
		}
	}

}

/*
 * Copyright 2002-2020 the original author or authors.
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
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
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

import javax.annotation.Priority;
import javax.security.auth.Subject;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
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
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
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

	private DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();


	@Test
	void unreferencedSingletonWasInstantiated() {
		KnowsIfInstantiated.clearInstantiationRecord();
		Properties p = new Properties();
		p.setProperty("x1.(class)", KnowsIfInstantiated.class.getName());
		assertThat(!KnowsIfInstantiated.wasInstantiated()).as("singleton not instantiated").isTrue();
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
		assertThat(!KnowsIfInstantiated.wasInstantiated()).as("singleton not instantiated").isTrue();
		registerBeanDefinitions(p);
		assertThat(!KnowsIfInstantiated.wasInstantiated()).as("singleton not instantiated").isTrue();
		lbf.preInstantiateSingletons();

		assertThat(!KnowsIfInstantiated.wasInstantiated()).as("singleton not instantiated").isTrue();
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
		assertThat(!DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isTrue();
		registerBeanDefinitions(p);
		assertThat(!DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isTrue();
		assertThat(lbf.getType("x1")).isEqualTo(TestBean.class);
		lbf.preInstantiateSingletons();

		assertThat(!DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isTrue();
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

		assertThat(!DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isTrue();
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertThat(beanNames.length).isEqualTo(0);
		beanNames = lbf.getBeanNamesForAnnotation(SuppressWarnings.class);
		assertThat(beanNames.length).isEqualTo(0);

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
		assertThat(!DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isTrue();
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

		assertThat(!DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isTrue();
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertThat(beanNames.length).isEqualTo(0);
		beanNames = lbf.getBeanNamesForAnnotation(SuppressWarnings.class);
		assertThat(beanNames.length).isEqualTo(0);

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
		assertThat(!DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isTrue();
	}

	@Test
	void nonInitializedFactoryBeanIgnoredByNonEagerTypeMatching() {
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.singleton", "false");
		registerBeanDefinitions(p);

		assertThat(!DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isTrue();
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertThat(beanNames.length).isEqualTo(0);
		beanNames = lbf.getBeanNamesForAnnotation(SuppressWarnings.class);
		assertThat(beanNames.length).isEqualTo(0);

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
		assertThat(!DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isTrue();
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

		assertThat(!DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isTrue();
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("x1");
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
		assertThat(!DummyFactory.wasPrototypeCreated()).as("prototype not instantiated").isTrue();

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
		assertThat(lbf.getAliases("x1").length).isEqualTo(1);
		assertThat(lbf.getAliases("x1")[0]).isEqualTo("x2");
		assertThat(lbf.getAliases("&x1").length).isEqualTo(1);
		assertThat(lbf.getAliases("&x1")[0]).isEqualTo("&x2");
		assertThat(lbf.getAliases("x2").length).isEqualTo(1);
		assertThat(lbf.getAliases("x2")[0]).isEqualTo("x1");
		assertThat(lbf.getAliases("&x2").length).isEqualTo(1);
		assertThat(lbf.getAliases("&x2")[0]).isEqualTo("&x1");
	}

	@Test
	void staticFactoryMethodFoundByNonEagerTypeMatching() {
		RootBeanDefinition rbd = new RootBeanDefinition(TestBeanFactory.class);
		rbd.setFactoryMethodName("createTestBean");
		lbf.registerBeanDefinition("x1", rbd);

		TestBeanFactory.initialized = false;
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("x1");
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
		assertThat(lbf.getType("&x1")).isEqualTo(null);
		assertThat(TestBeanFactory.initialized).isFalse();
	}

	@Test
	void staticPrototypeFactoryMethodFoundByNonEagerTypeMatching() {
		RootBeanDefinition rbd = new RootBeanDefinition(TestBeanFactory.class);
		rbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		rbd.setFactoryMethodName("createTestBean");
		lbf.registerBeanDefinition("x1", rbd);

		TestBeanFactory.initialized = false;
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("x1");
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
		assertThat(lbf.getType("&x1")).isEqualTo(null);
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
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("x1");
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
		assertThat(lbf.getType("&x1")).isEqualTo(null);
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
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("x1");
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
		assertThat(lbf.getType("&x1")).isEqualTo(null);
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
		assertThat(lbf.getType("&x2")).isEqualTo(null);
		assertThat(lbf.getAliases("x1").length).isEqualTo(1);
		assertThat(lbf.getAliases("x1")[0]).isEqualTo("x2");
		assertThat(lbf.getAliases("&x1").length).isEqualTo(1);
		assertThat(lbf.getAliases("&x1")[0]).isEqualTo("&x2");
		assertThat(lbf.getAliases("x2").length).isEqualTo(1);
		assertThat(lbf.getAliases("x2")[0]).isEqualTo("x1");
		assertThat(lbf.getAliases("&x2").length).isEqualTo(1);
		assertThat(lbf.getAliases("&x2")[0]).isEqualTo("&x1");
	}

	@Test
	void empty() {
		ListableBeanFactory lbf = new DefaultListableBeanFactory();
		assertThat(lbf.getBeanDefinitionNames() != null).as("No beans defined --> array != null").isTrue();
		assertThat(lbf.getBeanDefinitionNames().length == 0).as("No beans defined after no arg constructor").isTrue();
		assertThat(lbf.getBeanDefinitionCount() == 0).as("No beans defined after no arg constructor").isTrue();
	}

	@Test
	void emptyPropertiesPopulation() {
		Properties p = new Properties();
		registerBeanDefinitions(p);
		assertThat(lbf.getBeanDefinitionCount() == 0).as("No beans defined after ignorable invalid").isTrue();
	}

	@Test
	void harmlessIgnorableRubbish() {
		Properties p = new Properties();
		p.setProperty("foo", "bar");
		p.setProperty("qwert", "er");
		registerBeanDefinitions(p, "test");
		assertThat(lbf.getBeanDefinitionCount() == 0).as("No beans defined after harmless ignorable rubbish").isTrue();
	}

	@Test
	void propertiesPopulationWithNullPrefix() {
		Properties p = new Properties();
		p.setProperty("test.(class)", TestBean.class.getName());
		p.setProperty("test.name", "Tony");
		p.setProperty("test.age", "48");
		int count = registerBeanDefinitions(p);
		assertThat(count == 1).as("1 beans registered, not " + count).isTrue();
		singleTestBean(lbf);
	}

	@Test
	void propertiesPopulationWithPrefix() {
		String PREFIX = "beans.";
		Properties p = new Properties();
		p.setProperty(PREFIX + "test.(class)", TestBean.class.getName());
		p.setProperty(PREFIX + "test.name", "Tony");
		p.setProperty(PREFIX + "test.age", "0x30");
		int count = registerBeanDefinitions(p, PREFIX);
		assertThat(count == 1).as("1 beans registered, not " + count).isTrue();
		singleTestBean(lbf);
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
		assertThat(count == 2).as("2 beans registered, not " + count).isTrue();

		TestBean kerry = lbf.getBean("kerry", TestBean.class);
		assertThat("Kerry".equals(kerry.getName())).as("Kerry name is Kerry").isTrue();
		ITestBean spouse = kerry.getSpouse();
		assertThat(spouse != null).as("Kerry spouse is non null").isTrue();
		assertThat("Rod".equals(spouse.getName())).as("Kerry spouse name is Rod").isTrue();
	}

	@Test
	void propertiesWithDotsInKey() {
		Properties p = new Properties();

		p.setProperty("tb.(class)", TestBean.class.getName());
		p.setProperty("tb.someMap[my.key]", "my.value");

		int count = registerBeanDefinitions(p);
		assertThat(count == 1).as("1 beans registered, not " + count).isTrue();
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
		assertThat(self.getStringArray()).hasSize(1);
		assertThat(self.getStringArray()).contains("A");
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
		assertThat(self.getStringArray()).hasSize(1);
		assertThat(self.getStringArray()).contains("A");
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
		assertThat(self.getStringArray()).hasSize(2);
		assertThat(self.getStringArray()).contains("A");
		assertThat(self.getStringArray()).contains("B");
	}

	@Test
	void possibleMatches() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("ag", "foobar");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("tb", bd);

		assertThatExceptionOfType(BeanCreationException.class).as("invalid property").isThrownBy(() ->
				lbf.getBean("tb"))
			.withCauseInstanceOf(NotWritablePropertyException.class)
			.satisfies(ex -> {
				NotWritablePropertyException cause = (NotWritablePropertyException) ex.getCause();
				assertThat(cause.getPossibleMatches()).hasSize(1);
				assertThat(cause.getPossibleMatches()[0]).isEqualTo("age");
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
		assertThat(kerry1 != null).as("Non null").isTrue();
		assertThat(kerry1 == kerry2).as("Singletons equal").isTrue();

		lbf = new DefaultListableBeanFactory();
		p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.(scope)", "prototype");
		p.setProperty("kerry.age", "35");
		registerBeanDefinitions(p);
		kerry1 = (TestBean) lbf.getBean("kerry");
		kerry2 = (TestBean) lbf.getBean("kerry");
		assertThat(kerry1 != null).as("Non null").isTrue();
		assertThat(kerry1 != kerry2).as("Prototypes NOT equal").isTrue();

		lbf = new DefaultListableBeanFactory();
		p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.(scope)", "singleton");
		p.setProperty("kerry.age", "35");
		registerBeanDefinitions(p);
		kerry1 = (TestBean) lbf.getBean("kerry");
		kerry2 = (TestBean) lbf.getBean("kerry");
		assertThat(kerry1 != null).as("Non null").isTrue();
		assertThat(kerry1 == kerry2).as("Specified singletons equal").isTrue();
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
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				lbf.getBean("kerry"))
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
		assertThat(kerry1 == kerry2).as("Singletons equal").isTrue();

		lbf = new DefaultListableBeanFactory();
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
		assertThat(kerry1 != null).as("Non null").isTrue();
		assertThat(kerry1 != kerry2).as("Prototypes NOT equal").isTrue();

		lbf = new DefaultListableBeanFactory();
		p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.(singleton)", "true");
		p.setProperty("kerry.age", "35");
		registerBeanDefinitions(p);
		kerry1 = (TestBean) lbf.getBean("kerry");
		kerry2 = (TestBean) lbf.getBean("kerry");
		assertThat(kerry1 != null).as("Non null").isTrue();
		assertThat(kerry1 == kerry2).as("Specified singletons equal").isTrue();
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

		TestBean child = (TestBean) factory.getBean("child");
		assertThat(child.getName()).isEqualTo(EXPECTED_NAME);
		assertThat(child.getAge()).isEqualTo(EXPECTED_AGE);
		Object mergedBeanDefinition2 = factory.getMergedBeanDefinition("child");

		assertThat(mergedBeanDefinition2).as("Use cached merged bean definition").isEqualTo(mergedBeanDefinition2);
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
	void nameAlreadyBound() {
		Properties p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.age", "35");
		registerBeanDefinitions(p);
		try {
			registerBeanDefinitions(p);
		}
		catch (BeanDefinitionStoreException ex) {
			assertThat(ex.getBeanName()).isEqualTo("kerry");
			// expected
		}
	}

	private void singleTestBean(ListableBeanFactory lbf) {
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
	void aliasCircle() {
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
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("otherTest", "test2");
		lbf.registerAlias("test", "test2");
		assertThat(lbf.getBean("test")).isInstanceOf(NestedTestBean.class);
		assertThat(lbf.getBean("test2")).isInstanceOf(NestedTestBean.class);
	}

	@Test
	void beanDefinitionOverridingNotAllowed() {
		lbf.setAllowBeanDefinitionOverriding(false);
		BeanDefinition oldDef = new RootBeanDefinition(TestBean.class);
		BeanDefinition newDef = new RootBeanDefinition(NestedTestBean.class);
		lbf.registerBeanDefinition("test", oldDef);
		assertThatExceptionOfType(BeanDefinitionOverrideException.class).isThrownBy(() ->
				lbf.registerBeanDefinition("test", newDef))
				.satisfies(ex -> {
					assertThat(ex.getBeanName()).isEqualTo("test");
					assertThat(ex.getBeanDefinition()).isEqualTo(newDef);
					assertThat(ex.getExistingDefinition()).isEqualTo(oldDef);
				});
	}

	@Test
	void beanDefinitionOverridingWithAlias() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.registerAlias("test", "testAlias");
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("test", "testAlias");
		assertThat(lbf.getBean("test")).isInstanceOf(NestedTestBean.class);
		assertThat(lbf.getBean("testAlias")).isInstanceOf(NestedTestBean.class);
	}

	@Test
	void beanDefinitionOverridingWithConstructorArgumentMismatch() {
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
		assertThat(k.getSpouse() == r).isTrue();
	}

	@Test
	void canEscapeBeanReferenceSyntax() {
		String name = "*name";
		Properties p = new Properties();
		p.setProperty("r.(class)", TestBean.class.getName());
		p.setProperty("r.name", "*" + name);
		registerBeanDefinitions(p);
		TestBean r = (TestBean) lbf.getBean("r");
		assertThat(r.getName().equals(name)).isTrue();
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
		assertThat(testBean.getMyFloat().floatValue() == 1.1f).isTrue();
	}

	@Test
	void customConverter() {
		GenericConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new Converter<String, Float>() {
			@Override
			public Float convert(String source) {
				try {
					NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
					return nf.parse(source).floatValue();
				}
				catch (ParseException ex) {
					throw new IllegalArgumentException(ex);
				}
			}
		});
		lbf.setConversionService(conversionService);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", "1,1");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("testBean", bd);
		TestBean testBean = (TestBean) lbf.getBean("testBean");
		assertThat(testBean.getMyFloat().floatValue() == 1.1f).isTrue();
	}

	@Test
	void customEditorWithBeanReference() {
		lbf.addPropertyEditorRegistrar(new PropertyEditorRegistrar() {
			@Override
			public void registerCustomEditors(PropertyEditorRegistry registry) {
				NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
				registry.registerCustomEditor(Float.class, new CustomNumberEditor(Float.class, nf, true));
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", new RuntimeBeanReference("myFloat"));
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("testBean", bd);
		lbf.registerSingleton("myFloat", "1,1");
		TestBean testBean = (TestBean) lbf.getBean("testBean");
		assertThat(testBean.getMyFloat().floatValue() == 1.1f).isTrue();
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
		assertThat(testBean.getMyFloat().floatValue() == 1.1f).isTrue();
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
		assertThat(testBean.getMyFloat().floatValue() == 1.1f).isTrue();
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
		assertThat(beansOfType.size()).isEqualTo(2);
		assertThat(beansOfType.containsValue(test)).isTrue();
		assertThat(beansOfType.containsValue(singletonObject)).isTrue();

		beansOfType = lbf.getBeansOfType(null, false, true);
		assertThat(beansOfType.size()).isEqualTo(2);

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

		Map<?, ?>  beansOfType = lbf.getBeansOfType(TestBean.class, false, true);
		assertThat(beansOfType.size()).isEqualTo(2);
		assertThat(beansOfType.containsValue(test)).isTrue();
		assertThat(beansOfType.containsValue(singletonObject)).isTrue();

		beansOfType = lbf.getBeansOfType(null, false, true);

		Iterator<String> beanNames = lbf.getBeanNamesIterator();
		assertThat(beanNames.next()).isEqualTo("test");
		assertThat(beanNames.next()).isEqualTo("singletonObject");
		assertThat(beanNames.hasNext()).isFalse();
		assertThat(beansOfType.size()).isEqualTo(2);

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
		assertThat(lbf.getAliases("singletonObject").length).isEqualTo(0);
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
	void arrayPropertyWithOptionalAutowiring() throws MalformedURLException {
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
		BeanExpressionResolver beanExpressionResolver = mock(BeanExpressionResolver.class);
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
	void autowireWithNoDependencies() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("rod", bd);
		assertThat(lbf.getBeanDefinitionCount()).isEqualTo(1);
		Object registered = lbf.autowire(NoDependencies.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		assertThat(lbf.getBeanDefinitionCount()).isEqualTo(1);
		assertThat(registered instanceof NoDependencies).isTrue();
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
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				lbf.autowire(ConstructorDependency.class, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false))
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
		assertThat(bean.getSpouse1() == spouse).isTrue();
		assertThat(BeanFactoryUtils.beanOfType(lbf, TestBean.class) == spouse).isTrue();
	}

	@Test
	void autowireBeanByName() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spouse", bd);
		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);
		TestBean spouse = (TestBean) lbf.getBean("spouse");
		assertThat(bean.getSpouse()).isEqualTo(spouse);
		assertThat(BeanFactoryUtils.beanOfType(lbf, TestBean.class) == spouse).isTrue();
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
	void dependsOnCycle() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setDependsOn("tb2");
		lbf.registerBeanDefinition("tb1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setDependsOn("tb1");
		lbf.registerBeanDefinition("tb2", bd2);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				lbf.preInstantiateSingletons())
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
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				lbf::preInstantiateSingletons)
			.withMessageContaining("Circular")
			.withMessageContaining("'tb3'")
			.withMessageContaining("'tb1'");
	}

	@Test
	void getBeanByTypeWithNoneFound() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(TestBean.class));
	}

	@Test
	void getBeanByTypeWithLateRegistration() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(TestBean.class));
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName()).isEqualTo("bd1");
	}

	@Test
	void getBeanByTypeWithLateRegistrationAgainstFrozen() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory(parent);
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(TestBean.class))
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
		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(TestBean.class))
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory(parent);

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
		assertThat(resolved.size()).isEqualTo(2);
		assertThat(resolved.contains(lbf.getBean("bd1"))).isTrue();
		assertThat(resolved.contains(lbf.getBean("bd2"))).isTrue();

		resolved = new HashSet<>();
		provider.forEach(resolved::add);
		assertThat(resolved.size()).isEqualTo(2);
		assertThat(resolved.contains(lbf.getBean("bd1"))).isTrue();
		assertThat(resolved.contains(lbf.getBean("bd2"))).isTrue();

		resolved = provider.stream().collect(Collectors.toSet());
		assertThat(resolved.size()).isEqualTo(2);
		assertThat(resolved.contains(lbf.getBean("bd1"))).isTrue();
		assertThat(resolved.contains(lbf.getBean("bd2"))).isTrue();
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
		assertThat(resolved.size()).isEqualTo(2);
		assertThat(resolved.contains(lbf.getBean("bd1"))).isTrue();
		assertThat(resolved.contains(lbf.getBean("bd2"))).isTrue();

		resolved = new HashSet<>();
		provider.forEach(resolved::add);
		assertThat(resolved.size()).isEqualTo(2);
		assertThat(resolved.contains(lbf.getBean("bd1"))).isTrue();
		assertThat(resolved.contains(lbf.getBean("bd2"))).isTrue();

		resolved = provider.stream().collect(Collectors.toSet());
		assertThat(resolved.size()).isEqualTo(2);
		assertThat(resolved.contains(lbf.getBean("bd1"))).isTrue();
		assertThat(resolved.contains(lbf.getBean("bd2"))).isTrue();
	}

	@Test
	void getBeanByTypeInstanceWithMultiplePrimary() {
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		RootBeanDefinition bd2 = createConstructorDependencyBeanDefinition(43);
		bd1.setPrimary(true);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(() ->
				lbf.getBean(ConstructorDependency.class, 42))
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

		assertThat(lbf.getBeanNamesForType(ConstructorDependency.class).length).isEqualTo(1);
		assertThat(lbf.getBeanNamesForType(ConstructorDependencyFactoryBean.class).length).isEqualTo(1);
		assertThat(lbf.getBeanNamesForType(ResolvableType.forClassWithGenerics(FactoryBean.class, Object.class)).length).isEqualTo(1);
		assertThat(lbf.getBeanNamesForType(ResolvableType.forClassWithGenerics(FactoryBean.class, String.class)).length).isEqualTo(0);
		assertThat(lbf.getBeanNamesForType(ResolvableType.forClassWithGenerics(FactoryBean.class, Object.class), true, true).length).isEqualTo(1);
		assertThat(lbf.getBeanNamesForType(ResolvableType.forClassWithGenerics(FactoryBean.class, String.class), true, true).length).isEqualTo(0);
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
		Object mergedBeanDefinition2 = bean.getFactoryBean();
		assertThat(mergedBeanDefinition2).as("The FactoryBeanDependentBean should have been autowired 'by type' with the LazyInitFactory.").isEqualTo(mergedBeanDefinition2);
	}

	@Test
	void autowireBeanWithFactoryBeanByTypeWithPrimary() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		lbf.registerBeanDefinition("factoryBean", new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class));
		assertThat(lbf.containsSingleton("factoryBean")).isFalse();

		String[] beanNames = lbf.getBeanNamesForType(Runnable.class, false, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("&factoryBean");

		beanNames = lbf.getBeanNamesForType(Callable.class, false, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("&factoryBean");

		beanNames = lbf.getBeanNamesForType(RepositoryFactoryInformation.class, false, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("&factoryBean");

		beanNames = lbf.getBeanNamesForType(FactoryBean.class, false, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("&factoryBean");
	}

	@Test
	void getBeanNamesForTypeAfterFactoryBeanCreation() {
		lbf.registerBeanDefinition("factoryBean", new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class));
		lbf.getBean("&factoryBean");

		String[] beanNames = lbf.getBeanNamesForType(Runnable.class, false, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("&factoryBean");

		beanNames = lbf.getBeanNamesForType(Callable.class, false, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("&factoryBean");

		beanNames = lbf.getBeanNamesForType(RepositoryFactoryInformation.class, false, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("&factoryBean");

		beanNames = lbf.getBeanNamesForType(FactoryBean.class, false, false);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("&factoryBean");
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
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true))
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
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true))
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
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true))
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
			assertThat(bean.getSpouse() == otherBean).isTrue();
		}
	}

	@Test
	void circularReferenceThroughAutowiring() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(
				lbf::preInstantiateSingletons);
	}

	@Test
	void circularReferenceThroughFactoryBeanAutowiring() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyFactoryBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(
				lbf::preInstantiateSingletons);
	}

	@Test
	void circularReferenceThroughFactoryBeanTypeCheck() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyFactoryBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() ->
				lbf.getBeansOfType(String.class));
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
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(
				lbf::preInstantiateSingletons);
	}

	@Test
	void beanDefinitionWithInterface() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(ITestBean.class));
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				lbf.getBean("test"))
			.withMessageContaining("interface")
			.satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("test"));
	}

	@Test
	void beanDefinitionWithAbstractClass() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(AbstractBeanFactory.class));
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				lbf.getBean("test"))
			.withMessageContaining("abstract")
			.satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("test"));
	}

	@Test
	void prototypeFactoryBeanNotEagerlyCalled() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class));
		lbf.preInstantiateSingletons();
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

		assertThat(((AbstractBeanDefinition) factory.getMergedBeanDefinition("tb1")).getLazyInit()).isEqualTo(Boolean.TRUE);
		assertThat(((AbstractBeanDefinition) factory.getMergedBeanDefinition("tb2")).getLazyInit()).isEqualTo(Boolean.FALSE);
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
	void prototypeFactoryBeanNotEagerlyCalledInCaseOfBeanClassName() {
		lbf.registerBeanDefinition("test",
				new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class.getName(), null, null));
		lbf.preInstantiateSingletons();
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
		List<String> list = new ManagedList<>();
		list.add("myName");
		list.add("myBeanName");
		RootBeanDefinition bd = new RootBeanDefinition(DerivedTestBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bd.getConstructorArgumentValues().addGenericArgumentValue(list);
		lbf.registerBeanDefinition("test", bd);
		DerivedTestBean tb = (DerivedTestBean) lbf.getBean("test");
		assertThat(tb.getName()).isEqualTo("myName");
		assertThat(tb.getBeanName()).isEqualTo("myBeanName");
		DerivedTestBean tb2 = (DerivedTestBean) lbf.getBean("test");
		assertThat(tb != tb2).isTrue();
		assertThat(tb2.getName()).isEqualTo("myName");
		assertThat(tb2.getBeanName()).isEqualTo("myBeanName");
	}

	@Test
	void prototypeWithArrayConversionForFactoryMethod() {
		List<String> list = new ManagedList<>();
		list.add("myName");
		list.add("myBeanName");
		RootBeanDefinition bd = new RootBeanDefinition(DerivedTestBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bd.setFactoryMethodName("create");
		bd.getConstructorArgumentValues().addGenericArgumentValue(list);
		lbf.registerBeanDefinition("test", bd);
		DerivedTestBean tb = (DerivedTestBean) lbf.getBean("test");
		assertThat(tb.getName()).isEqualTo("myName");
		assertThat(tb.getBeanName()).isEqualTo("myBeanName");
		DerivedTestBean tb2 = (DerivedTestBean) lbf.getBean("test");
		assertThat(tb != tb2).isTrue();
		assertThat(tb2.getName()).isEqualTo("myName");
		assertThat(tb2.getBeanName()).isEqualTo("myBeanName");
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
		Object mergedBeanDefinition2 = BeanWithDestroyMethod.closeCount;
		assertThat(mergedBeanDefinition2).as("Destroy methods invoked").isEqualTo(mergedBeanDefinition2);
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
		Object mergedBeanDefinition2 = BeanWithDestroyMethod.closeCount;
		assertThat(mergedBeanDefinition2).as("Destroy methods invoked").isEqualTo(mergedBeanDefinition2);
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
		Object mergedBeanDefinition2 = BeanWithDestroyMethod.closeCount;
		assertThat(mergedBeanDefinition2).as("Destroy methods invoked").isEqualTo(mergedBeanDefinition2);
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
		List<String> tbNames = Arrays.asList(lbf.getBeanNamesForType(TestBean.class));
		assertThat(tbNames.contains("fmWithProperties")).isTrue();
		assertThat(tbNames.contains("fmWithArgs")).isTrue();
		assertThat(tbNames.size()).isEqualTo(2);

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
		Object mergedBeanDefinition2 = def.getScope();
		assertThat(mergedBeanDefinition2).as("Child 'scope' not overriding parent scope (it must).").isEqualTo(mergedBeanDefinition2);
	}

	@Test
	void scopeInheritanceForChildBeanDefinitions() {
		RootBeanDefinition parent = new RootBeanDefinition();
		parent.setScope("bonanza!");

		AbstractBeanDefinition child = new ChildBeanDefinition("parent");
		child.setBeanClass(TestBean.class);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("parent", parent);
		factory.registerBeanDefinition("child", child);

		BeanDefinition def = factory.getMergedBeanDefinition("child");
		Object mergedBeanDefinition2 = def.getScope();
		assertThat(mergedBeanDefinition2).as("Child 'scope' not inherited").isEqualTo(mergedBeanDefinition2);
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
		Object mergedBeanDefinition2 = tb.getName();
		assertThat(mergedBeanDefinition2).as("Name was set on field by IAPP").isEqualTo(mergedBeanDefinition2);
		if (!skipPropertyPopulation) {
			Object mergedBeanDefinition21 = tb.getAge();
			assertThat(mergedBeanDefinition21).as("Property value still set").isEqualTo(mergedBeanDefinition21);
		}
		else {
			Object mergedBeanDefinition21 = tb.getAge();
			assertThat(mergedBeanDefinition21).as("Property value was NOT set and still has default value").isEqualTo(mergedBeanDefinition21);
		}
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void initSecurityAwarePrototypeBean() {
		RootBeanDefinition bd = new RootBeanDefinition(TestSecuredBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bd.setInitMethodName("init");
		lbf.registerBeanDefinition("test", bd);
		final Subject subject = new Subject();
		subject.getPrincipals().add(new TestPrincipal("user1"));

		TestSecuredBean bean = (TestSecuredBean) Subject.doAsPrivileged(subject,
				(PrivilegedAction) () -> lbf.getBean("test"), null);
		assertThat(bean).isNotNull();
		assertThat(bean.getUserName()).isEqualTo("user1");
	}

	@Test
	void containsBeanReturnsTrueEvenForAbstractBeanDefinition() {
		lbf.registerBeanDefinition("abs", BeanDefinitionBuilder
				.rootBeanDefinition(TestBean.class).setAbstract(true).getBeanDefinition());
		assertThat(lbf.containsBean("abs")).isEqualTo(true);
		assertThat(lbf.containsBean("bogus")).isEqualTo(false);
	}

	@Test
	void resolveEmbeddedValue() {
		StringValueResolver r1 = mock(StringValueResolver.class);
		StringValueResolver r2 = mock(StringValueResolver.class);
		StringValueResolver r3 = mock(StringValueResolver.class);
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
		assertThat((Optional<?>) lbf.getBean(Optional.class)).isSameAs(Optional.empty());
	}

	@Test
	void nonPublicEnum() {
		RootBeanDefinition bd = new RootBeanDefinition(NonPublicEnumHolder.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("VALUE_1");
		lbf.registerBeanDefinition("holderBean", bd);
		NonPublicEnumHolder holder = (NonPublicEnumHolder) lbf.getBean("holderBean");
		assertThat(holder.getNonPublicEnum()).isEqualTo(NonPublicEnum.VALUE_1);
	}


	@SuppressWarnings("deprecation")
	private int registerBeanDefinitions(Properties p) {
		return (new org.springframework.beans.factory.support.PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
	}

	@SuppressWarnings("deprecation")
	private int registerBeanDefinitions(Properties p, String prefix) {
		return (new org.springframework.beans.factory.support.PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p, prefix);
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
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
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


	public static class BeanWithDisposableBean implements DisposableBean {

		private static boolean closed;

		@Override
		public void destroy() {
			closed = true;
		}
	}


	public static class BeanWithCloseable implements Closeable {

		private static boolean closed;

		@Override
		public void close() {
			closed = true;
		}
	}


	public static abstract class BaseClassWithDestroyMethod {

		public abstract BaseClassWithDestroyMethod close();
	}


	public static class BeanWithDestroyMethod extends BaseClassWithDestroyMethod {

		private static int closeCount = 0;

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


	public static abstract class RepositoryFactoryBeanSupport<T extends Repository<S, ID>, S, ID extends Serializable>
			implements RepositoryFactoryInformation<S, ID>, FactoryBean<T> {
	}


	public static class FactoryBeanThatShouldntBeCalled<T extends Repository<S, ID>, S, ID extends Serializable>
			extends RepositoryFactoryBeanSupport<T, S, ID> implements Runnable, Callable<T> {

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


	private static class CustomTypeConverter implements TypeConverter {

		private final NumberFormat numberFormat;

		public CustomTypeConverter(NumberFormat numberFormat) {
			this.numberFormat = numberFormat;
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object convertIfNecessary(Object value, @Nullable Class requiredType) {
			if (value instanceof String && Float.class.isAssignableFrom(requiredType)) {
				try {
					return new Float(this.numberFormat.parse((String) value).floatValue());
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
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object convertIfNecessary(Object value, @Nullable Class requiredType, @Nullable MethodParameter methodParam) {
			return convertIfNecessary(value, requiredType);
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object convertIfNecessary(Object value, @Nullable Class requiredType, @Nullable Field field) {
			return convertIfNecessary(value, requiredType);
		}
	}


	@SuppressWarnings("unused")
	private static class TestSecuredBean {

		private String userName;

		void init() {
			AccessControlContext acc = AccessController.getContext();
			Subject subject = Subject.getSubject(acc);
			if (subject == null) {
				return;
			}
			setNameFromPrincipal(subject.getPrincipals());
		}

		private void setNameFromPrincipal(Set<Principal> principals) {
			if (principals == null) {
				return;
			}
			for (Iterator<Principal> it = principals.iterator(); it.hasNext();) {
				Principal p = it.next();
				this.userName = p.getName();
				return;
			}
		}

		public String getUserName() {
			return this.userName;
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

		VALUE_1, VALUE_2;
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

}

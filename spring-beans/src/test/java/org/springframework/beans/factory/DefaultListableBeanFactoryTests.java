/*
 * Copyright 2002-2019 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
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
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ConstructorDependenciesBean;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.lang.Nullable;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.tests.sample.beans.DependenciesBean;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.LifecycleBean;
import org.springframework.tests.sample.beans.NestedTestBean;
import org.springframework.tests.sample.beans.SideEffectBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.tests.sample.beans.factory.DummyFactory;
import org.springframework.util.SerializationTestUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringValueResolver;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

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
public class DefaultListableBeanFactoryTests {

	private static final Log factoryLog = LogFactory.getLog(DefaultListableBeanFactory.class);

	private DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void testUnreferencedSingletonWasInstantiated() {
		KnowsIfInstantiated.clearInstantiationRecord();
		Properties p = new Properties();
		p.setProperty("x1.(class)", KnowsIfInstantiated.class.getName());
		assertTrue("singleton not instantiated", !KnowsIfInstantiated.wasInstantiated());
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		lbf.preInstantiateSingletons();
		assertTrue("singleton was instantiated", KnowsIfInstantiated.wasInstantiated());
	}

	@Test
	public void testLazyInitialization() {
		KnowsIfInstantiated.clearInstantiationRecord();
		Properties p = new Properties();
		p.setProperty("x1.(class)", KnowsIfInstantiated.class.getName());
		p.setProperty("x1.(lazy-init)", "true");
		assertTrue("singleton not instantiated", !KnowsIfInstantiated.wasInstantiated());
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		assertTrue("singleton not instantiated", !KnowsIfInstantiated.wasInstantiated());
		lbf.preInstantiateSingletons();

		assertTrue("singleton not instantiated", !KnowsIfInstantiated.wasInstantiated());
		lbf.getBean("x1");
		assertTrue("singleton was instantiated", KnowsIfInstantiated.wasInstantiated());
	}

	@Test
	public void testFactoryBeanDidNotCreatePrototype() {
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.singleton", "false");
		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
		assertEquals(TestBean.class, lbf.getType("x1"));
		lbf.preInstantiateSingletons();

		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
		lbf.getBean("x1");
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertTrue(lbf.containsBean("x1"));
		assertTrue(lbf.containsBean("&x1"));
		assertTrue("prototype was instantiated", DummyFactory.wasPrototypeCreated());
	}

	@Test
	public void testPrototypeFactoryBeanIgnoredByNonEagerTypeMatching() {
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.(singleton)", "false");
		p.setProperty("x1.singleton", "false");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);

		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertEquals(0, beanNames.length);
		beanNames = lbf.getBeanNamesForAnnotation(SuppressWarnings.class);
		assertEquals(0, beanNames.length);

		assertFalse(lbf.containsSingleton("x1"));
		assertTrue(lbf.containsBean("x1"));
		assertTrue(lbf.containsBean("&x1"));
		assertFalse(lbf.isSingleton("x1"));
		assertFalse(lbf.isSingleton("&x1"));
		assertTrue(lbf.isPrototype("x1"));
		assertTrue(lbf.isPrototype("&x1"));
		assertTrue(lbf.isTypeMatch("x1", TestBean.class));
		assertFalse(lbf.isTypeMatch("&x1", TestBean.class));
		assertTrue(lbf.isTypeMatch("&x1", DummyFactory.class));
		assertTrue(lbf.isTypeMatch("&x1", ResolvableType.forClass(DummyFactory.class)));
		assertTrue(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, Object.class)));
		assertFalse(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, String.class)));
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertEquals(DummyFactory.class, lbf.getType("&x1"));
		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
	}

	@Test
	public void testSingletonFactoryBeanIgnoredByNonEagerTypeMatching() {
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.(singleton)", "false");
		p.setProperty("x1.singleton", "true");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);

		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertEquals(0, beanNames.length);
		beanNames = lbf.getBeanNamesForAnnotation(SuppressWarnings.class);
		assertEquals(0, beanNames.length);

		assertFalse(lbf.containsSingleton("x1"));
		assertTrue(lbf.containsBean("x1"));
		assertTrue(lbf.containsBean("&x1"));
		assertFalse(lbf.isSingleton("x1"));
		assertFalse(lbf.isSingleton("&x1"));
		assertTrue(lbf.isPrototype("x1"));
		assertTrue(lbf.isPrototype("&x1"));
		assertTrue(lbf.isTypeMatch("x1", TestBean.class));
		assertFalse(lbf.isTypeMatch("&x1", TestBean.class));
		assertTrue(lbf.isTypeMatch("&x1", DummyFactory.class));
		assertTrue(lbf.isTypeMatch("&x1", ResolvableType.forClass(DummyFactory.class)));
		assertTrue(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, Object.class)));
		assertFalse(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, String.class)));
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertEquals(DummyFactory.class, lbf.getType("&x1"));
		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
	}

	@Test
	public void testNonInitializedFactoryBeanIgnoredByNonEagerTypeMatching() {
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.singleton", "false");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);

		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertEquals(0, beanNames.length);
		beanNames = lbf.getBeanNamesForAnnotation(SuppressWarnings.class);
		assertEquals(0, beanNames.length);

		assertFalse(lbf.containsSingleton("x1"));
		assertTrue(lbf.containsBean("x1"));
		assertTrue(lbf.containsBean("&x1"));
		assertFalse(lbf.isSingleton("x1"));
		assertTrue(lbf.isSingleton("&x1"));
		assertTrue(lbf.isPrototype("x1"));
		assertFalse(lbf.isPrototype("&x1"));
		assertTrue(lbf.isTypeMatch("x1", TestBean.class));
		assertFalse(lbf.isTypeMatch("&x1", TestBean.class));
		assertTrue(lbf.isTypeMatch("&x1", DummyFactory.class));
		assertTrue(lbf.isTypeMatch("&x1", ResolvableType.forClass(DummyFactory.class)));
		assertTrue(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, Object.class)));
		assertFalse(lbf.isTypeMatch("&x1", ResolvableType.forClassWithGenerics(FactoryBean.class, String.class)));
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertEquals(DummyFactory.class, lbf.getType("&x1"));
		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
	}

	@Test
	public void testInitializedFactoryBeanFoundByNonEagerTypeMatching() {
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.singleton", "false");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		lbf.preInstantiateSingletons();

		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertEquals(1, beanNames.length);
		assertEquals("x1", beanNames[0]);
		assertTrue(lbf.containsSingleton("x1"));
		assertTrue(lbf.containsBean("x1"));
		assertTrue(lbf.containsBean("&x1"));
		assertTrue(lbf.containsLocalBean("x1"));
		assertTrue(lbf.containsLocalBean("&x1"));
		assertFalse(lbf.isSingleton("x1"));
		assertTrue(lbf.isSingleton("&x1"));
		assertTrue(lbf.isPrototype("x1"));
		assertFalse(lbf.isPrototype("&x1"));
		assertTrue(lbf.isTypeMatch("x1", TestBean.class));
		assertFalse(lbf.isTypeMatch("&x1", TestBean.class));
		assertTrue(lbf.isTypeMatch("&x1", DummyFactory.class));
		assertTrue(lbf.isTypeMatch("x1", Object.class));
		assertTrue(lbf.isTypeMatch("&x1", Object.class));
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertEquals(DummyFactory.class, lbf.getType("&x1"));
		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());

		lbf.registerAlias("x1", "x2");
		assertTrue(lbf.containsBean("x2"));
		assertTrue(lbf.containsBean("&x2"));
		assertTrue(lbf.containsLocalBean("x2"));
		assertTrue(lbf.containsLocalBean("&x2"));
		assertFalse(lbf.isSingleton("x2"));
		assertTrue(lbf.isSingleton("&x2"));
		assertTrue(lbf.isPrototype("x2"));
		assertFalse(lbf.isPrototype("&x2"));
		assertTrue(lbf.isTypeMatch("x2", TestBean.class));
		assertFalse(lbf.isTypeMatch("&x2", TestBean.class));
		assertTrue(lbf.isTypeMatch("&x2", DummyFactory.class));
		assertTrue(lbf.isTypeMatch("x2", Object.class));
		assertTrue(lbf.isTypeMatch("&x2", Object.class));
		assertEquals(TestBean.class, lbf.getType("x2"));
		assertEquals(DummyFactory.class, lbf.getType("&x2"));
		assertEquals(1, lbf.getAliases("x1").length);
		assertEquals("x2", lbf.getAliases("x1")[0]);
		assertEquals(1, lbf.getAliases("&x1").length);
		assertEquals("&x2", lbf.getAliases("&x1")[0]);
		assertEquals(1, lbf.getAliases("x2").length);
		assertEquals("x1", lbf.getAliases("x2")[0]);
		assertEquals(1, lbf.getAliases("&x2").length);
		assertEquals("&x1", lbf.getAliases("&x2")[0]);
	}

	@Test
	public void testStaticFactoryMethodFoundByNonEagerTypeMatching() {
		RootBeanDefinition rbd = new RootBeanDefinition(TestBeanFactory.class);
		rbd.setFactoryMethodName("createTestBean");
		lbf.registerBeanDefinition("x1", rbd);

		TestBeanFactory.initialized = false;
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertEquals(1, beanNames.length);
		assertEquals("x1", beanNames[0]);
		assertFalse(lbf.containsSingleton("x1"));
		assertTrue(lbf.containsBean("x1"));
		assertFalse(lbf.containsBean("&x1"));
		assertTrue(lbf.isSingleton("x1"));
		assertFalse(lbf.isSingleton("&x1"));
		assertFalse(lbf.isPrototype("x1"));
		assertFalse(lbf.isPrototype("&x1"));
		assertTrue(lbf.isTypeMatch("x1", TestBean.class));
		assertFalse(lbf.isTypeMatch("&x1", TestBean.class));
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertEquals(null, lbf.getType("&x1"));
		assertFalse(TestBeanFactory.initialized);
	}

	@Test
	public void testStaticPrototypeFactoryMethodFoundByNonEagerTypeMatching() {
		RootBeanDefinition rbd = new RootBeanDefinition(TestBeanFactory.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.setFactoryMethodName("createTestBean");
		lbf.registerBeanDefinition("x1", rbd);

		TestBeanFactory.initialized = false;
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertEquals(1, beanNames.length);
		assertEquals("x1", beanNames[0]);
		assertFalse(lbf.containsSingleton("x1"));
		assertTrue(lbf.containsBean("x1"));
		assertFalse(lbf.containsBean("&x1"));
		assertFalse(lbf.isSingleton("x1"));
		assertFalse(lbf.isSingleton("&x1"));
		assertTrue(lbf.isPrototype("x1"));
		assertFalse(lbf.isPrototype("&x1"));
		assertTrue(lbf.isTypeMatch("x1", TestBean.class));
		assertFalse(lbf.isTypeMatch("&x1", TestBean.class));
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertEquals(null, lbf.getType("&x1"));
		assertFalse(TestBeanFactory.initialized);
	}

	@Test
	public void testNonStaticFactoryMethodFoundByNonEagerTypeMatching() {
		RootBeanDefinition factoryBd = new RootBeanDefinition(TestBeanFactory.class);
		lbf.registerBeanDefinition("factory", factoryBd);
		RootBeanDefinition rbd = new RootBeanDefinition(TestBeanFactory.class);
		rbd.setFactoryBeanName("factory");
		rbd.setFactoryMethodName("createTestBeanNonStatic");
		lbf.registerBeanDefinition("x1", rbd);

		TestBeanFactory.initialized = false;
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertEquals(1, beanNames.length);
		assertEquals("x1", beanNames[0]);
		assertFalse(lbf.containsSingleton("x1"));
		assertTrue(lbf.containsBean("x1"));
		assertFalse(lbf.containsBean("&x1"));
		assertTrue(lbf.isSingleton("x1"));
		assertFalse(lbf.isSingleton("&x1"));
		assertFalse(lbf.isPrototype("x1"));
		assertFalse(lbf.isPrototype("&x1"));
		assertTrue(lbf.isTypeMatch("x1", TestBean.class));
		assertFalse(lbf.isTypeMatch("&x1", TestBean.class));
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertEquals(null, lbf.getType("&x1"));
		assertFalse(TestBeanFactory.initialized);
	}

	@Test
	public void testNonStaticPrototypeFactoryMethodFoundByNonEagerTypeMatching() {
		RootBeanDefinition factoryBd = new RootBeanDefinition(TestBeanFactory.class);
		lbf.registerBeanDefinition("factory", factoryBd);
		RootBeanDefinition rbd = new RootBeanDefinition();
		rbd.setFactoryBeanName("factory");
		rbd.setFactoryMethodName("createTestBeanNonStatic");
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("x1", rbd);

		TestBeanFactory.initialized = false;
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
		assertEquals(1, beanNames.length);
		assertEquals("x1", beanNames[0]);
		assertFalse(lbf.containsSingleton("x1"));
		assertTrue(lbf.containsBean("x1"));
		assertFalse(lbf.containsBean("&x1"));
		assertTrue(lbf.containsLocalBean("x1"));
		assertFalse(lbf.containsLocalBean("&x1"));
		assertFalse(lbf.isSingleton("x1"));
		assertFalse(lbf.isSingleton("&x1"));
		assertTrue(lbf.isPrototype("x1"));
		assertFalse(lbf.isPrototype("&x1"));
		assertTrue(lbf.isTypeMatch("x1", TestBean.class));
		assertFalse(lbf.isTypeMatch("&x1", TestBean.class));
		assertTrue(lbf.isTypeMatch("x1", Object.class));
		assertFalse(lbf.isTypeMatch("&x1", Object.class));
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertEquals(null, lbf.getType("&x1"));
		assertFalse(TestBeanFactory.initialized);

		lbf.registerAlias("x1", "x2");
		assertTrue(lbf.containsBean("x2"));
		assertFalse(lbf.containsBean("&x2"));
		assertTrue(lbf.containsLocalBean("x2"));
		assertFalse(lbf.containsLocalBean("&x2"));
		assertFalse(lbf.isSingleton("x2"));
		assertFalse(lbf.isSingleton("&x2"));
		assertTrue(lbf.isPrototype("x2"));
		assertFalse(lbf.isPrototype("&x2"));
		assertTrue(lbf.isTypeMatch("x2", TestBean.class));
		assertFalse(lbf.isTypeMatch("&x2", TestBean.class));
		assertTrue(lbf.isTypeMatch("x2", Object.class));
		assertFalse(lbf.isTypeMatch("&x2", Object.class));
		assertEquals(TestBean.class, lbf.getType("x2"));
		assertEquals(null, lbf.getType("&x2"));
		assertEquals(1, lbf.getAliases("x1").length);
		assertEquals("x2", lbf.getAliases("x1")[0]);
		assertEquals(1, lbf.getAliases("&x1").length);
		assertEquals("&x2", lbf.getAliases("&x1")[0]);
		assertEquals(1, lbf.getAliases("x2").length);
		assertEquals("x1", lbf.getAliases("x2")[0]);
		assertEquals(1, lbf.getAliases("&x2").length);
		assertEquals("&x1", lbf.getAliases("&x2")[0]);
	}

	@Test
	public void testEmpty() {
		ListableBeanFactory lbf = new DefaultListableBeanFactory();
		assertTrue("No beans defined --> array != null", lbf.getBeanDefinitionNames() != null);
		assertTrue("No beans defined after no arg constructor", lbf.getBeanDefinitionNames().length == 0);
		assertTrue("No beans defined after no arg constructor", lbf.getBeanDefinitionCount() == 0);
	}

	@Test
	public void testEmptyPropertiesPopulation() {
		Properties p = new Properties();
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		assertTrue("No beans defined after ignorable invalid", lbf.getBeanDefinitionCount() == 0);
	}

	@Test
	public void testHarmlessIgnorableRubbish() {
		Properties p = new Properties();
		p.setProperty("foo", "bar");
		p.setProperty("qwert", "er");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p, "test");
		assertTrue("No beans defined after harmless ignorable rubbish", lbf.getBeanDefinitionCount() == 0);
	}

	@Test
	public void testPropertiesPopulationWithNullPrefix() {
		Properties p = new Properties();
		p.setProperty("test.(class)", TestBean.class.getName());
		p.setProperty("test.name", "Tony");
		p.setProperty("test.age", "48");
		int count = (new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		assertTrue("1 beans registered, not " + count, count == 1);
		testSingleTestBean(lbf);
	}

	@Test
	public void testPropertiesPopulationWithPrefix() {
		String PREFIX = "beans.";
		Properties p = new Properties();
		p.setProperty(PREFIX + "test.(class)", TestBean.class.getName());
		p.setProperty(PREFIX + "test.name", "Tony");
		p.setProperty(PREFIX + "test.age", "0x30");
		int count = (new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p, PREFIX);
		assertTrue("1 beans registered, not " + count, count == 1);
		testSingleTestBean(lbf);
	}

	@Test
	public void testSimpleReference() {
		String PREFIX = "beans.";
		Properties p = new Properties();

		p.setProperty(PREFIX + "rod.(class)", TestBean.class.getName());
		p.setProperty(PREFIX + "rod.name", "Rod");

		p.setProperty(PREFIX + "kerry.(class)", TestBean.class.getName());
		p.setProperty(PREFIX + "kerry.name", "Kerry");
		p.setProperty(PREFIX + "kerry.age", "35");
		p.setProperty(PREFIX + "kerry.spouse(ref)", "rod");

		int count = (new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p, PREFIX);
		assertTrue("2 beans registered, not " + count, count == 2);

		TestBean kerry = lbf.getBean("kerry", TestBean.class);
		assertTrue("Kerry name is Kerry", "Kerry".equals(kerry.getName()));
		ITestBean spouse = kerry.getSpouse();
		assertTrue("Kerry spouse is non null", spouse != null);
		assertTrue("Kerry spouse name is Rod", "Rod".equals(spouse.getName()));
	}

	@Test
	public void testPropertiesWithDotsInKey() {
		Properties p = new Properties();

		p.setProperty("tb.(class)", TestBean.class.getName());
		p.setProperty("tb.someMap[my.key]", "my.value");

		int count = (new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		assertTrue("1 beans registered, not " + count, count == 1);
		assertEquals(1, lbf.getBeanDefinitionCount());

		TestBean tb = lbf.getBean("tb", TestBean.class);
		assertEquals("my.value", tb.getSomeMap().get("my.key"));
	}

	@Test
	public void testUnresolvedReference() {
		String PREFIX = "beans.";
		Properties p = new Properties();

		try {
			p.setProperty(PREFIX + "kerry.(class)", TestBean.class.getName());
			p.setProperty(PREFIX + "kerry.name", "Kerry");
			p.setProperty(PREFIX + "kerry.age", "35");
			p.setProperty(PREFIX + "kerry.spouse(ref)", "rod");

			(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p, PREFIX);

			lbf.getBean("kerry");
			fail("Unresolved reference should have been detected");
		}
		catch (BeansException ex) {
			// cool
		}
	}

	@Test
	public void testSelfReference() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("spouse", new RuntimeBeanReference("self"));
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("self", bd);
		TestBean self = (TestBean) lbf.getBean("self");
		assertEquals(self, self.getSpouse());
	}

	@Test
	public void testPossibleMatches() {
		try {
			MutablePropertyValues pvs = new MutablePropertyValues();
			pvs.add("ag", "foobar");
			RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
			bd.setPropertyValues(pvs);
			lbf.registerBeanDefinition("tb", bd);
			lbf.getBean("tb");
			fail("Should throw exception on invalid property");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getCause() instanceof NotWritablePropertyException);
			NotWritablePropertyException cause = (NotWritablePropertyException) ex.getCause();
			// expected
			assertEquals(1, cause.getPossibleMatches().length);
			assertEquals("age", cause.getPossibleMatches()[0]);
		}
	}

	@Test
	public void testPrototype() {
		Properties p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.age", "35");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		TestBean kerry1 = (TestBean) lbf.getBean("kerry");
		TestBean kerry2 = (TestBean) lbf.getBean("kerry");
		assertTrue("Non null", kerry1 != null);
		assertTrue("Singletons equal", kerry1 == kerry2);

		lbf = new DefaultListableBeanFactory();
		p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.(scope)", "prototype");
		p.setProperty("kerry.age", "35");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		kerry1 = (TestBean) lbf.getBean("kerry");
		kerry2 = (TestBean) lbf.getBean("kerry");
		assertTrue("Non null", kerry1 != null);
		assertTrue("Prototypes NOT equal", kerry1 != kerry2);

		lbf = new DefaultListableBeanFactory();
		p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.(scope)", "singleton");
		p.setProperty("kerry.age", "35");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		kerry1 = (TestBean) lbf.getBean("kerry");
		kerry2 = (TestBean) lbf.getBean("kerry");
		assertTrue("Non null", kerry1 != null);
		assertTrue("Specified singletons equal", kerry1 == kerry2);
	}

	@Test
	public void testPrototypeCircleLeadsToException() {
		Properties p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.(singleton)", "false");
		p.setProperty("kerry.age", "35");
		p.setProperty("kerry.spouse", "*rod");
		p.setProperty("rod.(class)", TestBean.class.getName());
		p.setProperty("rod.(singleton)", "false");
		p.setProperty("rod.age", "34");
		p.setProperty("rod.spouse", "*kerry");

		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		try {
			lbf.getBean("kerry");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
			assertTrue(ex.contains(BeanCurrentlyInCreationException.class));
		}
	}

	@Test
	public void testPrototypeExtendsPrototype() {
		Properties p = new Properties();
		p.setProperty("wife.(class)", TestBean.class.getName());
		p.setProperty("wife.name", "kerry");

		p.setProperty("kerry.(parent)", "wife");
		p.setProperty("kerry.age", "35");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		TestBean kerry1 = (TestBean) lbf.getBean("kerry");
		TestBean kerry2 = (TestBean) lbf.getBean("kerry");
		assertEquals("kerry", kerry1.getName());
		assertNotNull("Non null", kerry1);
		assertTrue("Singletons equal", kerry1 == kerry2);

		lbf = new DefaultListableBeanFactory();
		p = new Properties();
		p.setProperty("wife.(class)", TestBean.class.getName());
		p.setProperty("wife.name", "kerry");
		p.setProperty("wife.(singleton)", "false");
		p.setProperty("kerry.(parent)", "wife");
		p.setProperty("kerry.(singleton)", "false");
		p.setProperty("kerry.age", "35");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		assertFalse(lbf.isSingleton("kerry"));
		kerry1 = (TestBean) lbf.getBean("kerry");
		kerry2 = (TestBean) lbf.getBean("kerry");
		assertTrue("Non null", kerry1 != null);
		assertTrue("Prototypes NOT equal", kerry1 != kerry2);

		lbf = new DefaultListableBeanFactory();
		p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.(singleton)", "true");
		p.setProperty("kerry.age", "35");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		kerry1 = (TestBean) lbf.getBean("kerry");
		kerry2 = (TestBean) lbf.getBean("kerry");
		assertTrue("Non null", kerry1 != null);
		assertTrue("Specified singletons equal", kerry1 == kerry2);
	}

	@Test
	public void testCanReferenceParentBeanFromChildViaAlias() {
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
		assertEquals(EXPECTED_NAME, child.getName());
		assertEquals(EXPECTED_AGE, child.getAge());

		assertEquals("Use cached merged bean definition",
				factory.getMergedBeanDefinition("child"), factory.getMergedBeanDefinition("child"));
	}

	@Test
	public void testGetTypeWorksAfterParentChildMerging() {
		RootBeanDefinition parentDefinition = new RootBeanDefinition(TestBean.class);
		ChildBeanDefinition childDefinition = new ChildBeanDefinition("parent", DerivedTestBean.class, null, null);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("parent", parentDefinition);
		factory.registerBeanDefinition("child", childDefinition);
		factory.freezeConfiguration();

		assertEquals(TestBean.class, factory.getType("parent"));
		assertEquals(DerivedTestBean.class, factory.getType("child"));
	}

	@Test
	public void testNameAlreadyBound() {
		Properties p = new Properties();
		p.setProperty("kerry.(class)", TestBean.class.getName());
		p.setProperty("kerry.age", "35");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		try {
			(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		}
		catch (BeanDefinitionStoreException ex) {
			assertEquals("kerry", ex.getBeanName());
			// expected
		}
	}

	private void testSingleTestBean(ListableBeanFactory lbf) {
		assertTrue("1 beans defined", lbf.getBeanDefinitionCount() == 1);
		String[] names = lbf.getBeanDefinitionNames();
		assertTrue(names != lbf.getBeanDefinitionNames());
		assertTrue("Array length == 1", names.length == 1);
		assertTrue("0th element == test", names[0].equals("test"));
		TestBean tb = (TestBean) lbf.getBean("test");
		assertTrue("Test is non null", tb != null);
		assertTrue("Test bean name is Tony", "Tony".equals(tb.getName()));
		assertTrue("Test bean age is 48", tb.getAge() == 48);
	}

	@Test
	public void testAliasCircle() {
		lbf.registerAlias("test", "test2");
		lbf.registerAlias("test2", "test3");

		try {
			lbf.registerAlias("test3", "test2");
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}

		try {
			lbf.registerAlias("test3", "test");
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}

		lbf.registerAlias("test", "test3");
	}

	@Test
	public void testAliasChaining() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("test", "testAlias");
		lbf.registerAlias("testAlias", "testAlias2");
		lbf.registerAlias("testAlias2", "testAlias3");
		Object bean = lbf.getBean("test");
		assertSame(bean, lbf.getBean("testAlias"));
		assertSame(bean, lbf.getBean("testAlias2"));
		assertSame(bean, lbf.getBean("testAlias3"));
	}

	@Test
	public void testBeanDefinitionOverriding() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("otherTest", "test2");
		lbf.registerAlias("test", "test2");
		assertTrue(lbf.getBean("test") instanceof NestedTestBean);
		assertTrue(lbf.getBean("test2") instanceof NestedTestBean);
	}

	@Test
	public void testBeanDefinitionOverridingNotAllowed() {
		lbf.setAllowBeanDefinitionOverriding(false);
		BeanDefinition oldDef = new RootBeanDefinition(TestBean.class);
		BeanDefinition newDef = new RootBeanDefinition(NestedTestBean.class);
		lbf.registerBeanDefinition("test", oldDef);
		try {
			lbf.registerBeanDefinition("test", newDef);
			fail("Should have thrown BeanDefinitionOverrideException");
		}
		catch (BeanDefinitionOverrideException ex) {
			assertEquals("test", ex.getBeanName());
			assertSame(newDef, ex.getBeanDefinition());
			assertSame(oldDef, ex.getExistingDefinition());
		}
	}

	@Test
	public void testBeanDefinitionOverridingWithAlias() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.registerAlias("test", "testAlias");
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("test", "testAlias");
		assertTrue(lbf.getBean("test") instanceof NestedTestBean);
		assertTrue(lbf.getBean("testAlias") instanceof NestedTestBean);
	}

	@Test
	public void beanDefinitionOverridingWithConstructorArgumentMismatch() {
		RootBeanDefinition bd1 = new RootBeanDefinition(NestedTestBean.class);
		bd1.getConstructorArgumentValues().addIndexedArgumentValue(1, "value1");
		lbf.registerBeanDefinition("test", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(NestedTestBean.class);
		bd2.getConstructorArgumentValues().addIndexedArgumentValue(0, "value0");
		lbf.registerBeanDefinition("test", bd2);
		assertTrue(lbf.getBean("test") instanceof NestedTestBean);
		assertEquals("value0", lbf.getBean("test", NestedTestBean.class).getCompany());
	}

	@Test
	public void testBeanDefinitionRemoval() {
		lbf.setAllowBeanDefinitionOverriding(false);
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.registerAlias("test", "test2");
		lbf.preInstantiateSingletons();
		lbf.removeBeanDefinition("test");
		lbf.removeAlias("test2");
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("test", "test2");
		assertTrue(lbf.getBean("test") instanceof NestedTestBean);
		assertTrue(lbf.getBean("test2") instanceof NestedTestBean);
	}

	@Test // gh-23542
	public void concurrentBeanDefinitionRemoval() {
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
	public void testBeanReferenceWithNewSyntax() {
		Properties p = new Properties();
		p.setProperty("r.(class)", TestBean.class.getName());
		p.setProperty("r.name", "rod");
		p.setProperty("k.(class)", TestBean.class.getName());
		p.setProperty("k.name", "kerry");
		p.setProperty("k.spouse", "*r");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		TestBean k = (TestBean) lbf.getBean("k");
		TestBean r = (TestBean) lbf.getBean("r");
		assertTrue(k.getSpouse() == r);
	}

	@Test
	public void testCanEscapeBeanReferenceSyntax() {
		String name = "*name";
		Properties p = new Properties();
		p.setProperty("r.(class)", TestBean.class.getName());
		p.setProperty("r.name", "*" + name);
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		TestBean r = (TestBean) lbf.getBean("r");
		assertTrue(r.getName().equals(name));
	}

	@Test
	public void testCustomEditor() {
		lbf.addPropertyEditorRegistrar(new PropertyEditorRegistrar() {
			@Override
			public void registerCustomEditors(PropertyEditorRegistry registry) {
				NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
				registry.registerCustomEditor(Float.class, new CustomNumberEditor(Float.class, nf, true));
			}
		});
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", "1,1");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("testBean", bd);
		TestBean testBean = (TestBean) lbf.getBean("testBean");
		assertTrue(testBean.getMyFloat().floatValue() == 1.1f);
	}

	@Test
	public void testCustomConverter() {
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
		assertTrue(testBean.getMyFloat().floatValue() == 1.1f);
	}

	@Test
	public void testCustomEditorWithBeanReference() {
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
		assertTrue(testBean.getMyFloat().floatValue() == 1.1f);
	}

	@Test
	public void testCustomTypeConverter() {
		NumberFormat nf = NumberFormat.getInstance(Locale.GERMAN);
		lbf.setTypeConverter(new CustomTypeConverter(nf));
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("myFloat", "1,1");
		ConstructorArgumentValues cav = new ConstructorArgumentValues();
		cav.addIndexedArgumentValue(0, "myName");
		cav.addIndexedArgumentValue(1, "myAge");
		lbf.registerBeanDefinition("testBean", new RootBeanDefinition(TestBean.class, cav, pvs));
		TestBean testBean = (TestBean) lbf.getBean("testBean");
		assertEquals("myName", testBean.getName());
		assertEquals(5, testBean.getAge());
		assertTrue(testBean.getMyFloat().floatValue() == 1.1f);
	}

	@Test
	public void testCustomTypeConverterWithBeanReference() {
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
		assertEquals("myName", testBean.getName());
		assertEquals(5, testBean.getAge());
		assertTrue(testBean.getMyFloat().floatValue() == 1.1f);
	}

	@Test
	public void testRegisterExistingSingletonWithReference() {
		Properties p = new Properties();
		p.setProperty("test.(class)", TestBean.class.getName());
		p.setProperty("test.name", "Tony");
		p.setProperty("test.age", "48");
		p.setProperty("test.spouse(ref)", "singletonObject");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		Object singletonObject = new TestBean();
		lbf.registerSingleton("singletonObject", singletonObject);

		assertTrue(lbf.isSingleton("singletonObject"));
		assertEquals(TestBean.class, lbf.getType("singletonObject"));
		TestBean test = (TestBean) lbf.getBean("test");
		assertEquals(singletonObject, lbf.getBean("singletonObject"));
		assertEquals(singletonObject, test.getSpouse());

		Map<?, ?> beansOfType = lbf.getBeansOfType(TestBean.class, false, true);
		assertEquals(2, beansOfType.size());
		assertTrue(beansOfType.containsValue(test));
		assertTrue(beansOfType.containsValue(singletonObject));

		beansOfType = lbf.getBeansOfType(null, false, true);
		assertEquals(2, beansOfType.size());

		Iterator<String> beanNames = lbf.getBeanNamesIterator();
		assertEquals("test", beanNames.next());
		assertEquals("singletonObject", beanNames.next());
		assertFalse(beanNames.hasNext());

		assertTrue(lbf.containsSingleton("test"));
		assertTrue(lbf.containsSingleton("singletonObject"));
		assertTrue(lbf.containsBeanDefinition("test"));
		assertFalse(lbf.containsBeanDefinition("singletonObject"));
	}

	@Test
	public void testRegisterExistingSingletonWithNameOverriding() {
		Properties p = new Properties();
		p.setProperty("test.(class)", TestBean.class.getName());
		p.setProperty("test.name", "Tony");
		p.setProperty("test.age", "48");
		p.setProperty("test.spouse(ref)", "singletonObject");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		lbf.registerBeanDefinition("singletonObject", new RootBeanDefinition(PropertiesFactoryBean.class));
		Object singletonObject = new TestBean();
		lbf.registerSingleton("singletonObject", singletonObject);
		lbf.preInstantiateSingletons();

		assertTrue(lbf.isSingleton("singletonObject"));
		assertEquals(TestBean.class, lbf.getType("singletonObject"));
		TestBean test = (TestBean) lbf.getBean("test");
		assertEquals(singletonObject, lbf.getBean("singletonObject"));
		assertEquals(singletonObject, test.getSpouse());

		Map<?, ?>  beansOfType = lbf.getBeansOfType(TestBean.class, false, true);
		assertEquals(2, beansOfType.size());
		assertTrue(beansOfType.containsValue(test));
		assertTrue(beansOfType.containsValue(singletonObject));

		beansOfType = lbf.getBeansOfType(null, false, true);

		Iterator<String> beanNames = lbf.getBeanNamesIterator();
		assertEquals("test", beanNames.next());
		assertEquals("singletonObject", beanNames.next());
		assertFalse(beanNames.hasNext());
		assertEquals(2, beansOfType.size());

		assertTrue(lbf.containsSingleton("test"));
		assertTrue(lbf.containsSingleton("singletonObject"));
		assertTrue(lbf.containsBeanDefinition("test"));
		assertTrue(lbf.containsBeanDefinition("singletonObject"));
	}

	@Test
	public void testRegisterExistingSingletonWithAutowire() {
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

		assertTrue(lbf.containsBean("singletonObject"));
		assertTrue(lbf.isSingleton("singletonObject"));
		assertEquals(TestBean.class, lbf.getType("singletonObject"));
		assertEquals(0, lbf.getAliases("singletonObject").length);
		DependenciesBean test = (DependenciesBean) lbf.getBean("test");
		assertEquals(singletonObject, lbf.getBean("singletonObject"));
		assertEquals(singletonObject, test.getSpouse());
	}

	@Test
	public void testRegisterExistingSingletonWithAlreadyBound() {
		Object singletonObject = new TestBean();
		lbf.registerSingleton("singletonObject", singletonObject);
		try {
			lbf.registerSingleton("singletonObject", singletonObject);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void testReregisterBeanDefinition() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("testBean", bd1);
		assertTrue(lbf.getBean("testBean") instanceof TestBean);
		RootBeanDefinition bd2 = new RootBeanDefinition(NestedTestBean.class);
		bd2.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("testBean", bd2);
		assertTrue(lbf.getBean("testBean") instanceof NestedTestBean);
	}

	@Test
	public void testArrayPropertyWithAutowiring() throws MalformedURLException {
		lbf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		lbf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		lbf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");

		assertEquals(new UrlResource("http://localhost:8080"), ab.getResourceArray()[0]);
		assertEquals(new UrlResource("http://localhost:9090"), ab.getResourceArray()[1]);
	}

	@Test
	public void testArrayPropertyWithOptionalAutowiring() throws MalformedURLException {
		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		lbf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");

		assertNull(ab.getResourceArray());
	}

	@Test
	public void testArrayConstructorWithAutowiring() {
		lbf.registerSingleton("integer1", new Integer(4));
		lbf.registerSingleton("integer2", new Integer(5));

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");

		assertEquals(new Integer(4), ab.getIntegerArray()[0]);
		assertEquals(new Integer(5), ab.getIntegerArray()[1]);
	}

	@Test
	public void testArrayConstructorWithOptionalAutowiring() {
		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");

		assertNull(ab.getIntegerArray());
	}

	@Test
	public void testDoubleArrayConstructorWithAutowiring() throws MalformedURLException {
		lbf.registerSingleton("integer1", new Integer(4));
		lbf.registerSingleton("integer2", new Integer(5));
		lbf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		lbf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");

		assertEquals(new Integer(4), ab.getIntegerArray()[0]);
		assertEquals(new Integer(5), ab.getIntegerArray()[1]);
		assertEquals(new UrlResource("http://localhost:8080"), ab.getResourceArray()[0]);
		assertEquals(new UrlResource("http://localhost:9090"), ab.getResourceArray()[1]);
	}

	@Test
	public void testDoubleArrayConstructorWithOptionalAutowiring() throws MalformedURLException {
		lbf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		lbf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) lbf.getBean("arrayBean");

		assertNull(ab.getIntegerArray());
		assertNull(ab.getResourceArray());
	}

	@Test
	public void testExpressionInStringArray() {
		BeanExpressionResolver beanExpressionResolver = mock(BeanExpressionResolver.class);
		when(beanExpressionResolver.evaluate(eq("#{foo}"), ArgumentMatchers.any(BeanExpressionContext.class)))
				.thenReturn("classpath:/org/springframework/beans/factory/xml/util.properties");
		lbf.setBeanExpressionResolver(beanExpressionResolver);

		RootBeanDefinition rbd = new RootBeanDefinition(PropertiesFactoryBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("locations", new String[]{"#{foo}"});
		rbd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("myProperties", rbd);
		Properties properties = (Properties) lbf.getBean("myProperties");
		assertEquals("bar", properties.getProperty("foo"));
	}

	@Test
	public void testAutowireWithNoDependencies() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("rod", bd);
		assertEquals(1, lbf.getBeanDefinitionCount());
		Object registered = lbf.autowire(NoDependencies.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		assertEquals(1, lbf.getBeanDefinitionCount());
		assertTrue(registered instanceof NoDependencies);
	}

	@Test
	public void testAutowireWithSatisfiedJavaBeanDependency() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("rod", bd);
		assertEquals(1, lbf.getBeanDefinitionCount());
		// Depends on age, name and spouse (TestBean)
		Object registered = lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		assertEquals(1, lbf.getBeanDefinitionCount());
		DependenciesBean kerry = (DependenciesBean) registered;
		TestBean rod = (TestBean) lbf.getBean("rod");
		assertSame(rod, kerry.getSpouse());
	}

	@Test
	public void testAutowireWithSatisfiedConstructorDependency() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", "Rod");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("rod", bd);
		assertEquals(1, lbf.getBeanDefinitionCount());
		Object registered = lbf.autowire(ConstructorDependency.class, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
		assertEquals(1, lbf.getBeanDefinitionCount());
		ConstructorDependency kerry = (ConstructorDependency) registered;
		TestBean rod = (TestBean) lbf.getBean("rod");
		assertSame(rod, kerry.spouse);
	}

	@Test
	public void testAutowireWithTwoMatchesForConstructorDependency() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("rod", bd);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("rod2", bd2);
		try {
			lbf.autowire(ConstructorDependency.class, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException ex) {
			// expected
			assertTrue(ex.getMessage().contains("rod"));
			assertTrue(ex.getMessage().contains("rod2"));
		}
	}

	@Test
	public void testAutowireWithUnsatisfiedConstructorDependency() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("name", "Rod"));
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("rod", bd);
		assertEquals(1, lbf.getBeanDefinitionCount());
		try {
			lbf.autowire(UnsatisfiedConstructorDependency.class, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, true);
			fail("Should have unsatisfied constructor dependency on SideEffectBean");
		}
		catch (UnsatisfiedDependencyException ex) {
			// expected
		}
	}

	@Test
	public void testAutowireConstructor() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spouse", bd);
		ConstructorDependenciesBean bean = (ConstructorDependenciesBean)
				lbf.autowire(ConstructorDependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, true);
		Object spouse = lbf.getBean("spouse");
		assertTrue(bean.getSpouse1() == spouse);
		assertTrue(BeanFactoryUtils.beanOfType(lbf, TestBean.class) == spouse);
	}

	@Test
	public void testAutowireBeanByName() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spouse", bd);
		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);
		TestBean spouse = (TestBean) lbf.getBean("spouse");
		assertEquals(spouse, bean.getSpouse());
		assertTrue(BeanFactoryUtils.beanOfType(lbf, TestBean.class) == spouse);
	}

	@Test
	public void testAutowireBeanByNameWithDependencyCheck() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spous", bd);
		try {
			lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException ex) {
			// expected
		}
	}

	@Test
	public void testAutowireBeanByNameWithNoDependencyCheck() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spous", bd);
		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		assertNull(bean.getSpouse());
	}

	@Test
	public void testDependsOnCycle() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setDependsOn("tb2");
		lbf.registerBeanDefinition("tb1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setDependsOn("tb1");
		lbf.registerBeanDefinition("tb2", bd2);
		try {
			lbf.preInstantiateSingletons();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
			assertTrue(ex.getMessage().contains("Circular"));
			assertTrue(ex.getMessage().contains("'tb2'"));
			assertTrue(ex.getMessage().contains("'tb1'"));
		}
	}

	@Test
	public void testImplicitDependsOnCycle() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setDependsOn("tb2");
		lbf.registerBeanDefinition("tb1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setDependsOn("tb3");
		lbf.registerBeanDefinition("tb2", bd2);
		RootBeanDefinition bd3 = new RootBeanDefinition(TestBean.class);
		bd3.setDependsOn("tb1");
		lbf.registerBeanDefinition("tb3", bd3);
		try {
			lbf.preInstantiateSingletons();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
			assertTrue(ex.getMessage().contains("Circular"));
			assertTrue(ex.getMessage().contains("'tb3'"));
			assertTrue(ex.getMessage().contains("'tb1'"));
		}
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void testGetBeanByTypeWithNoneFound() {
		lbf.getBean(TestBean.class);
	}

	@Test
	public void testGetBeanByTypeWithLateRegistration() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		try {
			lbf.getBean(TestBean.class);
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName(), equalTo("bd1"));
	}

	@Test
	public void testGetBeanByTypeWithLateRegistrationAgainstFrozen() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		lbf.freezeConfiguration();
		try {
			lbf.getBean(TestBean.class);
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName(), equalTo("bd1"));
	}

	@Test
	public void testGetBeanByTypeDefinedInParent() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		parent.registerBeanDefinition("bd1", bd1);
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory(parent);
		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName(), equalTo("bd1"));
	}

	@Test(expected = NoUniqueBeanDefinitionException.class)
	public void testGetBeanByTypeWithAmbiguity() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);
		lbf.getBean(TestBean.class);
	}

	@Test
	public void testGetBeanByTypeWithPrimary() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setLazyInit(true);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);
		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName(), equalTo("bd2"));
		assertFalse(lbf.containsSingleton("bd1"));
	}

	@Test
	public void testGetBeanByTypeWithMultiplePrimary() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setPrimary(true);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);
		thrown.expect(NoUniqueBeanDefinitionException.class);
		thrown.expectMessage(containsString("more than one 'primary'"));
		lbf.getBean(TestBean.class);
	}

	@Test
	public void testGetBeanByTypeWithPriority() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd1 = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(LowPriorityTestBean.class);
		RootBeanDefinition bd3 = new RootBeanDefinition(NullTestBeanFactoryBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);
		lbf.registerBeanDefinition("bd3", bd3);
		lbf.preInstantiateSingletons();
		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName(), equalTo("bd1"));
	}

	@Test
	public void testMapInjectionWithPriority() {
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
		assertThat(bean.getBeanName(), equalTo("bd1"));
	}

	@Test
	public void testGetBeanByTypeWithMultiplePriority() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd1 = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(HighPriorityTestBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);
		thrown.expect(NoUniqueBeanDefinitionException.class);
		thrown.expectMessage(containsString("Multiple beans found with the same priority"));
		thrown.expectMessage(containsString("5"));  // conflicting priority
		lbf.getBean(TestBean.class);
	}

	@Test
	public void testGetBeanByTypeWithPriorityAndNullInstance() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd1 = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(NullTestBeanFactoryBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);
		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName(), equalTo("bd1"));
	}

	@Test
	public void testGetBeanByTypePrimaryHasPrecedenceOverPriority() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd1 = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);
		TestBean bean = lbf.getBean(TestBean.class);
		assertThat(bean.getBeanName(), equalTo("bd2"));
	}

	@Test
	public void testGetBeanByTypeFiltersOutNonAutowireCandidates() {
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition na1 = new RootBeanDefinition(TestBean.class);
		na1.setAutowireCandidate(false);

		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("na1", na1);
		TestBean actual = lbf.getBean(TestBean.class);  // na1 was filtered
		assertSame(lbf.getBean("bd1", TestBean.class), actual);

		lbf.registerBeanDefinition("bd2", bd2);
		try {
			lbf.getBean(TestBean.class);
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
	}

	@Test
	public void getBeanByTypeWithNullRequiredType() {
		thrown.expect(IllegalArgumentException.class);
		lbf.getBean((Class<?>) null);
	}

	@Test
	public void getBeanProviderByTypeWithNullRequiredType() {
		thrown.expect(IllegalArgumentException.class);
		lbf.getBeanProvider((Class<?>) null);
	}

	@Test
	public void resolveNamedBeanByTypeWithNullRequiredType() {
		thrown.expect(IllegalArgumentException.class);
		lbf.resolveNamedBean((Class<?>) null);
	}

	@Test
	public void testGetBeanByTypeInstanceWithNoneFound() {

		try {
			lbf.getBean(ConstructorDependency.class);
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
		try {
			lbf.getBean(ConstructorDependency.class, 42);
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}

		ObjectProvider<ConstructorDependency> provider = lbf.getBeanProvider(ConstructorDependency.class);
		try {
			provider.getObject();
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
		try {
			provider.getObject(42);
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
		assertNull(provider.getIfAvailable());
		assertNull(provider.getIfUnique());
	}

	@Test
	public void testGetBeanByTypeInstanceDefinedInParent() {
		DefaultListableBeanFactory parent = new DefaultListableBeanFactory();
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		parent.registerBeanDefinition("bd1", bd1);
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory(parent);

		ConstructorDependency bean = lbf.getBean(ConstructorDependency.class);
		assertThat(bean.beanName, equalTo("bd1"));
		assertThat(bean.spouseAge, equalTo(99));
		bean = lbf.getBean(ConstructorDependency.class, 42);
		assertThat(bean.beanName, equalTo("bd1"));
		assertThat(bean.spouseAge, equalTo(42));

		ObjectProvider<ConstructorDependency> provider = lbf.getBeanProvider(ConstructorDependency.class);
		bean = provider.getObject();
		assertThat(bean.beanName, equalTo("bd1"));
		assertThat(bean.spouseAge, equalTo(99));
		bean = provider.getObject(42);
		assertThat(bean.beanName, equalTo("bd1"));
		assertThat(bean.spouseAge, equalTo(42));
		bean = provider.getIfAvailable();
		assertThat(bean.beanName, equalTo("bd1"));
		assertThat(bean.spouseAge, equalTo(99));
		bean = provider.getIfUnique();
		assertThat(bean.beanName, equalTo("bd1"));
		assertThat(bean.spouseAge, equalTo(99));
	}

	@Test
	public void testGetBeanByTypeInstanceWithAmbiguity() {
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		RootBeanDefinition bd2 = new RootBeanDefinition(ConstructorDependency.class);
		bd2.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bd2.getConstructorArgumentValues().addGenericArgumentValue("43");
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		try {
			lbf.getBean(ConstructorDependency.class);
			fail("Should have thrown NoUniqueBeanDefinitionException");
		}
		catch (NoUniqueBeanDefinitionException ex) {
			// expected
		}
		try {
			lbf.getBean(ConstructorDependency.class, 42);
			fail("Should have thrown NoUniqueBeanDefinitionException");
		}
		catch (NoUniqueBeanDefinitionException ex) {
			// expected
		}

		ObjectProvider<ConstructorDependency> provider = lbf.getBeanProvider(ConstructorDependency.class);
		try {
			provider.getObject();
			fail("Should have thrown NoUniqueBeanDefinitionException");
		}
		catch (NoUniqueBeanDefinitionException ex) {
			// expected
		}
		try {
			provider.getObject(42);
			fail("Should have thrown NoUniqueBeanDefinitionException");
		}
		catch (NoUniqueBeanDefinitionException ex) {
			// expected
		}
		try {
			provider.getIfAvailable();
			fail("Should have thrown NoUniqueBeanDefinitionException");
		}
		catch (NoUniqueBeanDefinitionException ex) {
			// expected
		}
		assertNull(provider.getIfUnique());

		Set<Object> resolved = new HashSet<>();
		for (ConstructorDependency instance : provider) {
			resolved.add(instance);
		}
		assertEquals(2, resolved.size());
		assertTrue(resolved.contains(lbf.getBean("bd1")));
		assertTrue(resolved.contains(lbf.getBean("bd2")));

		resolved = new HashSet<>();
		provider.forEach(resolved::add);
		assertEquals(2, resolved.size());
		assertTrue(resolved.contains(lbf.getBean("bd1")));
		assertTrue(resolved.contains(lbf.getBean("bd2")));

		resolved = provider.stream().collect(Collectors.toSet());
		assertEquals(2, resolved.size());
		assertTrue(resolved.contains(lbf.getBean("bd1")));
		assertTrue(resolved.contains(lbf.getBean("bd2")));
	}

	@Test
	public void testGetBeanByTypeInstanceWithPrimary() {
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		RootBeanDefinition bd2 = createConstructorDependencyBeanDefinition(43);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		ConstructorDependency bean = lbf.getBean(ConstructorDependency.class);
		assertThat(bean.beanName, equalTo("bd2"));
		assertThat(bean.spouseAge, equalTo(43));
		bean = lbf.getBean(ConstructorDependency.class, 42);
		assertThat(bean.beanName, equalTo("bd2"));
		assertThat(bean.spouseAge, equalTo(42));

		ObjectProvider<ConstructorDependency> provider = lbf.getBeanProvider(ConstructorDependency.class);
		bean = provider.getObject();
		assertThat(bean.beanName, equalTo("bd2"));
		assertThat(bean.spouseAge, equalTo(43));
		bean = provider.getObject(42);
		assertThat(bean.beanName, equalTo("bd2"));
		assertThat(bean.spouseAge, equalTo(42));
		bean = provider.getIfAvailable();
		assertThat(bean.beanName, equalTo("bd2"));
		assertThat(bean.spouseAge, equalTo(43));
		bean = provider.getIfUnique();
		assertThat(bean.beanName, equalTo("bd2"));
		assertThat(bean.spouseAge, equalTo(43));

		Set<Object> resolved = new HashSet<>();
		for (ConstructorDependency instance : provider) {
			resolved.add(instance);
		}
		assertEquals(2, resolved.size());
		assertTrue(resolved.contains(lbf.getBean("bd1")));
		assertTrue(resolved.contains(lbf.getBean("bd2")));

		resolved = new HashSet<>();
		provider.forEach(resolved::add);
		assertEquals(2, resolved.size());
		assertTrue(resolved.contains(lbf.getBean("bd1")));
		assertTrue(resolved.contains(lbf.getBean("bd2")));

		resolved = provider.stream().collect(Collectors.toSet());
		assertEquals(2, resolved.size());
		assertTrue(resolved.contains(lbf.getBean("bd1")));
		assertTrue(resolved.contains(lbf.getBean("bd2")));
	}

	@Test
	public void testGetBeanByTypeInstanceWithMultiplePrimary() {
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		RootBeanDefinition bd2 = createConstructorDependencyBeanDefinition(43);
		bd1.setPrimary(true);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);

		thrown.expect(NoUniqueBeanDefinitionException.class);
		thrown.expectMessage(containsString("more than one 'primary'"));
		lbf.getBean(ConstructorDependency.class, 42);
	}

	@Test
	public void testGetBeanByTypeInstanceFiltersOutNonAutowireCandidates() {
		RootBeanDefinition bd1 = createConstructorDependencyBeanDefinition(99);
		RootBeanDefinition bd2 = createConstructorDependencyBeanDefinition(43);
		RootBeanDefinition na1 = createConstructorDependencyBeanDefinition(21);
		na1.setAutowireCandidate(false);

		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("na1", na1);
		ConstructorDependency actual = lbf.getBean(ConstructorDependency.class, 42);  // na1 was filtered
		assertThat(actual.beanName, equalTo("bd1"));

		lbf.registerBeanDefinition("bd2", bd2);
		try {
			lbf.getBean(TestBean.class, 67);
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
	}

	@Test
	public void testBeanProviderSerialization() throws Exception {
		lbf.setSerializationId("test");

		ObjectProvider<ConstructorDependency> provider = lbf.getBeanProvider(ConstructorDependency.class);
		ObjectProvider deserialized = (ObjectProvider) SerializationTestUtils.serializeAndDeserialize(provider);
		try {
			deserialized.getObject();
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
		try {
			deserialized.getObject(42);
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
		assertNull(deserialized.getIfAvailable());
		assertNull(deserialized.getIfUnique());
	}

	@Test
	public void testGetBeanWithArgsNotCreatedForFactoryBeanChecking() {
		RootBeanDefinition bd1 = new RootBeanDefinition(ConstructorDependency.class);
		bd1.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("bd1", bd1);
		RootBeanDefinition bd2 = new RootBeanDefinition(ConstructorDependencyFactoryBean.class);
		bd2.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("bd2", bd2);

		ConstructorDependency bean = lbf.getBean(ConstructorDependency.class, 42);
		assertThat(bean.beanName, equalTo("bd1"));
		assertThat(bean.spouseAge, equalTo(42));

		assertEquals(1, lbf.getBeanNamesForType(ConstructorDependency.class).length);
		assertEquals(1, lbf.getBeanNamesForType(ConstructorDependencyFactoryBean.class).length);
		assertEquals(1, lbf.getBeanNamesForType(ResolvableType.forClassWithGenerics(FactoryBean.class, Object.class)).length);
		assertEquals(0, lbf.getBeanNamesForType(ResolvableType.forClassWithGenerics(FactoryBean.class, String.class)).length);
	}

	private RootBeanDefinition createConstructorDependencyBeanDefinition(int age) {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependency.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bd.getConstructorArgumentValues().addGenericArgumentValue(age);
		return bd;
	}

	@Test
	public void testAutowireBeanByType() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("test", bd);
		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		TestBean test = (TestBean) lbf.getBean("test");
		assertEquals(test, bean.getSpouse());
	}

	/**
	 * Verifies that a dependency on a {@link FactoryBean} can be autowired
	 * <em>by type</em>, specifically addressing the JIRA issue raised in <a
	 * href="https://opensource.atlassian.com/projects/spring/browse/SPR-4040"
	 * target="_blank">SPR-4040</a>.
	 */
	@Test
	public void testAutowireBeanWithFactoryBeanByType() {
		RootBeanDefinition bd = new RootBeanDefinition(LazyInitFactory.class);
		lbf.registerBeanDefinition("factoryBean", bd);
		LazyInitFactory factoryBean = (LazyInitFactory) lbf.getBean("&factoryBean");
		assertNotNull("The FactoryBean should have been registered.", factoryBean);
		FactoryBeanDependentBean bean = (FactoryBeanDependentBean) lbf.autowire(FactoryBeanDependentBean.class,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		assertEquals("The FactoryBeanDependentBean should have been autowired 'by type' with the LazyInitFactory.",
				factoryBean, bean.getFactoryBean());
	}

	@Test
	public void testGetTypeForAbstractFactoryBean() {
		RootBeanDefinition bd = new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class);
		bd.setAbstract(true);
		lbf.registerBeanDefinition("factoryBean", bd);
		assertNull(lbf.getType("factoryBean"));
	}

	@Test
	public void testGetBeanNamesForTypeBeforeFactoryBeanCreation() {
		lbf.registerBeanDefinition("factoryBean", new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class));
		assertFalse(lbf.containsSingleton("factoryBean"));

		String[] beanNames = lbf.getBeanNamesForType(Runnable.class, false, false);
		assertEquals(1, beanNames.length);
		assertEquals("&factoryBean", beanNames[0]);

		beanNames = lbf.getBeanNamesForType(Callable.class, false, false);
		assertEquals(1, beanNames.length);
		assertEquals("&factoryBean", beanNames[0]);

		beanNames = lbf.getBeanNamesForType(RepositoryFactoryInformation.class, false, false);
		assertEquals(1, beanNames.length);
		assertEquals("&factoryBean", beanNames[0]);

		beanNames = lbf.getBeanNamesForType(FactoryBean.class, false, false);
		assertEquals(1, beanNames.length);
		assertEquals("&factoryBean", beanNames[0]);
	}

	@Test
	public void testGetBeanNamesForTypeAfterFactoryBeanCreation() {
		lbf.registerBeanDefinition("factoryBean", new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class));
		lbf.getBean("&factoryBean");

		String[] beanNames = lbf.getBeanNamesForType(Runnable.class, false, false);
		assertEquals(1, beanNames.length);
		assertEquals("&factoryBean", beanNames[0]);

		beanNames = lbf.getBeanNamesForType(Callable.class, false, false);
		assertEquals(1, beanNames.length);
		assertEquals("&factoryBean", beanNames[0]);

		beanNames = lbf.getBeanNamesForType(RepositoryFactoryInformation.class, false, false);
		assertEquals(1, beanNames.length);
		assertEquals("&factoryBean", beanNames[0]);

		beanNames = lbf.getBeanNamesForType(FactoryBean.class, false, false);
		assertEquals(1, beanNames.length);
		assertEquals("&factoryBean", beanNames[0]);
	}

	/**
	 * Verifies that a dependency on a {@link FactoryBean} can <strong>not</strong>
	 * be autowired <em>by name</em>, as &amp; is an illegal character in
	 * Java method names. In other words, you can't name a method
	 * {@code set&amp;FactoryBean(...)}.
	 */
	@Test(expected = TypeMismatchException.class)
	public void testAutowireBeanWithFactoryBeanByName() {
		RootBeanDefinition bd = new RootBeanDefinition(LazyInitFactory.class);
		lbf.registerBeanDefinition("factoryBean", bd);
		LazyInitFactory factoryBean = (LazyInitFactory) lbf.getBean("&factoryBean");
		assertNotNull("The FactoryBean should have been registered.", factoryBean);
		lbf.autowire(FactoryBeanDependentBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);
	}

	@Test
	public void testAutowireBeanByTypeWithTwoMatches() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);
		try {
			lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException ex) {
			// expected
			assertTrue(ex.getMessage().contains("test"));
			assertTrue(ex.getMessage().contains("spouse"));
		}
	}

	@Test
	public void testAutowireBeanByTypeWithDependencyCheck() {
		try {
			lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException ex) {
			// expected
		}
	}

	@Test
	public void testAutowireBeanByTypeWithNoDependencyCheck() {
		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		assertNull(bean.getSpouse());
	}

	@Test
	public void testAutowireBeanByTypeWithTwoMatchesAndOnePrimary() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPrimary(true);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);

		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		assertThat(bean.getSpouse(), equalTo(lbf.getBean("test")));
	}

	@Test
	public void testAutowireBeanByTypeWithTwoPrimaryCandidates() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPrimary(true);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);

		try {
			lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException ex) {
			// expected
			assertNotNull("Exception should have cause", ex.getCause());
			assertEquals("Wrong cause type", NoUniqueBeanDefinitionException.class, ex.getCause().getClass());
		}
	}

	@Test
	public void testAutowireBeanByTypeWithTwoMatchesAndPriority() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(LowPriorityTestBean.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);

		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		assertThat(bean.getSpouse(), equalTo(lbf.getBean("test")));
	}

	@Test
	public void testAutowireBeanByTypeWithIdenticalPriorityCandidates() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(HighPriorityTestBean.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);

		try {
			lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException ex) {
			// expected
			assertNotNull("Exception should have cause", ex.getCause());
			assertEquals("Wrong cause type", NoUniqueBeanDefinitionException.class, ex.getCause().getClass());
			assertTrue(ex.getMessage().contains("5"));  // conflicting priority
		}
	}

	@Test
	public void testAutowireBeanByTypePrimaryTakesPrecedenceOverPriority() {
		lbf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		RootBeanDefinition bd = new RootBeanDefinition(HighPriorityTestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPrimary(true);
		lbf.registerBeanDefinition("test", bd);
		lbf.registerBeanDefinition("spouse", bd2);

		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		assertThat(bean.getSpouse(), equalTo(lbf.getBean("spouse")));
	}

	@Test
	public void testAutowireExistingBeanByName() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spouse", bd);
		DependenciesBean existingBean = new DependenciesBean();
		lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);
		TestBean spouse = (TestBean) lbf.getBean("spouse");
		assertEquals(existingBean.getSpouse(), spouse);
		assertSame(spouse, BeanFactoryUtils.beanOfType(lbf, TestBean.class));
	}

	@Test
	public void testAutowireExistingBeanByNameWithDependencyCheck() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spous", bd);
		DependenciesBean existingBean = new DependenciesBean();
		try {
			lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException ex) {
			// expected
		}
	}

	@Test
	public void testAutowireExistingBeanByNameWithNoDependencyCheck() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spous", bd);
		DependenciesBean existingBean = new DependenciesBean();
		lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		assertNull(existingBean.getSpouse());
	}

	@Test
	public void testAutowireExistingBeanByType() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("test", bd);
		DependenciesBean existingBean = new DependenciesBean();
		lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		TestBean test = (TestBean) lbf.getBean("test");
		assertEquals(existingBean.getSpouse(), test);
	}

	@Test
	public void testAutowireExistingBeanByTypeWithDependencyCheck() {
		DependenciesBean existingBean = new DependenciesBean();
		try {
			lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException expected) {
		}
	}

	@Test
	public void testAutowireExistingBeanByTypeWithNoDependencyCheck() {
		DependenciesBean existingBean = new DependenciesBean();
		lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		assertNull(existingBean.getSpouse());
	}

	@Test
	public void testInvalidAutowireMode() {
		try {
			lbf.autowireBeanProperties(new TestBean(), AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void testApplyBeanPropertyValues() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("age", "99");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("test", bd);
		TestBean tb = new TestBean();
		assertEquals(0, tb.getAge());
		lbf.applyBeanPropertyValues(tb, "test");
		assertEquals(99, tb.getAge());
	}

	@Test
	public void testApplyBeanPropertyValuesWithIncompleteDefinition() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("age", "99");
		RootBeanDefinition bd = new RootBeanDefinition();
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("test", bd);
		TestBean tb = new TestBean();
		assertEquals(0, tb.getAge());
		lbf.applyBeanPropertyValues(tb, "test");
		assertEquals(99, tb.getAge());
		assertNull(tb.getBeanFactory());
		assertNull(tb.getSpouse());
	}

	@Test
	public void testCreateBean() {
		TestBean tb = lbf.createBean(TestBean.class);
		assertSame(lbf, tb.getBeanFactory());
		lbf.destroyBean(tb);
	}

	@Test
	public void testCreateBeanWithDisposableBean() {
		DerivedTestBean tb = lbf.createBean(DerivedTestBean.class);
		assertSame(lbf, tb.getBeanFactory());
		lbf.destroyBean(tb);
		assertTrue(tb.wasDestroyed());
	}

	@Test
	public void testConfigureBean() {
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("age", "99");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		lbf.registerBeanDefinition("test", bd);
		TestBean tb = new TestBean();
		assertEquals(0, tb.getAge());
		lbf.configureBean(tb, "test");
		assertEquals(99, tb.getAge());
		assertSame(lbf, tb.getBeanFactory());
		assertNull(tb.getSpouse());
	}

	@Test
	public void testConfigureBeanWithAutowiring() {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spouse", bd);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("age", "99");
		RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
		tbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_NAME);
		lbf.registerBeanDefinition("test", tbd);
		TestBean tb = new TestBean();
		lbf.configureBean(tb, "test");
		assertSame(lbf, tb.getBeanFactory());
		TestBean spouse = (TestBean) lbf.getBean("spouse");
		assertEquals(spouse, tb.getSpouse());
	}

	@Test
	public void testExtensiveCircularReference() {
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
			assertTrue(bean.getSpouse() == otherBean);
		}
	}

	@Test
	public void testCircularReferenceThroughAutowiring() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);
		try {
			lbf.preInstantiateSingletons();
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException expected) {
		}
	}

	@Test
	public void testCircularReferenceThroughFactoryBeanAutowiring() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyFactoryBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);
		try {
			lbf.preInstantiateSingletons();
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException expected) {
		}
	}

	@Test
	public void testCircularReferenceThroughFactoryBeanTypeCheck() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyFactoryBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);
		try {
			lbf.getBeansOfType(String.class);
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException expected) {
		}
	}

	@Test
	public void testAvoidCircularReferenceThroughAutowiring() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyFactoryBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);
		RootBeanDefinition bd2 = new RootBeanDefinition(String.class);
		bd2.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("string", bd2);
		lbf.preInstantiateSingletons();
	}

	@Test
	public void testConstructorDependencyWithClassResolution() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyWithClassResolution.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("java.lang.String");
		lbf.registerBeanDefinition("test", bd);
		lbf.preInstantiateSingletons();
	}

	@Test
	public void testConstructorDependencyWithUnresolvableClass() {
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyWithClassResolution.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("java.lang.Strin");
		lbf.registerBeanDefinition("test", bd);
		try {
			lbf.preInstantiateSingletons();
			fail("Should have thrown UnsatisfiedDependencyException");
		}
		catch (UnsatisfiedDependencyException expected) {
			assertTrue(expected.toString().contains("java.lang.Strin"));
		}
	}

	@Test
	public void testBeanDefinitionWithInterface() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(ITestBean.class));
		try {
			lbf.getBean("test");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertEquals("test", ex.getBeanName());
			assertTrue(ex.getMessage().toLowerCase().contains("interface"));
		}
	}

	@Test
	public void testBeanDefinitionWithAbstractClass() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(AbstractBeanFactory.class));
		try {
			lbf.getBean("test");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertEquals("test", ex.getBeanName());
			assertTrue(ex.getMessage().toLowerCase().contains("abstract"));
		}
	}

	@Test
	public void testPrototypeFactoryBeanNotEagerlyCalled() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class));
		lbf.preInstantiateSingletons();
	}

	@Test
	public void testLazyInitFactory() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(LazyInitFactory.class));
		lbf.preInstantiateSingletons();
		LazyInitFactory factory = (LazyInitFactory) lbf.getBean("&test");
		assertFalse(factory.initialized);
	}

	@Test
	public void testSmartInitFactory() {
		lbf.registerBeanDefinition("test", new RootBeanDefinition(EagerInitFactory.class));
		lbf.preInstantiateSingletons();
		EagerInitFactory factory = (EagerInitFactory) lbf.getBean("&test");
		assertTrue(factory.initialized);
	}

	@Test
	public void testPrototypeFactoryBeanNotEagerlyCalledInCaseOfBeanClassName() {
		lbf.registerBeanDefinition("test",
				new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class.getName(), null, null));
		lbf.preInstantiateSingletons();
	}

	@Test
	public void testPrototypeStringCreatedRepeatedly() {
		RootBeanDefinition stringDef = new RootBeanDefinition(String.class);
		stringDef.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		stringDef.getConstructorArgumentValues().addGenericArgumentValue(new TypedStringValue("value"));
		lbf.registerBeanDefinition("string", stringDef);
		String val1 = lbf.getBean("string", String.class);
		String val2 = lbf.getBean("string", String.class);
		assertEquals("value", val1);
		assertEquals("value", val2);
		assertNotSame(val1, val2);
	}

	@Test
	public void testPrototypeWithArrayConversionForConstructor() {
		List<String> list = new ManagedList<>();
		list.add("myName");
		list.add("myBeanName");
		RootBeanDefinition bd = new RootBeanDefinition(DerivedTestBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bd.getConstructorArgumentValues().addGenericArgumentValue(list);
		lbf.registerBeanDefinition("test", bd);
		DerivedTestBean tb = (DerivedTestBean) lbf.getBean("test");
		assertEquals("myName", tb.getName());
		assertEquals("myBeanName", tb.getBeanName());
		DerivedTestBean tb2 = (DerivedTestBean) lbf.getBean("test");
		assertTrue(tb != tb2);
		assertEquals("myName", tb2.getName());
		assertEquals("myBeanName", tb2.getBeanName());
	}

	@Test
	public void testPrototypeWithArrayConversionForFactoryMethod() {
		List<String> list = new ManagedList<>();
		list.add("myName");
		list.add("myBeanName");
		RootBeanDefinition bd = new RootBeanDefinition(DerivedTestBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bd.setFactoryMethodName("create");
		bd.getConstructorArgumentValues().addGenericArgumentValue(list);
		lbf.registerBeanDefinition("test", bd);
		DerivedTestBean tb = (DerivedTestBean) lbf.getBean("test");
		assertEquals("myName", tb.getName());
		assertEquals("myBeanName", tb.getBeanName());
		DerivedTestBean tb2 = (DerivedTestBean) lbf.getBean("test");
		assertTrue(tb != tb2);
		assertEquals("myName", tb2.getName());
		assertEquals("myBeanName", tb2.getBeanName());
	}

	@Test
	public void testPrototypeCreationIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("test", rbd);
		lbf.freezeConfiguration();
		StopWatch sw = new StopWatch();
		sw.start("prototype");
		for (int i = 0; i < 100000; i++) {
			lbf.getBean("test");
		}
		sw.stop();
		// System.out.println(sw.getTotalTimeMillis());
		assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 3000);
	}

	@Test
	public void testPrototypeCreationWithDependencyCheckIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		RootBeanDefinition rbd = new RootBeanDefinition(LifecycleBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.setDependencyCheck(RootBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
		lbf.registerBeanDefinition("test", rbd);
		lbf.addBeanPostProcessor(new LifecycleBean.PostProcessor());
		lbf.freezeConfiguration();
		StopWatch sw = new StopWatch();
		sw.start("prototype");
		for (int i = 0; i < 100000; i++) {
			lbf.getBean("test");
		}
		sw.stop();
		// System.out.println(sw.getTotalTimeMillis());
		assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 3000);
	}

	@Test
	@Ignore  // TODO re-enable when ConstructorResolver TODO sorted out
	public void testPrototypeCreationWithConstructorArgumentsIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.getConstructorArgumentValues().addGenericArgumentValue("juergen");
		rbd.getConstructorArgumentValues().addGenericArgumentValue("99");
		lbf.registerBeanDefinition("test", rbd);
		lbf.freezeConfiguration();
		StopWatch sw = new StopWatch();
		sw.start("prototype");
		for (int i = 0; i < 100000; i++) {
			TestBean tb = (TestBean) lbf.getBean("test");
			assertEquals("juergen", tb.getName());
			assertEquals(99, tb.getAge());
		}
		sw.stop();
		// System.out.println(sw.getTotalTimeMillis());
		assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 3000);
	}

	@Test
	public void testPrototypeCreationWithResolvedConstructorArgumentsIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("spouse"));
		lbf.registerBeanDefinition("test", rbd);
		lbf.registerBeanDefinition("spouse", new RootBeanDefinition(TestBean.class));
		lbf.freezeConfiguration();
		TestBean spouse = (TestBean) lbf.getBean("spouse");
		StopWatch sw = new StopWatch();
		sw.start("prototype");
		for (int i = 0; i < 100000; i++) {
			TestBean tb = (TestBean) lbf.getBean("test");
			assertSame(spouse, tb.getSpouse());
		}
		sw.stop();
		// System.out.println(sw.getTotalTimeMillis());
		assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 4000);
	}

	@Test
	public void testPrototypeCreationWithPropertiesIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.getPropertyValues().add("name", "juergen");
		rbd.getPropertyValues().add("age", "99");
		lbf.registerBeanDefinition("test", rbd);
		lbf.freezeConfiguration();
		StopWatch sw = new StopWatch();
		sw.start("prototype");
		for (int i = 0; i < 100000; i++) {
			TestBean tb = (TestBean) lbf.getBean("test");
			assertEquals("juergen", tb.getName());
			assertEquals(99, tb.getAge());
		}
		sw.stop();
		// System.out.println(sw.getTotalTimeMillis());
		assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 4000);
	}

	@Test
	public void testPrototypeCreationWithResolvedPropertiesIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.getPropertyValues().add("spouse", new RuntimeBeanReference("spouse"));
		lbf.registerBeanDefinition("test", rbd);
		lbf.registerBeanDefinition("spouse", new RootBeanDefinition(TestBean.class));
		lbf.freezeConfiguration();
		TestBean spouse = (TestBean) lbf.getBean("spouse");
		StopWatch sw = new StopWatch();
		sw.start("prototype");
		for (int i = 0; i < 100000; i++) {
			TestBean tb = (TestBean) lbf.getBean("test");
			assertSame(spouse, tb.getSpouse());
		}
		sw.stop();
		// System.out.println(sw.getTotalTimeMillis());
		assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 4000);
	}

	@Test
	public void testSingletonLookupByNameIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.freezeConfiguration();
		StopWatch sw = new StopWatch();
		sw.start("singleton");
		for (int i = 0; i < 1000000; i++) {
			lbf.getBean("test");
		}
		sw.stop();
		// System.out.println(sw.getTotalTimeMillis());
		assertTrue("Singleton lookup took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 1000);
	}

	@Test
	public void testSingletonLookupByTypeIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.freezeConfiguration();
		StopWatch sw = new StopWatch();
		sw.start("singleton");
		for (int i = 0; i < 1000000; i++) {
			lbf.getBean(TestBean.class);
		}
		sw.stop();
		// System.out.println(sw.getTotalTimeMillis());
		assertTrue("Singleton lookup took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 1000);
	}

	@Test
	public void testBeanPostProcessorWithWrappedObjectAndDisposableBean() {
		RootBeanDefinition bd = new RootBeanDefinition(BeanWithDisposableBean.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return new TestBean();
			}
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				return bean;
			}
		});
		BeanWithDisposableBean.closed = false;
		lbf.preInstantiateSingletons();
		lbf.destroySingletons();
		assertTrue("Destroy method invoked", BeanWithDisposableBean.closed);
	}

	@Test
	public void testBeanPostProcessorWithWrappedObjectAndCloseable() {
		RootBeanDefinition bd = new RootBeanDefinition(BeanWithCloseable.class);
		lbf.registerBeanDefinition("test", bd);
		lbf.addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return new TestBean();
			}
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				return bean;
			}
		});
		BeanWithDisposableBean.closed = false;
		lbf.preInstantiateSingletons();
		lbf.destroySingletons();
		assertTrue("Destroy method invoked", BeanWithCloseable.closed);
	}

	@Test
	public void testBeanPostProcessorWithWrappedObjectAndDestroyMethod() {
		RootBeanDefinition bd = new RootBeanDefinition(BeanWithDestroyMethod.class);
		bd.setDestroyMethodName("close");
		lbf.registerBeanDefinition("test", bd);
		lbf.addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return new TestBean();
			}
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				return bean;
			}
		});
		BeanWithDestroyMethod.closeCount = 0;
		lbf.preInstantiateSingletons();
		lbf.destroySingletons();
		assertEquals("Destroy methods invoked", 1, BeanWithDestroyMethod.closeCount);
	}

	@Test
	public void testDestroyMethodOnInnerBean() {
		RootBeanDefinition innerBd = new RootBeanDefinition(BeanWithDestroyMethod.class);
		innerBd.setDestroyMethodName("close");
		RootBeanDefinition bd = new RootBeanDefinition(BeanWithDestroyMethod.class);
		bd.setDestroyMethodName("close");
		bd.getPropertyValues().add("inner", innerBd);
		lbf.registerBeanDefinition("test", bd);
		BeanWithDestroyMethod.closeCount = 0;
		lbf.preInstantiateSingletons();
		lbf.destroySingletons();
		assertEquals("Destroy methods invoked", 2, BeanWithDestroyMethod.closeCount);
	}

	@Test
	public void testDestroyMethodOnInnerBeanAsPrototype() {
		RootBeanDefinition innerBd = new RootBeanDefinition(BeanWithDestroyMethod.class);
		innerBd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		innerBd.setDestroyMethodName("close");
		RootBeanDefinition bd = new RootBeanDefinition(BeanWithDestroyMethod.class);
		bd.setDestroyMethodName("close");
		bd.getPropertyValues().add("inner", innerBd);
		lbf.registerBeanDefinition("test", bd);
		BeanWithDestroyMethod.closeCount = 0;
		lbf.preInstantiateSingletons();
		lbf.destroySingletons();
		assertEquals("Destroy methods invoked", 1, BeanWithDestroyMethod.closeCount);
	}

	@Test
	public void testFindTypeOfSingletonFactoryMethodOnBeanInstance() {
		findTypeOfPrototypeFactoryMethodOnBeanInstance(true);
	}

	@Test
	public void testFindTypeOfPrototypeFactoryMethodOnBeanInstance() {
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
			factoryMethodDefinitionWithProperties.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		}
		lbf.registerBeanDefinition("fmWithProperties", factoryMethodDefinitionWithProperties);

		RootBeanDefinition factoryMethodDefinitionGeneric = new RootBeanDefinition();
		factoryMethodDefinitionGeneric.setFactoryBeanName("factoryBeanInstance");
		factoryMethodDefinitionGeneric.setFactoryMethodName("createGeneric");
		if (!singleton) {
			factoryMethodDefinitionGeneric.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		}
		lbf.registerBeanDefinition("fmGeneric", factoryMethodDefinitionGeneric);

		RootBeanDefinition factoryMethodDefinitionWithArgs = new RootBeanDefinition();
		factoryMethodDefinitionWithArgs.setFactoryBeanName("factoryBeanInstance");
		factoryMethodDefinitionWithArgs.setFactoryMethodName("createWithArgs");
		ConstructorArgumentValues cvals = new ConstructorArgumentValues();
		cvals.addGenericArgumentValue(expectedNameFromArgs);
		factoryMethodDefinitionWithArgs.setConstructorArgumentValues(cvals);
		if (!singleton) {
			factoryMethodDefinitionWithArgs.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		}
		lbf.registerBeanDefinition("fmWithArgs", factoryMethodDefinitionWithArgs);

		assertEquals(4, lbf.getBeanDefinitionCount());
		List<String> tbNames = Arrays.asList(lbf.getBeanNamesForType(TestBean.class));
		assertTrue(tbNames.contains("fmWithProperties"));
		assertTrue(tbNames.contains("fmWithArgs"));
		assertEquals(2, tbNames.size());

		TestBean tb = (TestBean) lbf.getBean("fmWithProperties");
		TestBean second = (TestBean) lbf.getBean("fmWithProperties");
		if (singleton) {
			assertSame(tb, second);
		}
		else {
			assertNotSame(tb, second);
		}
		assertEquals(expectedNameFromProperties, tb.getName());

		tb = (TestBean) lbf.getBean("fmGeneric");
		second = (TestBean) lbf.getBean("fmGeneric");
		if (singleton) {
			assertSame(tb, second);
		}
		else {
			assertNotSame(tb, second);
		}
		assertEquals(expectedNameFromProperties, tb.getName());

		TestBean tb2 = (TestBean) lbf.getBean("fmWithArgs");
		second = (TestBean) lbf.getBean("fmWithArgs");
		if (singleton) {
			assertSame(tb2, second);
		}
		else {
			assertNotSame(tb2, second);
		}
		assertEquals(expectedNameFromArgs, tb2.getName());
	}

	@Test(expected = IllegalStateException.class)
	public void testScopingBeanToUnregisteredScopeResultsInAnException() {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
		beanDefinition.setScope("he put himself so low could hardly look me in the face");

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("testBean", beanDefinition);
		factory.getBean("testBean");
	}

	@Test
	public void testExplicitScopeInheritanceForChildBeanDefinitions() {
		String theChildScope = "bonanza!";

		RootBeanDefinition parent = new RootBeanDefinition();
		parent.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);

		AbstractBeanDefinition child = BeanDefinitionBuilder.childBeanDefinition("parent").getBeanDefinition();
		child.setBeanClass(TestBean.class);
		child.setScope(theChildScope);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("parent", parent);
		factory.registerBeanDefinition("child", child);

		AbstractBeanDefinition def = (AbstractBeanDefinition) factory.getBeanDefinition("child");
		assertEquals("Child 'scope' not overriding parent scope (it must).", theChildScope, def.getScope());
	}

	@Test
	public void testScopeInheritanceForChildBeanDefinitions() {
		RootBeanDefinition parent = new RootBeanDefinition();
		parent.setScope("bonanza!");

		AbstractBeanDefinition child = new ChildBeanDefinition("parent");
		child.setBeanClass(TestBean.class);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("parent", parent);
		factory.registerBeanDefinition("child", child);

		BeanDefinition def = factory.getMergedBeanDefinition("child");
		assertEquals("Child 'scope' not inherited", "bonanza!", def.getScope());
	}

	@Test
	public void testFieldSettingWithInstantiationAwarePostProcessorNoShortCircuit() {
		doTestFieldSettingWithInstantiationAwarePostProcessor(false);
	}

	@Test
	public void testFieldSettingWithInstantiationAwarePostProcessorWithShortCircuit() {
		doTestFieldSettingWithInstantiationAwarePostProcessor(true);
	}

	private void doTestFieldSettingWithInstantiationAwarePostProcessor(final boolean skipPropertyPopulation) {
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		int ageSetByPropertyValue = 27;
		bd.getPropertyValues().addPropertyValue(new PropertyValue("age", new Integer(ageSetByPropertyValue)));
		lbf.registerBeanDefinition("test", bd);
		final String nameSetOnField = "nameSetOnField";
		lbf.addBeanPostProcessor(new InstantiationAwareBeanPostProcessorAdapter() {
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
					fail("Unexpected exception: " + ex);
					// Keep compiler happy about return
					throw new IllegalStateException();
				}
			}
		});
		lbf.preInstantiateSingletons();
		TestBean tb = (TestBean) lbf.getBean("test");
		assertEquals("Name was set on field by IAPP", nameSetOnField, tb.getName());
		if (!skipPropertyPopulation) {
			assertEquals("Property value still set", ageSetByPropertyValue, tb.getAge());
		}
		else {
			assertEquals("Property value was NOT set and still has default value", 0, tb.getAge());
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInitSecurityAwarePrototypeBean() {
		RootBeanDefinition bd = new RootBeanDefinition(TestSecuredBean.class);
		bd.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
		bd.setInitMethodName("init");
		lbf.registerBeanDefinition("test", bd);
		final Subject subject = new Subject();
		subject.getPrincipals().add(new TestPrincipal("user1"));

		TestSecuredBean bean = (TestSecuredBean) Subject.doAsPrivileged(subject,
				new PrivilegedAction() {
					@Override
					public Object run() {
						return lbf.getBean("test");
					}
				}, null);
		assertNotNull(bean);
		assertEquals("user1", bean.getUserName());
	}

	@Test
	public void testContainsBeanReturnsTrueEvenForAbstractBeanDefinition() {
		lbf.registerBeanDefinition("abs", BeanDefinitionBuilder
				.rootBeanDefinition(TestBean.class).setAbstract(true).getBeanDefinition());
		assertThat(lbf.containsBean("abs"), equalTo(true));
		assertThat(lbf.containsBean("bogus"), equalTo(false));
	}

	@Test
	public void resolveEmbeddedValue() {
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
	public void populatedJavaUtilOptionalBean() {
		RootBeanDefinition bd = new RootBeanDefinition(Optional.class);
		bd.setFactoryMethodName("of");
		bd.getConstructorArgumentValues().addGenericArgumentValue("CONTENT");
		lbf.registerBeanDefinition("optionalBean", bd);
		assertEquals(Optional.of("CONTENT"), lbf.getBean(Optional.class));
	}

	@Test
	public void emptyJavaUtilOptionalBean() {
		RootBeanDefinition bd = new RootBeanDefinition(Optional.class);
		bd.setFactoryMethodName("empty");
		lbf.registerBeanDefinition("optionalBean", bd);
		assertSame(Optional.empty(), lbf.getBean(Optional.class));
	}

	@Test
	public void testNonPublicEnum() {
		RootBeanDefinition bd = new RootBeanDefinition(NonPublicEnumHolder.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("VALUE_1");
		lbf.registerBeanDefinition("holderBean", bd);
		NonPublicEnumHolder holder = (NonPublicEnumHolder) lbf.getBean("holderBean");
		assertEquals(NonPublicEnum.VALUE_1, holder.getNonPublicEnum());
	}

	/**
	 * Test that by-type bean lookup caching is working effectively by searching for a
	 * bean of type B 10K times within a container having 1K additional beans of type A.
	 * Prior to by-type caching, each bean lookup would traverse the entire container
	 * (all 1001 beans), performing expensive assignability checks, etc. Now these
	 * operations are necessary only once, providing a dramatic performance improvement.
	 * On load-free modern hardware (e.g. an 8-core MPB), this method should complete well
	 * under the 1000 ms timeout, usually ~= 300ms. With caching removed and on the same
	 * hardware the method will take ~13000 ms. See SPR-6870.
	 */
	@Test(timeout = 1000)
	public void testByTypeLookupIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);

		for (int i = 0; i < 1000; i++) {
			lbf.registerBeanDefinition("a" + i, new RootBeanDefinition(A.class));
		}
		lbf.registerBeanDefinition("b", new RootBeanDefinition(B.class));

		lbf.freezeConfiguration();

		for (int i = 0; i < 10000; i++) {
			lbf.getBean(B.class);
		}
	}

	@Test(timeout = 1000)
	public void testRegistrationOfManyBeanDefinitionsIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		lbf.registerBeanDefinition("b", new RootBeanDefinition(B.class));
		// lbf.getBean("b");

		for (int i = 0; i < 100000; i++) {
			lbf.registerBeanDefinition("a" + i, new RootBeanDefinition(A.class));
		}
	}

	@Test(timeout = 1000)
	public void testRegistrationOfManySingletonsIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		lbf.registerBeanDefinition("b", new RootBeanDefinition(B.class));
		// lbf.getBean("b");

		for (int i = 0; i < 100000; i++) {
			lbf.registerSingleton("a" + i, new A());
		}
	}


	static class A { }

	static class B { }


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
		@SuppressWarnings("unchecked")
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
				return new Integer(5);
			}
			else {
				return value;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object convertIfNecessary(Object value, @Nullable Class requiredType, @Nullable MethodParameter methodParam) {
			return convertIfNecessary(value, requiredType);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object convertIfNecessary(Object value, @Nullable Class requiredType, @Nullable Field field) {
			return convertIfNecessary(value, requiredType);
		}
	}


	private static class TestPrincipal implements Principal {

		private String name;

		public TestPrincipal(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof TestPrincipal)) {
				return false;
			}
			TestPrincipal p = (TestPrincipal) obj;
			return this.name.equals(p.name);
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}
	}


	@SuppressWarnings("unused")
	private static class TestSecuredBean {

		private String userName;

		public void init() {
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

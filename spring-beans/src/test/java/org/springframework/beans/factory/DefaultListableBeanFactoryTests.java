/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
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
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ConstructorDependenciesBean;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
import org.springframework.util.StopWatch;

/**
 * Tests properties population and autowire behavior.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Sam Brannen
 * @author Chris Beams
 */
public class DefaultListableBeanFactoryTests {

	private static final Log factoryLog = LogFactory.getLog(DefaultListableBeanFactory.class);


	@Test
	public void testUnreferencedSingletonWasInstantiated() {
		KnowsIfInstantiated.clearInstantiationRecord();
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertEquals(DummyFactory.class, lbf.getType("&x1"));
		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
	}

	@Test
	public void testPrototypeSingletonFactoryBeanIgnoredByNonEagerTypeMatching() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertEquals(DummyFactory.class, lbf.getType("&x1"));
		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
	}

	@Test
	public void testNonInitializedFactoryBeanIgnoredByNonEagerTypeMatching() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		Properties p = new Properties();
		p.setProperty("x1.(class)", DummyFactory.class.getName());
		// Reset static state
		DummyFactory.reset();
		p.setProperty("x1.singleton", "false");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);

		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
		String[] beanNames = lbf.getBeanNamesForType(TestBean.class, true, false);
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
		assertEquals(TestBean.class, lbf.getType("x1"));
		assertEquals(DummyFactory.class, lbf.getType("&x1"));
		assertTrue("prototype not instantiated", !DummyFactory.wasPrototypeCreated());
	}

	@Test
	public void testInitializedFactoryBeanFoundByNonEagerTypeMatching() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		Properties p = new Properties();
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		assertTrue("No beans defined after ignorable invalid", lbf.getBeanDefinitionCount() == 0);
	}

	@Test
	public void testHarmlessIgnorableRubbish() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		Properties p = new Properties();
		p.setProperty("foo", "bar");
		p.setProperty("qwert", "er");
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p, "test");
		assertTrue("No beans defined after harmless ignorable rubbish", lbf.getBeanDefinitionCount() == 0);
	}

	@Test
	public void testPropertiesPopulationWithNullPrefix() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
			DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
			MutablePropertyValues pvs = new MutablePropertyValues();
			pvs.add("ag", "foobar");
			RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
			bd.setPropertyValues(pvs);
			lbf.registerBeanDefinition("tb", bd);
			lbf.getBean("tb");
			fail("Should throw exception on invalid property");
		}
		catch (BeanCreationException ex) {
			ex.printStackTrace();
			assertTrue(ex.getCause() instanceof NotWritablePropertyException);
			NotWritablePropertyException cause = (NotWritablePropertyException) ex.getCause();
			// expected
			assertEquals(1, cause.getPossibleMatches().length);
			assertEquals("age", cause.getPossibleMatches()[0]);
		}
	}

	@Test
	public void testPrototype() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		parentDefinition.getPropertyValues().add("age", new Integer(EXPECTED_AGE));

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
	public void testNameAlreadyBound() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		assertTrue("Array length == 1", names.length == 1);
		assertTrue("0th element == test", names[0].equals("test"));
		TestBean tb = (TestBean) lbf.getBean("test");
		assertTrue("Test is non null", tb != null);
		assertTrue("Test bean name is Tony", "Tony".equals(tb.getName()));
		assertTrue("Test bean age is 48", tb.getAge() == 48);
	}

	@Test
	public void testAliasCircle() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		lbf.registerAlias("test", "test2");
		lbf.registerAlias("test2", "test3");
		try {
			lbf.registerAlias("test3", "test");
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void testBeanDefinitionOverriding() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("otherTest", "test2");
		lbf.registerAlias("test", "test2");
		assertTrue(lbf.getBean("test") instanceof NestedTestBean);
		assertTrue(lbf.getBean("test2") instanceof NestedTestBean);
	}

	@Test
	public void testBeanDefinitionRemoval() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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

	@Test
	public void testBeanDefinitionOverridingNotAllowed() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		lbf.setAllowBeanDefinitionOverriding(false);
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		try {
			lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			assertEquals("test", ex.getBeanName());
			// expected
		}
	}

	@Test
	public void testBeanDefinitionOverridingWithAlias() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		lbf.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
		lbf.registerAlias("test", "testAlias");
		lbf.registerBeanDefinition("test", new RootBeanDefinition(NestedTestBean.class));
		lbf.registerAlias("test", "testAlias");
		assertTrue(lbf.getBean("test") instanceof NestedTestBean);
		assertTrue(lbf.getBean("testAlias") instanceof NestedTestBean);
	}

	@Test
	public void testAliasChaining() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
	public void testBeanReferenceWithNewSyntax() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		Properties p = new Properties();
		p.setProperty("r.(class)", TestBean.class.getName());
		p.setProperty("r.name", "*" + name);
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
		TestBean r = (TestBean) lbf.getBean("r");
		assertTrue(r.getName().equals(name));
	}

	@Test
	public void testCustomEditor() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
	}

	@Test
	public void testRegisterExistingSingletonWithNameOverriding() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		Properties p = new Properties();
		p.setProperty("test.(class)", TestBean.class.getName());
		p.setProperty("test.name", "Tony");
		p.setProperty("test.age", "48");
		p.setProperty("test.spouse(ref)", "singletonObject");
		p.setProperty("singletonObject.(class)", org.springframework.beans.factory.config.PropertiesFactoryBean.class.getName());
		(new PropertiesBeanDefinitionReader(lbf)).registerBeanDefinitions(p);
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
		assertEquals(2, beansOfType.size());
	}

	@Test
	public void testRegisterExistingSingletonWithAutowire() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		bf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) bf.getBean("arrayBean");

		assertEquals(new UrlResource("http://localhost:8080"), ab.getResourceArray()[0]);
		assertEquals(new UrlResource("http://localhost:9090"), ab.getResourceArray()[1]);
	}

	@Test
	public void testArrayPropertyWithOptionalAutowiring() throws MalformedURLException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		bf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) bf.getBean("arrayBean");

		assertNull(ab.getResourceArray());
	}

	@Test
	public void testArrayConstructorWithAutowiring() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("integer1", new Integer(4));
		bf.registerSingleton("integer2", new Integer(5));

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) bf.getBean("arrayBean");

		assertEquals(new Integer(4), ab.getIntegerArray()[0]);
		assertEquals(new Integer(5), ab.getIntegerArray()[1]);
	}

	@Test
	public void testArrayConstructorWithOptionalAutowiring() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) bf.getBean("arrayBean");

		assertNull(ab.getIntegerArray());
	}

	@Test
	public void testDoubleArrayConstructorWithAutowiring() throws MalformedURLException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("integer1", new Integer(4));
		bf.registerSingleton("integer2", new Integer(5));
		bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) bf.getBean("arrayBean");

		assertEquals(new Integer(4), ab.getIntegerArray()[0]);
		assertEquals(new Integer(5), ab.getIntegerArray()[1]);
		assertEquals(new UrlResource("http://localhost:8080"), ab.getResourceArray()[0]);
		assertEquals(new UrlResource("http://localhost:9090"), ab.getResourceArray()[1]);
	}

	@Test
	public void testDoubleArrayConstructorWithOptionalAutowiring() throws MalformedURLException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("resource1", new UrlResource("http://localhost:8080"));
		bf.registerSingleton("resource2", new UrlResource("http://localhost:9090"));

		RootBeanDefinition rbd = new RootBeanDefinition(ArrayBean.class);
		rbd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		bf.registerBeanDefinition("arrayBean", rbd);
		ArrayBean ab = (ArrayBean) bf.getBean("arrayBean");

		assertNull(ab.getIntegerArray());
		assertNull(ab.getResourceArray());
	}

	@Test
	public void testAutowireWithNoDependencies() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("rod", bd);
		assertEquals(1, lbf.getBeanDefinitionCount());
		Object registered = lbf.autowire(NoDependencies.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		assertEquals(1, lbf.getBeanDefinitionCount());
		assertTrue(registered instanceof NoDependencies);
	}

	@Test
	public void testAutowireWithSatisfiedJavaBeanDependency() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
			assertTrue(ex.getMessage().indexOf("rod") != -1);
			assertTrue(ex.getMessage().indexOf("rod2") != -1);
		}
	}

	@Test
	public void testAutowireWithUnsatisfiedConstructorDependency() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spous", bd);
		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		assertNull(bean.getSpouse());
	}

	@Test(expected=NoSuchBeanDefinitionException.class)
	public void testGetBeanByTypeWithAmbiguity() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("bd2", bd2);
		lbf.getBean(TestBean.class);
	}

	@Test
	public void testGetBeanByTypeFiltersOutNonAutowireCandidates() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		RootBeanDefinition na1 = new RootBeanDefinition(TestBean.class);
		na1.setAutowireCandidate(false);

		lbf.registerBeanDefinition("bd1", bd1);
		lbf.registerBeanDefinition("na1", na1);
		TestBean actual = lbf.getBean(TestBean.class); // na1 was filtered
		assertSame(lbf.getBean("bd1", TestBean.class), actual);

		lbf.registerBeanDefinition("bd2", bd2);
		try {
			lbf.getBean(TestBean.class);
			fail("Should have thrown NoSuchBeanDefinitionException");
		} catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
	}

	@Test
	public void testAutowireBeanByType() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
	 * href="http://opensource.atlassian.com/projects/spring/browse/SPR-4040"
	 * target="_blank">SPR-4040</a>.
	 */
	@Test
	public void testAutowireBeanWithFactoryBeanByType() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class);
		bd.setAbstract(true);
		lbf.registerBeanDefinition("factoryBean", bd);
		assertNull(lbf.getType("factoryBean"));
	}

	/**
	 * Verifies that a dependency on a {@link FactoryBean} can <strong>not</strong>
	 * be autowired <em>by name</em>, as &amp; is an illegal character in
	 * Java method names. In other words, you can't name a method
	 * {@code set&amp;FactoryBean(...)}.
	 */
	@Test(expected=TypeMismatchException.class)
	public void testAutowireBeanWithFactoryBeanByName() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(LazyInitFactory.class);
		lbf.registerBeanDefinition("factoryBean", bd);
		LazyInitFactory factoryBean = (LazyInitFactory) lbf.getBean("&factoryBean");
		assertNotNull("The FactoryBean should have been registered.", factoryBean);
		lbf.autowire(FactoryBeanDependentBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);
	}

	@Test
	public void testAutowireBeanByTypeWithTwoMatches() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
			assertTrue(ex.getMessage().indexOf("test") != -1);
			assertTrue(ex.getMessage().indexOf("spouse") != -1);
		}
	}

	@Test
	public void testAutowireBeanByTypeWithTwoMatchesAndParameterNameDiscovery() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
			assertTrue(ex.getMessage().indexOf("test") != -1);
			assertTrue(ex.getMessage().indexOf("spouse") != -1);
		}
	}

	@Test
	public void testAutowireBeanByTypeWithDependencyCheck() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		DependenciesBean bean = (DependenciesBean)
				lbf.autowire(DependenciesBean.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		assertNull(bean.getSpouse());
	}

	@Test
	public void testAutowireExistingBeanByName() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("spous", bd);
		DependenciesBean existingBean = new DependenciesBean();
		lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		assertNull(existingBean.getSpouse());
	}

	@Test
	public void testAutowireExistingBeanByType() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		lbf.registerBeanDefinition("test", bd);
		DependenciesBean existingBean = new DependenciesBean();
		lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		TestBean test = (TestBean) lbf.getBean("test");
		assertEquals(existingBean.getSpouse(), test);
	}

	@Test
	public void testAutowireExistingBeanByTypeWithDependencyCheck() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		DependenciesBean existingBean = new DependenciesBean();
		lbf.autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		assertNull(existingBean.getSpouse());
	}

	@Test
	public void testInvalidAutowireMode() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		try {
			lbf.autowireBeanProperties(new TestBean(), AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void testApplyBeanPropertyValues() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
	public void testConfigureBean() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorDependencyFactoryBean.class);
		bd.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("test", bd);
		RootBeanDefinition bd2 = new RootBeanDefinition(String.class);
		bd2.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		lbf.registerBeanDefinition("string", bd2);
		lbf.preInstantiateSingletons();
	}

	@Test
	public void testBeanDefinitionWithInterface() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		lbf.registerBeanDefinition("test", new RootBeanDefinition(ITestBean.class));
		try {
			lbf.getBean("test");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertEquals("test", ex.getBeanName());
			assertTrue(ex.getMessage().toLowerCase().indexOf("interface") != -1);
		}
	}

	@Test
	public void testBeanDefinitionWithAbstractClass() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		lbf.registerBeanDefinition("test", new RootBeanDefinition(AbstractBeanFactory.class));
		try {
			lbf.getBean("test");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertEquals("test", ex.getBeanName());
			assertTrue(ex.getMessage().toLowerCase().indexOf("abstract") != -1);
		}
	}

	@Test
	public void testPrototypeFactoryBeanNotEagerlyCalled() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		lbf.registerBeanDefinition("test", new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class));
		lbf.preInstantiateSingletons();
	}

	@Test
	public void testLazyInitFactory() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		lbf.registerBeanDefinition("test", new RootBeanDefinition(LazyInitFactory.class));
		lbf.preInstantiateSingletons();
		LazyInitFactory factory = (LazyInitFactory) lbf.getBean("&test");
		assertFalse(factory.initialized);
	}

	@Test
	public void testSmartInitFactory() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		lbf.registerBeanDefinition("test", new RootBeanDefinition(EagerInitFactory.class));
		lbf.preInstantiateSingletons();
		EagerInitFactory factory = (EagerInitFactory) lbf.getBean("&test");
		assertTrue(factory.initialized);
	}

	@Test
	public void testPrototypeFactoryBeanNotEagerlyCalledInCaseOfBeanClassName() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		lbf.registerBeanDefinition("test",
				new RootBeanDefinition(FactoryBeanThatShouldntBeCalled.class.getName(), null, null));
		lbf.preInstantiateSingletons();
	}

	@Test
	public void testPrototypeStringCreatedRepeatedly() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		List<String> list = new ManagedList<String>();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		List<String> list = new ManagedList<String>();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		lbf.registerBeanDefinition("test", rbd);
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(LifecycleBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.setDependencyCheck(RootBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
		lbf.registerBeanDefinition("test", rbd);
		lbf.addBeanPostProcessor(new LifecycleBean.PostProcessor());
		StopWatch sw = new StopWatch();
		sw.start("prototype");
		for (int i = 0; i < 100000; i++) {
			lbf.getBean("test");
		}
		sw.stop();
		// System.out.println(sw.getTotalTimeMillis());
		assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 3000);
	}

	/**
	 * @Test
	 * public void testPrototypeCreationIsFastEnough2() throws Exception {
	 * if (factoryLog.isTraceEnabled() || factoryLog.isDebugEnabled()) {
	 * // Skip this test: Trace logging blows the time limit.
	 * return;
	 * }
	 * DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
	 * Method setBeanNameMethod = TestBean.class.getMethod("setBeanName", String.class);
	 * Method setBeanFactoryMethod = TestBean.class.getMethod("setBeanFactory", BeanFactory.class);
	 * StopWatch sw = new StopWatch();
	 * sw.start("prototype");
	 * for (int i = 0; i < 100000; i++) {
	 * TestBean tb = TestBean.class.newInstance();
	 * setBeanNameMethod.invoke(tb, "test");
	 * setBeanFactoryMethod.invoke(tb, lbf);
	 * }
	 * sw.stop();
	 * // System.out.println(sw.getTotalTimeMillis());
	 * assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 500);
	 * }
	 */

	@Test
	@Ignore  // TODO re-enable when ConstructorResolver TODO sorted out
	public void testPrototypeCreationWithConstructorArgumentsIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.getConstructorArgumentValues().addGenericArgumentValue("juergen");
		rbd.getConstructorArgumentValues().addGenericArgumentValue("99");
		lbf.registerBeanDefinition("test", rbd);
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

	/**
	 * @Test
	 * public void testPrototypeCreationWithConstructorArgumentsIsFastEnough2() throws Exception {
	 * if (factoryLog.isTraceEnabled() || factoryLog.isDebugEnabled()) {
	 * // Skip this test: Trace logging blows the time limit.
	 * return;
	 * }
	 * DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
	 * Constructor<TestBean> ctor = TestBean.class.getConstructor(String.class, int.class);
	 * Method setBeanNameMethod = TestBean.class.getMethod("setBeanName", String.class);
	 * Method setBeanFactoryMethod = TestBean.class.getMethod("setBeanFactory", BeanFactory.class);
	 * StopWatch sw = new StopWatch();
	 * sw.start("prototype");
	 * for (int i = 0; i < 100000; i++) {
	 * TestBean tb = ctor.newInstance("juergen", 99);
	 * setBeanNameMethod.invoke(tb, "test");
	 * setBeanFactoryMethod.invoke(tb, lbf);
	 * assertEquals("juergen", tb.getName());
	 * assertEquals(99, tb.getAge());
	 * }
	 * sw.stop();
	 * // System.out.println(sw.getTotalTimeMillis());
	 * assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 1500);
	 * }
	 */

	@Test
	public void testPrototypeCreationWithResolvedConstructorArgumentsIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("spouse"));
		lbf.registerBeanDefinition("test", rbd);
		lbf.registerBeanDefinition("spouse", new RootBeanDefinition(TestBean.class));
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.getPropertyValues().add("name", "juergen");
		rbd.getPropertyValues().add("age", "99");
		lbf.registerBeanDefinition("test", rbd);
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

	/**
	 * public void testPrototypeCreationWithPropertiesIsFastEnough2() throws Exception {
	 * if (factoryLog.isTraceEnabled() || factoryLog.isDebugEnabled()) {
	 * // Skip this test: Trace logging blows the time limit.
	 * return;
	 * }
	 * DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
	 * StopWatch sw = new StopWatch();
	 * Method setBeanNameMethod = TestBean.class.getMethod("setBeanName", String.class);
	 * Method setBeanFactoryMethod = TestBean.class.getMethod("setBeanFactory", BeanFactory.class);
	 * Method setNameMethod = TestBean.class.getMethod("setName", String.class);
	 * Method setAgeMethod = TestBean.class.getMethod("setAge", int.class);
	 * sw.start("prototype");
	 * for (int i = 0; i < 100000; i++) {
	 * TestBean tb = TestBean.class.newInstance();
	 * setBeanNameMethod.invoke(tb, "test");
	 * setBeanFactoryMethod.invoke(tb, lbf);
	 * setNameMethod.invoke(tb, "juergen");
	 * setAgeMethod.invoke(tb, 99);
	 * }
	 * sw.stop();
	 * // System.out.println(sw.getTotalTimeMillis());
	 * assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 750);
	 * }
	 */
	@Test
	public void testPrototypeCreationWithResolvedPropertiesIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.getPropertyValues().add("spouse", new RuntimeBeanReference("spouse"));
		lbf.registerBeanDefinition("test", rbd);
		lbf.registerBeanDefinition("spouse", new RootBeanDefinition(TestBean.class));
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
	public void testBeanPostProcessorWithWrappedObjectAndDisposableBean() {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		BeanWithDestroyMethod.closed = false;
		lbf.preInstantiateSingletons();
		lbf.destroySingletons();
		assertTrue("Destroy method invoked", BeanWithDestroyMethod.closed);
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

		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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

	@Test(expected=IllegalStateException.class)
	public void testScopingBeanToUnregisteredScopeResultsInAnException() throws Exception {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TestBean.class);
		AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
		beanDefinition.setScope("he put himself so low could hardly look me in the face");

		final DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("testBean", beanDefinition);
		factory.getBean("testBean");
	}

	@Test
	public void testExplicitScopeInheritanceForChildBeanDefinitions() throws Exception {
		String theChildScope = "bonanza!";

		RootBeanDefinition parent = new RootBeanDefinition();
		parent.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);

		AbstractBeanDefinition child = BeanDefinitionBuilder
				.childBeanDefinition("parent").getBeanDefinition();
		child.setBeanClass(TestBean.class);
		child.setScope(theChildScope);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("parent", parent);
		factory.registerBeanDefinition("child", child);

		AbstractBeanDefinition def = (AbstractBeanDefinition) factory.getBeanDefinition("child");
		assertEquals("Child 'scope' not overriding parent scope (it must).", theChildScope, def.getScope());
	}

	@Test
	public void testScopeInheritanceForChildBeanDefinitions() throws Exception {
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

	@SuppressWarnings("unchecked")
	private void doTestFieldSettingWithInstantiationAwarePostProcessor(final boolean skipPropertyPopulation) {
		DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		final DefaultListableBeanFactory lbf = new DefaultListableBeanFactory();
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
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("abs", BeanDefinitionBuilder
				.rootBeanDefinition(TestBean.class).setAbstract(true).getBeanDefinition());
		assertThat(bf.containsBean("abs"), is(true));
		assertThat(bf.containsBean("bogus"), is(false));
	}


	static class A { }
	static class B { }

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
	@Test(timeout=1000)
	public void testByTypeLookupIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

		for (int i = 0; i < 1000; i++) {
			bf.registerBeanDefinition("a"+i, new RootBeanDefinition(A.class));
		}
		bf.registerBeanDefinition("b", new RootBeanDefinition(B.class));

		bf.freezeConfiguration();

		for (int i=0; i<10000; i++) {
			bf.getBean(B.class);
		}
	}


	public static class NoDependencies {

		private NoDependencies() {
		}
	}


	public static class ConstructorDependency {

		public TestBean spouse;

		public ConstructorDependency(TestBean spouse) {
			this.spouse = spouse;
		}

		private ConstructorDependency(TestBean spouse, TestBean otherSpouse) {
			throw new IllegalArgumentException("Should never be called");
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


	public static class BeanWithDestroyMethod {

		private static boolean closed;

		public void close() {
			closed = true;
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


	public static class FactoryBeanThatShouldntBeCalled implements FactoryBean<Object> {

		@Override
		public Object getObject() {
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
	}


	public static class LazyInitFactory implements FactoryBean<Object> {

		public boolean initialized = false;

		@Override
		public Object getObject() throws Exception {
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
		public Object getObject() throws Exception {
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
		public Object convertIfNecessary(Object value, Class requiredType) {
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
		public Object convertIfNecessary(Object value, Class requiredType, MethodParameter methodParam) {
			return convertIfNecessary(value, requiredType);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object convertIfNecessary(Object value, Class requiredType, Field field) {
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

		public int hashCode() {
			return this.name.hashCode();
		}
	}


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

}

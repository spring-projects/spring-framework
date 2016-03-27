/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.annotation;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Chris Beams
 */
public class ComponentScanParserBeanDefinitionDefaultsTests {

	private static final String TEST_BEAN_NAME = "componentScanParserBeanDefinitionDefaultsTests.DefaultsTestBean";

	private static final String LOCATION_PREFIX = "org/springframework/context/annotation/";


	@Before
	public void setUp() {
		DefaultsTestBean.INIT_COUNT = 0;
	}

	@Test
	public void testDefaultLazyInit() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
		assertFalse("lazy-init should be false", context.getBeanDefinition(TEST_BEAN_NAME).isLazyInit());
		assertEquals("initCount should be 0", 0, DefaultsTestBean.INIT_COUNT);
		context.refresh();
		assertEquals("bean should have been instantiated", 1, DefaultsTestBean.INIT_COUNT);
	}

	@Test
	public void testLazyInitTrue() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultLazyInitTrueTests.xml");
		assertTrue("lazy-init should be true", context.getBeanDefinition(TEST_BEAN_NAME).isLazyInit());
		assertEquals("initCount should be 0", 0, DefaultsTestBean.INIT_COUNT);
		context.refresh();
		assertEquals("bean should not have been instantiated yet", 0, DefaultsTestBean.INIT_COUNT);
		context.getBean(TEST_BEAN_NAME);
		assertEquals("bean should have been instantiated", 1, DefaultsTestBean.INIT_COUNT);
	}

	@Test
	public void testLazyInitFalse() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultLazyInitFalseTests.xml");
		assertFalse("lazy-init should be false", context.getBeanDefinition(TEST_BEAN_NAME).isLazyInit());
		assertEquals("initCount should be 0", 0, DefaultsTestBean.INIT_COUNT);
		context.refresh();
		assertEquals("bean should have been instantiated", 1, DefaultsTestBean.INIT_COUNT);
	}

	@Test
	public void testDefaultAutowire() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertNull("no dependencies should have been autowired", bean.getConstructorDependency());
		assertNull("no dependencies should have been autowired", bean.getPropertyDependency1());
		assertNull("no dependencies should have been autowired", bean.getPropertyDependency2());
	}

	@Test
	public void testAutowireNo() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireNoTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertNull("no dependencies should have been autowired", bean.getConstructorDependency());
		assertNull("no dependencies should have been autowired", bean.getPropertyDependency1());
		assertNull("no dependencies should have been autowired", bean.getPropertyDependency2());
	}

	@Test
	public void testAutowireConstructor() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireConstructorTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertNotNull("constructor dependency should have been autowired", bean.getConstructorDependency());
		assertEquals("cd", bean.getConstructorDependency().getName());
		assertNull("property dependencies should not have been autowired", bean.getPropertyDependency1());
		assertNull("property dependencies should not have been autowired", bean.getPropertyDependency2());
	}

	@Test
	public void testAutowireByType() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireByTypeTests.xml");
		try {
			context.refresh();
			fail("expected exception due to multiple matches for byType autowiring");
		}
		catch (UnsatisfiedDependencyException ex) {
			// expected
		}
	}

	@Test
	public void testAutowireByName() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireByNameTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertNull("constructor dependency should not have been autowired", bean.getConstructorDependency());
		assertNull("propertyDependency1 should not have been autowired", bean.getPropertyDependency1());
		assertNotNull("propertyDependency2 should have been autowired", bean.getPropertyDependency2());
		assertEquals("pd2", bean.getPropertyDependency2().getName());
	}

	@Test
	public void testDefaultDependencyCheck() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertNull("constructor dependency should not have been autowired", bean.getConstructorDependency());
		assertNull("property dependencies should not have been autowired", bean.getPropertyDependency1());
		assertNull("property dependencies should not have been autowired", bean.getPropertyDependency2());
	}

	@Test
	public void testDependencyCheckAll() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultDependencyCheckAllTests.xml");
		try {
			context.refresh();
			fail("expected exception due to dependency check");
		}
		catch (UnsatisfiedDependencyException ex) {
			// expected
		}
	}

	@Test
	public void testDependencyCheckObjectsWithAutowireByName() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultDependencyCheckObjectsWithAutowireByNameTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertNull("constructor dependency should not have been autowired", bean.getConstructorDependency());
		assertNotNull("property dependencies should have been autowired", bean.getPropertyDependency1());
		assertNotNull("property dependencies should have been autowired", bean.getPropertyDependency2());
	}

	@Test
	public void testDefaultInitAndDestroyMethodsNotDefined() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertFalse("bean should not have been initialized", bean.isInitialized());
		context.close();
		assertFalse("bean should not have been destroyed", bean.isDestroyed());
	}

	@Test
	public void testDefaultInitAndDestroyMethodsDefined() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultInitAndDestroyMethodsTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertTrue("bean should have been initialized", bean.isInitialized());
		context.close();
		assertTrue("bean should have been destroyed", bean.isDestroyed());
	}

	@Test
	public void testDefaultNonExistingInitAndDestroyMethodsDefined() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultNonExistingInitAndDestroyMethodsTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertFalse("bean should not have been initialized", bean.isInitialized());
		context.close();
		assertFalse("bean should not have been destroyed", bean.isDestroyed());
	}


	@SuppressWarnings("unused")
	private static class DefaultsTestBean {

		static int INIT_COUNT;

		private ConstructorDependencyTestBean constructorDependency;

		private PropertyDependencyTestBean propertyDependency1;

		private PropertyDependencyTestBean propertyDependency2;

		private boolean initialized;

		private boolean destroyed;

		public DefaultsTestBean() {
			INIT_COUNT++;
		}

		public DefaultsTestBean(ConstructorDependencyTestBean cdtb) {
			this();
			this.constructorDependency = cdtb;
		}

		public void init() {
			this.initialized = true;
		}

		public boolean isInitialized() {
			return this.initialized;
		}

		public void destroy() {
			this.destroyed = true;
		}

		public boolean isDestroyed() {
			return this.destroyed;
		}

		public void setPropertyDependency1(PropertyDependencyTestBean pdtb) {
			this.propertyDependency1 = pdtb;
		}

		public void setPropertyDependency2(PropertyDependencyTestBean pdtb) {
			this.propertyDependency2 = pdtb;
		}

		public ConstructorDependencyTestBean getConstructorDependency() {
			return this.constructorDependency;
		}

		public PropertyDependencyTestBean getPropertyDependency1() {
			return this.propertyDependency1;
		}

		public PropertyDependencyTestBean getPropertyDependency2() {
			return this.propertyDependency2;
		}
	}


	@SuppressWarnings("unused")
	private static class PropertyDependencyTestBean {

		private String name;

		public PropertyDependencyTestBean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}


	@SuppressWarnings("unused")
	private static class ConstructorDependencyTestBean {

		private String name;

		public ConstructorDependencyTestBean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}

}

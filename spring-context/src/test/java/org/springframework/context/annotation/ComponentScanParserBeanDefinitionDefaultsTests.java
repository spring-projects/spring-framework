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

package org.springframework.context.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Chris Beams
 */
public class ComponentScanParserBeanDefinitionDefaultsTests {

	private static final String TEST_BEAN_NAME = "componentScanParserBeanDefinitionDefaultsTests.DefaultsTestBean";

	private static final String LOCATION_PREFIX = "org/springframework/context/annotation/";


	@BeforeEach
	public void setUp() {
		DefaultsTestBean.INIT_COUNT = 0;
	}

	@Test
	public void testDefaultLazyInit() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
		assertThat(context.getBeanDefinition(TEST_BEAN_NAME).isLazyInit()).as("lazy-init should be false").isFalse();
		assertThat(DefaultsTestBean.INIT_COUNT).as("initCount should be 0").isEqualTo(0);
		context.refresh();
		assertThat(DefaultsTestBean.INIT_COUNT).as("bean should have been instantiated").isEqualTo(1);
	}

	@Test
	public void testLazyInitTrue() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultLazyInitTrueTests.xml");
		assertThat(context.getBeanDefinition(TEST_BEAN_NAME).isLazyInit()).as("lazy-init should be true").isTrue();
		assertThat(DefaultsTestBean.INIT_COUNT).as("initCount should be 0").isEqualTo(0);
		context.refresh();
		assertThat(DefaultsTestBean.INIT_COUNT).as("bean should not have been instantiated yet").isEqualTo(0);
		context.getBean(TEST_BEAN_NAME);
		assertThat(DefaultsTestBean.INIT_COUNT).as("bean should have been instantiated").isEqualTo(1);
	}

	@Test
	public void testLazyInitFalse() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultLazyInitFalseTests.xml");
		assertThat(context.getBeanDefinition(TEST_BEAN_NAME).isLazyInit()).as("lazy-init should be false").isFalse();
		assertThat(DefaultsTestBean.INIT_COUNT).as("initCount should be 0").isEqualTo(0);
		context.refresh();
		assertThat(DefaultsTestBean.INIT_COUNT).as("bean should have been instantiated").isEqualTo(1);
	}

	@Test
	public void testDefaultAutowire() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertThat(bean.getConstructorDependency()).as("no dependencies should have been autowired").isNull();
		assertThat(bean.getPropertyDependency1()).as("no dependencies should have been autowired").isNull();
		assertThat(bean.getPropertyDependency2()).as("no dependencies should have been autowired").isNull();
	}

	@Test
	public void testAutowireNo() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireNoTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertThat(bean.getConstructorDependency()).as("no dependencies should have been autowired").isNull();
		assertThat(bean.getPropertyDependency1()).as("no dependencies should have been autowired").isNull();
		assertThat(bean.getPropertyDependency2()).as("no dependencies should have been autowired").isNull();
	}

	@Test
	public void testAutowireConstructor() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireConstructorTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertThat(bean.getConstructorDependency()).as("constructor dependency should have been autowired").isNotNull();
		assertThat(bean.getConstructorDependency().getName()).isEqualTo("cd");
		assertThat(bean.getPropertyDependency1()).as("property dependencies should not have been autowired").isNull();
		assertThat(bean.getPropertyDependency2()).as("property dependencies should not have been autowired").isNull();
	}

	@Test
	public void testAutowireByType() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireByTypeTests.xml");
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(
				context::refresh);
	}

	@Test
	public void testAutowireByName() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultAutowireByNameTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertThat(bean.getConstructorDependency()).as("constructor dependency should not have been autowired").isNull();
		assertThat(bean.getPropertyDependency1()).as("propertyDependency1 should not have been autowired").isNull();
		assertThat(bean.getPropertyDependency2()).as("propertyDependency2 should have been autowired").isNotNull();
		assertThat(bean.getPropertyDependency2().getName()).isEqualTo("pd2");
	}

	@Test
	public void testDefaultDependencyCheck() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertThat(bean.getConstructorDependency()).as("constructor dependency should not have been autowired").isNull();
		assertThat(bean.getPropertyDependency1()).as("property dependencies should not have been autowired").isNull();
		assertThat(bean.getPropertyDependency2()).as("property dependencies should not have been autowired").isNull();
	}

	@Test
	public void testDefaultInitAndDestroyMethodsNotDefined() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultWithNoOverridesTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertThat(bean.isInitialized()).as("bean should not have been initialized").isFalse();
		context.close();
		assertThat(bean.isDestroyed()).as("bean should not have been destroyed").isFalse();
	}

	@Test
	public void testDefaultInitAndDestroyMethodsDefined() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultInitAndDestroyMethodsTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertThat(bean.isInitialized()).as("bean should have been initialized").isTrue();
		context.close();
		assertThat(bean.isDestroyed()).as("bean should have been destroyed").isTrue();
	}

	@Test
	public void testDefaultNonExistingInitAndDestroyMethodsDefined() {
		GenericApplicationContext context = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		reader.loadBeanDefinitions(LOCATION_PREFIX + "defaultNonExistingInitAndDestroyMethodsTests.xml");
		context.refresh();
		DefaultsTestBean bean = (DefaultsTestBean) context.getBean(TEST_BEAN_NAME);
		assertThat(bean.isInitialized()).as("bean should not have been initialized").isFalse();
		context.close();
		assertThat(bean.isDestroyed()).as("bean should not have been destroyed").isFalse();
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

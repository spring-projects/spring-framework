/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 */
public class DefaultLifecycleMethodsTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@Before
	public void setUp() throws Exception {
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(new ClassPathResource(
				"defaultLifecycleMethods.xml", getClass()));
	}

	@Test
	public void lifecycleMethodsInvoked() {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("lifecycleAware");
		assertTrue("Bean not initialized", bean.isInitCalled());
		assertFalse("Bean destroyed too early", bean.isDestroyCalled());
		this.beanFactory.destroySingletons();
		assertTrue("Bean not destroyed", bean.isDestroyCalled());
	}

	@Test
	public void lifecycleMethodsDisabled() throws Exception {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("lifecycleMethodsDisabled");
		assertFalse("Bean init method called incorrectly", bean.isInitCalled());
		this.beanFactory.destroySingletons();
		assertFalse("Bean destroy method called incorrectly", bean.isDestroyCalled());
	}

	@Test
	public void ignoreDefaultLifecycleMethods() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(
				"ignoreDefaultLifecycleMethods.xml", getClass()));
		bf.preInstantiateSingletons();
		bf.destroySingletons();
	}

	@Test
	public void overrideDefaultLifecycleMethods() throws Exception {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("overrideLifecycleMethods");
		assertFalse("Default init method called incorrectly.", bean.isInitCalled());
		assertTrue("Custom init method not called.", bean.isCustomInitCalled());
		this.beanFactory.destroySingletons();
		assertFalse("Default destory method called incorrectly.", bean.isDestroyCalled());
		assertTrue("Custom destory method not called.", bean.isCustomDestroyCalled());
	}


	public static class LifecycleAwareBean {

		private boolean initCalled;

		private boolean destroyCalled;

		private boolean customInitCalled;

		private boolean customDestroyCalled;

		public void init() {
			this.initCalled = true;
		}

		public void destroy() {
			this.destroyCalled = true;
		}

		public void customInit() {
			this.customInitCalled = true;
		}

		public void customDestroy() {
			this.customDestroyCalled = true;
		}

		public boolean isInitCalled() {
			return initCalled;
		}

		public boolean isDestroyCalled() {
			return destroyCalled;
		}

		public boolean isCustomInitCalled() {
			return customInitCalled;
		}

		public boolean isCustomDestroyCalled() {
			return customDestroyCalled;
		}
	}

}

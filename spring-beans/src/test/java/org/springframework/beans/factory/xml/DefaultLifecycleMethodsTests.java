/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.beans.factory.xml;

import junit.framework.TestCase;

import org.springframework.core.io.ClassPathResource;

/**
 * @author Rob Harrop
 */
public class DefaultLifecycleMethodsTests extends TestCase {

	private XmlBeanFactory beanFactory;

	@Override
	protected void setUp() throws Exception {
		this.beanFactory = new XmlBeanFactory(new ClassPathResource("defaultLifecycleMethods.xml", getClass()));
	}

	public void testLifecycleMethodsInvoked() {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("lifecycleAware");
		assertTrue("Bean not initialized", bean.isInitCalled());
		assertFalse("Bean destroyed too early", bean.isDestroyCalled());
		this.beanFactory.destroySingletons();
		assertTrue("Bean not destroyed", bean.isDestroyCalled());
	}

	public void testLifecycleMethodsDisabled() throws Exception {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("lifecycleMethodsDisabled");
		assertFalse("Bean init method called incorrectly", bean.isInitCalled());
		this.beanFactory.destroySingletons();
		assertFalse("Bean destroy method called incorrectly", bean.isDestroyCalled());
	}

	public void testIgnoreDefaultLifecycleMethods() throws Exception {
		try {
			XmlBeanFactory bf = new XmlBeanFactory(new ClassPathResource("ignoreDefaultLifecycleMethods.xml", getClass()));
			bf.preInstantiateSingletons();
			bf.destroySingletons();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Should ignore non-existent default lifecycle methods");
		}
	}

	public void testOverrideDefaultLifecycleMethods() throws Exception {
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

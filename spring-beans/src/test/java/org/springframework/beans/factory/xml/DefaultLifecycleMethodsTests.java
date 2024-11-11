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

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class DefaultLifecycleMethodsTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@BeforeEach
	void setup() {
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				new ClassPathResource("defaultLifecycleMethods.xml", getClass()));
	}


	@Test
	void lifecycleMethodsInvoked() {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("lifecycleAware");
		assertThat(bean.isInitCalled()).as("Bean not initialized").isTrue();
		assertThat(bean.isCustomInitCalled()).as("Custom init method called incorrectly").isFalse();
		assertThat(bean.isDestroyCalled()).as("Bean destroyed too early").isFalse();
		this.beanFactory.destroySingletons();
		assertThat(bean.isDestroyCalled()).as("Bean not destroyed").isTrue();
		assertThat(bean.isCustomDestroyCalled()).as("Custom destroy method called incorrectly").isFalse();
	}

	@Test
	void lifecycleMethodsDisabled() {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("lifecycleMethodsDisabled");
		assertThat(bean.isInitCalled()).as("Bean init method called incorrectly").isFalse();
		assertThat(bean.isCustomInitCalled()).as("Custom init method called incorrectly").isFalse();
		this.beanFactory.destroySingletons();
		assertThat(bean.isDestroyCalled()).as("Bean destroy method called incorrectly").isFalse();
		assertThat(bean.isCustomDestroyCalled()).as("Custom destroy method called incorrectly").isFalse();
	}

	@Test
	void ignoreDefaultLifecycleMethods() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(
				"ignoreDefaultLifecycleMethods.xml", getClass()));
		bf.preInstantiateSingletons();
		bf.destroySingletons();
	}

	@Test
	void overrideDefaultLifecycleMethods() {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("overrideLifecycleMethods");
		assertThat(bean.isInitCalled()).as("Default init method called incorrectly").isFalse();
		assertThat(bean.isCustomInitCalled()).as("Custom init method not called").isTrue();
		this.beanFactory.destroySingletons();
		assertThat(bean.isDestroyCalled()).as("Default destroy method called incorrectly").isFalse();
		assertThat(bean.isCustomDestroyCalled()).as("Custom destroy method not called").isTrue();
	}

	@Test
	void childWithDefaultLifecycleMethods() {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("childWithDefaultLifecycleMethods");
		assertThat(bean.isInitCalled()).as("Bean not initialized").isTrue();
		assertThat(bean.isCustomInitCalled()).as("Custom init method called incorrectly").isFalse();
		assertThat(bean.isDestroyCalled()).as("Bean destroyed too early").isFalse();
		this.beanFactory.destroySingletons();
		assertThat(bean.isDestroyCalled()).as("Bean not destroyed").isTrue();
		assertThat(bean.isCustomDestroyCalled()).as("Custom destroy method called incorrectly").isFalse();
	}

	@Test
	void childWithLifecycleMethodsDisabled() {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("childWithLifecycleMethodsDisabled");
		assertThat(bean.isInitCalled()).as("Bean init method called incorrectly").isFalse();
		assertThat(bean.isCustomInitCalled()).as("Custom init method called incorrectly").isFalse();
		this.beanFactory.destroySingletons();
		assertThat(bean.isDestroyCalled()).as("Bean destroy method called incorrectly").isFalse();
		assertThat(bean.isCustomDestroyCalled()).as("Custom destroy method called incorrectly").isFalse();
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

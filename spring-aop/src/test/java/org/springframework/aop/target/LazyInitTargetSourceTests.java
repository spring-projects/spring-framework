/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.target;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @since 07.01.2005
 */
class LazyInitTargetSourceTests {

	private static final Class<?> CLASS = LazyInitTargetSourceTests.class;

	private static final Resource SINGLETON_CONTEXT = qualifiedResource(CLASS, "singleton.xml");
	private static final Resource CUSTOM_TARGET_CONTEXT = qualifiedResource(CLASS, "customTarget.xml");
	private static final Resource FACTORY_BEAN_CONTEXT = qualifiedResource(CLASS, "factoryBean.xml");

	private final DefaultListableBeanFactory bf = new DefaultListableBeanFactory();


	@Test
	void lazyInitSingletonTargetSource() {
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(SINGLETON_CONTEXT);
		bf.preInstantiateSingletons();

		ITestBean tb = (ITestBean) bf.getBean("proxy");
		assertThat(bf.containsSingleton("target")).isFalse();
		assertThat(tb.getAge()).isEqualTo(10);
		assertThat(bf.containsSingleton("target")).isTrue();
	}

	@Test
	void customLazyInitSingletonTargetSource() {
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(CUSTOM_TARGET_CONTEXT);
		bf.preInstantiateSingletons();

		ITestBean tb = (ITestBean) bf.getBean("proxy");
		assertThat(bf.containsSingleton("target")).isFalse();
		assertThat(tb.getName()).isEqualTo("Rob Harrop");
		assertThat(bf.containsSingleton("target")).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	void lazyInitFactoryBeanTargetSource() {
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(FACTORY_BEAN_CONTEXT);
		bf.preInstantiateSingletons();

		Set<Object> set1 = (Set<Object>) bf.getBean("proxy1");
		assertThat(bf.containsSingleton("target1")).isFalse();
		assertThat(set1).contains("10");
		assertThat(bf.containsSingleton("target1")).isTrue();

		Set<Object> set2 = (Set<Object>) bf.getBean("proxy2");
		assertThat(bf.containsSingleton("target2")).isFalse();
		assertThat(set2).contains("20");
		assertThat(bf.containsSingleton("target2")).isTrue();
	}


	@SuppressWarnings("serial")
	static class CustomLazyInitTargetSource extends LazyInitTargetSource {

		@Override
		protected void postProcessTargetObject(Object targetObject) {
			((ITestBean) targetObject).setName("Rob Harrop");
		}
	}

}

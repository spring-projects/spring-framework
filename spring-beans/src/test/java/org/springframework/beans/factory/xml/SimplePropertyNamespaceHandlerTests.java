/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 */
class SimplePropertyNamespaceHandlerTests {

	@Test
	void simpleBeanConfigured() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
				new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
		ITestBean rob = (TestBean) beanFactory.getBean("rob");
		ITestBean sally = (TestBean) beanFactory.getBean("sally");
		assertThat(rob.getName()).isEqualTo("Rob Harrop");
		assertThat(rob.getAge()).isEqualTo(24);
		assertThat(sally).isEqualTo(rob.getSpouse());
	}

	@Test
	void innerBeanConfigured() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
				new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
		TestBean sally = (TestBean) beanFactory.getBean("sally2");
		ITestBean rob = sally.getSpouse();
		assertThat(rob.getName()).isEqualTo("Rob Harrop");
		assertThat(rob.getAge()).isEqualTo(24);
		assertThat(sally).isEqualTo(rob.getSpouse());
	}

	@Test
	void withPropertyDefinedTwice() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
							new ClassPathResource("simplePropertyNamespaceHandlerTestsWithErrors.xml", getClass())));
	}

	@Test
	void propertyWithNameEndingInRef() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
				new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
		ITestBean sally = (TestBean) beanFactory.getBean("derivedSally");
		assertThat(sally.getSpouse().getName()).isEqualTo("r");
	}

}

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

package org.springframework.beans.factory.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Karl Pietrzak
 * @author Juergen Hoeller
 */
public class LookupMethodTests {

	private DefaultListableBeanFactory beanFactory;


	@BeforeEach
	public void setUp() {
		beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("lookupMethodTests.xml", getClass()));
	}


	@Test
	public void testWithoutConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertThat(bean).isNotNull();
		Object expected = bean.get();
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
	}

	@Test
	public void testWithOverloadedArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertThat(bean).isNotNull();
		TestBean expected = bean.get("haha");
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(expected.getName()).isEqualTo("haha");
	}

	@Test
	public void testWithOneConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertThat(bean).isNotNull();
		TestBean expected = bean.getOneArgument("haha");
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(expected.getName()).isEqualTo("haha");
	}

	@Test
	public void testWithTwoConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertThat(bean).isNotNull();
		TestBean expected = bean.getTwoArguments("haha", 72);
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(expected.getName()).isEqualTo("haha");
		assertThat(expected.getAge()).isEqualTo(72);
	}

	@Test
	public void testWithThreeArgsShouldFail() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertThat(bean).isNotNull();
		assertThatExceptionOfType(AbstractMethodError.class).as("does not have a three arg constructor").isThrownBy(() ->
			bean.getThreeArguments("name", 1, 2));
	}

	@Test
	public void testWithOverriddenLookupMethod() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("extendedBean");
		assertThat(bean).isNotNull();
		TestBean expected = bean.getOneArgument("haha");
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(expected.getName()).isEqualTo("haha");
		assertThat(expected.isJedi()).isTrue();
	}


	public static abstract class AbstractBean {

		public abstract TestBean get();

		public abstract TestBean get(String name);  // overloaded

		public abstract TestBean getOneArgument(String name);

		public abstract TestBean getTwoArguments(String name, int age);

		public abstract TestBean getThreeArguments(String name, int age, int anotherArg);
	}

}

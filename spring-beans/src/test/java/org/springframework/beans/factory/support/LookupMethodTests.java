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

package org.springframework.beans.factory.support;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Karl Pietrzak
 * @author Juergen Hoeller
 */
public class LookupMethodTests {

	private DefaultListableBeanFactory beanFactory;


	@Before
	public void setUp() {
		beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("lookupMethodTests.xml", getClass()));
	}


	@Test
	public void testWithoutConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		Object expected = bean.get();
		assertEquals(TestBean.class, expected.getClass());
	}

	@Test
	public void testWithOverloadedArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		TestBean expected = bean.get("haha");
		assertEquals(TestBean.class, expected.getClass());
		assertEquals("haha", expected.getName());
	}

	@Test
	public void testWithOneConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		TestBean expected = bean.getOneArgument("haha");
		assertEquals(TestBean.class, expected.getClass());
		assertEquals("haha", expected.getName());
	}

	@Test
	public void testWithTwoConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		TestBean expected = bean.getTwoArguments("haha", 72);
		assertEquals(TestBean.class, expected.getClass());
		assertEquals("haha", expected.getName());
		assertEquals(72, expected.getAge());
	}

	@Test
	public void testWithThreeArgsShouldFail() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		try {
			bean.getThreeArguments("name", 1, 2);
			fail("TestBean does not have a three arg constructor so this should not have worked");
		}
		catch (AbstractMethodError ex) {
		}
	}

	@Test
	public void testWithOverriddenLookupMethod() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("extendedBean");
		assertNotNull(bean);
		TestBean expected = bean.getOneArgument("haha");
		assertEquals(TestBean.class, expected.getClass());
		assertEquals("haha", expected.getName());
		assertTrue(expected.isJedi());
	}


	public static abstract class AbstractBean {

		public abstract TestBean get();

		public abstract TestBean get(String name);  // overloaded

		public abstract TestBean getOneArgument(String name);

		public abstract TestBean getTwoArguments(String name, int age);

		public abstract TestBean getThreeArguments(String name, int age, int anotherArg);
	}

}

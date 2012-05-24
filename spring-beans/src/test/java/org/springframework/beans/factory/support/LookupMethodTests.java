/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.beans.factory.support;

import junit.framework.TestCase;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

import test.beans.AbstractBean;
import test.beans.TestBean;

/**
 * Tests the use of lookup-method
 * @author Karl Pietrzak
 */
public class LookupMethodTests extends TestCase {
	

	private DefaultListableBeanFactory beanFactory;

	protected void setUp() throws Exception {
		final ClassPathResource resource = new ClassPathResource("/org/springframework/beans/factory/xml/lookupMethodBeanTests.xml", getClass());
		this.beanFactory = new DefaultListableBeanFactory();
		final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.loadBeanDefinitions(resource);
	}

	/**
	 * lookup method's bean has no constructor arguments
	 */
	public void testWithoutConstructorArg() {
		AbstractBean bean = (AbstractBean)this.beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		final Object expected = bean.get();
		assertEquals(TestBean.class, expected.getClass());
	}

	/**
	 * Creates a new instance of {@link TestBean} using the constructor which takes a single <code>String</code>
	 */
	public void testWithOneConstructorArg() {
		AbstractBean bean = (AbstractBean)this.beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		final TestBean expected = bean.getOneArgument("haha");
		assertEquals(TestBean.class, expected.getClass());
		assertEquals("haha", expected.getName());
	}
	
	/**
	 * Creates a new instance of {@link TestBean} using the constructor which takes a <code>String</code> and an <code>int</code>
	 */
	public void testWithTwoConstructorArg() {
		AbstractBean bean = (AbstractBean)this.beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		final TestBean expected = bean.getTwoArguments("haha", 72);
		assertEquals(TestBean.class, expected.getClass());
		assertEquals("haha", expected.getName());
		assertEquals(72, expected.getAge());
	}
	
	/**
	 *  {@link TestBean} doesn't have a constructor that takes a <code>String</code> and two <code>int</code>'s
	 */
	public void testWithThreeArgsShouldFail() {
		AbstractBean bean = (AbstractBean)this.beanFactory.getBean("abstractBean");
		assertNotNull(bean);
		try {
			bean.getThreeArguments("name", 1, 2);
			fail("TestBean does not have a three arg constructor so this should not have worked");
		} catch (AbstractMethodError e) {
			
		}
	}
}

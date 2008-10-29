/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class ShortcutTests extends TestCase {

	public void testSimpleBeanConfigured() throws Exception {
		XmlBeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("shortcutTests.xml", getClass()));
		ITestBean rob = (TestBean) beanFactory.getBean("rob");
		ITestBean sally = (TestBean) beanFactory.getBean("sally");
		assertEquals("Rob Harrop", rob.getName());
		assertEquals(24, rob.getAge());
		assertEquals(rob.getSpouse(), sally);
	}

	public void testInnerBeanConfigured() throws Exception {
		XmlBeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("shortcutTests.xml", getClass()));
		TestBean sally = (TestBean) beanFactory.getBean("sally2");
		ITestBean rob = (TestBean) sally.getSpouse();
		assertEquals("Rob Harrop", rob.getName());
		assertEquals(24, rob.getAge());
		assertEquals(rob.getSpouse(), sally);
	}

	public void testWithPropertyDefinedTwice() throws Exception {
		try {
			new XmlBeanFactory(new ClassPathResource("shortcutTestsWithErrors.xml", getClass()));
			fail("Should not be able to load a file with property specified twice.");
		}
		catch (BeanDefinitionStoreException e) {
			// success
		}
	}

	public void testPropertyWithNameEndingInRef() throws Exception {
		XmlBeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("shortcutTests.xml", getClass()));
		ITestBean sally = (TestBean) beanFactory.getBean("derivedSally");
		assertEquals("r", sally.getSpouse().getName());
	}

}

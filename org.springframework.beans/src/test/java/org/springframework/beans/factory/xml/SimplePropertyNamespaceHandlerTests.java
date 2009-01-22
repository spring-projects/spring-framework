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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import test.beans.ITestBean;
import test.beans.TestBean;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 */
public class SimplePropertyNamespaceHandlerTests {

	@Test
	public void simpleBeanConfigured() throws Exception {
		XmlBeanFactory beanFactory =
				new XmlBeanFactory(new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
		ITestBean rob = (TestBean) beanFactory.getBean("rob");
		ITestBean sally = (TestBean) beanFactory.getBean("sally");
		assertEquals("Rob Harrop", rob.getName());
		assertEquals(24, rob.getAge());
		assertEquals(rob.getSpouse(), sally);
	}

	@Test
	public void innerBeanConfigured() throws Exception {
		XmlBeanFactory beanFactory =
				new XmlBeanFactory(new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
		TestBean sally = (TestBean) beanFactory.getBean("sally2");
		ITestBean rob = sally.getSpouse();
		assertEquals("Rob Harrop", rob.getName());
		assertEquals(24, rob.getAge());
		assertEquals(rob.getSpouse(), sally);
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void withPropertyDefinedTwice() throws Exception {
		new XmlBeanFactory(new ClassPathResource("simplePropertyNamespaceHandlerTestsWithErrors.xml", getClass()));
	}

	@Test
	public void propertyWithNameEndingInRef() throws Exception {
		XmlBeanFactory beanFactory =
				new XmlBeanFactory(new ClassPathResource("simplePropertyNamespaceHandlerTests.xml", getClass()));
		ITestBean sally = (TestBean) beanFactory.getBean("derivedSally");
		assertEquals("r", sally.getSpouse().getName());
	}

}

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

package org.springframework.beans.factory.generic;

import junit.framework.TestCase;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.beans.TestBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.AssertThrows;

import java.util.Map;

/**
 * @author Rob Harrop
 * @see 2.0
 */
public class GenericBeanFactoryAccessorTests extends TestCase {

	private GenericBeanFactoryAccessor beanFactoryAccessor;

	protected void setUp() throws Exception {
		XmlBeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("genericBeanFactoryAccessorTests.xml", getClass()));
		this.beanFactoryAccessor = new GenericBeanFactoryAccessor(beanFactory);
	}

	public void testGetBean() throws Exception {
		TestBean testBean = this.beanFactoryAccessor.getBean("testBean");
		assertNotNull("TestBean should not be null", testBean);
	}

	public void testGetBeanWithType() throws Exception {
		TestBean testBean = this.beanFactoryAccessor.getBean("testBean", TestBean.class);
		assertNotNull("TestBean should not be null", testBean);
	}

	public void testGetBeanFails() throws Exception {
		new AssertThrows(ClassCastException.class) {
			public void test() throws Exception {
				Integer bean = beanFactoryAccessor.getBean("testBean");
			}
		}.runTest();
	}

	public void testGetBeansOfType() throws Exception {
		Map<String, TestBean> beansOfType = this.beanFactoryAccessor.getBeansOfType(TestBean.class);
		assertEquals(3, beansOfType.size());
		assertNotNull(beansOfType.get("testBean"));
		assertNotNull(beansOfType.get("otherTestBean"));
		assertNotNull(beansOfType.get("prototypeTestBean"));
	}

	public void testGetBeansOfTypeExtended() throws Exception {
		Map<String, TestBean> beansOfType = this.beanFactoryAccessor.getBeansOfType(TestBean.class, false, false);
		assertEquals(2, beansOfType.size());
		assertNotNull(beansOfType.get("testBean"));
		assertNotNull(beansOfType.get("otherTestBean"));
	}

	public void testGetBeansWithAnnotation() throws Exception {
		Map<String, Object> beansWithAnnotation = this.beanFactoryAccessor.getBeansWithAnnotation(MyAnnotation.class);
		assertEquals(1, beansWithAnnotation.size());
		assertNotNull(beansWithAnnotation.get("annotatedBean"));
	}
}

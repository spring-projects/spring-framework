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

package org.springframework.beans.factory.config;

import junit.framework.TestCase;

import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Juergen Hoeller
 * @since 04.10.2004
 */
public class PropertyPathFactoryBeanTests extends TestCase {

	public void testPropertyPathFactoryBeanWithSingletonResult() {
		XmlBeanFactory xbf = new XmlBeanFactory(new ClassPathResource("propertyPath.xml", getClass()));
		assertEquals(new Integer(12), xbf.getBean("propertyPath1"));
		assertEquals(new Integer(11), xbf.getBean("propertyPath2"));
		assertEquals(new Integer(10), xbf.getBean("tb.age"));
		assertEquals(ITestBean.class, xbf.getType("otb.spouse"));
		Object result1 = xbf.getBean("otb.spouse");
		Object result2 = xbf.getBean("otb.spouse");
		assertTrue(result1 instanceof TestBean);
		assertTrue(result1 == result2);
		assertEquals(99, ((TestBean) result1).getAge());
	}

	public void testPropertyPathFactoryBeanWithPrototypeResult() {
		XmlBeanFactory xbf = new XmlBeanFactory(new ClassPathResource("propertyPath.xml", getClass()));
		assertNull(xbf.getType("tb.spouse"));
		assertEquals(TestBean.class, xbf.getType("propertyPath3"));
		Object result1 = xbf.getBean("tb.spouse");
		Object result2 = xbf.getBean("propertyPath3");
		Object result3 = xbf.getBean("propertyPath3");
		assertTrue(result1 instanceof TestBean);
		assertTrue(result2 instanceof TestBean);
		assertTrue(result3 instanceof TestBean);
		assertEquals(11, ((TestBean) result1).getAge());
		assertEquals(11, ((TestBean) result2).getAge());
		assertEquals(11, ((TestBean) result3).getAge());
		assertTrue(result1 != result2);
		assertTrue(result1 != result3);
		assertTrue(result2 != result3);
	}

	public void testPropertyPathFactoryBeanWithNullResult() {
		XmlBeanFactory xbf = new XmlBeanFactory(new ClassPathResource("propertyPath.xml", getClass()));
		assertNull(xbf.getType("tb.spouse.spouse"));
		assertNull(xbf.getBean("tb.spouse.spouse"));
	}

	public void testPropertyPathFactoryBeanAsInnerBean() {
		XmlBeanFactory xbf = new XmlBeanFactory(new ClassPathResource("propertyPath.xml", getClass()));
		TestBean spouse = (TestBean) xbf.getBean("otb.spouse");
		TestBean tbWithInner = (TestBean) xbf.getBean("tbWithInner");
		assertSame(spouse, tbWithInner.getSpouse());
		assertTrue(!tbWithInner.getFriends().isEmpty());
		assertSame(spouse, tbWithInner.getFriends().iterator().next());
	}

}

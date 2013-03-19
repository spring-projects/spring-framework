/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.springframework.tests.TestResourceUtils.qualifiedResource;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;


/**
 * Unit tests for {@link PropertyPathFactoryBean}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 04.10.2004
 */
public class PropertyPathFactoryBeanTests {

	private static final Resource CONTEXT = qualifiedResource(PropertyPathFactoryBeanTests.class, "context.xml");

	@Test
	public void testPropertyPathFactoryBeanWithSingletonResult() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
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

	@Test
	public void testPropertyPathFactoryBeanWithPrototypeResult() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
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

	@Test
	public void testPropertyPathFactoryBeanWithNullResult() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
		assertNull(xbf.getType("tb.spouse.spouse"));
		assertNull(xbf.getBean("tb.spouse.spouse"));
	}

	@Test
	public void testPropertyPathFactoryBeanAsInnerBean() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
		TestBean spouse = (TestBean) xbf.getBean("otb.spouse");
		TestBean tbWithInner = (TestBean) xbf.getBean("tbWithInner");
		assertSame(spouse, tbWithInner.getSpouse());
		assertTrue(!tbWithInner.getFriends().isEmpty());
		assertSame(spouse, tbWithInner.getFriends().iterator().next());
	}

}

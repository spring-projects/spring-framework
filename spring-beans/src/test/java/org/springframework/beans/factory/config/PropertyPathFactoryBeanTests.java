/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.*;
import static test.util.TestResourceUtils.qualifiedResource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;

import test.beans.ITestBean;
import test.beans.TestBean;

/**
 * Unit tests for {@link PropertyPathFactoryBean}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 04.10.2004
 */
public class PropertyPathFactoryBeanTests {

	private static final Resource CONTEXT = qualifiedResource(PropertyPathFactoryBeanTests.class, "context.xml");

	private DefaultListableBeanFactory bf;

	@Before
	public void setup() {
		this.bf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
		reader.loadBeanDefinitions(CONTEXT);
	}

	@Test
	public void testPropertyPathFactoryBeanWithSingletonResult() {
		assertEquals(new Integer(12), bf.getBean("propertyPath1"));
		assertEquals(new Integer(11), bf.getBean("propertyPath2"));
		assertEquals(new Integer(10), bf.getBean("tb.age"));
		assertEquals(ITestBean.class, bf.getType("otb.spouse"));
		Object result1 = bf.getBean("otb.spouse");
		Object result2 = bf.getBean("otb.spouse");
		assertTrue(result1 instanceof TestBean);
		assertTrue(result1 == result2);
		assertEquals(99, ((TestBean) result1).getAge());
	}

	@Test
	public void testPropertyPathFactoryBeanWithPrototypeResult() {
		assertNull(bf.getType("tb.spouse"));
		assertEquals(TestBean.class, bf.getType("propertyPath3"));
		Object result1 = bf.getBean("tb.spouse");
		Object result2 = bf.getBean("propertyPath3");
		Object result3 = bf.getBean("propertyPath3");
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
		assertNull(bf.getType("tb.spouse.spouse"));
		assertNull(bf.getBean("tb.spouse.spouse"));
	}

	@Test
	public void testPropertyPathFactoryBeanAsInnerBean() {
		TestBean spouse = (TestBean) bf.getBean("otb.spouse");
		TestBean tbWithInner = (TestBean) bf.getBean("tbWithInner");
		assertSame(spouse, tbWithInner.getSpouse());
		assertTrue(!tbWithInner.getFriends().isEmpty());
		assertSame(spouse, tbWithInner.getFriends().iterator().next());
	}

}

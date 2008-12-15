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

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * Simple test to illustrate and verify scope usage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class SimpleScopeTests {

	private DefaultListableBeanFactory beanFactory;
	
	@Before
	public void setUp() {
		beanFactory = new DefaultListableBeanFactory();
		Scope scope = new NoOpScope() {
			private int index;
			private List<TestBean> objects = new LinkedList<TestBean>(); {
				objects.add(new TestBean());
				objects.add(new TestBean());
			}
			public Object get(String name, ObjectFactory<?> objectFactory) {
				if (index >= objects.size()) {
					index = 0;
				}
				return objects.get(index++);
			}
		};

		beanFactory.registerScope("myScope", scope);

		String[] scopeNames = beanFactory.getRegisteredScopeNames();
		assertEquals(1, scopeNames.length);
		assertEquals("myScope", scopeNames[0]);
		assertSame(scope, beanFactory.getRegisteredScope("myScope"));

		XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(beanFactory);
		xbdr.loadBeanDefinitions("org/springframework/beans/factory/config/simpleScope.xml");
	}
	
	@Test
	public void testCanGetScopedObject() {
		TestBean tb1 = (TestBean) beanFactory.getBean("usesScope");
		TestBean tb2 = (TestBean) beanFactory.getBean("usesScope");
		assertNotSame(tb1, tb2);
		TestBean tb3 = (TestBean) beanFactory.getBean("usesScope");
		assertSame(tb3, tb1);
	}

}

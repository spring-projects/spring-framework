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

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Simple test to illustrate and verify scope usage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class SimpleScopeTests extends TestCase {

	private GenericApplicationContext applicationContext;
	
	protected void setUp() {
		applicationContext = new GenericApplicationContext();
		Scope scope = new NoOpScope() {
			private int index;
			private List objects = new LinkedList(); {
				objects.add(new TestBean());
				objects.add(new TestBean());
			}
			public Object get(String name, ObjectFactory objectFactory) {
				if (index >= objects.size()) {
					index = 0;
				}
				return objects.get(index++);
			}
		};

		applicationContext.getBeanFactory().registerScope("myScope", scope);

		String[] scopeNames = applicationContext.getBeanFactory().getRegisteredScopeNames();
		assertEquals(1, scopeNames.length);
		assertEquals("myScope", scopeNames[0]);
		assertSame(scope, applicationContext.getBeanFactory().getRegisteredScope("myScope"));

		XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(applicationContext);
		xbdr.loadBeanDefinitions("org/springframework/beans/factory/config/simpleScope.xml");
		
		applicationContext.refresh();
	}
	
	public void testCanGetScopedObject() {
		TestBean tb1 = (TestBean) applicationContext.getBean("usesScope");
		TestBean tb2 = (TestBean) applicationContext.getBean("usesScope");
		assertNotSame(tb1, tb2);
		TestBean tb3 = (TestBean) applicationContext.getBean("usesScope");
		assertSame(tb3, tb1);
	}

}

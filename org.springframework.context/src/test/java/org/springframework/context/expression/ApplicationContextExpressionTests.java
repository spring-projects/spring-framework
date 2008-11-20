/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.context.expression;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Juergen Hoeller
 */
public class ApplicationContextExpressionTests extends TestCase {

	public void testGenericApplicationContext() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.getBeanFactory().registerScope("myScope", new Scope() {
			public Object get(String name, ObjectFactory objectFactory) {
				return objectFactory.getObject();
			}
			public Object remove(String name) {
				return null;
			}
			public void registerDestructionCallback(String name, Runnable callback) {
			}
			public Object resolveContextualObject(String key) {
				if (key.equals("mySpecialAttr")) {
					return "42";
				}
				else {
					return null;
				}
			}
			public String getConversationId() {
				return null;
			}
		});
		GenericBeanDefinition bd1 = new GenericBeanDefinition();
		bd1.setBeanClass(TestBean.class);
		bd1.getPropertyValues().addPropertyValue("name", "myName");
		ac.registerBeanDefinition("tb1", bd1);
		GenericBeanDefinition bd2 = new GenericBeanDefinition();
		bd2.setBeanClass(TestBean.class);
		bd2.setScope("myScope");
		bd2.getPropertyValues().addPropertyValue("name", "XXX#{tb1.name}YYY#{mySpecialAttr}ZZZ");
		bd2.getPropertyValues().addPropertyValue("age", "#{mySpecialAttr}");
		bd2.getPropertyValues().addPropertyValue("country", "#{systemProperties.country}");
		ac.registerBeanDefinition("tb2", bd2);
		System.getProperties().put("country", "UK");
		try {
			ac.refresh();
			TestBean tb2 = (TestBean) ac.getBean("tb2");
			assertEquals("XXXmyNameYYY42ZZZ", tb2.getName());
			assertEquals(42, tb2.getAge());
			assertEquals("UK", tb2.getCountry());
		}
		finally {
			System.getProperties().remove("country");
		}
	}

}

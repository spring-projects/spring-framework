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

package org.springframework.aop.target;

import static org.junit.Assert.*;
import static test.util.TestResourceUtils.beanFactoryFromQualifiedResource;

import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import test.beans.ITestBean;

/**
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @since 07.01.2005
 */
public final class LazyInitTargetSourceTests {

	@Test
	public void testLazyInitSingletonTargetSource() {
		DefaultListableBeanFactory bf = beanFactoryFromQualifiedResource(getClass(), "singleton.xml");
		bf.preInstantiateSingletons();

		ITestBean tb = (ITestBean) bf.getBean("proxy");
		assertFalse(bf.containsSingleton("target"));
		assertEquals(10, tb.getAge());
		assertTrue(bf.containsSingleton("target"));
	}

	@Test
	public void testCustomLazyInitSingletonTargetSource() {
		DefaultListableBeanFactory bf = beanFactoryFromQualifiedResource(getClass(),
			"customTarget.xml");
		bf.preInstantiateSingletons();

		ITestBean tb = (ITestBean) bf.getBean("proxy");
		assertFalse(bf.containsSingleton("target"));
		assertEquals("Rob Harrop", tb.getName());
		assertTrue(bf.containsSingleton("target"));
	}

	@Test
	public void testLazyInitFactoryBeanTargetSource() {
		DefaultListableBeanFactory bf = beanFactoryFromQualifiedResource(getClass(),
			"factoryBean.xml");
		bf.preInstantiateSingletons();

		Set<?> set1 = (Set<?>) bf.getBean("proxy1");
		assertFalse(bf.containsSingleton("target1"));
		assertTrue(set1.contains("10"));
		assertTrue(bf.containsSingleton("target1"));

		Set<?> set2 = (Set<?>) bf.getBean("proxy2");
		assertFalse(bf.containsSingleton("target2"));
		assertTrue(set2.contains("20"));
		assertTrue(bf.containsSingleton("target2"));
	}


	public static class CustomLazyInitTargetSource extends LazyInitTargetSource {

		protected void postProcessTargetObject(Object targetObject) {
			((ITestBean) targetObject).setName("Rob Harrop");
		}
	}

}

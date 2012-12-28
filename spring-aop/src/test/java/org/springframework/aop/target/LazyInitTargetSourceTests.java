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

package org.springframework.aop.target;

import static org.junit.Assert.*;
import static test.util.TestResourceUtils.qualifiedResource;

import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.Resource;

import test.beans.ITestBean;

/**
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @since 07.01.2005
 */
public final class LazyInitTargetSourceTests {

	private static final Class<?> CLASS = LazyInitTargetSourceTests.class;

	private static final Resource SINGLETON_CONTEXT = qualifiedResource(CLASS, "singleton.xml");
	private static final Resource CUSTOM_TARGET_CONTEXT = qualifiedResource(CLASS, "customTarget.xml");
	private static final Resource FACTORY_BEAN_CONTEXT = qualifiedResource(CLASS, "factoryBean.xml");

	@Test
	public void testLazyInitSingletonTargetSource() {
		XmlBeanFactory bf = new XmlBeanFactory(SINGLETON_CONTEXT);
		bf.preInstantiateSingletons();

		ITestBean tb = (ITestBean) bf.getBean("proxy");
		assertFalse(bf.containsSingleton("target"));
		assertEquals(10, tb.getAge());
		assertTrue(bf.containsSingleton("target"));
	}

	@Test
	public void testCustomLazyInitSingletonTargetSource() {
		XmlBeanFactory bf = new XmlBeanFactory(CUSTOM_TARGET_CONTEXT);
		bf.preInstantiateSingletons();

		ITestBean tb = (ITestBean) bf.getBean("proxy");
		assertFalse(bf.containsSingleton("target"));
		assertEquals("Rob Harrop", tb.getName());
		assertTrue(bf.containsSingleton("target"));
	}

	@Test
	public void testLazyInitFactoryBeanTargetSource() {
		XmlBeanFactory bf = new XmlBeanFactory(FACTORY_BEAN_CONTEXT);
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


	@SuppressWarnings("serial")
	public static class CustomLazyInitTargetSource extends LazyInitTargetSource {

		@Override
		protected void postProcessTargetObject(Object targetObject) {
			((ITestBean) targetObject).setName("Rob Harrop");
		}
	}

}

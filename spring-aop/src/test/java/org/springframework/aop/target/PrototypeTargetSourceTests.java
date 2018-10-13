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

package org.springframework.aop.target;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.tests.sample.beans.SideEffectBean;

import static org.junit.Assert.*;
import static org.springframework.tests.TestResourceUtils.*;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class PrototypeTargetSourceTests {

	private static final Resource CONTEXT = qualifiedResource(PrototypeTargetSourceTests.class, "context.xml");

	/** Initial count value set in bean factory XML */
	private static final int INITIAL_COUNT = 10;

	private BeanFactory beanFactory;

	@Before
	public void setUp() throws Exception {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader((BeanDefinitionRegistry) this.beanFactory).loadBeanDefinitions(CONTEXT);
	}

	/**
	 * Test that multiple invocations of the prototype bean will result
	 * in no change to visible state, as a new instance is used.
	 * With the singleton, there will be change.
	 */
	@Test
	public void testPrototypeAndSingletonBehaveDifferently() {
		SideEffectBean singleton = (SideEffectBean) beanFactory.getBean("singleton");
		assertEquals(INITIAL_COUNT, singleton.getCount());
		singleton.doWork();
		assertEquals(INITIAL_COUNT + 1, singleton.getCount());

		SideEffectBean prototype = (SideEffectBean) beanFactory.getBean("prototype");
		assertEquals(INITIAL_COUNT, prototype.getCount());
		prototype.doWork();
		assertEquals(INITIAL_COUNT, prototype.getCount());
	}


}

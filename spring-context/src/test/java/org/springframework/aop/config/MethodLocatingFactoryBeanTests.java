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

package org.springframework.aop.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public final class MethodLocatingFactoryBeanTests {

	private static final String BEAN_NAME = "string";
	private MethodLocatingFactoryBean factory;
	private BeanFactory beanFactory;

	@Before
	public void setUp() {
		factory = new MethodLocatingFactoryBean();
		beanFactory = mock(BeanFactory.class);
	}

	@Test
	public void testIsSingleton() {
		assertTrue(factory.isSingleton());
	}

	@Test
	public void testGetObjectType() {
		assertEquals(Method.class, factory.getObjectType());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithNullTargetBeanName() {
		factory.setMethodName("toString()");
		factory.setBeanFactory(beanFactory);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithEmptyTargetBeanName() {
		factory.setTargetBeanName("");
		factory.setMethodName("toString()");
		factory.setBeanFactory(beanFactory);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithNullTargetMethodName() {
		factory.setTargetBeanName(BEAN_NAME);
		factory.setBeanFactory(beanFactory);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithEmptyTargetMethodName() {
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("");
		factory.setBeanFactory(beanFactory);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWhenTargetBeanClassCannotBeResolved() {
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("toString()");
		factory.setBeanFactory(beanFactory);
		verify(beanFactory).getType(BEAN_NAME);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSunnyDayPath() throws Exception {
		given(beanFactory.getType(BEAN_NAME)).willReturn((Class)String.class);
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("toString()");
		factory.setBeanFactory(beanFactory);
		Object result = factory.getObject();
		assertNotNull(result);
		assertTrue(result instanceof Method);
		Method method = (Method) result;
		assertEquals("Bingo", method.invoke("Bingo"));
	}

	@Test(expected=IllegalArgumentException.class)
	@SuppressWarnings("unchecked")
	public void testWhereMethodCannotBeResolved() {
		given(beanFactory.getType(BEAN_NAME)).willReturn((Class)String.class);
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("loadOfOld()");
		factory.setBeanFactory(beanFactory);
	}

}

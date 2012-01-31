/*
 * Copyright 2002-2009 the original author or authors.
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

import java.lang.reflect.Method;

import static org.easymock.EasyMock.*;
import org.junit.After;
import static org.junit.Assert.*;
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
		
		// methods must set up expectations and call replay() manually for this mock
		beanFactory = createMock(BeanFactory.class);
	}
	
	@After
	public void tearDown() {
		verify(beanFactory);
	}

	@Test
	public void testIsSingleton() {
		replay(beanFactory);
		assertTrue(factory.isSingleton());
	}

	@Test
	public void testGetObjectType() {
		replay(beanFactory);
		assertEquals(Method.class, factory.getObjectType());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithNullTargetBeanName() {
		replay(beanFactory);
		
		factory.setMethodName("toString()");
		factory.setBeanFactory(beanFactory);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithEmptyTargetBeanName() {
		replay(beanFactory);
		
		factory.setTargetBeanName("");
		factory.setMethodName("toString()");
		factory.setBeanFactory(beanFactory);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithNullTargetMethodName() {
		replay(beanFactory);
		
		factory.setTargetBeanName(BEAN_NAME);
		factory.setBeanFactory(beanFactory);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithEmptyTargetMethodName() {
		replay(beanFactory);
		
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("");
		factory.setBeanFactory(beanFactory);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWhenTargetBeanClassCannotBeResolved() {
		expect(beanFactory.getType(BEAN_NAME)).andReturn(null);
		replay(beanFactory);
		
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("toString()");
		factory.setBeanFactory(beanFactory);
	}

	@Test
	public void testSunnyDayPath() throws Exception {
		expect((Class) beanFactory.getType(BEAN_NAME)).andReturn(String.class);
		replay(beanFactory);
		
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
	public void testWhereMethodCannotBeResolved() {
		expect((Class) beanFactory.getType(BEAN_NAME)).andReturn(String.class);
		replay(beanFactory);
		
		factory.setTargetBeanName(BEAN_NAME);
		factory.setMethodName("loadOfOld()");
		factory.setBeanFactory(beanFactory);
	}

}

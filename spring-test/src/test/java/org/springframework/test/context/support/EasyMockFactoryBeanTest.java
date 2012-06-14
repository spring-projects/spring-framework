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

package org.springframework.test.context.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.lang.reflect.Proxy;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.easymock.internal.ObjectMethodsFilter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;

/**
 * Test cases for {@link EasyMockFactoryBean} class.
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class EasyMockFactoryBeanTest {

	private EasyMockFactoryBean<Closeable> factory;

	private BeanFactory beanFactory;

	@Before
	public void setUp() {
		factory = new EasyMockFactoryBean<Closeable>();

		beanFactory = EasyMock.createStrictMock(BeanFactory.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAssertions() {
		factory.afterPropertiesSet();
	}

	@Test
	public void testFlowWithCustomMocksControl() {
		IMocksControl control = EasyMock.createControl();

		factory.setMockInterface(Closeable.class);
		factory.setMocksControl(control);
		factory.afterPropertiesSet();

		assertTrue(factory.isSingleton());
		assertEquals(Closeable.class, factory.getObjectType());

		// Ignore compiler warning, as after type erasure the return type of
		// getObject() is java.lang.Object.
		assertTrue(factory.getObject() instanceof Closeable);

		assertTrue(control == ((ObjectMethodsFilter) Proxy
				.getInvocationHandler(factory.getObject())).getDelegate()
				.getControl());
	}

	@Test
	public void testFlowWithMocksControlInContext() {
		IMocksControl control = EasyMock.createControl();

		EasyMock.expect(
				beanFactory.getBean(
						EasyMockFactoryBean.DEFAULT_MOCKS_CONTROL_BEAN_NAME,
						IMocksControl.class)).andReturn(control);

		EasyMock.replay(beanFactory);

		factory.setMockInterface(Closeable.class);
		factory.setBeanFactory(beanFactory);
		factory.afterPropertiesSet();

		assertTrue(factory.isSingleton());
		assertEquals(Closeable.class, factory.getObjectType());

		// Ignore compiler warning, as after type erasure the return type of
		// getObject() is java.lang.Object.
		assertTrue(factory.getObject() instanceof Closeable);

		assertTrue(control == ((ObjectMethodsFilter) Proxy
				.getInvocationHandler(factory.getObject())).getDelegate()
				.getControl());

		EasyMock.verify(beanFactory);
	}

	@Test
	public void testFlowWithNoMocksControlInContext() {
		EasyMock.expect(
				beanFactory.getBean(
						EasyMockFactoryBean.DEFAULT_MOCKS_CONTROL_BEAN_NAME,
						IMocksControl.class)).andReturn(null);

		EasyMock.replay(beanFactory);

		factory.setMockClass(Closeable.class);
		factory.setBeanFactory(beanFactory);
		factory.afterPropertiesSet();

		assertTrue(factory.isSingleton());
		assertEquals(Closeable.class, factory.getObjectType());

		// Ignore compiler warning, as after type erasure the return type of
		// getObject() is java.lang.Object.
		assertTrue(factory.getObject() instanceof Closeable);

		EasyMock.verify(beanFactory);
	}
}

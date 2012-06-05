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
 * Test cases for {@link EasyMockBeanFactory} class.
 * 
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 */
public class EasyMockBeanFactoryTest {

	private EasyMockBeanFactory<Closeable> easyMockBeanFactory;

	private BeanFactory beanFactory;

	@Before
	public void setUp() {
		easyMockBeanFactory = new EasyMockBeanFactory<Closeable>();

		beanFactory = EasyMock.createStrictMock(BeanFactory.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAssertions() {
		easyMockBeanFactory.afterPropertiesSet();
	}

	@Test
	public void testFlowWithCustomMocksControl() {
		IMocksControl control = EasyMock.createControl();

		easyMockBeanFactory.setMockInterface(Closeable.class);
		easyMockBeanFactory.setMocksControl(control);
		easyMockBeanFactory.afterPropertiesSet();

		assertTrue(easyMockBeanFactory.isSingleton());
		assertEquals(Closeable.class, easyMockBeanFactory.getObjectType());

		// Ignore compiler warning, as after type erasure the return type of getObject() is java.lang.Object.
		assertTrue(easyMockBeanFactory.getObject() instanceof Closeable);

		assertTrue(control == ((ObjectMethodsFilter) Proxy.getInvocationHandler(easyMockBeanFactory.getObject()))
				.getDelegate().getControl());
	}

	@Test
	public void testFlowWithMocksControlInContext() {
		IMocksControl control = EasyMock.createControl();

		EasyMock.expect(beanFactory.getBean(EasyMockBeanFactory.DEFAULT_MOCKS_CONTROL_BEAN_NAME, IMocksControl.class))
				.andReturn(control);

		EasyMock.replay(beanFactory);

		easyMockBeanFactory.setMockInterface(Closeable.class);
		easyMockBeanFactory.setBeanFactory(beanFactory);
		easyMockBeanFactory.afterPropertiesSet();

		assertTrue(easyMockBeanFactory.isSingleton());
		assertEquals(Closeable.class, easyMockBeanFactory.getObjectType());

		// Ignore compiler warning, as after type erasure the return type of getObject() is java.lang.Object.
		assertTrue(easyMockBeanFactory.getObject() instanceof Closeable);

		assertTrue(control == ((ObjectMethodsFilter) Proxy.getInvocationHandler(easyMockBeanFactory.getObject()))
				.getDelegate().getControl());

		EasyMock.verify(beanFactory);
	}

	@Test
	public void testFlowWithNoMocksControlInContext() {
		EasyMock.expect(beanFactory.getBean(EasyMockBeanFactory.DEFAULT_MOCKS_CONTROL_BEAN_NAME, IMocksControl.class))
				.andReturn(null);

		EasyMock.replay(beanFactory);

		easyMockBeanFactory.setMockClass(Closeable.class);
		easyMockBeanFactory.setBeanFactory(beanFactory);
		easyMockBeanFactory.afterPropertiesSet();

		assertTrue(easyMockBeanFactory.isSingleton());
		assertEquals(Closeable.class, easyMockBeanFactory.getObjectType());

		// Ignore compiler warning, as after type erasure the return type of getObject() is java.lang.Object.
		assertTrue(easyMockBeanFactory.getObject() instanceof Closeable);

		EasyMock.verify(beanFactory);
	}
}

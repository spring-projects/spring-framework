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

package org.springframework.beans.factory;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;

import test.beans.DerivedTestBean;
import test.beans.TestBean;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 04.07.2006
 */
public final class SharedBeanRegistryTests {

	@Test
	public void testSingletons() {
		DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();

		TestBean tb = new TestBean();
		beanRegistry.registerSingleton("tb", tb);
		assertSame(tb, beanRegistry.getSingleton("tb"));

		TestBean tb2 = (TestBean) beanRegistry.getSingleton("tb2", new ObjectFactory<Object>() {
			@Override
			public Object getObject() throws BeansException {
				return new TestBean();
			}
		});
		assertSame(tb2, beanRegistry.getSingleton("tb2"));

		assertSame(tb, beanRegistry.getSingleton("tb"));
		assertSame(tb2, beanRegistry.getSingleton("tb2"));
		assertEquals(2, beanRegistry.getSingletonCount());
		assertEquals(2, beanRegistry.getSingletonNames().length);
		assertTrue(Arrays.asList(beanRegistry.getSingletonNames()).contains("tb"));
		assertTrue(Arrays.asList(beanRegistry.getSingletonNames()).contains("tb2"));

		beanRegistry.destroySingletons();
		assertEquals(0, beanRegistry.getSingletonCount());
		assertEquals(0, beanRegistry.getSingletonNames().length);
	}

	@Test
	public void testDisposableBean() {
		DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();

		DerivedTestBean tb = new DerivedTestBean();
		beanRegistry.registerSingleton("tb", tb);
		beanRegistry.registerDisposableBean("tb", tb);
		assertSame(tb, beanRegistry.getSingleton("tb"));

		assertSame(tb, beanRegistry.getSingleton("tb"));
		assertEquals(1, beanRegistry.getSingletonCount());
		assertEquals(1, beanRegistry.getSingletonNames().length);
		assertTrue(Arrays.asList(beanRegistry.getSingletonNames()).contains("tb"));
		assertFalse(tb.wasDestroyed());

		beanRegistry.destroySingletons();
		assertEquals(0, beanRegistry.getSingletonCount());
		assertEquals(0, beanRegistry.getSingletonNames().length);
		assertTrue(tb.wasDestroyed());
	}

}

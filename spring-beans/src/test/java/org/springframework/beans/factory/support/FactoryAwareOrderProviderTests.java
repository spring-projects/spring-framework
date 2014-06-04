/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.beans.factory.support;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.Order;
import org.springframework.util.ReflectionUtils;

/**
 *
 * @author Stephane Nicoll
 */
public class FactoryAwareOrderProviderTests {

	@Rule
	public final TestName name = new TestName();

	@Mock
	private ConfigurableListableBeanFactory beanFactory;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void noBeanName() {
		FactoryAwareOrderProvider orderProvider = createOrderProvider(new HashMap<Object, String>());
		assertNull(orderProvider.getOrder(25));
	}

	@Test
	public void beanNameNotRegistered() {
		HashMap<Object, String> beans = new HashMap<>();
		beans.put(25, "myBean");
		given(beanFactory.containsBeanDefinition("myBean")).willReturn(false);
		FactoryAwareOrderProvider orderProvider = createOrderProvider(beans);
		assertNull(orderProvider.getOrder(25));
		verify(beanFactory).containsBeanDefinition("myBean");
	}


	@Test
	public void beanNameNoRootBeanDefinition() {
		HashMap<Object, String> beans = new HashMap<>();
		beans.put(25, "myBean");
		given(beanFactory.containsBeanDefinition("myBean")).willReturn(true);
		given(beanFactory.getMergedBeanDefinition("myBean")).willReturn(mock(BeanDefinition.class));
		FactoryAwareOrderProvider orderProvider = createOrderProvider(beans);
		assertNull(orderProvider.getOrder(25));
		verify(beanFactory).containsBeanDefinition("myBean");
		verify(beanFactory).getMergedBeanDefinition("myBean");
	}

	@Test
	public void beanNameNoFactory() {
		HashMap<Object, String> beans = new HashMap<>();
		beans.put(25, "myBean");
		RootBeanDefinition rbd = mock(RootBeanDefinition.class);
		given(rbd.getResolvedFactoryMethod()).willReturn(null);

		given(beanFactory.containsBeanDefinition("myBean")).willReturn(true);
		given(beanFactory.getMergedBeanDefinition("myBean")).willReturn(rbd);
		FactoryAwareOrderProvider orderProvider = createOrderProvider(beans);
		assertNull(orderProvider.getOrder(25));
		verify(beanFactory).containsBeanDefinition("myBean");
		verify(beanFactory).getMergedBeanDefinition("myBean");
	}

	@Test
	public void beanNameFactoryNoOrderValue() {
		HashMap<Object, String> beans = new HashMap<>();
		beans.put(25, "myBean");

		Method m = ReflectionUtils.findMethod(getClass(), name.getMethodName());
		RootBeanDefinition rbd = mock(RootBeanDefinition.class);
		given(rbd.getResolvedFactoryMethod()).willReturn(m);

		given(beanFactory.containsBeanDefinition("myBean")).willReturn(true);
		given(beanFactory.getMergedBeanDefinition("myBean")).willReturn(rbd);
		FactoryAwareOrderProvider orderProvider = createOrderProvider(beans);
		assertNull(orderProvider.getOrder(25));
		verify(beanFactory).containsBeanDefinition("myBean");
		verify(beanFactory).getMergedBeanDefinition("myBean");
	}

	@Test
	@Order(500)
	public void beanNameFactoryOrderValue() {
		HashMap<Object, String> beans = new HashMap<>();
		beans.put(25, "myBean");

		Method m = ReflectionUtils.findMethod(getClass(), name.getMethodName());
		RootBeanDefinition rbd = mock(RootBeanDefinition.class);
		given(rbd.getResolvedFactoryMethod()).willReturn(m);

		given(beanFactory.containsBeanDefinition("myBean")).willReturn(true);
		given(beanFactory.getMergedBeanDefinition("myBean")).willReturn(rbd);
		FactoryAwareOrderProvider orderProvider = createOrderProvider(beans);
		assertEquals(Integer.valueOf(500), orderProvider.getOrder(25));
		verify(beanFactory).containsBeanDefinition("myBean");
		verify(beanFactory).getMergedBeanDefinition("myBean");
	}

	private FactoryAwareOrderProvider createOrderProvider(HashMap<Object, String> beans) {
		return new FactoryAwareOrderProvider(beans, beanFactory);
	}

}

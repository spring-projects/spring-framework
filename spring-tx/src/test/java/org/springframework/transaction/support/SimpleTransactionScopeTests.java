/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.transaction.support;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class SimpleTransactionScopeTests {

	@Test
	@SuppressWarnings("resource")
	public void getFromScope() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerScope("tx", new SimpleTransactionScope());

		GenericBeanDefinition bd1 = new GenericBeanDefinition();
		bd1.setBeanClass(TestBean.class);
		bd1.setScope("tx");
		bd1.setPrimary(true);
		context.registerBeanDefinition("txScopedObject1", bd1);

		GenericBeanDefinition bd2 = new GenericBeanDefinition();
		bd2.setBeanClass(DerivedTestBean.class);
		bd2.setScope("tx");
		context.registerBeanDefinition("txScopedObject2", bd2);

		context.refresh();

		try {
			context.getBean(TestBean.class);
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected - no synchronization active
			assertTrue(ex.getCause() instanceof IllegalStateException);
		}

		try {
			context.getBean(DerivedTestBean.class);
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected - no synchronization active
			assertTrue(ex.getCause() instanceof IllegalStateException);
		}

		TestBean bean1 = null;
		DerivedTestBean bean2 = null;
		DerivedTestBean bean2a = null;
		DerivedTestBean bean2b = null;

		TransactionSynchronizationManager.initSynchronization();
		try {
			bean1 = context.getBean(TestBean.class);
			assertSame(bean1, context.getBean(TestBean.class));

			bean2 = context.getBean(DerivedTestBean.class);
			assertSame(bean2, context.getBean(DerivedTestBean.class));
			context.getBeanFactory().destroyScopedBean("txScopedObject2");
			assertFalse(TransactionSynchronizationManager.hasResource("txScopedObject2"));
			assertTrue(bean2.wasDestroyed());

			bean2a = context.getBean(DerivedTestBean.class);
			assertSame(bean2a, context.getBean(DerivedTestBean.class));
			assertNotSame(bean2, bean2a);
			context.getBeanFactory().getRegisteredScope("tx").remove("txScopedObject2");
			assertFalse(TransactionSynchronizationManager.hasResource("txScopedObject2"));
			assertFalse(bean2a.wasDestroyed());

			bean2b = context.getBean(DerivedTestBean.class);
			assertSame(bean2b, context.getBean(DerivedTestBean.class));
			assertNotSame(bean2, bean2a);
			assertNotSame(bean2, bean2b);
			assertNotSame(bean2a, bean2b);
		}
		finally {
			TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
			TransactionSynchronizationManager.clearSynchronization();
		}

		assertFalse(bean2a.wasDestroyed());
		assertTrue(bean2b.wasDestroyed());
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());

		try {
			context.getBean(TestBean.class);
			fail("Should have thrown IllegalStateException");
		}
		catch (BeanCreationException ex) {
			// expected - no synchronization active
			assertTrue(ex.getCause() instanceof IllegalStateException);
		}

		try {
			context.getBean(DerivedTestBean.class);
			fail("Should have thrown IllegalStateException");
		}
		catch (BeanCreationException ex) {
			// expected - no synchronization active
			assertTrue(ex.getCause() instanceof IllegalStateException);
		}
	}

}

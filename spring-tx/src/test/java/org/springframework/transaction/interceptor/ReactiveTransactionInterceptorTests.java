/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.interceptor;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.transaction.ReactiveTransactionManager;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TransactionInterceptor} with reactive methods.
 *
 * @author Mark Paluch
 */
public class ReactiveTransactionInterceptorTests extends AbstractReactiveTransactionAspectTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Override
	protected Object advised(Object target, ReactiveTransactionManager ptm, TransactionAttributeSource[] tas) {
		TransactionInterceptor ti = new TransactionInterceptor();
		ti.setTransactionManager(ptm);
		ti.setTransactionAttributeSources(tas);

		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(0, ti);
		return pf.getProxy();
	}

	/**
	 * Template method to create an advised object given the
	 * target object and transaction setup.
	 * Creates a TransactionInterceptor and applies it.
	 */
	@Override
	protected Object advised(Object target, ReactiveTransactionManager ptm, TransactionAttributeSource tas) {
		TransactionInterceptor ti = new TransactionInterceptor();
		ti.setTransactionManager(ptm);

		assertThat(ti.getTransactionManager()).isEqualTo(ptm);
		ti.setTransactionAttributeSource(tas);
		assertThat(ti.getTransactionAttributeSource()).isEqualTo(tas);

		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(0, ti);
		return pf.getProxy();
	}
	
	private TransactionInterceptor createTransactionInterceptor(BeanFactory beanFactory,
			String transactionManagerName, ReactiveTransactionManager transactionManager) {

		TransactionInterceptor ti = new TransactionInterceptor();
		if (beanFactory != null) {
			ti.setBeanFactory(beanFactory);
		}
		if (transactionManagerName != null) {
			ti.setTransactionManagerBeanName(transactionManagerName);

		}
		if (transactionManager != null) {
			ti.setTransactionManager(transactionManager);
		}
		ti.setTransactionAttributeSource(new NameMatchTransactionAttributeSource());
		ti.afterPropertiesSet();
		return ti;
	}
	
		private TransactionInterceptor transactionInterceptorWithTransactionManager(
			ReactiveTransactionManager transactionManager, BeanFactory beanFactory) {

		return createTransactionInterceptor(beanFactory, null, transactionManager);
	}

	private TransactionInterceptor transactionInterceptorWithTransactionManagerName(
			String transactionManagerName, BeanFactory beanFactory) {

		return createTransactionInterceptor(beanFactory, transactionManagerName, null);
	}

	private TransactionInterceptor simpleTransactionInterceptor(BeanFactory beanFactory) {
		return createTransactionInterceptor(beanFactory, null, null);
	}

	private ReactiveTransactionManager associateTransactionManager(BeanFactory beanFactory, String name) {
		ReactiveTransactionManager transactionManager = mock(ReactiveTransactionManager.class);
		when(beanFactory.containsBean(name)).thenReturn(true);
		when(beanFactory.getBean(name, ReactiveTransactionManager.class)).thenReturn(transactionManager);
		return transactionManager;
	}

}
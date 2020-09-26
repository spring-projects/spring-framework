/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.dao.annotation;

import java.util.ArrayList;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.stereotype.Repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for standalone usage of a PersistenceExceptionTranslationInterceptor,
 * as explicit advice bean in a BeanFactory rather than applied as part of a
 * PersistenceExceptionTranslationAdvisor.
 *
 * @author Juergen Hoeller
 * @author Tadaya Tsuyukubo
 */
public class PersistenceExceptionTranslationInterceptorTests extends PersistenceExceptionTranslationAdvisorTests {

	@Override
	protected void addPersistenceExceptionTranslation(ProxyFactory pf, PersistenceExceptionTranslator pet) {
		if (AnnotationUtils.findAnnotation(pf.getTargetClass(), Repository.class) != null) {
			DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
			bf.registerBeanDefinition("peti", new RootBeanDefinition(PersistenceExceptionTranslationInterceptor.class));
			bf.registerSingleton("pet", pet);
			pf.addAdvice((PersistenceExceptionTranslationInterceptor) bf.getBean("peti"));
		}
	}

	@Test
	void detectPersistenceExceptionTranslators() throws Throwable {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
		bf.registerBeanDefinition("peti", new RootBeanDefinition(PersistenceExceptionTranslationInterceptor.class));

		List<Integer> callOrder = new ArrayList<>();
		bf.registerSingleton("pet20", new CallOrderAwareExceptionTranslator(20, callOrder));
		bf.registerSingleton("pet10", new CallOrderAwareExceptionTranslator(10, callOrder));
		bf.registerSingleton("pet30", new CallOrderAwareExceptionTranslator(30, callOrder));

		PersistenceExceptionTranslationInterceptor interceptor =
				bf.getBean("peti", PersistenceExceptionTranslationInterceptor.class);
		interceptor.setAlwaysTranslate(true);

		RuntimeException exception = new RuntimeException();
		MethodInvocation invocation = mock(MethodInvocation.class);
		given(invocation.proceed()).willThrow(exception);

		assertThatThrownBy(() -> interceptor.invoke(invocation)).isSameAs(exception);

		assertThat(callOrder).containsExactly(10, 20, 30);
	}


	private static class CallOrderAwareExceptionTranslator implements PersistenceExceptionTranslator, Ordered {

		private final int order;

		private final List<Integer> callOrder;

		public CallOrderAwareExceptionTranslator(int order, List<Integer> callOrder) {
			this.order = order;
			this.callOrder = callOrder;
		}

		@Override
		public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
			callOrder.add(this.order);
			return null;
		}

		@Override
		public int getOrder() {
			return this.order;
		}
	}

}

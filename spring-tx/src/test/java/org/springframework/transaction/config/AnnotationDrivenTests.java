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

package org.springframework.transaction.config;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.transaction.CallCountingTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class AnnotationDrivenTests {

	@Test
	public void withProxyTargetClass() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("annotationDrivenProxyTargetClassTests.xml", getClass());
		doTestWithMultipleTransactionManagers(context);
	}

	@Test
	public void withConfigurationClass() throws Exception {
		ApplicationContext parent = new AnnotationConfigApplicationContext(TransactionManagerConfiguration.class);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"annotationDrivenConfigurationClassTests.xml"}, getClass(), parent);
		doTestWithMultipleTransactionManagers(context);
	}

	@Test
	public void withAnnotatedTransactionManagers() throws Exception {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.registerBeanDefinition("transactionManager1", new RootBeanDefinition(SynchTransactionManager.class));
		parent.registerBeanDefinition("transactionManager2", new RootBeanDefinition(NoSynchTransactionManager.class));
		parent.refresh();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"annotationDrivenConfigurationClassTests.xml"}, getClass(), parent);
		doTestWithMultipleTransactionManagers(context);
	}

	private void doTestWithMultipleTransactionManagers(ApplicationContext context) {
		CallCountingTransactionManager tm1 = context.getBean("transactionManager1", CallCountingTransactionManager.class);
		CallCountingTransactionManager tm2 = context.getBean("transactionManager2", CallCountingTransactionManager.class);
		TransactionalService service = context.getBean("service", TransactionalService.class);
		assertThat(AopUtils.isCglibProxy(service)).isTrue();
		service.setSomething("someName");
		assertThat(tm1.commits).isEqualTo(1);
		assertThat(tm2.commits).isEqualTo(0);
		service.doSomething();
		assertThat(tm1.commits).isEqualTo(1);
		assertThat(tm2.commits).isEqualTo(1);
		service.setSomething("someName");
		assertThat(tm1.commits).isEqualTo(2);
		assertThat(tm2.commits).isEqualTo(1);
		service.doSomething();
		assertThat(tm1.commits).isEqualTo(2);
		assertThat(tm2.commits).isEqualTo(2);
	}

	@Test
	@SuppressWarnings("resource")
	public void serializableWithPreviousUsage() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("annotationDrivenProxyTargetClassTests.xml", getClass());
		TransactionalService service = context.getBean("service", TransactionalService.class);
		service.setSomething("someName");
		service = (TransactionalService) SerializationTestUtils.serializeAndDeserialize(service);
		service.setSomething("someName");
	}

	@Test
	@SuppressWarnings("resource")
	public void serializableWithoutPreviousUsage() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("annotationDrivenProxyTargetClassTests.xml", getClass());
		TransactionalService service = context.getBean("service", TransactionalService.class);
		service = (TransactionalService) SerializationTestUtils.serializeAndDeserialize(service);
		service.setSomething("someName");
	}


	@SuppressWarnings("serial")
	public static class TransactionCheckingInterceptor implements MethodInterceptor, Serializable {

		@Override
		public Object invoke(MethodInvocation methodInvocation) throws Throwable {
			if (methodInvocation.getMethod().getName().equals("setSomething")) {
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
			}
			else {
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
			}
			return methodInvocation.proceed();
		}
	}

}

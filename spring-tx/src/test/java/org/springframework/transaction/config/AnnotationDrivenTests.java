/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class AnnotationDrivenTests {

	@Test
	void withProxyTargetClass() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("annotationDrivenProxyTargetClassTests.xml", getClass());
		doTestWithMultipleTransactionManagers(context);
	}

	@Test
	void withConfigurationClass() {
		ApplicationContext parent = new AnnotationConfigApplicationContext(TransactionManagerConfiguration.class);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"annotationDrivenConfigurationClassTests.xml"}, getClass(), parent);
		doTestWithMultipleTransactionManagers(context);
	}

	@Test
	void withAnnotatedTransactionManagers() {
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
	void serializableWithPreviousUsage() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("annotationDrivenProxyTargetClassTests.xml", getClass());
		TransactionalService service = context.getBean("service", TransactionalService.class);
		service.setSomething("someName");
		service = SerializationTestUtils.serializeAndDeserialize(service);
		service.setSomething("someName");
	}

	@Test
	void serializableWithoutPreviousUsage() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("annotationDrivenProxyTargetClassTests.xml", getClass());
		TransactionalService service = context.getBean("service", TransactionalService.class);
		service = SerializationTestUtils.serializeAndDeserialize(service);
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

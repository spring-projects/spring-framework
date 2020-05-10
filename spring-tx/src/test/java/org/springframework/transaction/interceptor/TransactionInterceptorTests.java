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

import java.io.Serializable;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Mock object based tests for TransactionInterceptor.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.03.2003
 */
public class TransactionInterceptorTests extends AbstractTransactionAspectTests {

	@Override
	protected Object advised(Object target, PlatformTransactionManager ptm, TransactionAttributeSource[] tas) {
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
	protected Object advised(Object target, PlatformTransactionManager ptm, TransactionAttributeSource tas) {
		TransactionInterceptor ti = new TransactionInterceptor();
		ti.setTransactionManager(ptm);
		assertThat(ti.getTransactionManager()).isEqualTo(ptm);
		ti.setTransactionAttributeSource(tas);
		assertThat(ti.getTransactionAttributeSource()).isEqualTo(tas);

		ProxyFactory pf = new ProxyFactory(target);
		pf.addAdvice(0, ti);
		return pf.getProxy();
	}


	/**
	 * A TransactionInterceptor should be serializable if its
	 * PlatformTransactionManager is.
	 */
	@Test
	public void serializableWithAttributeProperties() throws Exception {
		TransactionInterceptor ti = new TransactionInterceptor();
		Properties props = new Properties();
		props.setProperty("methodName", "PROPAGATION_REQUIRED");
		ti.setTransactionAttributes(props);
		PlatformTransactionManager ptm = new SerializableTransactionManager();
		ti.setTransactionManager(ptm);
		ti = (TransactionInterceptor) SerializationTestUtils.serializeAndDeserialize(ti);

		// Check that logger survived deserialization
		assertThat(ti.logger).isNotNull();
		boolean condition = ti.getTransactionManager() instanceof SerializableTransactionManager;
		assertThat(condition).isTrue();
		assertThat(ti.getTransactionAttributeSource()).isNotNull();
	}

	@Test
	public void serializableWithCompositeSource() throws Exception {
		NameMatchTransactionAttributeSource tas1 = new NameMatchTransactionAttributeSource();
		Properties props = new Properties();
		props.setProperty("methodName", "PROPAGATION_REQUIRED");
		tas1.setProperties(props);

		NameMatchTransactionAttributeSource tas2 = new NameMatchTransactionAttributeSource();
		props = new Properties();
		props.setProperty("otherMethodName", "PROPAGATION_REQUIRES_NEW");
		tas2.setProperties(props);

		TransactionInterceptor ti = new TransactionInterceptor();
		ti.setTransactionAttributeSources(tas1, tas2);
		PlatformTransactionManager ptm = new SerializableTransactionManager();
		ti.setTransactionManager(ptm);
		ti = (TransactionInterceptor) SerializationTestUtils.serializeAndDeserialize(ti);

		boolean condition3 = ti.getTransactionManager() instanceof SerializableTransactionManager;
		assertThat(condition3).isTrue();
		boolean condition2 = ti.getTransactionAttributeSource() instanceof CompositeTransactionAttributeSource;
		assertThat(condition2).isTrue();
		CompositeTransactionAttributeSource ctas = (CompositeTransactionAttributeSource) ti.getTransactionAttributeSource();
		boolean condition1 = ctas.getTransactionAttributeSources()[0] instanceof NameMatchTransactionAttributeSource;
		assertThat(condition1).isTrue();
		boolean condition = ctas.getTransactionAttributeSources()[1] instanceof NameMatchTransactionAttributeSource;
		assertThat(condition).isTrue();
	}

	@Test
	public void determineTransactionManagerWithNoBeanFactory() {
		PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
		TransactionInterceptor ti = transactionInterceptorWithTransactionManager(transactionManager, null);

		assertThat(ti.determineTransactionManager(new DefaultTransactionAttribute())).isSameAs(transactionManager);
	}

	@Test
	public void determineTransactionManagerWithNoBeanFactoryAndNoTransactionAttribute() {
		PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
		TransactionInterceptor ti = transactionInterceptorWithTransactionManager(transactionManager, null);

		assertThat(ti.determineTransactionManager(null)).isSameAs(transactionManager);
	}

	@Test
	public void determineTransactionManagerWithNoTransactionAttribute() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);

		assertThat(ti.determineTransactionManager(null)).isNull();
	}

	@Test
	public void determineTransactionManagerWithQualifierUnknown() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);
		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		attribute.setQualifier("fooTransactionManager");

		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				ti.determineTransactionManager(attribute))
			.withMessageContaining("'fooTransactionManager'");
	}

	@Test
	public void determineTransactionManagerWithQualifierAndDefault() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
		TransactionInterceptor ti = transactionInterceptorWithTransactionManager(transactionManager, beanFactory);
		PlatformTransactionManager fooTransactionManager =
				associateTransactionManager(beanFactory, "fooTransactionManager");

		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		attribute.setQualifier("fooTransactionManager");

		assertThat(ti.determineTransactionManager(attribute)).isSameAs(fooTransactionManager);
	}

	@Test
	public void determineTransactionManagerWithQualifierAndDefaultName() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		associateTransactionManager(beanFactory, "defaultTransactionManager");
		TransactionInterceptor ti = transactionInterceptorWithTransactionManagerName(
				"defaultTransactionManager", beanFactory);

		PlatformTransactionManager fooTransactionManager =
				associateTransactionManager(beanFactory, "fooTransactionManager");
		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		attribute.setQualifier("fooTransactionManager");

		assertThat(ti.determineTransactionManager(attribute)).isSameAs(fooTransactionManager);
	}

	@Test
	public void determineTransactionManagerWithEmptyQualifierAndDefaultName() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		PlatformTransactionManager defaultTransactionManager
				= associateTransactionManager(beanFactory, "defaultTransactionManager");
		TransactionInterceptor ti = transactionInterceptorWithTransactionManagerName(
				"defaultTransactionManager", beanFactory);

		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		attribute.setQualifier("");

		assertThat(ti.determineTransactionManager(attribute)).isSameAs(defaultTransactionManager);
	}

	@Test
	public void determineTransactionManagerWithQualifierSeveralTimes() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);

		PlatformTransactionManager txManager = associateTransactionManager(beanFactory, "fooTransactionManager");

		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		attribute.setQualifier("fooTransactionManager");
		TransactionManager actual = ti.determineTransactionManager(attribute);
		assertThat(actual).isSameAs(txManager);

		// Call again, should be cached
		TransactionManager actual2 = ti.determineTransactionManager(attribute);
		assertThat(actual2).isSameAs(txManager);
		verify(beanFactory, times(1)).containsBean("fooTransactionManager");
		verify(beanFactory, times(1)).getBean("fooTransactionManager", TransactionManager.class);
	}

	@Test
	public void determineTransactionManagerWithBeanNameSeveralTimes() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		TransactionInterceptor ti = transactionInterceptorWithTransactionManagerName(
				"fooTransactionManager", beanFactory);

		PlatformTransactionManager txManager = 	associateTransactionManager(beanFactory, "fooTransactionManager");

		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		TransactionManager actual = ti.determineTransactionManager(attribute);
		assertThat(actual).isSameAs(txManager);

		// Call again, should be cached
		TransactionManager actual2 = ti.determineTransactionManager(attribute);
		assertThat(actual2).isSameAs(txManager);
		verify(beanFactory, times(1)).getBean("fooTransactionManager", TransactionManager.class);
	}

	@Test
	public void determineTransactionManagerDefaultSeveralTimes() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);

		PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
		given(beanFactory.getBean(TransactionManager.class)).willReturn(txManager);

		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		TransactionManager actual = ti.determineTransactionManager(attribute);
		assertThat(actual).isSameAs(txManager);

		// Call again, should be cached
		TransactionManager actual2 = ti.determineTransactionManager(attribute);
		assertThat(actual2).isSameAs(txManager);
		verify(beanFactory, times(1)).getBean(TransactionManager.class);
	}


	private TransactionInterceptor createTransactionInterceptor(BeanFactory beanFactory,
			String transactionManagerName, PlatformTransactionManager transactionManager) {

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
			PlatformTransactionManager transactionManager, BeanFactory beanFactory) {

		return createTransactionInterceptor(beanFactory, null, transactionManager);
	}

	private TransactionInterceptor transactionInterceptorWithTransactionManagerName(
			String transactionManagerName, BeanFactory beanFactory) {

		return createTransactionInterceptor(beanFactory, transactionManagerName, null);
	}

	private TransactionInterceptor simpleTransactionInterceptor(BeanFactory beanFactory) {
		return createTransactionInterceptor(beanFactory, null, null);
	}

	private PlatformTransactionManager associateTransactionManager(BeanFactory beanFactory, String name) {
		PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
		given(beanFactory.containsBean(name)).willReturn(true);
		given(beanFactory.getBean(name, TransactionManager.class)).willReturn(transactionManager);
		return transactionManager;
	}


	/**
	 * We won't use this: we just want to know it's serializable.
	 */
	@SuppressWarnings("serial")
	public static class SerializableTransactionManager implements PlatformTransactionManager, Serializable {

		@Override
		public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void commit(TransactionStatus status) throws TransactionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void rollback(TransactionStatus status) throws TransactionException {
			throw new UnsupportedOperationException();
		}
	}

}

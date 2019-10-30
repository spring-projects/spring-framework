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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.SerializationTestUtils;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Mock object based tests for TransactionInterceptor.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16.03.2003
 */
public class TransactionInterceptorTests extends AbstractTransactionAspectTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


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
		assertEquals(ptm, ti.getTransactionManager());
		ti.setTransactionAttributeSource(tas);
		assertEquals(tas, ti.getTransactionAttributeSource());

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
		assertNotNull(ti.logger);
		assertTrue(ti.getTransactionManager() instanceof SerializableTransactionManager);
		assertNotNull(ti.getTransactionAttributeSource());
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

		assertTrue(ti.getTransactionManager() instanceof SerializableTransactionManager);
		assertTrue(ti.getTransactionAttributeSource() instanceof CompositeTransactionAttributeSource);
		CompositeTransactionAttributeSource ctas = (CompositeTransactionAttributeSource) ti.getTransactionAttributeSource();
		assertTrue(ctas.getTransactionAttributeSources()[0] instanceof NameMatchTransactionAttributeSource);
		assertTrue(ctas.getTransactionAttributeSources()[1] instanceof NameMatchTransactionAttributeSource);
	}

	@Test
	public void determineTransactionManagerWithNoBeanFactory() {
		PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
		TransactionInterceptor ti = transactionInterceptorWithTransactionManager(transactionManager, null);

		assertSame(transactionManager, ti.determineTransactionManager(new DefaultTransactionAttribute()));
	}

	@Test
	public void determineTransactionManagerWithNoBeanFactoryAndNoTransactionAttribute() {
		PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
		TransactionInterceptor ti = transactionInterceptorWithTransactionManager(transactionManager, null);

		assertSame(transactionManager, ti.determineTransactionManager(null));
	}

	@Test
	public void determineTransactionManagerWithNoTransactionAttribute() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);

		assertNull(ti.determineTransactionManager(null));
	}

	@Test
	public void determineTransactionManagerWithQualifierUnknown() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);
		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		attribute.setQualifier("fooTransactionManager");

		thrown.expect(NoSuchBeanDefinitionException.class);
		thrown.expectMessage("'fooTransactionManager'");
		ti.determineTransactionManager(attribute);
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

		assertSame(fooTransactionManager, ti.determineTransactionManager(attribute));
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

		assertSame(fooTransactionManager, ti.determineTransactionManager(attribute));
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

		assertSame(defaultTransactionManager, ti.determineTransactionManager(attribute));
	}

	@Test
	public void determineTransactionManagerWithQualifierSeveralTimes() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);

		PlatformTransactionManager txManager = associateTransactionManager(beanFactory, "fooTransactionManager");

		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		attribute.setQualifier("fooTransactionManager");
		PlatformTransactionManager actual = ti.determineTransactionManager(attribute);
		assertSame(txManager, actual);

		// Call again, should be cached
		PlatformTransactionManager actual2 = ti.determineTransactionManager(attribute);
		assertSame(txManager, actual2);
		verify(beanFactory, times(1)).containsBean("fooTransactionManager");
		verify(beanFactory, times(1)).getBean("fooTransactionManager", PlatformTransactionManager.class);
	}

	@Test
	public void determineTransactionManagerWithBeanNameSeveralTimes() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		TransactionInterceptor ti = transactionInterceptorWithTransactionManagerName(
				"fooTransactionManager", beanFactory);

		PlatformTransactionManager txManager = 	associateTransactionManager(beanFactory, "fooTransactionManager");

		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		PlatformTransactionManager actual = ti.determineTransactionManager(attribute);
		assertSame(txManager, actual);

		// Call again, should be cached
		PlatformTransactionManager actual2 = ti.determineTransactionManager(attribute);
		assertSame(txManager, actual2);
		verify(beanFactory, times(1)).getBean("fooTransactionManager", PlatformTransactionManager.class);
	}

	@Test
	public void determineTransactionManagerDefaultSeveralTimes() {
		BeanFactory beanFactory = mock(BeanFactory.class);
		TransactionInterceptor ti = simpleTransactionInterceptor(beanFactory);

		PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
		given(beanFactory.getBean(PlatformTransactionManager.class)).willReturn(txManager);

		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		PlatformTransactionManager actual = ti.determineTransactionManager(attribute);
		assertSame(txManager, actual);

		// Call again, should be cached
		PlatformTransactionManager actual2 = ti.determineTransactionManager(attribute);
		assertSame(txManager, actual2);
		verify(beanFactory, times(1)).getBean(PlatformTransactionManager.class);
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
		given(beanFactory.getBean(name, PlatformTransactionManager.class)).willReturn(transactionManager);
		return transactionManager;
	}


	/**
	 * We won't use this: we just want to know it's serializable.
	 */
	@SuppressWarnings("serial")
	public static class SerializableTransactionManager implements PlatformTransactionManager, Serializable {

		@Override
		public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
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

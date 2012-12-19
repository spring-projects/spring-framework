/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.transaction.interceptor;

import java.io.Serializable;
import java.util.Properties;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.SerializationTestUtils;

/**
 * Mock object based tests for TransactionInterceptor.
 *
 * @author Rod Johnson
 * @since 16.03.2003
 */
public class TransactionInterceptorTests extends AbstractTransactionAspectTests {

	protected Object advised(Object target, PlatformTransactionManager ptm, TransactionAttributeSource[] tas) throws Exception {
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
	public void testSerializableWithAttributeProperties() throws Exception {
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

	public void testSerializableWithCompositeSource() throws Exception {
		NameMatchTransactionAttributeSource tas1 = new NameMatchTransactionAttributeSource();
		Properties props = new Properties();
		props.setProperty("methodName", "PROPAGATION_REQUIRED");
		tas1.setProperties(props);

		NameMatchTransactionAttributeSource tas2 = new NameMatchTransactionAttributeSource();
		props = new Properties();
		props.setProperty("otherMethodName", "PROPAGATION_REQUIRES_NEW");
		tas2.setProperties(props);

		TransactionInterceptor ti = new TransactionInterceptor();
		ti.setTransactionAttributeSources(new TransactionAttributeSource[] {tas1, tas2});
		PlatformTransactionManager ptm = new SerializableTransactionManager();
		ti.setTransactionManager(ptm);
		ti = (TransactionInterceptor) SerializationTestUtils.serializeAndDeserialize(ti);

		assertTrue(ti.getTransactionManager() instanceof SerializableTransactionManager);
		assertTrue(ti.getTransactionAttributeSource() instanceof CompositeTransactionAttributeSource);
		CompositeTransactionAttributeSource ctas = (CompositeTransactionAttributeSource) ti.getTransactionAttributeSource();
		assertTrue(ctas.getTransactionAttributeSources()[0] instanceof NameMatchTransactionAttributeSource);
		assertTrue(ctas.getTransactionAttributeSources()[1] instanceof NameMatchTransactionAttributeSource);
	}


	/**
	 * We won't use this: we just want to know it's serializable.
	 */
	@SuppressWarnings("serial")
	public static class SerializableTransactionManager implements PlatformTransactionManager, Serializable {

		public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
			throw new UnsupportedOperationException();
		}

		public void commit(TransactionStatus status) throws TransactionException {
			throw new UnsupportedOperationException();
		}

		public void rollback(TransactionStatus status) throws TransactionException {
			throw new UnsupportedOperationException();
		}

	}
}

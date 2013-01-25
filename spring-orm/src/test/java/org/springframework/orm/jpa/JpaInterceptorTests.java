/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.orm.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.Invocation;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Costin Leau
 * @author Phillip Webb
 */
public class JpaInterceptorTests {

	private EntityManagerFactory factory;

	private EntityManager entityManager;


	@Before
	public void setUp() throws Exception {
		factory = mock(EntityManagerFactory.class);
		entityManager = mock(EntityManager.class);
	}

	@After
	public void tearDown() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void testInterceptorWithNewEntityManager() throws PersistenceException {
		given(factory.createEntityManager()).willReturn(entityManager);
		given(entityManager.isOpen()).willReturn(true);

		JpaInterceptor interceptor = new JpaInterceptor();
		interceptor.setEntityManagerFactory(factory);
		try {
			interceptor.invoke(new TestInvocation(factory));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		verify(entityManager).close();
	}

	@Test
	public void testInterceptorWithNewEntityManagerAndLazyFlush() throws PersistenceException {
		given(factory.createEntityManager()).willReturn(entityManager);
		given(entityManager.isOpen()).willReturn(true);

		JpaInterceptor interceptor = new JpaInterceptor();
		interceptor.setFlushEager(false);
		interceptor.setEntityManagerFactory(factory);
		try {
			interceptor.invoke(new TestInvocation(factory));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		verify(entityManager).close();
	}

	@Test
	public void testInterceptorWithThreadBound() {
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(entityManager));
		JpaInterceptor interceptor = new JpaInterceptor();
		interceptor.setEntityManagerFactory(factory);
		try {
			interceptor.invoke(new TestInvocation(factory));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}
	}

	@Test
	public void testInterceptorWithThreadBoundAndFlushEager() throws PersistenceException {
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(entityManager));
		JpaInterceptor interceptor = new JpaInterceptor();
		interceptor.setFlushEager(true);
		interceptor.setEntityManagerFactory(factory);
		try {
			interceptor.invoke(new TestInvocation(factory));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		verify(entityManager).flush();
	}

	@Test
	public void testInterceptorWithThreadBoundAndFlushCommit() {
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(entityManager));
		JpaInterceptor interceptor = new JpaInterceptor();
		interceptor.setFlushEager(false);
		interceptor.setEntityManagerFactory(factory);
		try {
			interceptor.invoke(new TestInvocation(factory));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}
	}

	@Test
	public void testInterceptorWithFlushFailure() throws Throwable {
		given(factory.createEntityManager()).willReturn(entityManager);

		PersistenceException exception = new PersistenceException();
		willThrow(exception).given(entityManager).flush();
		given(entityManager.isOpen()).willReturn(true);

		JpaInterceptor interceptor = new JpaInterceptor();
		interceptor.setFlushEager(true);
		interceptor.setEntityManagerFactory(factory);
		try {
			interceptor.invoke(new TestInvocation(factory));
			//fail("Should have thrown JpaSystemException");
		}
		catch (JpaSystemException ex) {
			// expected
			assertEquals(exception, ex.getCause());
		}

		verify(entityManager).close();
	}

	@Test
	public void testInterceptorWithFlushFailureWithoutConversion() throws Throwable {
		given(factory.createEntityManager()).willReturn(entityManager);

		PersistenceException exception = new PersistenceException();
		willThrow(exception).given(entityManager).flush();
		given(entityManager.isOpen()).willReturn(true);

		JpaInterceptor interceptor = new JpaInterceptor();
		interceptor.setFlushEager(true);
		interceptor.setExceptionConversionEnabled(false);
		interceptor.setEntityManagerFactory(factory);
		try {
			interceptor.invoke(new TestInvocation(factory));
			//fail("Should have thrown JpaSystemException");
		}
		catch (PersistenceException ex) {
			// expected
			assertEquals(exception, ex);
		}

		verify(entityManager).close();
	}


	@SuppressWarnings("unused")
	private static class TestInvocation implements MethodInvocation {

		private EntityManagerFactory entityManagerFactory;

		public TestInvocation(EntityManagerFactory entityManagerFactory) {
			this.entityManagerFactory = entityManagerFactory;
		}

		@Override
		public Object proceed() throws Throwable {
			if (!TransactionSynchronizationManager.hasResource(this.entityManagerFactory)) {
				throw new IllegalStateException("Session not bound");
			}
			return null;
		}

		public int getCurrentInterceptorIndex() {
			return 0;
		}

		public int getNumberOfInterceptors() {
			return 0;
		}

		public Interceptor getInterceptor(int i) {
			return null;
		}

		@Override
		public Method getMethod() {
			return null;
		}

		@Override
		public AccessibleObject getStaticPart() {
			return null;
		}

		public Object getArgument(int i) {
			return null;
		}

		@Override
		public Object[] getArguments() {
			return null;
		}

		public void setArgument(int i, Object handler) {
		}

		public int getArgumentCount() {
			return 0;
		}

		@Override
		public Object getThis() {
			return null;
		}

		public Object getProxy() {
			return null;
		}

		public Invocation cloneInstance() {
			return null;
		}

		public void release() {
		}
	}

}

/*
 * Copyright 2002-2010 the original author or authors.
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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import junit.framework.TestCase;
import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.Invocation;
import org.aopalliance.intercept.MethodInvocation;
import org.easymock.MockControl;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Costin Leau
 */
public class JpaInterceptorTests extends TestCase {

	private MockControl factoryControl, managerControl;

	private EntityManagerFactory factory;

	private EntityManager entityManager;


	@Override
	protected void setUp() throws Exception {
		factoryControl = MockControl.createControl(EntityManagerFactory.class);
		factory = (EntityManagerFactory) factoryControl.getMock();
		managerControl = MockControl.createControl(EntityManager.class);
		entityManager = (EntityManager) managerControl.getMock();
	}

	@Override
	protected void tearDown() throws Exception {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		factoryControl = null;
		factory = null;
		managerControl = null;
		entityManager = null;

	}

	public void testInterceptorWithNewEntityManager() throws PersistenceException {
		factoryControl.expectAndReturn(factory.createEntityManager(), entityManager);
		managerControl.expectAndReturn(entityManager.isOpen(), true);
		entityManager.close();

		factoryControl.replay();
		managerControl.replay();

		JpaInterceptor interceptor = new JpaInterceptor();
		interceptor.setEntityManagerFactory(factory);
		try {
			interceptor.invoke(new TestInvocation(factory));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		factoryControl.verify();
		managerControl.verify();
	}

	public void testInterceptorWithNewEntityManagerAndLazyFlush() throws PersistenceException {
		factoryControl.expectAndReturn(factory.createEntityManager(), entityManager);
		managerControl.expectAndReturn(entityManager.isOpen(), true);
		entityManager.close();

		factoryControl.replay();
		managerControl.replay();

		JpaInterceptor interceptor = new JpaInterceptor();
		interceptor.setFlushEager(false);
		interceptor.setEntityManagerFactory(factory);
		try {
			interceptor.invoke(new TestInvocation(factory));
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		factoryControl.verify();
		managerControl.verify();
	}

	public void testInterceptorWithThreadBound() {
		factoryControl.replay();
		managerControl.replay();

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

		factoryControl.verify();
		managerControl.verify();
	}

	public void testInterceptorWithThreadBoundAndFlushEager() throws PersistenceException {
		//entityManager.setFlushMode(FlushModeType.AUTO);
		entityManager.flush();

		factoryControl.replay();
		managerControl.replay();

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

		factoryControl.verify();
		managerControl.verify();
	}

	public void testInterceptorWithThreadBoundAndFlushCommit() {
		//entityManager.setFlushMode(FlushModeType.COMMIT);
		//entityManager.flush();

		factoryControl.replay();
		managerControl.replay();

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

		factoryControl.verify();
		managerControl.verify();
	}

	public void testInterceptorWithFlushFailure() throws Throwable {
		factoryControl.expectAndReturn(factory.createEntityManager(), entityManager);
		entityManager.flush();

		PersistenceException exception = new PersistenceException();
		managerControl.setThrowable(exception, 1);
		managerControl.expectAndReturn(entityManager.isOpen(), true);
		entityManager.close();

		factoryControl.replay();
		managerControl.replay();

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

		factoryControl.verify();
		managerControl.verify();
	}

	public void testInterceptorWithFlushFailureWithoutConversion() throws Throwable {
		factoryControl.expectAndReturn(factory.createEntityManager(), entityManager);
		entityManager.flush();

		PersistenceException exception = new PersistenceException();
		managerControl.setThrowable(exception, 1);
		managerControl.expectAndReturn(entityManager.isOpen(), true);
		entityManager.close();

		factoryControl.replay();
		managerControl.replay();

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

		factoryControl.verify();
		managerControl.verify();
	}


	private static class TestInvocation implements MethodInvocation {

		private EntityManagerFactory entityManagerFactory;

		public TestInvocation(EntityManagerFactory entityManagerFactory) {
			this.entityManagerFactory = entityManagerFactory;
		}

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

		public Method getMethod() {
			return null;
		}

		public AccessibleObject getStaticPart() {
			return null;
		}

		public Object getArgument(int i) {
			return null;
		}

		public Object[] getArguments() {
			return null;
		}

		public void setArgument(int i, Object handler) {
		}

		public int getArgumentCount() {
			return 0;
		}

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

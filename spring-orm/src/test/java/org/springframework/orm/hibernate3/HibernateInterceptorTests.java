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

package org.springframework.orm.hibernate3;

import java.sql.SQLException;

import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 05.03.2005
 */
public class HibernateInterceptorTests {

	private SessionFactory sessionFactory;
	private Session session;
	private MethodInvocation invocation;

	@Before
	public void setUp() throws Throwable {
		this.sessionFactory = mock(SessionFactory.class);
		this.session = mock(Session.class);
		this.invocation = mock(MethodInvocation.class);
		given(sessionFactory.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sessionFactory);
		given(invocation.proceed()).willAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				if (!TransactionSynchronizationManager.hasResource(sessionFactory)) {
					throw new IllegalStateException("Session not bound");
				}
				return null;
			}
		});
	}

	@After
	public void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void testInterceptorWithNewSession() throws HibernateException {
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sessionFactory);
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testInterceptorWithNewSessionAndFlushNever() throws HibernateException {
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setFlushModeName("FLUSH_NEVER");
		interceptor.setSessionFactory(sessionFactory);
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session, never()).flush();
		verify(session).close();
	}

	@Test
	public void testInterceptorWithNewSessionAndFilter() throws HibernateException {
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sessionFactory);
		interceptor.setFilterName("myFilter");
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testInterceptorWithThreadBound() {
		given(session.isOpen()).willReturn(true);

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sessionFactory);
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			verify(session, never()).flush();
			verify(session, never()).close();
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}
	}

	@Test
	public void testInterceptorWithThreadBoundAndFlushEager() throws HibernateException {
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setFlushMode(HibernateInterceptor.FLUSH_EAGER);
		interceptor.setSessionFactory(sessionFactory);
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}

		verify(session).flush();
	}

	@Test
	public void testInterceptorWithThreadBoundAndFlushEagerSwitch() throws HibernateException {
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.NEVER);

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setFlushMode(HibernateInterceptor.FLUSH_EAGER);
		interceptor.setSessionFactory(sessionFactory);
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}

		InOrder ordered = inOrder(session);
		ordered.verify(session).setFlushMode(FlushMode.AUTO);
		ordered.verify(session).flush();
		ordered.verify(session).setFlushMode(FlushMode.NEVER);
	}

	@Test
	public void testInterceptorWithThreadBoundAndFlushCommit() {
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sessionFactory);
		interceptor.setFlushMode(HibernateInterceptor.FLUSH_COMMIT);
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}

		InOrder ordered = inOrder(session);
		ordered.verify(session).setFlushMode(FlushMode.COMMIT);
		ordered.verify(session).setFlushMode(FlushMode.AUTO);
		verify(session, never()).flush();
	}

	@Test
	public void testInterceptorWithThreadBoundAndFlushAlways() {
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sessionFactory);
		interceptor.setFlushMode(HibernateInterceptor.FLUSH_ALWAYS);
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}

		InOrder ordered = inOrder(session);
		ordered.verify(session).setFlushMode(FlushMode.ALWAYS);
		ordered.verify(session).setFlushMode(FlushMode.AUTO);
		verify(session, never()).flush();
	}

	@Test
	public void testInterceptorWithThreadBoundAndFilter() {
		given(session.isOpen()).willReturn(true);

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sessionFactory);
		interceptor.setFilterName("myFilter");
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}

		InOrder ordered = inOrder(session);
		ordered.verify(session).enableFilter("myFilter");
		ordered.verify(session).disableFilter("myFilter");
	}

	@Test
	public void testInterceptorWithThreadBoundAndFilters() {
		given(session.isOpen()).willReturn(true);

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sessionFactory);
		interceptor.setFilterNames(new String[] {"myFilter", "yourFilter"});
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}

		InOrder ordered = inOrder(session);
		ordered.verify(session).enableFilter("myFilter");
		ordered.verify(session).enableFilter("yourFilter");
		ordered.verify(session).disableFilter("myFilter");
		ordered.verify(session).disableFilter("yourFilter");
	}

	@Test
	public void testInterceptorWithFlushFailure() throws Throwable {
		SQLException sqlEx = new SQLException("argh", "27");
		ConstraintViolationException jdbcEx = new ConstraintViolationException("", sqlEx, null);
		willThrow(jdbcEx).given(session).flush();

		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sessionFactory);
		try {
			interceptor.invoke(invocation);
			fail("Should have thrown DataIntegrityViolationException");
		}
		catch (DataIntegrityViolationException ex) {
			// expected
			assertEquals(jdbcEx, ex.getCause());
		}

		verify(session).close();
	}

	@Test
	public void testInterceptorWithThreadBoundEmptyHolder() {
		SessionHolder holder = new SessionHolder("key", session);
		holder.removeSession("key");
		TransactionSynchronizationManager.bindResource(sessionFactory, holder);
		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sessionFactory);
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testInterceptorWithEntityInterceptor() throws HibernateException {
		org.hibernate.Interceptor entityInterceptor = mock(org.hibernate.Interceptor.class);
		given(sessionFactory.openSession(entityInterceptor)).willReturn(session);

		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sessionFactory);
		interceptor.setEntityInterceptor(entityInterceptor);
		try {
			interceptor.invoke(invocation);
		}
		catch (Throwable t) {
			fail("Should not have thrown Throwable: " + t.getMessage());
		}

		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testInterceptorWithEntityInterceptorBeanName() throws HibernateException {
		org.hibernate.Interceptor entityInterceptor = mock(org.hibernate.Interceptor.class);
		org.hibernate.Interceptor entityInterceptor2 = mock(org.hibernate.Interceptor.class);

		given(sessionFactory.openSession(entityInterceptor)).willReturn(session);
		given(sessionFactory.openSession(entityInterceptor2)).willReturn(session);

		BeanFactory beanFactory = mock(BeanFactory.class);
		given(beanFactory.getBean("entityInterceptor", org.hibernate.Interceptor.class)).willReturn(
				entityInterceptor, entityInterceptor2);

		HibernateInterceptor interceptor = new HibernateInterceptor();
		interceptor.setSessionFactory(sessionFactory);
		interceptor.setEntityInterceptorBeanName("entityInterceptor");
		interceptor.setBeanFactory(beanFactory);
		for (int i = 0; i < 2; i++) {
			try {
				interceptor.invoke(invocation);
			}
			catch (Throwable t) {
				fail("Should not have thrown Throwable: " + t.getMessage());
			}
		}

		verify(session, times(2)).flush();
		verify(session, times(2)).close();
	}
}

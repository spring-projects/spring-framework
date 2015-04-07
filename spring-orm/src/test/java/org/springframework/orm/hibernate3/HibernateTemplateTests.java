/*
 * Copyright 2002-2014 the original author or authors.
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

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.PersistentObjectException;
import org.hibernate.PropertyValueException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.ReplicationMode;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.classic.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 05.03.2005
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class HibernateTemplateTests {

	private SessionFactory sessionFactory;
	private Session session;
	private HibernateTemplate hibernateTemplate;

	@Before
	public void setUp() {
		this.sessionFactory = mock(SessionFactory.class);
		this.session = mock(Session.class);
		this.hibernateTemplate = new HibernateTemplate(sessionFactory);
		given(sessionFactory.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sessionFactory);
	}

	@After
	public void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void testExecuteWithNewSession() throws HibernateException {
		assertTrue("Correct allowCreate default", hibernateTemplate.isAllowCreate());
		assertTrue("Correct flushMode default", hibernateTemplate.getFlushMode() == HibernateTemplate.FLUSH_AUTO);
		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testExecuteWithNewSessionAndFlushNever() throws HibernateException {
		hibernateTemplate.setFlushMode(HibernateTemplate.FLUSH_NEVER);
		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session).close();
	}

	@Test
	public void testExecuteWithNewSessionAndFilter() throws HibernateException {
		hibernateTemplate.setFilterName("myFilter");
		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
		verify(session).enableFilter("myFilter");
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testExecuteWithNewSessionAndFilters() throws HibernateException {
		hibernateTemplate.setFilterNames(new String[] {"myFilter", "yourFilter"});
		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
		InOrder ordered = inOrder(session);
		ordered.verify(session).enableFilter("myFilter");
		ordered.verify(session).enableFilter("yourFilter");
		ordered.verify(session).flush();
		ordered.verify(session).close();
	}

	@Test
	public void testExecuteWithNotAllowCreate() {
		reset(sessionFactory);
		given(sessionFactory.getCurrentSession()).willThrow(new HibernateException(""));
		HibernateTemplate ht = new HibernateTemplate();
		ht.setSessionFactory(sessionFactory);
		ht.setAllowCreate(false);
		try {
			ht.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					return null;
				}
			});
			fail("Should have thrown DataAccessException");
		}
		catch (DataAccessResourceFailureException ex) {
			// expected
		}
	}

	@Test
	public void testExecuteWithNotAllowCreateAndThreadBound() {
		given(sessionFactory.getCurrentSession()).willReturn(session);
		hibernateTemplate.setAllowCreate(false);

		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
	}

	@Test
	public void testExecuteWithThreadBoundAndFlushEager() throws HibernateException {
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.setFlushModeName("FLUSH_EAGER");
		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		try {
			final List l = new ArrayList();
			l.add("test");
			List result = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					return l;
				}
			});
			assertTrue("Correct result list", result == l);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}
		verify(session).flush();
	}

	@Test
	public void testExecuteWithThreadBoundAndFilter() {
		given(session.isOpen()).willReturn(true);
		hibernateTemplate.setFilterName("myFilter");

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		try {
			final List l = new ArrayList();
			l.add("test");
			List result = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					return l;
				}
			});
			assertTrue("Correct result list", result == l);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}
		InOrder ordered = inOrder(session);
		ordered.verify(session).enableFilter("myFilter");
		ordered.verify(session).disableFilter("myFilter");
	}

	@Test
	public void testExecuteWithThreadBoundAndFilters() {
		given(session.isOpen()).willReturn(true);
		hibernateTemplate.setFilterNames(new String[] {"myFilter", "yourFilter"});

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		try {
			final List l = new ArrayList();
			l.add("test");
			List result = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					return l;
				}
			});
			assertTrue("Correct result list", result == l);
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
	public void testExecuteWithThreadBoundAndParameterizedFilter() {
		Filter filter = mock(Filter.class);
		given(session.isOpen()).willReturn(true);
		given(session.enableFilter("myFilter")).willReturn(filter);
		hibernateTemplate.setAllowCreate(false);
		hibernateTemplate.setFilterName("myFilter");

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		try {
			final List l = new ArrayList();
			l.add("test");
			Filter f = hibernateTemplate.enableFilter("myFilter");
			assertTrue("Correct filter", f == filter);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}
		InOrder ordered = inOrder(session);
		ordered.verify(session).getEnabledFilter("myFilter");
		ordered.verify(session).enableFilter("myFilter");
	}

	@Test
	public void testExecuteWithThreadBoundAndParameterizedExistingFilter() {
		Filter filter = mock(Filter.class);
		given(session.isOpen()).willReturn(true);
		given(session.enableFilter("myFilter")).willReturn(filter);
		hibernateTemplate.setAllowCreate(false);
		hibernateTemplate.setFilterName("myFilter");

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		try {
			final List l = new ArrayList();
			l.add("test");
			Filter f = hibernateTemplate.enableFilter("myFilter");
			assertTrue("Correct filter", f == filter);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}
		verify(session).getEnabledFilter("myFilter");
	}

	@Test
	public void testExecuteWithThreadBoundAndNewSession() throws HibernateException {
		Connection con = mock(Connection.class);
		Session session2 = mock(Session.class);
		given(session2.connection()).willReturn(con);
		given(sessionFactory.openSession(con)).willReturn(session);
		hibernateTemplate.setAlwaysUseNewSession(true);

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session2));
		try {
			final List l = new ArrayList();
			l.add("test");
			List result = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					return l;
				}
			});
			assertTrue("Correct result list", result == l);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testExecuteWithThreadBoundAndNewSessionAndEntityInterceptor() throws HibernateException {
		Interceptor entityInterceptor = mock(Interceptor.class);
		Connection con = mock(Connection.class);
		Session session2 = mock(Session.class);
		given(session2.connection()).willReturn(con);
		given(sessionFactory.openSession(con, entityInterceptor)).willReturn(session);
		hibernateTemplate.setAlwaysUseNewSession(true);
		hibernateTemplate.setEntityInterceptor(entityInterceptor);

		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session2));
		try {
			final List l = new ArrayList();
			l.add("test");
			List result = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					return l;
				}
			});
			assertTrue("Correct result list", result == l);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		}
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testExecuteWithEntityInterceptor() throws HibernateException {
		Interceptor entityInterceptor = mock(Interceptor.class);
		given(sessionFactory.openSession(entityInterceptor)).willReturn(session);
		hibernateTemplate.setEntityInterceptor(entityInterceptor);
		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.executeFind(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testExecuteWithCacheQueries() throws HibernateException {
		Query query1 = mock(Query.class);
		Query query2 = mock(Query.class);
		Criteria criteria = mock(Criteria.class);
		given(session.createQuery("some query")).willReturn(query1);
		given(query1.setCacheable(true)).willReturn(query1);
		given(session.getNamedQuery("some query name")).willReturn(query2);
		given(query2.setCacheable(true)).willReturn(query2);
		given(session.createCriteria(TestBean.class)).willReturn(criteria);
		given(criteria.setCacheable(true)).willReturn(criteria);

		hibernateTemplate.setCacheQueries(true);
		hibernateTemplate.execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(org.hibernate.Session sess) throws HibernateException {
				assertNotSame(session, sess);
				assertTrue(Proxy.isProxyClass(sess.getClass()));
				sess.createQuery("some query");
				sess.getNamedQuery("some query name");
				sess.createCriteria(TestBean.class);
				// should be ignored
				sess.close();
				return null;
			}
		});

		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testExecuteWithCacheQueriesAndCacheRegion() throws HibernateException {
		Query query1 = mock(Query.class);
		Query query2 = mock(Query.class);
		Criteria criteria = mock(Criteria.class);
		given(session.createQuery("some query")).willReturn(query1);
		given(query1.setCacheable(true)).willReturn(query1);
		given(query1.setCacheRegion("myRegion")).willReturn(query1);
		given(session.getNamedQuery("some query name")).willReturn(query2);
		given(query2.setCacheable(true)).willReturn(query2);
		given(query2.setCacheRegion("myRegion")).willReturn(query2);
		given(session.createCriteria(TestBean.class)).willReturn(criteria);
		given(criteria.setCacheable(true)).willReturn(criteria);
		given(criteria.setCacheRegion("myRegion")).willReturn(criteria);

		hibernateTemplate.setCacheQueries(true);
		hibernateTemplate.setQueryCacheRegion("myRegion");
		hibernateTemplate.execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(org.hibernate.Session sess) throws HibernateException {
				assertNotSame(session, sess);
				assertTrue(Proxy.isProxyClass(sess.getClass()));
				sess.createQuery("some query");
				sess.getNamedQuery("some query name");
				sess.createCriteria(TestBean.class);
				// should be ignored
				sess.close();
				return null;
			}
		});

		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testExecuteWithCacheQueriesAndCacheRegionAndNativeSession() throws HibernateException {
		Query query1 = mock(Query.class);
		Query query2 = mock(Query.class);
		Criteria criteria = mock(Criteria.class);

		given(session.createQuery("some query")).willReturn(query1);
		given(session.getNamedQuery("some query name")).willReturn(query2);
		given(session.createCriteria(TestBean.class)).willReturn(criteria);

		hibernateTemplate.setExposeNativeSession(true);
		hibernateTemplate.setCacheQueries(true);
		hibernateTemplate.setQueryCacheRegion("myRegion");
		hibernateTemplate.execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(org.hibernate.Session sess) throws HibernateException {
				assertSame(session, sess);
				sess.createQuery("some query");
				sess.getNamedQuery("some query name");
				sess.createCriteria(TestBean.class);
				return null;
			}
		});

		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testExecuteWithFetchSizeAndMaxResults() throws HibernateException {
		Query query1 = mock(Query.class);
		Query query2 = mock(Query.class);
		Criteria criteria = mock(Criteria.class);

		given(session.createQuery("some query")).willReturn(query1);
		given(query1.setFetchSize(10)).willReturn(query1);
		given(query1.setMaxResults(20)).willReturn(query1);
		given(session.getNamedQuery("some query name")).willReturn(query2);
		given(query2.setFetchSize(10)).willReturn(query2);
		given(query2.setMaxResults(20)).willReturn(query2);
		given(session.createCriteria(TestBean.class)).willReturn(criteria);
		given(criteria.setFetchSize(10)).willReturn(criteria);
		given(criteria.setMaxResults(20)).willReturn(criteria);

		hibernateTemplate.setFetchSize(10);
		hibernateTemplate.setMaxResults(20);
		hibernateTemplate.execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(org.hibernate.Session sess) throws HibernateException {
				sess.createQuery("some query");
				sess.getNamedQuery("some query name");
				sess.createCriteria(TestBean.class);
				return null;
			}
		});

		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testGet() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.get(TestBean.class, "")).willReturn(tb);
		Object result = hibernateTemplate.get(TestBean.class, "");
		assertTrue("Correct result", result == tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testGetWithLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.get(TestBean.class, "", LockMode.UPGRADE_NOWAIT)).willReturn(tb);
		Object result = hibernateTemplate.get(TestBean.class, "", LockMode.UPGRADE_NOWAIT);
		assertTrue("Correct result", result == tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testGetWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.get("myEntity", "")).willReturn(tb);
		Object result = hibernateTemplate.get("myEntity", "");
		assertTrue("Correct result", result == tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testGetWithEntityNameAndLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.get("myEntity", "", LockMode.UPGRADE_NOWAIT)).willReturn(tb);
		Object result = hibernateTemplate.get("myEntity", "", LockMode.UPGRADE_NOWAIT);
		assertTrue("Correct result", result == tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testLoad() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.load(TestBean.class, "")).willReturn(tb);
		Object result = hibernateTemplate.load(TestBean.class, "");
		assertTrue("Correct result", result == tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testLoadWithNotFound() throws HibernateException {
		ObjectNotFoundException onfex = new ObjectNotFoundException("id", TestBean.class.getName());
		given(session.load(TestBean.class, "id")).willThrow(onfex);
		try {
			hibernateTemplate.load(TestBean.class, "id");
			fail("Should have thrown HibernateObjectRetrievalFailureException");
		}
		catch (HibernateObjectRetrievalFailureException ex) {
			// expected
			assertEquals(TestBean.class.getName(), ex.getPersistentClassName());
			assertEquals("id", ex.getIdentifier());
			assertEquals(onfex, ex.getCause());
		}
		verify(session).close();
	}

	@Test
	public void testLoadWithLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.load(TestBean.class, "", LockMode.UPGRADE)).willReturn(tb);
		Object result = hibernateTemplate.load(TestBean.class, "", LockMode.UPGRADE);
		assertTrue("Correct result", result == tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testLoadWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.load("myEntity", "")).willReturn(tb);
		Object result = hibernateTemplate.load("myEntity", "");
		assertTrue("Correct result", result == tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testLoadWithEntityNameLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.load("myEntity", "", LockMode.UPGRADE)).willReturn(tb);
		Object result = hibernateTemplate.load("myEntity", "", LockMode.UPGRADE);
		assertTrue("Correct result", result == tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testLoadWithObject() throws HibernateException {
		TestBean tb = new TestBean();
		hibernateTemplate.load(tb, "");
		verify(session).load(tb, "");
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testLoadAll() throws HibernateException {
		Criteria criteria = mock(Criteria.class);
		List list = new ArrayList();
		given(session.createCriteria(TestBean.class)).willReturn(criteria);
		given(criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)).willReturn(criteria);
		given(criteria.list()).willReturn(list);
		List result = hibernateTemplate.loadAll(TestBean.class);
		assertTrue("Correct result", result == list);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testLoadAllWithCacheable() throws HibernateException {
		Criteria criteria = mock(Criteria.class);
		List list = new ArrayList();
		given(session.createCriteria(TestBean.class)).willReturn(criteria);
		given(criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)).willReturn(criteria);
		given(criteria.setCacheable(true)).willReturn(criteria);
		given(criteria.list()).willReturn(list);

		hibernateTemplate.setCacheQueries(true);
		List result = hibernateTemplate.loadAll(TestBean.class);
		assertTrue("Correct result", result == list);
		verify(criteria).setCacheable(true);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testLoadAllWithCacheableAndCacheRegion() throws HibernateException {
		Criteria criteria = mock(Criteria.class);
		List list = new ArrayList();
		given(session.createCriteria(TestBean.class)).willReturn(criteria);
		given(criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)).willReturn(criteria);
		given(criteria.setCacheable(true)).willReturn(criteria);
		given(criteria.setCacheRegion("myCacheRegion")).willReturn(criteria);
		given(criteria.list()).willReturn(list);

		hibernateTemplate.setCacheQueries(true);
		hibernateTemplate.setQueryCacheRegion("myCacheRegion");
		List result = hibernateTemplate.loadAll(TestBean.class);
		assertTrue("Correct result", result == list);
		verify(criteria).setCacheable(true);
		verify(criteria).setCacheRegion("myCacheRegion");
		verify(session).flush();
		verify(session).close();
	}

	@Test public void testRefresh() throws HibernateException {
		TestBean tb = new TestBean();
		hibernateTemplate.refresh(tb);
		verify(session).refresh(tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test public void testContains() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.contains(tb)).willReturn(true);
		assertTrue(hibernateTemplate.contains(tb));
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testEvict() throws HibernateException {
		TestBean tb = new TestBean();
		hibernateTemplate.evict(tb);
		verify(session).evict(tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testLock() throws HibernateException {
		TestBean tb = new TestBean();
		hibernateTemplate.lock(tb, LockMode.WRITE);
		verify(session).lock(tb, LockMode.WRITE);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testLockWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		hibernateTemplate.lock("myEntity", tb, LockMode.WRITE);
		verify(session).lock("myEntity", tb, LockMode.WRITE);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testSave() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.save(tb)).willReturn(0);
		assertEquals("Correct return value", hibernateTemplate.save(tb), 0);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testSaveWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.save("myEntity", tb)).willReturn(0);
		assertEquals("Correct return value", hibernateTemplate.save("myEntity", tb), 0);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testUpdate() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.update(tb);
		verify(session).update(tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testUpdateWithLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.update(tb, LockMode.UPGRADE);
		verify(session).update(tb);
		verify(session).lock(tb, LockMode.UPGRADE);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testUpdateWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.update("myEntity", tb);
		verify(session).update("myEntity", tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testUpdateWithEntityNameAndLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.update("myEntity", tb, LockMode.UPGRADE);
		verify(session).update("myEntity", tb);
		verify(session).lock(tb, LockMode.UPGRADE);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testSaveOrUpdate() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.saveOrUpdate(tb);
		verify(session).saveOrUpdate(tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testSaveOrUpdateWithFlushModeNever() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.MANUAL);
		try {
			hibernateTemplate.saveOrUpdate(tb);
			fail("Should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// expected
		}
		verify(session).close();
	}

	@Test
	public void testSaveOrUpdateWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();

		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.saveOrUpdate("myEntity", tb);
		verify(session).saveOrUpdate("myEntity", tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testReplicate() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.replicate(tb, ReplicationMode.LATEST_VERSION);
		verify(session).replicate(tb, ReplicationMode.LATEST_VERSION);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testReplicateWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.replicate("myEntity", tb, ReplicationMode.LATEST_VERSION);
		verify(session).replicate("myEntity", tb, ReplicationMode.LATEST_VERSION);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testPersist() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.persist(tb);
		verify(session).persist(tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testPersistWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.persist("myEntity", tb);
		verify(session).persist("myEntity", tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testMerge() throws HibernateException {
		TestBean tb = new TestBean();
		TestBean tbMerged = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.merge(tb)).willReturn(tbMerged);
		assertSame(tbMerged, hibernateTemplate.merge(tb));
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testMergeWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		TestBean tbMerged = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.merge("myEntity", tb)).willReturn(tbMerged);
		assertSame(tbMerged, hibernateTemplate.merge("myEntity", tb));
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testDelete() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.delete(tb);
		verify(session).delete(tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testDeleteWithLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.delete(tb, LockMode.UPGRADE);
		verify(session).lock(tb, LockMode.UPGRADE);
		verify(session).delete(tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testDeleteWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.delete("myEntity", tb);
		verify(session).delete("myEntity", tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testDeleteWithEntityNameAndLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.delete("myEntity", tb, LockMode.UPGRADE);
		verify(session).lock("myEntity", tb, LockMode.UPGRADE);
		verify(session).delete("myEntity", tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testDeleteAll() throws HibernateException {
		TestBean tb1 = new TestBean();
		TestBean tb2 = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		List tbs = new ArrayList();
		tbs.add(tb1);
		tbs.add(tb2);
		hibernateTemplate.deleteAll(tbs);
		verify(session).delete(same(tb1));
		verify(session).delete(same(tb2));
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFlush() throws HibernateException {
		hibernateTemplate.setFlushMode(HibernateTemplate.FLUSH_NEVER);
		hibernateTemplate.flush();
		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testClear() throws HibernateException {
		hibernateTemplate.clear();
		verify(session).clear();
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFind() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.find("some query string");
		assertTrue("Correct list", result == list);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindWithParameter() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.find("some query string", "myvalue");
		assertTrue("Correct list", result == list);
		verify(query).setParameter(0, "myvalue");
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindWithParameters() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue1")).willReturn(query);
		given(query.setParameter(1, 2)).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.find("some query string", new Object[] {"myvalue1", 2});
		assertTrue("Correct list", result == list);
		verify(query).setParameter(0, "myvalue1");
		verify(query).setParameter(1, 2);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindWithNamedParameter() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter("myparam", "myvalue")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedParam("some query string", "myparam", "myvalue");
		assertTrue("Correct list", result == list);
		verify(query).setParameter("myparam", "myvalue");
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindWithNamedParameters() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter("myparam1", "myvalue1")).willReturn(query);
		given(query.setParameter("myparam2", 2)).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedParam("some query string",
				new String[] {"myparam1", "myparam2"},
				new Object[] {"myvalue1", 2});
		assertTrue("Correct list", result == list);
		verify(query).setParameter("myparam1", "myvalue1");
		verify(query).setParameter("myparam2", 2);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindByValueBean() throws HibernateException {
		Query query = mock(Query.class);
		TestBean tb = new TestBean();
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setProperties(tb)).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByValueBean("some query string", tb);
		assertTrue("Correct list", result == list);
		verify(query).setProperties(tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindByNamedQuery() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedQuery("some query name");
		assertTrue("Correct list", result == list);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindByNamedQueryWithParameter() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setParameter(0, "myvalue")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedQuery("some query name", "myvalue");
		assertTrue("Correct list", result == list);
		verify(query).setParameter(0, "myvalue");
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindByNamedQueryWithParameters() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setParameter(0, "myvalue1")).willReturn(query);
		given(query.setParameter(1, 2)).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedQuery("some query name", new Object[] {"myvalue1", 2});
		assertTrue("Correct list", result == list);
		verify(query).setParameter(0, "myvalue1");
		verify(query).setParameter(1, 2);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindByNamedQueryWithNamedParameter() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setParameter("myparam", "myvalue")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedQueryAndNamedParam("some query name", "myparam", "myvalue");
		assertTrue("Correct list", result == list);
		verify(query).setParameter("myparam", "myvalue");
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindByNamedQueryWithNamedParameters() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setParameter("myparam1", "myvalue1")).willReturn(query);
		given(query.setParameter("myparam2", 2)).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedQueryAndNamedParam("some query name",
				new String[] {"myparam1", "myparam2"},
				new Object[] {"myvalue1", 2});
		assertTrue("Correct list", result == list);
		verify(query).setParameter("myparam1", "myvalue1");
		verify(query).setParameter("myparam2", 2);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindByNamedQueryAndValueBean() throws HibernateException {
		Query query = mock(Query.class);
		TestBean tb = new TestBean();
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setProperties(tb)).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedQueryAndValueBean("some query name", tb);
		assertTrue("Correct list", result == list);
		verify(query).setProperties(tb);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindWithCacheable() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setCacheable(true)).willReturn(query);
		given(query.list()).willReturn(list);
		hibernateTemplate.setCacheQueries(true);
		List result = hibernateTemplate.find("some query string");
		assertTrue("Correct list", result == list);
		verify(query).setCacheable(true);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindWithCacheableAndCacheRegion() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setCacheable(true)).willReturn(query);
		given(query.setCacheRegion("myCacheRegion")).willReturn(query);
		given(query.list()).willReturn(list);
		hibernateTemplate.setCacheQueries(true);
		hibernateTemplate.setQueryCacheRegion("myCacheRegion");
		List result = hibernateTemplate.find("some query string");
		assertTrue("Correct list", result == list);
		verify(query).setCacheable(true);
		verify(query).setCacheRegion("myCacheRegion");
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindByNamedQueryWithCacheable() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setCacheable(true)).willReturn(query);
		given(query.list()).willReturn(list);
		hibernateTemplate.setCacheQueries(true);
		List result = hibernateTemplate.findByNamedQuery("some query name");
		assertTrue("Correct list", result == list);
		verify(query).setCacheable(true);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testFindByNamedQueryWithCacheableAndCacheRegion() throws HibernateException {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setCacheable(true)).willReturn(query);
		given(query.setCacheRegion("myCacheRegion")).willReturn(query);
		given(query.list()).willReturn(list);
		hibernateTemplate.setCacheQueries(true);
		hibernateTemplate.setQueryCacheRegion("myCacheRegion");
		List result = hibernateTemplate.findByNamedQuery("some query name");
		assertTrue("Correct list", result == list);
		verify(query).setCacheable(true);
		verify(query).setCacheRegion("myCacheRegion");
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testIterate() throws HibernateException {
		Query query = mock(Query.class);
		Iterator it = Collections.EMPTY_LIST.iterator();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.iterate()).willReturn(it);
		Iterator result = hibernateTemplate.iterate("some query string");
		assertTrue("Correct list", result == it);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testIterateWithParameter() throws HibernateException {
		Query query = mock(Query.class);
		Iterator it = Collections.EMPTY_LIST.iterator();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue")).willReturn(query);
		given(query.iterate()).willReturn(it);
		Iterator result = hibernateTemplate.iterate("some query string", "myvalue");
		assertTrue("Correct list", result == it);
		verify(query).setParameter(0, "myvalue");
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testIterateWithParameters() throws HibernateException {
		Query query = mock(Query.class);
		Iterator it = Collections.EMPTY_LIST.iterator();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue1")).willReturn(query);
		given(query.setParameter(1, 2)).willReturn(query);
		given(query.iterate()).willReturn(it);
		Iterator result = hibernateTemplate.iterate("some query string",
				new Object[] {"myvalue1", 2});
		assertTrue("Correct list", result == it);
		verify(query).setParameter(0, "myvalue1");
		verify(query).setParameter(1, 2);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testBulkUpdate() throws HibernateException {
		Query query = mock(Query.class);
		given(session.createQuery("some query string")).willReturn(query);
		given(query.executeUpdate()).willReturn(5);
		int result = hibernateTemplate.bulkUpdate("some query string");
		assertTrue("Correct list", result == 5);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testBulkUpdateWithParameter() throws HibernateException {
		Query query = mock(Query.class);
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue")).willReturn(query);
		given(query.executeUpdate()).willReturn(5);
		int result = hibernateTemplate.bulkUpdate("some query string", "myvalue");
		assertTrue("Correct list", result == 5);
		verify(query).setParameter(0, "myvalue");
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testBulkUpdateWithParameters() throws HibernateException {
		Query query = mock(Query.class);
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue1")).willReturn(query);
		given(query.setParameter(1, 2)).willReturn(query);
		given(query.executeUpdate()).willReturn(5);
		int result = hibernateTemplate.bulkUpdate("some query string",
				new Object[] {"myvalue1", 2});
		assertTrue("Correct list", result == 5);
		verify(query).setParameter(0, "myvalue1");
		verify(query).setParameter(1, 2);
		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testExceptions() throws HibernateException {
		SQLException sqlEx = new SQLException("argh", "27");

		final JDBCConnectionException jcex = new JDBCConnectionException("mymsg", sqlEx);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw jcex;
				}
			});
			fail("Should have thrown DataAccessResourceFailureException");
		}
		catch (DataAccessResourceFailureException ex) {
			// expected
			assertEquals(jcex, ex.getCause());
			assertTrue(ex.getMessage().indexOf("mymsg") != -1);
		}

		final SQLGrammarException sgex = new SQLGrammarException("mymsg", sqlEx);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw sgex;
				}
			});
			fail("Should have thrown InvalidDataAccessResourceUsageException");
		}
		catch (InvalidDataAccessResourceUsageException ex) {
			// expected
			assertEquals(sgex, ex.getCause());
			assertTrue(ex.getMessage().indexOf("mymsg") != -1);
		}

		final LockAcquisitionException laex = new LockAcquisitionException("mymsg", sqlEx);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw laex;
				}
			});
			fail("Should have thrown CannotAcquireLockException");
		}
		catch (CannotAcquireLockException ex) {
			// expected
			assertEquals(laex, ex.getCause());
			assertTrue(ex.getMessage().indexOf("mymsg") != -1);
		}

		final ConstraintViolationException cvex = new ConstraintViolationException("mymsg", sqlEx, "myconstraint");
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw cvex;
				}
			});
			fail("Should have thrown DataIntegrityViolationException");
		}
		catch (DataIntegrityViolationException ex) {
			// expected
			assertEquals(cvex, ex.getCause());
			assertTrue(ex.getMessage().indexOf("mymsg") != -1);
		}

		final DataException dex = new DataException("mymsg", sqlEx);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw dex;
				}
			});
			fail("Should have thrown DataIntegrityViolationException");
		}
		catch (DataIntegrityViolationException ex) {
			// expected
			assertEquals(dex, ex.getCause());
			assertTrue(ex.getMessage().indexOf("mymsg") != -1);
		}

		final JDBCException jdex = new JDBCException("mymsg", sqlEx);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw jdex;
				}
			});
			fail("Should have thrown HibernateJdbcException");
		}
		catch (HibernateJdbcException ex) {
			// expected
			assertEquals(jdex, ex.getCause());
			assertTrue(ex.getMessage().indexOf("mymsg") != -1);
		}

		final PropertyValueException pvex = new PropertyValueException("mymsg", "myentity", "myproperty");
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw pvex;
				}
			});
			fail("Should have thrown DataIntegrityViolationException");
		}
		catch (DataIntegrityViolationException ex) {
			// expected
			assertEquals(pvex, ex.getCause());
			assertTrue(ex.getMessage().indexOf("mymsg") != -1);
		}

		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw new PersistentObjectException("");
				}
			});
			fail("Should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// expected
		}

		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw new TransientObjectException("");
				}
			});
			fail("Should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// expected
		}

		final ObjectDeletedException odex = new ObjectDeletedException("msg", "id", TestBean.class.getName());
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw odex;
				}
			});
			fail("Should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// expected
			assertEquals(odex, ex.getCause());
		}

		final QueryException qex = new QueryException("msg");
		qex.setQueryString("query");
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw qex;
				}
			});
			fail("Should have thrown InvalidDataAccessResourceUsageException");
		}
		catch (HibernateQueryException ex) {
			// expected
			assertEquals(qex, ex.getCause());
			assertEquals("query", ex.getQueryString());
		}

		final UnresolvableObjectException uoex = new UnresolvableObjectException("id", TestBean.class.getName());
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw uoex;
				}
			});
			fail("Should have thrown HibernateObjectRetrievalFailureException");
		}
		catch (HibernateObjectRetrievalFailureException ex) {
			// expected
			assertEquals(TestBean.class.getName(), ex.getPersistentClassName());
			assertEquals("id", ex.getIdentifier());
			assertEquals(uoex, ex.getCause());
		}

		final ObjectNotFoundException onfe = new ObjectNotFoundException("id", TestBean.class.getName());
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw onfe;
				}
			});
			fail("Should have thrown HibernateObjectRetrievalFailureException");
		}
		catch (HibernateObjectRetrievalFailureException ex) {
			// expected
			assertEquals(TestBean.class.getName(), ex.getPersistentClassName());
			assertEquals("id", ex.getIdentifier());
			assertEquals(onfe, ex.getCause());
		}

		final WrongClassException wcex = new WrongClassException("msg", "id", TestBean.class.getName());
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw wcex;
				}
			});
			fail("Should have thrown HibernateObjectRetrievalFailureException");
		}
		catch (HibernateObjectRetrievalFailureException ex) {
			// expected
			assertEquals(TestBean.class.getName(), ex.getPersistentClassName());
			assertEquals("id", ex.getIdentifier());
			assertEquals(wcex, ex.getCause());
		}

		final NonUniqueResultException nuex = new NonUniqueResultException(2);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw nuex;
				}
			});
			fail("Should have thrown IncorrectResultSizeDataAccessException");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// expected
			assertEquals(1, ex.getExpectedSize());
			assertEquals(-1, ex.getActualSize());
		}

		final StaleObjectStateException sosex = new StaleObjectStateException(TestBean.class.getName(), "id");
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw sosex;
				}
			});
			fail("Should have thrown HibernateOptimisticLockingFailureException");
		}
		catch (HibernateOptimisticLockingFailureException ex) {
			// expected
			assertEquals(TestBean.class.getName(), ex.getPersistentClassName());
			assertEquals("id", ex.getIdentifier());
			assertEquals(sosex, ex.getCause());
		}

		final StaleStateException ssex = new StaleStateException("msg");
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw ssex;
				}
			});
			fail("Should have thrown HibernateOptimisticLockingFailureException");
		}
		catch (HibernateOptimisticLockingFailureException ex) {
			// expected
			assertNull(ex.getPersistentClassName());
			assertNull(ex.getIdentifier());
			assertEquals(ssex, ex.getCause());
		}

		final HibernateException hex = new HibernateException("msg");
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw hex;
				}
			});
			fail("Should have thrown HibernateSystemException");
		}
		catch (HibernateSystemException ex) {
			// expected
			assertEquals(hex, ex.getCause());
		}
	}

	@Test
	public void testFallbackExceptionTranslation() throws HibernateException {
		SQLException sqlEx = new SQLException("argh", "27");

		final GenericJDBCException gjex = new GenericJDBCException("mymsg", sqlEx);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					throw gjex;
				}
			});
			fail("Should have thrown DataIntegrityViolationException");
		}
		catch (DataIntegrityViolationException ex) {
			// expected
			assertEquals(sqlEx, ex.getCause());
			assertTrue(ex.getMessage().indexOf("mymsg") != -1);
		}
	}
}

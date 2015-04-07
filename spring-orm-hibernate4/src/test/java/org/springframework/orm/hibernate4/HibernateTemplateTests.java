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

package org.springframework.orm.hibernate4;

import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.PersistentObjectException;
import org.hibernate.PropertyValueException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
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
 * @since 4.0.1
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
		given(sessionFactory.getCurrentSession()).willReturn(session);
	}

	@After
	public void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void testExecuteWithNewSession()  {
		given(sessionFactory.getCurrentSession()).willThrow(new HibernateException("no current session"));
		given(sessionFactory.openSession()).willReturn(session);

		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.execute(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session)  {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
		verify(session).close();
	}

	@Test
	public void testExecuteWithNewSessionAndFilter()  {
		given(sessionFactory.getCurrentSession()).willThrow(new HibernateException("no current session"));
		given(sessionFactory.openSession()).willReturn(session);
		hibernateTemplate.setFilterNames("myFilter");

		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.execute(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session) {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
		verify(session).enableFilter("myFilter");
		verify(session).close();
	}

	@Test
	public void testExecuteWithNewSessionAndFilters()  {
		given(sessionFactory.getCurrentSession()).willThrow(new HibernateException("no current session"));
		given(sessionFactory.openSession()).willReturn(session);
		hibernateTemplate.setFilterNames("myFilter", "yourFilter");

		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.execute(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session)  {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
		InOrder ordered = inOrder(session);
		ordered.verify(session).enableFilter("myFilter");
		ordered.verify(session).enableFilter("yourFilter");
		ordered.verify(session).close();
	}

	@Test
	public void testExecuteWithThreadBound() {
		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.execute(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session)  {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
	}

	@Test
	public void testExecuteWithThreadBoundAndFilter() {
		hibernateTemplate.setFilterNames("myFilter");

		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.execute(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session)  {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);

		InOrder ordered = inOrder(session);
		ordered.verify(session).enableFilter("myFilter");
		ordered.verify(session).disableFilter("myFilter");
	}

	@Test
	public void testExecuteWithThreadBoundAndFilters() {
		hibernateTemplate.setFilterNames("myFilter", "yourFilter");

		final List l = new ArrayList();
		l.add("test");
		List result = hibernateTemplate.execute(new HibernateCallback<List>() {
			@Override
			public List doInHibernate(Session session)  {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);

		InOrder ordered = inOrder(session);
		ordered.verify(session).enableFilter("myFilter");
		ordered.verify(session).enableFilter("yourFilter");
		ordered.verify(session).disableFilter("myFilter");
		ordered.verify(session).disableFilter("yourFilter");
	}

	@Test
	public void testExecuteWithThreadBoundAndParameterizedFilter() {
		Filter filter = mock(Filter.class);
		given(session.enableFilter("myFilter")).willReturn(filter);
		hibernateTemplate.setFilterNames("myFilter");

		final List l = new ArrayList();
		l.add("test");
		Filter f = hibernateTemplate.enableFilter("myFilter");
		assertTrue("Correct filter", f == filter);

		InOrder ordered = inOrder(session);
		ordered.verify(session).getEnabledFilter("myFilter");
		ordered.verify(session).enableFilter("myFilter");
	}

	@Test
	public void testExecuteWithThreadBoundAndParameterizedExistingFilter() {
		Filter filter = mock(Filter.class);
		given(session.enableFilter("myFilter")).willReturn(filter);
		hibernateTemplate.setFilterNames("myFilter");

		final List l = new ArrayList();
		l.add("test");
		Filter f = hibernateTemplate.enableFilter("myFilter");
		assertTrue("Correct filter", f == filter);

		verify(session).getEnabledFilter("myFilter");
	}

	@Test
	public void testExecuteWithCacheQueries()  {
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
			public Object doInHibernate(Session sess) {
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
	}

	@Test
	public void testExecuteWithCacheQueriesAndCacheRegion()  {
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
			public Object doInHibernate(Session sess) {
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
	}

	@Test
	public void testExecuteWithCacheQueriesAndCacheRegionAndNativeSession()  {
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
			public Object doInHibernate(Session sess)  {
				assertSame(session, sess);
				sess.createQuery("some query");
				sess.getNamedQuery("some query name");
				sess.createCriteria(TestBean.class);
				return null;
			}
		});
	}

	@Test
	public void testExecuteWithFetchSizeAndMaxResults()  {
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
			public Object doInHibernate(Session sess)  {
				sess.createQuery("some query");
				sess.getNamedQuery("some query name");
				sess.createCriteria(TestBean.class);
				return null;
			}
		});
	}

	@Test
	public void testGet()  {
		TestBean tb = new TestBean();
		given(session.get(TestBean.class, "")).willReturn(tb);
		Object result = hibernateTemplate.get(TestBean.class, "");
		assertTrue("Correct result", result == tb);
	}

	@Test
	public void testGetWithEntityName()  {
		TestBean tb = new TestBean();
		given(session.get("myEntity", "")).willReturn(tb);
		Object result = hibernateTemplate.get("myEntity", "");
		assertTrue("Correct result", result == tb);
	}

	@Test
	public void testLoad()  {
		TestBean tb = new TestBean();
		given(session.load(TestBean.class, "")).willReturn(tb);
		Object result = hibernateTemplate.load(TestBean.class, "");
		assertTrue("Correct result", result == tb);
	}

	@Test
	public void testLoadWithNotFound()  {
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
	}

	@Test
	public void testLoadWithEntityName()  {
		TestBean tb = new TestBean();
		given(session.load("myEntity", "")).willReturn(tb);
		Object result = hibernateTemplate.load("myEntity", "");
		assertTrue("Correct result", result == tb);
	}

	@Test
	public void testLoadWithObject()  {
		TestBean tb = new TestBean();
		hibernateTemplate.load(tb, "");
		verify(session).load(tb, "");
	}

	@Test
	public void testLoadAll()  {
		Criteria criteria = mock(Criteria.class);
		List list = new ArrayList();
		given(session.createCriteria(TestBean.class)).willReturn(criteria);
		given(criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)).willReturn(criteria);
		given(criteria.list()).willReturn(list);
		List result = hibernateTemplate.loadAll(TestBean.class);
		assertTrue("Correct result", result == list);
	}

	@Test
	public void testLoadAllWithCacheable()  {
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
	}

	@Test
	public void testLoadAllWithCacheableAndCacheRegion()  {
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
	}

	@Test public void testRefresh()  {
		TestBean tb = new TestBean();
		hibernateTemplate.refresh(tb);
		verify(session).refresh(tb);
	}

	@Test public void testContains()  {
		TestBean tb = new TestBean();
		given(session.contains(tb)).willReturn(true);
		assertTrue(hibernateTemplate.contains(tb));
	}

	@Test
	public void testEvict()  {
		TestBean tb = new TestBean();
		hibernateTemplate.evict(tb);
		verify(session).evict(tb);
	}

	@Test
	public void testSave()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.save(tb)).willReturn(0);
		assertEquals("Correct return value", hibernateTemplate.save(tb), 0);
	}

	@Test
	public void testSaveWithEntityName()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.save("myEntity", tb)).willReturn(0);
		assertEquals("Correct return value", hibernateTemplate.save("myEntity", tb), 0);
	}

	@Test
	public void testUpdate()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.update(tb);
		verify(session).update(tb);
	}

	@Test
	public void testUpdateWithEntityName()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.update("myEntity", tb);
		verify(session).update("myEntity", tb);
	}

	@Test
	public void testSaveOrUpdate()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.saveOrUpdate(tb);
		verify(session).saveOrUpdate(tb);
	}

	@Test
	public void testSaveOrUpdateWithFlushModeNever()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.MANUAL);
		try {
			hibernateTemplate.saveOrUpdate(tb);
			fail("Should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// expected
		}
	}

	@Test
	public void testSaveOrUpdateWithEntityName()  {
		TestBean tb = new TestBean();

		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.saveOrUpdate("myEntity", tb);
		verify(session).saveOrUpdate("myEntity", tb);
	}

	@Test
	public void testReplicate()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.replicate(tb, ReplicationMode.LATEST_VERSION);
		verify(session).replicate(tb, ReplicationMode.LATEST_VERSION);
	}

	@Test
	public void testReplicateWithEntityName()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.replicate("myEntity", tb, ReplicationMode.LATEST_VERSION);
		verify(session).replicate("myEntity", tb, ReplicationMode.LATEST_VERSION);
	}

	@Test
	public void testPersist()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.persist(tb);
		verify(session).persist(tb);
	}

	@Test
	public void testPersistWithEntityName()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.persist("myEntity", tb);
		verify(session).persist("myEntity", tb);
	}

	@Test
	public void testMerge()  {
		TestBean tb = new TestBean();
		TestBean tbMerged = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.merge(tb)).willReturn(tbMerged);
		assertSame(tbMerged, hibernateTemplate.merge(tb));
	}

	@Test
	public void testMergeWithEntityName()  {
		TestBean tb = new TestBean();
		TestBean tbMerged = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.merge("myEntity", tb)).willReturn(tbMerged);
		assertSame(tbMerged, hibernateTemplate.merge("myEntity", tb));
	}

	@Test
	public void testDelete()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.delete(tb);
		verify(session).delete(tb);
	}

	@Test
	public void testDeleteWithEntityName()  {
		TestBean tb = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		hibernateTemplate.delete("myEntity", tb);
		verify(session).delete("myEntity", tb);
	}

	@Test
	public void testDeleteAll()  {
		TestBean tb1 = new TestBean();
		TestBean tb2 = new TestBean();
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		List tbs = new ArrayList();
		tbs.add(tb1);
		tbs.add(tb2);
		hibernateTemplate.deleteAll(tbs);
		verify(session).delete(same(tb1));
		verify(session).delete(same(tb2));
	}

	@Test
	public void testFlush()  {
		hibernateTemplate.flush();
		verify(session).flush();
	}

	@Test
	public void testClear()  {
		hibernateTemplate.clear();
		verify(session).clear();
	}

	@Test
	public void testFind()  {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.find("some query string");
		assertTrue("Correct list", result == list);
	}

	@Test
	public void testFindWithParameter()  {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.find("some query string", "myvalue");
		assertTrue("Correct list", result == list);
		verify(query).setParameter(0, "myvalue");
	}

	@Test
	public void testFindWithParameters()  {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue1")).willReturn(query);
		given(query.setParameter(1, 2)).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.find("some query string", "myvalue1", 2);
		assertTrue("Correct list", result == list);
		verify(query).setParameter(0, "myvalue1");
		verify(query).setParameter(1, 2);
	}

	@Test
	public void testFindWithNamedParameter()  {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter("myparam", "myvalue")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedParam("some query string", "myparam", "myvalue");
		assertTrue("Correct list", result == list);
		verify(query).setParameter("myparam", "myvalue");
	}

	@Test
	public void testFindWithNamedParameters()  {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter("myparam1", "myvalue1")).willReturn(query);
		given(query.setParameter("myparam2", 2)).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedParam("some query string",
				new String[]{"myparam1", "myparam2"},
				new Object[]{"myvalue1", 2});
		assertTrue("Correct list", result == list);
		verify(query).setParameter("myparam1", "myvalue1");
		verify(query).setParameter("myparam2", 2);
	}

	@Test
	public void testFindByValueBean()  {
		Query query = mock(Query.class);
		TestBean tb = new TestBean();
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setProperties(tb)).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByValueBean("some query string", tb);
		assertTrue("Correct list", result == list);
		verify(query).setProperties(tb);
	}

	@Test
	public void testFindByNamedQuery()  {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedQuery("some query name");
		assertTrue("Correct list", result == list);
	}

	@Test
	public void testFindByNamedQueryWithParameter()  {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setParameter(0, "myvalue")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedQuery("some query name", "myvalue");
		assertTrue("Correct list", result == list);
		verify(query).setParameter(0, "myvalue");
	}

	@Test
	public void testFindByNamedQueryWithParameters()  {
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
	}

	@Test
	public void testFindByNamedQueryWithNamedParameter()  {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setParameter("myparam", "myvalue")).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedQueryAndNamedParam("some query name", "myparam", "myvalue");
		assertTrue("Correct list", result == list);
		verify(query).setParameter("myparam", "myvalue");
	}

	@Test
	public void testFindByNamedQueryWithNamedParameters()  {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setParameter("myparam1", "myvalue1")).willReturn(query);
		given(query.setParameter("myparam2", 2)).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedQueryAndNamedParam("some query name",
				new String[]{"myparam1", "myparam2"},
				new Object[]{"myvalue1", 2});
		assertTrue("Correct list", result == list);
		verify(query).setParameter("myparam1", "myvalue1");
		verify(query).setParameter("myparam2", 2);
	}

	@Test
	public void testFindByNamedQueryAndValueBean()  {
		Query query = mock(Query.class);
		TestBean tb = new TestBean();
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setProperties(tb)).willReturn(query);
		given(query.list()).willReturn(list);
		List result = hibernateTemplate.findByNamedQueryAndValueBean("some query name", tb);
		assertTrue("Correct list", result == list);
		verify(query).setProperties(tb);
	}

	@Test
	public void testFindWithCacheable()  {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setCacheable(true)).willReturn(query);
		given(query.list()).willReturn(list);
		hibernateTemplate.setCacheQueries(true);
		List result = hibernateTemplate.find("some query string");
		assertTrue("Correct list", result == list);
		verify(query).setCacheable(true);
	}

	@Test
	public void testFindWithCacheableAndCacheRegion()  {
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
	}

	@Test
	public void testFindByNamedQueryWithCacheable()  {
		Query query = mock(Query.class);
		List list = new ArrayList();
		given(session.getNamedQuery("some query name")).willReturn(query);
		given(query.setCacheable(true)).willReturn(query);
		given(query.list()).willReturn(list);
		hibernateTemplate.setCacheQueries(true);
		List result = hibernateTemplate.findByNamedQuery("some query name");
		assertTrue("Correct list", result == list);
		verify(query).setCacheable(true);
	}

	@Test
	public void testFindByNamedQueryWithCacheableAndCacheRegion()  {
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
	}

	@Test
	public void testIterate()  {
		Query query = mock(Query.class);
		Iterator it = Collections.EMPTY_LIST.iterator();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.iterate()).willReturn(it);
		Iterator result = hibernateTemplate.iterate("some query string");
		assertTrue("Correct list", result == it);
	}

	@Test
	public void testIterateWithParameter()  {
		Query query = mock(Query.class);
		Iterator it = Collections.EMPTY_LIST.iterator();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue")).willReturn(query);
		given(query.iterate()).willReturn(it);
		Iterator result = hibernateTemplate.iterate("some query string", "myvalue");
		assertTrue("Correct list", result == it);
		verify(query).setParameter(0, "myvalue");
	}

	@Test
	public void testIterateWithParameters()  {
		Query query = mock(Query.class);
		Iterator it = Collections.EMPTY_LIST.iterator();
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue1")).willReturn(query);
		given(query.setParameter(1, 2)).willReturn(query);
		given(query.iterate()).willReturn(it);
		Iterator result = hibernateTemplate.iterate("some query string", "myvalue1", 2);
		assertTrue("Correct list", result == it);
		verify(query).setParameter(0, "myvalue1");
		verify(query).setParameter(1, 2);
	}

	@Test
	public void testBulkUpdate()  {
		Query query = mock(Query.class);
		given(session.createQuery("some query string")).willReturn(query);
		given(query.executeUpdate()).willReturn(5);
		int result = hibernateTemplate.bulkUpdate("some query string");
		assertTrue("Correct list", result == 5);
	}

	@Test
	public void testBulkUpdateWithParameter()  {
		Query query = mock(Query.class);
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue")).willReturn(query);
		given(query.executeUpdate()).willReturn(5);
		int result = hibernateTemplate.bulkUpdate("some query string", "myvalue");
		assertTrue("Correct list", result == 5);
		verify(query).setParameter(0, "myvalue");
	}

	@Test
	public void testBulkUpdateWithParameters()  {
		Query query = mock(Query.class);
		given(session.createQuery("some query string")).willReturn(query);
		given(query.setParameter(0, "myvalue1")).willReturn(query);
		given(query.setParameter(1, 2)).willReturn(query);
		given(query.executeUpdate()).willReturn(5);
		int result = hibernateTemplate.bulkUpdate("some query string", "myvalue1", 2);
		assertTrue("Correct list", result == 5);
		verify(query).setParameter(0, "myvalue1");
		verify(query).setParameter(1, 2);
	}

	@Test
	public void testExceptions()  {
		SQLException sqlEx = new SQLException("argh", "27");

		final JDBCConnectionException jcex = new JDBCConnectionException("mymsg", sqlEx);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session)  {
					throw jcex;
				}
			});
			fail("Should have thrown DataAccessResourceFailureException");
		}
		catch (DataAccessResourceFailureException ex) {
			// expected
			assertEquals(jcex, ex.getCause());
			assertTrue(ex.getMessage().contains("mymsg"));
		}

		final SQLGrammarException sgex = new SQLGrammarException("mymsg", sqlEx);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session)  {
					throw sgex;
				}
			});
			fail("Should have thrown InvalidDataAccessResourceUsageException");
		}
		catch (InvalidDataAccessResourceUsageException ex) {
			// expected
			assertEquals(sgex, ex.getCause());
			assertTrue(ex.getMessage().contains("mymsg"));
		}

		final LockAcquisitionException laex = new LockAcquisitionException("mymsg", sqlEx);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session)  {
					throw laex;
				}
			});
			fail("Should have thrown CannotAcquireLockException");
		}
		catch (CannotAcquireLockException ex) {
			// expected
			assertEquals(laex, ex.getCause());
			assertTrue(ex.getMessage().contains("mymsg"));
		}

		final ConstraintViolationException cvex = new ConstraintViolationException("mymsg", sqlEx, "myconstraint");
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session)  {
					throw cvex;
				}
			});
			fail("Should have thrown DataIntegrityViolationException");
		}
		catch (DataIntegrityViolationException ex) {
			// expected
			assertEquals(cvex, ex.getCause());
			assertTrue(ex.getMessage().contains("mymsg"));
		}

		final DataException dex = new DataException("mymsg", sqlEx);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session)  {
					throw dex;
				}
			});
			fail("Should have thrown DataIntegrityViolationException");
		}
		catch (DataIntegrityViolationException ex) {
			// expected
			assertEquals(dex, ex.getCause());
			assertTrue(ex.getMessage().contains("mymsg"));
		}

		final JDBCException jdex = new JDBCException("mymsg", sqlEx);
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session)  {
					throw jdex;
				}
			});
			fail("Should have thrown HibernateJdbcException");
		}
		catch (HibernateJdbcException ex) {
			// expected
			assertEquals(jdex, ex.getCause());
			assertTrue(ex.getMessage().contains("mymsg"));
		}

		final PropertyValueException pvex = new PropertyValueException("mymsg", "myentity", "myproperty");
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session)  {
					throw pvex;
				}
			});
			fail("Should have thrown DataIntegrityViolationException");
		}
		catch (DataIntegrityViolationException ex) {
			// expected
			assertEquals(pvex, ex.getCause());
			assertTrue(ex.getMessage().contains("mymsg"));
		}

		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session)  {
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
				public Object doInHibernate(Session session)  {
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
				public Object doInHibernate(Session session)  {
					throw odex;
				}
			});
			fail("Should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// expected
			assertEquals(odex, ex.getCause());
		}

		final QueryException qex = new QueryException("msg", "query");
		try {
			hibernateTemplate.execute(new HibernateCallback<Object>() {
				@Override
				public Object doInHibernate(Session session)  {
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
				public Object doInHibernate(Session session)  {
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
				public Object doInHibernate(Session session)  {
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
				public Object doInHibernate(Session session)  {
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
				public Object doInHibernate(Session session)  {
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
				public Object doInHibernate(Session session)  {
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
				public Object doInHibernate(Session session)  {
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
				public Object doInHibernate(Session session)  {
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

}

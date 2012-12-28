/*
 * Copyright 2002-2012 the original author or authors.
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

import junit.framework.TestCase;
import org.easymock.MockControl;
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

import org.springframework.beans.TestBean;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Juergen Hoeller
 * @since 05.03.2005
 */
public class HibernateTemplateTests extends TestCase {

	private MockControl sfControl;
	private SessionFactory sf;
	private MockControl sessionControl;
	private Session session;

	@Override
	protected void setUp() {
		sfControl = MockControl.createControl(SessionFactory.class);
		sf = (SessionFactory) sfControl.getMock();
		sessionControl = MockControl.createControl(Session.class);
		session = (Session) sessionControl.getMock();
	}

	public void testExecuteWithNewSession() throws HibernateException {
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		assertTrue("Correct allowCreate default", ht.isAllowCreate());
		assertTrue("Correct flushMode default", ht.getFlushMode() == HibernateTemplate.FLUSH_AUTO);
		final List l = new ArrayList();
		l.add("test");
		List result = ht.executeFind(new HibernateCallback() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
	}

	public void testExecuteWithNewSessionAndFlushNever() throws HibernateException {
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.setFlushMode(FlushMode.MANUAL);
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setFlushMode(HibernateTemplate.FLUSH_NEVER);
		final List l = new ArrayList();
		l.add("test");
		List result = ht.executeFind(new HibernateCallback() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
	}

	public void testExecuteWithNewSessionAndFilter() throws HibernateException {
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.enableFilter("myFilter");
		sessionControl.setReturnValue(null, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setFilterName("myFilter");
		final List l = new ArrayList();
		l.add("test");
		List result = ht.executeFind(new HibernateCallback() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
	}

	public void testExecuteWithNewSessionAndFilters() throws HibernateException {
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.enableFilter("myFilter");
		sessionControl.setReturnValue(null, 1);
		session.enableFilter("yourFilter");
		sessionControl.setReturnValue(null, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setFilterNames(new String[] {"myFilter", "yourFilter"});
		final List l = new ArrayList();
		l.add("test");
		List result = ht.executeFind(new HibernateCallback() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
	}

	public void testExecuteWithNotAllowCreate() {
		sf.getCurrentSession();
		sfControl.setThrowable(new HibernateException(""));
		sfControl.replay();

		HibernateTemplate ht = new HibernateTemplate();
		ht.setSessionFactory(sf);
		ht.setAllowCreate(false);
		try {
			ht.execute(new HibernateCallback() {
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

	public void testExecuteWithNotAllowCreateAndThreadBound() {
		sf.getCurrentSession();
		sfControl.setReturnValue(session);
		sfControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setAllowCreate(false);

		final List l = new ArrayList();
		l.add("test");
		List result = ht.executeFind(new HibernateCallback() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
	}

	public void testExecuteWithThreadBoundAndFlushEager() throws HibernateException {
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.flush();
		sessionControl.setVoidCallable(1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setFlushModeName("FLUSH_EAGER");

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		try {
			final List l = new ArrayList();
			l.add("test");
			List result = ht.executeFind(new HibernateCallback() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					return l;
				}
			});
			assertTrue("Correct result list", result == l);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}
	}

	public void testExecuteWithThreadBoundAndFilter() {
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 1);
		session.enableFilter("myFilter");
		sessionControl.setReturnValue(null, 1);
		session.disableFilter("myFilter");
		sessionControl.setVoidCallable(1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setFilterName("myFilter");

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		try {
			final List l = new ArrayList();
			l.add("test");
			List result = ht.executeFind(new HibernateCallback() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					return l;
				}
			});
			assertTrue("Correct result list", result == l);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}
	}

	public void testExecuteWithThreadBoundAndFilters() {
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 1);
		session.enableFilter("myFilter");
		sessionControl.setReturnValue(null, 1);
		session.enableFilter("yourFilter");
		sessionControl.setReturnValue(null, 1);
		session.disableFilter("myFilter");
		sessionControl.setVoidCallable(1);
		session.disableFilter("yourFilter");
		sessionControl.setVoidCallable(1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setFilterNames(new String[] {"myFilter", "yourFilter"});

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		try {
			final List l = new ArrayList();
			l.add("test");
			List result = ht.executeFind(new HibernateCallback() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					return l;
				}
			});
			assertTrue("Correct result list", result == l);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}
	}

	public void testExecuteWithThreadBoundAndParameterizedFilter() {
		MockControl filterControl = MockControl.createControl(Filter.class);
		Filter filter = (Filter) filterControl.getMock();

		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 1);
		session.getEnabledFilter("myFilter");
		sessionControl.setReturnValue(null, 1);
		session.enableFilter("myFilter");
		sessionControl.setReturnValue(filter, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setAllowCreate(false);
		ht.setFilterName("myFilter");

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		try {
			final List l = new ArrayList();
			l.add("test");
			Filter f = ht.enableFilter("myFilter");
			assertTrue("Correct filter", f == filter);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}
	}

	public void testExecuteWithThreadBoundAndParameterizedExistingFilter() {
		MockControl filterControl = MockControl.createControl(Filter.class);
		Filter filter = (Filter) filterControl.getMock();

		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 1);
		session.getEnabledFilter("myFilter");
		sessionControl.setReturnValue(filter, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setAllowCreate(false);
		ht.setFilterName("myFilter");

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		try {
			final List l = new ArrayList();
			l.add("test");
			Filter f = ht.enableFilter("myFilter");
			assertTrue("Correct filter", f == filter);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}
	}

	public void testExecuteWithThreadBoundAndNewSession() throws HibernateException {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl session2Control = MockControl.createControl(Session.class);
		Session session2 = (Session) session2Control.getMock();

		session2.connection();
		session2Control.setReturnValue(con, 1);
		sf.openSession(con);
		sfControl.setReturnValue(session, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		session2Control.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setAlwaysUseNewSession(true);

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session2));
		try {
			final List l = new ArrayList();
			l.add("test");
			List result = ht.executeFind(new HibernateCallback() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					return l;
				}
			});
			assertTrue("Correct result list", result == l);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}
	}

	public void testExecuteWithThreadBoundAndNewSessionAndEntityInterceptor() throws HibernateException {
		MockControl interceptorControl = MockControl.createControl(org.hibernate.Interceptor.class);
		Interceptor entityInterceptor = (Interceptor) interceptorControl.getMock();

		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl session2Control = MockControl.createControl(Session.class);
		Session session2 = (Session) session2Control.getMock();

		session2.connection();
		session2Control.setReturnValue(con, 1);
		sf.openSession(con, entityInterceptor);
		sfControl.setReturnValue(session, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		session2Control.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setAlwaysUseNewSession(true);
		ht.setEntityInterceptor(entityInterceptor);

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session2));
		try {
			final List l = new ArrayList();
			l.add("test");
			List result = ht.executeFind(new HibernateCallback() {
				@Override
				public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
					return l;
				}
			});
			assertTrue("Correct result list", result == l);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}
	}

	public void testExecuteWithEntityInterceptor() throws HibernateException {
		MockControl interceptorControl = MockControl.createControl(org.hibernate.Interceptor.class);
		Interceptor entityInterceptor = (Interceptor) interceptorControl.getMock();

		sf.openSession(entityInterceptor);
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setEntityInterceptor(entityInterceptor);
		final List l = new ArrayList();
		l.add("test");
		List result = ht.executeFind(new HibernateCallback() {
			@Override
			public Object doInHibernate(org.hibernate.Session session) throws HibernateException {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
	}

	public void testExecuteWithCacheQueries() throws HibernateException {
		MockControl query1Control = MockControl.createControl(Query.class);
		Query query1 = (Query) query1Control.getMock();
		MockControl query2Control = MockControl.createControl(Query.class);
		Query query2 = (Query) query2Control.getMock();
		MockControl criteriaControl = MockControl.createControl(Criteria.class);
		Criteria criteria = (Criteria) criteriaControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query");
		sessionControl.setReturnValue(query1);
		query1.setCacheable(true);
		query1Control.setReturnValue(query1, 1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query2);
		query2.setCacheable(true);
		query2Control.setReturnValue(query2, 1);
		session.createCriteria(TestBean.class);
		sessionControl.setReturnValue(criteria, 1);
		criteria.setCacheable(true);
		criteriaControl.setReturnValue(criteria, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		query1Control.replay();
		query2Control.replay();
		criteriaControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setCacheQueries(true);
		ht.execute(new HibernateCallback() {
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

		query1Control.verify();
		query2Control.verify();
		criteriaControl.verify();
	}

	public void testExecuteWithCacheQueriesAndCacheRegion() throws HibernateException {
		MockControl query1Control = MockControl.createControl(Query.class);
		Query query1 = (Query) query1Control.getMock();
		MockControl query2Control = MockControl.createControl(Query.class);
		Query query2 = (Query) query2Control.getMock();
		MockControl criteriaControl = MockControl.createControl(Criteria.class);
		Criteria criteria = (Criteria) criteriaControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query");
		sessionControl.setReturnValue(query1);
		query1.setCacheable(true);
		query1Control.setReturnValue(query1, 1);
		query1.setCacheRegion("myRegion");
		query1Control.setReturnValue(query1, 1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query2);
		query2.setCacheable(true);
		query2Control.setReturnValue(query2, 1);
		query2.setCacheRegion("myRegion");
		query2Control.setReturnValue(query2, 1);
		session.createCriteria(TestBean.class);
		sessionControl.setReturnValue(criteria, 1);
		criteria.setCacheable(true);
		criteriaControl.setReturnValue(criteria, 1);
		criteria.setCacheRegion("myRegion");
		criteriaControl.setReturnValue(criteria, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		query1Control.replay();
		query2Control.replay();
		criteriaControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setCacheQueries(true);
		ht.setQueryCacheRegion("myRegion");
		ht.execute(new HibernateCallback() {
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

		query1Control.verify();
		query2Control.verify();
		criteriaControl.verify();
	}

	public void testExecuteWithCacheQueriesAndCacheRegionAndNativeSession() throws HibernateException {
		MockControl query1Control = MockControl.createControl(Query.class);
		Query query1 = (Query) query1Control.getMock();
		MockControl query2Control = MockControl.createControl(Query.class);
		Query query2 = (Query) query2Control.getMock();
		MockControl criteriaControl = MockControl.createControl(Criteria.class);
		Criteria criteria = (Criteria) criteriaControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query");
		sessionControl.setReturnValue(query1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query2);
		session.createCriteria(TestBean.class);
		sessionControl.setReturnValue(criteria, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		query1Control.replay();
		query2Control.replay();
		criteriaControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		ht.setCacheQueries(true);
		ht.setQueryCacheRegion("myRegion");
		ht.execute(new HibernateCallback() {
			@Override
			public Object doInHibernate(org.hibernate.Session sess) throws HibernateException {
				assertSame(session, sess);
				sess.createQuery("some query");
				sess.getNamedQuery("some query name");
				sess.createCriteria(TestBean.class);
				return null;
			}
		});

		query1Control.verify();
		query2Control.verify();
		criteriaControl.verify();
	}

	public void testExecuteWithFetchSizeAndMaxResults() throws HibernateException {
		MockControl query1Control = MockControl.createControl(Query.class);
		Query query1 = (Query) query1Control.getMock();
		MockControl query2Control = MockControl.createControl(Query.class);
		Query query2 = (Query) query2Control.getMock();
		MockControl criteriaControl = MockControl.createControl(Criteria.class);
		Criteria criteria = (Criteria) criteriaControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query");
		sessionControl.setReturnValue(query1);
		query1.setFetchSize(10);
		query1Control.setReturnValue(query1, 1);
		query1.setMaxResults(20);
		query1Control.setReturnValue(query1, 1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query2);
		query2.setFetchSize(10);
		query2Control.setReturnValue(query2, 1);
		query2.setMaxResults(20);
		query2Control.setReturnValue(query2, 1);
		session.createCriteria(TestBean.class);
		sessionControl.setReturnValue(criteria, 1);
		criteria.setFetchSize(10);
		criteriaControl.setReturnValue(criteria, 1);
		criteria.setMaxResults(20);
		criteriaControl.setReturnValue(criteria, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		query1Control.replay();
		query2Control.replay();
		criteriaControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setFetchSize(10);
		ht.setMaxResults(20);
		ht.execute(new HibernateCallback() {
			@Override
			public Object doInHibernate(org.hibernate.Session sess) throws HibernateException {
				sess.createQuery("some query");
				sess.getNamedQuery("some query name");
				sess.createCriteria(TestBean.class);
				return null;
			}
		});

		query1Control.verify();
		query2Control.verify();
		criteriaControl.verify();
	}

	public void testGet() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.get(TestBean.class, "");
		sessionControl.setReturnValue(tb, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		Object result = ht.get(TestBean.class, "");
		assertTrue("Correct result", result == tb);
	}

	public void testGetWithLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.get(TestBean.class, "", LockMode.UPGRADE_NOWAIT);
		sessionControl.setReturnValue(tb, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		Object result = ht.get(TestBean.class, "", LockMode.UPGRADE_NOWAIT);
		assertTrue("Correct result", result == tb);
	}

	public void testGetWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.get("myEntity", "");
		sessionControl.setReturnValue(tb, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		Object result = ht.get("myEntity", "");
		assertTrue("Correct result", result == tb);
	}

	public void testGetWithEntityNameAndLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.get("myEntity", "", LockMode.UPGRADE_NOWAIT);
		sessionControl.setReturnValue(tb, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		Object result = ht.get("myEntity", "", LockMode.UPGRADE_NOWAIT);
		assertTrue("Correct result", result == tb);
	}

	public void testLoad() throws HibernateException {
		TestBean tb = new TestBean();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.load(TestBean.class, "");
		sessionControl.setReturnValue(tb, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		Object result = ht.load(TestBean.class, "");
		assertTrue("Correct result", result == tb);
	}

	public void testLoadWithNotFound() throws HibernateException {
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.load(TestBean.class, "id");
		ObjectNotFoundException onfex = new ObjectNotFoundException("id", TestBean.class.getName());
		sessionControl.setThrowable(onfex);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		try {
			ht.load(TestBean.class, "id");
			fail("Should have thrown HibernateObjectRetrievalFailureException");
		}
		catch (HibernateObjectRetrievalFailureException ex) {
			// expected
			assertEquals(TestBean.class.getName(), ex.getPersistentClassName());
			assertEquals("id", ex.getIdentifier());
			assertEquals(onfex, ex.getCause());
		}
	}

	public void testLoadWithLockMode() throws HibernateException {
		TestBean tb = new TestBean();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.load(TestBean.class, "", LockMode.UPGRADE);
		sessionControl.setReturnValue(tb, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		Object result = ht.load(TestBean.class, "", LockMode.UPGRADE);
		assertTrue("Correct result", result == tb);
	}

	public void testLoadWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.load("myEntity", "");
		sessionControl.setReturnValue(tb, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		Object result = ht.load("myEntity", "");
		assertTrue("Correct result", result == tb);
	}

	public void testLoadWithEntityNameLockMode() throws HibernateException {
		TestBean tb = new TestBean();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.load("myEntity", "", LockMode.UPGRADE);
		sessionControl.setReturnValue(tb, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		Object result = ht.load("myEntity", "", LockMode.UPGRADE);
		assertTrue("Correct result", result == tb);
	}

	public void testLoadWithObject() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.load(tb, "");
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.load(tb, "");
	}

	public void testLoadAll() throws HibernateException {
		MockControl criteriaControl = MockControl.createControl(Criteria.class);
		Criteria criteria = (Criteria) criteriaControl.getMock();
		List list = new ArrayList();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createCriteria(TestBean.class);
		sessionControl.setReturnValue(criteria, 1);
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		criteriaControl.setReturnValue(criteria);
		criteria.list();
		criteriaControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		criteriaControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.loadAll(TestBean.class);
		assertTrue("Correct result", result == list);

		criteriaControl.verify();
	}

	public void testLoadAllWithCacheable() throws HibernateException {
		MockControl criteriaControl = MockControl.createControl(Criteria.class);
		Criteria criteria = (Criteria) criteriaControl.getMock();
		List list = new ArrayList();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createCriteria(TestBean.class);
		sessionControl.setReturnValue(criteria, 1);
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		criteriaControl.setReturnValue(criteria);
		criteria.setCacheable(true);
		criteriaControl.setReturnValue(criteria, 1);
		criteria.list();
		criteriaControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		criteriaControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setCacheQueries(true);
		List result = ht.loadAll(TestBean.class);
		assertTrue("Correct result", result == list);

		criteriaControl.verify();
	}

	public void testLoadAllWithCacheableAndCacheRegion() throws HibernateException {
		MockControl criteriaControl = MockControl.createControl(Criteria.class);
		Criteria criteria = (Criteria) criteriaControl.getMock();
		List list = new ArrayList();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createCriteria(TestBean.class);
		sessionControl.setReturnValue(criteria, 1);
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		criteriaControl.setReturnValue(criteria);
		criteria.setCacheable(true);
		criteriaControl.setReturnValue(criteria, 1);
		criteria.setCacheRegion("myCacheRegion");
		criteriaControl.setReturnValue(criteria, 1);
		criteria.list();
		criteriaControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		criteriaControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setCacheQueries(true);
		ht.setQueryCacheRegion("myCacheRegion");
		List result = ht.loadAll(TestBean.class);
		assertTrue("Correct result", result == list);

		criteriaControl.verify();
	}

	public void testRefresh() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.refresh(tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.refresh(tb);
	}

	public void testContains() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.contains(tb);
		sessionControl.setReturnValue(true, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		assertTrue(ht.contains(tb));
	}

	public void testEvict() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.evict(tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.evict(tb);
	}

	public void testLock() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.lock(tb, LockMode.WRITE);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
			sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.lock(tb, LockMode.WRITE);
	}

	public void testLockWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.lock("myEntity", tb, LockMode.WRITE);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
			sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.lock("myEntity", tb, LockMode.WRITE);
	}

	public void testSave() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.save(tb);
		sessionControl.setReturnValue(new Integer(0), 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		assertEquals("Correct return value", ht.save(tb), new Integer(0));
	}

	public void testSaveWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.save("myEntity", tb);
		sessionControl.setReturnValue(new Integer(0), 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		assertEquals("Correct return value", ht.save("myEntity", tb), new Integer(0));
	}

	public void testUpdate() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.update(tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.update(tb);
	}

	public void testUpdateWithLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.update(tb);
		sessionControl.setVoidCallable(1);
		session.lock(tb, LockMode.UPGRADE);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.update(tb, LockMode.UPGRADE);
	}

	public void testUpdateWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.update("myEntity", tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.update("myEntity", tb);
	}

	public void testUpdateWithEntityNameAndLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.update("myEntity", tb);
		sessionControl.setVoidCallable(1);
		session.lock(tb, LockMode.UPGRADE);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.update("myEntity", tb, LockMode.UPGRADE);
	}

	public void testSaveOrUpdate() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.saveOrUpdate(tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.saveOrUpdate(tb);
	}

	public void testSaveOrUpdateWithFlushModeNever() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.MANUAL);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		try {
			ht.saveOrUpdate(tb);
			fail("Should have thrown InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// expected
		}
	}

	public void testSaveOrUpdateWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.saveOrUpdate("myEntity", tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.saveOrUpdate("myEntity", tb);
	}

	public void testSaveOrUpdateAll() throws HibernateException {
		TestBean tb1 = new TestBean();
		TestBean tb2 = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.saveOrUpdate(tb1);
		sessionControl.setVoidCallable(1);
		session.saveOrUpdate(tb2);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List tbs = new ArrayList();
		tbs.add(tb1);
		tbs.add(tb2);
		ht.saveOrUpdateAll(tbs);
	}

	public void testReplicate() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.replicate(tb, ReplicationMode.LATEST_VERSION);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.replicate(tb, ReplicationMode.LATEST_VERSION);
	}

	public void testReplicateWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.replicate("myEntity", tb, ReplicationMode.LATEST_VERSION);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.replicate("myEntity", tb, ReplicationMode.LATEST_VERSION);
	}

	public void testPersist() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.persist(tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.persist(tb);
	}

	public void testPersistWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.persist("myEntity", tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.persist("myEntity", tb);
	}

	public void testMerge() throws HibernateException {
		TestBean tb = new TestBean();
		TestBean tbMerged = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.merge(tb);
		sessionControl.setReturnValue(tbMerged, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		assertSame(tbMerged, ht.merge(tb));
	}

	public void testMergeWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		TestBean tbMerged = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.merge("myEntity", tb);
		sessionControl.setReturnValue(tbMerged, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		assertSame(tbMerged, ht.merge("myEntity", tb));
	}

	public void testDelete() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.delete(tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.delete(tb);
	}

	public void testDeleteWithLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.lock(tb, LockMode.UPGRADE);
		sessionControl.setVoidCallable(1);
		session.delete(tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.delete(tb, LockMode.UPGRADE);
	}

	public void testDeleteWithEntityName() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.delete("myEntity", tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.delete("myEntity", tb);
	}

	public void testDeleteWithEntityNameAndLockMode() throws HibernateException {
		TestBean tb = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.lock("myEntity", tb, LockMode.UPGRADE);
		sessionControl.setVoidCallable(1);
		session.delete("myEntity", tb);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.delete("myEntity", tb, LockMode.UPGRADE);
	}

	public void testDeleteAll() throws HibernateException {
		TestBean tb1 = new TestBean();
		TestBean tb2 = new TestBean();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO);
		session.delete(tb1);
		sessionControl.setVoidCallable(1);
		session.delete(tb2);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List tbs = new ArrayList();
		tbs.add(tb1);
		tbs.add(tb2);
		ht.deleteAll(tbs);
	}

	public void testFlush() throws HibernateException {
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.setFlushMode(FlushMode.MANUAL);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setFlushMode(HibernateTemplate.FLUSH_NEVER);
		ht.flush();
	}

	public void testClear() throws HibernateException {
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.clear();
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.clear();
	}

	public void testFind() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.find("some query string");
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindWithParameter() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.setParameter(0, "myvalue");
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.find("some query string", "myvalue");
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindWithParameters() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.setParameter(0, "myvalue1");
		queryControl.setReturnValue(query, 1);
		query.setParameter(1, new Integer(2));
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.find("some query string", new Object[] {"myvalue1", new Integer(2)});
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindWithNamedParameter() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.setParameter("myparam", "myvalue");
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.findByNamedParam("some query string", "myparam", "myvalue");
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindWithNamedParameters() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.setParameter("myparam1", "myvalue1");
		queryControl.setReturnValue(query, 1);
		query.setParameter("myparam2", new Integer(2));
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.findByNamedParam("some query string",
				new String[] {"myparam1", "myparam2"},
				new Object[] {"myvalue1", new Integer(2)});
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindByValueBean() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		TestBean tb = new TestBean();
		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.setProperties(tb);
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.findByValueBean("some query string", tb);
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindByNamedQuery() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.findByNamedQuery("some query name");
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindByNamedQueryWithParameter() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query, 1);
		query.setParameter(0, "myvalue");
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.findByNamedQuery("some query name", "myvalue");
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindByNamedQueryWithParameters() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query, 1);
		query.setParameter(0, "myvalue1");
		queryControl.setReturnValue(query, 1);
		query.setParameter(1, new Integer(2));
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.findByNamedQuery("some query name", new Object[] {"myvalue1", new Integer(2)});
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindByNamedQueryWithNamedParameter() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query, 1);
		query.setParameter("myparam", "myvalue");
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.findByNamedQueryAndNamedParam("some query name", "myparam", "myvalue");
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindByNamedQueryWithNamedParameters() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query, 1);
		query.setParameter("myparam1", "myvalue1");
		queryControl.setReturnValue(query, 1);
		query.setParameter("myparam2", new Integer(2));
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.findByNamedQueryAndNamedParam("some query name",
				new String[] {"myparam1", "myparam2"},
				new Object[] {"myvalue1", new Integer(2)});
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindByNamedQueryAndValueBean() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		TestBean tb = new TestBean();
		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query, 1);
		query.setProperties(tb);
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		List result = ht.findByNamedQueryAndValueBean("some query name", tb);
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindWithCacheable() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.setCacheable(true);
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setCacheQueries(true);
		List result = ht.find("some query string");
		assertTrue("Correct list", result == list);
		sfControl.verify();
	}

	public void testFindWithCacheableAndCacheRegion() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.setCacheable(true);
		queryControl.setReturnValue(query, 1);
		query.setCacheRegion("myCacheRegion");
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setCacheQueries(true);
		ht.setQueryCacheRegion("myCacheRegion");
		List result = ht.find("some query string");
		assertTrue("Correct list", result == list);
		sfControl.verify();
	}

	public void testFindByNamedQueryWithCacheable() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query, 1);
		query.setCacheable(true);
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setCacheQueries(true);
		List result = ht.findByNamedQuery("some query name");
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testFindByNamedQueryWithCacheableAndCacheRegion() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		List list = new ArrayList();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getNamedQuery("some query name");
		sessionControl.setReturnValue(query, 1);
		query.setCacheable(true);
		queryControl.setReturnValue(query, 1);
		query.setCacheRegion("myCacheRegion");
		queryControl.setReturnValue(query, 1);
		query.list();
		queryControl.setReturnValue(list, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setCacheQueries(true);
		ht.setQueryCacheRegion("myCacheRegion");
		List result = ht.findByNamedQuery("some query name");
		assertTrue("Correct list", result == list);
		queryControl.verify();
	}

	public void testIterate() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		Iterator it = Collections.EMPTY_LIST.iterator();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.iterate();
		queryControl.setReturnValue(it, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		Iterator result = ht.iterate("some query string");
		assertTrue("Correct list", result == it);
		queryControl.verify();
	}

	public void testIterateWithParameter() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		Iterator it = Collections.EMPTY_LIST.iterator();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.setParameter(0, "myvalue");
		queryControl.setReturnValue(query, 1);
		query.iterate();
		queryControl.setReturnValue(it, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		Iterator result = ht.iterate("some query string", "myvalue");
		assertTrue("Correct list", result == it);
		queryControl.verify();
	}

	public void testIterateWithParameters() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		Iterator it = Collections.EMPTY_LIST.iterator();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.setParameter(0, "myvalue1");
		queryControl.setReturnValue(query, 1);
		query.setParameter(1, new Integer(2));
		queryControl.setReturnValue(query, 1);
		query.iterate();
		queryControl.setReturnValue(it, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		Iterator result = ht.iterate("some query string",
				new Object[] {"myvalue1", new Integer(2)});
		assertTrue("Correct list", result == it);
		sfControl.verify();
	}

	public void testBulkUpdate() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.executeUpdate();
		queryControl.setReturnValue(5, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		int result = ht.bulkUpdate("some query string");
		assertTrue("Correct list", result == 5);
		queryControl.verify();
	}

	public void testBulkUpdateWithParameter() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.setParameter(0, "myvalue");
		queryControl.setReturnValue(query, 1);
		query.executeUpdate();
		queryControl.setReturnValue(5, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		int result = ht.bulkUpdate("some query string", "myvalue");
		assertTrue("Correct list", result == 5);
		queryControl.verify();
	}

	public void testBulkUpdateWithParameters() throws HibernateException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		query.setParameter(0, "myvalue1");
		queryControl.setReturnValue(query, 1);
		query.setParameter(1, new Integer(2));
		queryControl.setReturnValue(query, 1);
		query.executeUpdate();
		queryControl.setReturnValue(5, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		HibernateTemplate ht = new HibernateTemplate(sf);
		int result = ht.bulkUpdate("some query string",
				new Object[] {"myvalue1", new Integer(2)});
		assertTrue("Correct list", result == 5);
		queryControl.verify();
	}

	public void testExceptions() throws HibernateException {
		SQLException sqlEx = new SQLException("argh", "27");

		final JDBCConnectionException jcex = new JDBCConnectionException("mymsg", sqlEx);
		try {
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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
			createTemplate().execute(new HibernateCallback() {
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

	public void testFallbackExceptionTranslation() throws HibernateException {
		SQLException sqlEx = new SQLException("argh", "27");

		final GenericJDBCException gjex = new GenericJDBCException("mymsg", sqlEx);
		try {
			createTemplate().execute(new HibernateCallback() {
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

	private HibernateTemplate createTemplate() throws HibernateException {
		sfControl.reset();
		sessionControl.reset();
		sf.openSession();
		sfControl.setReturnValue(session);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();
		return new HibernateTemplate(sf);
	}

	@Override
	protected void tearDown() {
		try {
			sfControl.verify();
			sessionControl.verify();
		}
		catch (IllegalStateException ex) {
			// ignore: test method didn't call replay
		}
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

}

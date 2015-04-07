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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @since 3.2
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class HibernateTransactionManagerTests {

	@After
	public void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	@Test
	public void testTransactionCommit() throws Exception {
		final DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		final ImplementingSession session = mock(ImplementingSession.class);
		Transaction tx = mock(Transaction.class);
		Query query = mock(Query.class);

		final List list = new ArrayList();
		list.add("test");
		given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
		given(sf.openSession()).willReturn(session);
		given(session.getTransaction()).willReturn(tx);
		given(session.connection()).willReturn(con);
		given(session.isOpen()).willReturn(true);
		given(session.createQuery("some query string")).willReturn(query);
		given(query.list()).willReturn(list);
		given(session.isConnected()).willReturn(true);

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean() {
			@Override
			protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
				return sf;
			}
		};
		lsfb.afterPropertiesSet();
		final SessionFactory sfProxy = lsfb.getObject();

		HibernateTransactionManager tm = new HibernateTransactionManager();
		tm.setSessionFactory(sfProxy);
		tm.setDataSource(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		tt.setTimeout(10);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sfProxy));
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sfProxy)).getSession();
				return session.createQuery("some query string").list();
			}
		});
		assertTrue("Correct result list", result == list);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		verify(tx).setTimeout(10);
		verify(tx).begin();
		verify(tx).commit();
		verify(session).close();
	}

	@Test
	public void testTransactionRollback() throws Exception {
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		ImplementingSession session = mock(ImplementingSession.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);
		given(session.isOpen()).willReturn(true);
		given(session.isConnected()).willReturn(true);
		given(session.connection()).willReturn(con);

		PlatformTransactionManager tm = new HibernateTransactionManager(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					throw new RuntimeException("application exception");
				}
			});
			fail("Should have thrown RuntimeException");
		}
		catch (RuntimeException ex) {
			// expected
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		verify(session).close();
		verify(tx).rollback();
	}

	@Test
	public void testTransactionRollbackOnly() throws Exception {
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		ImplementingSession session = mock(ImplementingSession.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.isConnected()).willReturn(true);
		given(session.connection()).willReturn(con);

		PlatformTransactionManager tm = new HibernateTransactionManager(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				session.flush();
				status.setRollbackOnly();
				return null;
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		verify(session).flush();
		verify(session).close();
		verify(tx).rollback();
	}

	@Test
	public void testParticipatingTransactionWithCommit() throws Exception {
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		final ImplementingSession session = mock(ImplementingSession.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.isConnected()).willReturn(true);
		given(session.connection()).willReturn(con);

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean() {
			@Override
			protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
				return sf;
			}
		};
		lsfb.afterPropertiesSet();
		final SessionFactory sfProxy = lsfb.getObject();

		PlatformTransactionManager tm = new HibernateTransactionManager(sfProxy);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				return tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
						session.flush();
						return l;
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		verify(session).flush();
		verify(session).close();
		verify(tx).commit();
	}

	@Test
	public void testParticipatingTransactionWithRollback() throws Exception {
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		ImplementingSession session = mock(ImplementingSession.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.isConnected()).willReturn(true);
		given(session.connection()).willReturn(con);

		PlatformTransactionManager tm = new HibernateTransactionManager(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					return tt.execute(new TransactionCallback() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							throw new RuntimeException("application exception");
						}
					});
				}
			});
			fail("Should have thrown RuntimeException");
		}
		catch (RuntimeException ex) {
			// expected
		}

		verify(session).close();
		verify(tx).rollback();
	}

	@Test
	public void testParticipatingTransactionWithRollbackOnly() throws Exception {
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		ImplementingSession session = mock(ImplementingSession.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);
		given(session.isOpen()).willReturn(true);
		given(session.isConnected()).willReturn(true);
		given(session.connection()).willReturn(con);

		PlatformTransactionManager tm = new HibernateTransactionManager(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					return tt.execute(new TransactionCallback() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							status.setRollbackOnly();
							return null;
						}
					});
				}
			});
			fail("Should have thrown UnexpectedRollbackException");
		}
		catch (UnexpectedRollbackException ex) {
			// expected
		}

		verify(session).close();
		verify(tx).rollback();
	}

	@Test
	public void testParticipatingTransactionWithRequiresNew() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		ImplementingSession session1 = mock(ImplementingSession.class);
		ImplementingSession session2 = mock(ImplementingSession.class);
		Connection con = mock(Connection.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session1, session2);
		given(session1.beginTransaction()).willReturn(tx);
		given(session1.isOpen()).willReturn(true);
		given(session2.beginTransaction()).willReturn(tx);
		given(session2.isOpen()).willReturn(true);
		given(session2.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session1.isConnected()).willReturn(true);
		given(session1.connection()).willReturn(con);
		given(session2.isConnected()).willReturn(true);
		given(session2.connection()).willReturn(con);

		PlatformTransactionManager tm = new HibernateTransactionManager(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertTrue("Has thread session", holder != null);
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
						assertTrue("Not enclosing session", session != holder.getSession());
						session.flush();
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						return null;
					}
				});
				assertTrue("Same thread session as before",
						holder.getSession() == ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				return null;
			}
		});
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		verify(session2).flush();
		verify(session1).close();
		verify(session2).close();
		verify(tx, times(2)).commit();
	}

	@Test
	public void testParticipatingTransactionWithNotSupported() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		ImplementingSession session = mock(ImplementingSession.class);
		Connection con = mock(Connection.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.beginTransaction()).willReturn(tx);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session.isConnected()).willReturn(true);
		given(session.connection()).willReturn(con);

		HibernateTransactionManager tm = new HibernateTransactionManager(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertTrue("Has thread session", holder != null);
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
				tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
						return null;
					}
				});
				assertTrue("Same thread session as before",
						holder.getSession() == ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				return null;
			}
		});
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		verify(session).close();
		verify(tx).commit();
	}

	@Test
	public void testTransactionWithPropagationSupports() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.getFlushMode()).willReturn(FlushMode.MANUAL);

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean() {
			@Override
			protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
				return sf;
			}
		};
		lsfb.afterPropertiesSet();
		final SessionFactory sfProxy = lsfb.getObject();

		PlatformTransactionManager tm = new HibernateTransactionManager(sfProxy);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				Session session = sf.openSession();
				session.flush();
				session.close();
				return null;
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
		InOrder ordered = inOrder(session);
		ordered.verify(session).flush();
		ordered.verify(session).close();
	}

	@Test
	public void testTransactionWithPropagationSupportsAndCurrentSession() throws Exception {
		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final Session session = mock(Session.class);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.getFlushMode()).willReturn(FlushMode.MANUAL);

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean() {
			@Override
			protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
				return sf;
			}
		};
		lsfb.afterPropertiesSet();
		final SessionFactory sfProxy = lsfb.getObject();

		PlatformTransactionManager tm = new HibernateTransactionManager(sfProxy);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				Session session = new SpringSessionContext(sf).currentSession();
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sfProxy));
				session.flush();
				return null;
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
		InOrder ordered = inOrder(session);
		ordered.verify(session).flush();
		ordered.verify(session).close();
	}

	@Test
	public void testTransactionWithPropagationSupportsAndInnerTransaction() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final ImplementingSession session1 = mock(ImplementingSession.class);
		final ImplementingSession session2 = mock(ImplementingSession.class);
		Connection con = mock(Connection.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session1, session2);
		given(session1.getSessionFactory()).willReturn(sf);
		given(session1.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session2.beginTransaction()).willReturn(tx);
		given(session2.connection()).willReturn(con);
		given(session2.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session2.isOpen()).willReturn(true);
		given(session2.isConnected()).willReturn(true);

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean() {
			@Override
			protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
				return sf;
			}
		};
		lsfb.afterPropertiesSet();
		final SessionFactory sfProxy = lsfb.getObject();

		PlatformTransactionManager tm = new HibernateTransactionManager(sfProxy);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		final TransactionTemplate tt2 = new TransactionTemplate(tm);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sfProxy));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				Session session = sfProxy.openSession();
				assertSame(session1, session);
				tt2.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
						assertSame(session2, session);
						session.flush();
						return null;
					}
				});
				session.flush();
				session.close();
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				return null;
			}
		});
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		verify(session1).flush();
		verify(session1).close();
		verify(session2).flush();
		verify(session2).close();
		verify(tx).commit();
	}

	@Test
	public void testTransactionCommitWithEntityInterceptor() throws Exception {
		Interceptor entityInterceptor = mock(Interceptor.class);
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		ImplementingSession session = mock(ImplementingSession.class);
		SessionBuilder options = mock(SessionBuilder.class);
		Transaction tx = mock(Transaction.class);

		given(sf.withOptions()).willReturn(options);
		given(options.interceptor(entityInterceptor)).willReturn(options);
		given(options.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);
		given(session.isOpen()).willReturn(true);
		given(session.isConnected()).willReturn(true);
		given(session.connection()).willReturn(con);

		HibernateTransactionManager tm = new HibernateTransactionManager(sf);
		tm.setEntityInterceptor(entityInterceptor);
		tm.setAllowResultAccessAfterCompletion(true);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(session).close();
		verify(tx).commit();
	}

	@Test
	public void testTransactionCommitWithEntityInterceptorBeanName() throws Exception {
		Interceptor entityInterceptor = mock(Interceptor.class);
		Interceptor entityInterceptor2 = mock(Interceptor.class);
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		ImplementingSession session = mock(ImplementingSession.class);
		SessionBuilder options = mock(SessionBuilder.class);
		Transaction tx = mock(Transaction.class);

		given(sf.withOptions()).willReturn(options);
		given(options.interceptor(entityInterceptor)).willReturn(options);
		given(options.interceptor(entityInterceptor2)).willReturn(options);
		given(options.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);
		given(session.isOpen()).willReturn(true);
		given(session.isConnected()).willReturn(true);
		given(session.connection()).willReturn(con);

		BeanFactory beanFactory = mock(BeanFactory.class);
		given(beanFactory.getBean("entityInterceptor", Interceptor.class)).willReturn(
				entityInterceptor, entityInterceptor2);

		HibernateTransactionManager tm = new HibernateTransactionManager(sf);
		tm.setEntityInterceptorBeanName("entityInterceptor");
		tm.setBeanFactory(beanFactory);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		for (int i = 0; i < 2; i++) {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				public void doInTransactionWithoutResult(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				}
			});
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(session, times(2)).close();
		verify(tx, times(2)).commit();
	}

	@Test
	public void testTransactionCommitWithReadOnly() throws Exception {
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		ImplementingSession session = mock(ImplementingSession.class);
		Transaction tx = mock(Transaction.class);
		Query query = mock(Query.class);

		final List list = new ArrayList();
		list.add("test");
		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);
		given(session.connection()).willReturn(con);
		given(session.isOpen()).willReturn(true);
		given(session.createQuery("some query string")).willReturn(query);
		given(query.list()).willReturn(list);
		given(session.isConnected()).willReturn(true);
		given(con.isReadOnly()).willReturn(true);

		HibernateTransactionManager tm = new HibernateTransactionManager(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setReadOnly(true);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				return session.createQuery("some query string").list();
			}
		});
		assertTrue("Correct result list", result == list);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(con).setReadOnly(true);
		verify(tx).commit();
		verify(con).setReadOnly(false);
		verify(session).close();
	}

	@Test
	public void testTransactionCommitWithFlushFailure() throws Exception {
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		ImplementingSession session = mock(ImplementingSession.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);
		given(session.isOpen()).willReturn(true);
		SQLException sqlEx = new SQLException("argh", "27");
		Exception rootCause = null;
		ConstraintViolationException jdbcEx = new ConstraintViolationException("mymsg", sqlEx, null);
		rootCause = jdbcEx;
		willThrow(jdbcEx).given(tx).commit();
		given(session.isConnected()).willReturn(true);
		given(session.connection()).willReturn(con);

		HibernateTransactionManager tm = new HibernateTransactionManager(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					return l;
				}
			});
			fail("Should have thrown DataIntegrityViolationException");
		}
		catch (DataIntegrityViolationException ex) {
			// expected
			assertEquals(rootCause, ex.getCause());
			assertTrue(ex.getMessage().contains("mymsg"));
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(session).close();
		verify(tx).rollback();
	}

	@Test
	public void testTransactionCommitWithPreBound() throws Exception {
		final DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		final ImplementingSession session = mock(ImplementingSession.class);
		Transaction tx = mock(Transaction.class);

		given(session.beginTransaction()).willReturn(tx);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.MANUAL);
		given(session.connection()).willReturn(con);
		given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
		given(session.isConnected()).willReturn(true);

		HibernateTransactionManager tm = new HibernateTransactionManager();
		tm.setSessionFactory(sf);
		tm.setDataSource(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
				SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertTrue("Has thread transaction", sessionHolder.getTransaction() != null);
				Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				assertEquals(session, sess);
				return l;
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
		assertTrue("Hasn't thread transaction", sessionHolder.getTransaction() == null);
		TransactionSynchronizationManager.unbindResource(sf);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		InOrder ordered = inOrder(session, con);
		ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		ordered.verify(session).setFlushMode(FlushMode.AUTO);
		ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		ordered.verify(session).setFlushMode(FlushMode.MANUAL);
		verify(tx).commit();
		verify(session).disconnect();
	}

	@Test
	public void testTransactionCommitWithPreBoundAndResultAccessAfterCommit() throws Exception {
		final DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		final ImplementingSession session = mock(ImplementingSession.class);
		Transaction tx = mock(Transaction.class);

		given(session.beginTransaction()).willReturn(tx);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.MANUAL);
		given(session.connection()).willReturn(con);
		given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
		given(con.getHoldability()).willReturn(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		given(session.isConnected()).willReturn(true);

		HibernateTransactionManager tm = new HibernateTransactionManager();
		tm.setSessionFactory(sf);
		tm.setDataSource(ds);
		tm.setAllowResultAccessAfterCompletion(true);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
				SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertTrue("Has thread transaction", sessionHolder.getTransaction() != null);
				Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				assertEquals(session, sess);
				return l;
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
		assertTrue("Hasn't thread transaction", sessionHolder.getTransaction() == null);
		TransactionSynchronizationManager.unbindResource(sf);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		InOrder ordered = inOrder(session, con);
		ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		ordered.verify(con).setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
		ordered.verify(session).setFlushMode(FlushMode.AUTO);
		ordered.verify(con).setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		ordered.verify(session).setFlushMode(FlushMode.MANUAL);
		verify(tx).commit();
	}

	@Test
	public void testTransactionRollbackWithPreBound() throws Exception {
		final DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		final SessionFactory sf = mock(SessionFactory.class);
		final ImplementingSession session = mock(ImplementingSession.class);
		final Transaction tx1 = mock(Transaction.class);
		final Transaction tx2 = mock(Transaction.class);

		given(session.beginTransaction()).willReturn(tx1, tx2);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.MANUAL);
		given(session.isConnected()).willReturn(true);
		given(session.connection()).willReturn(con);

		HibernateTransactionManager tm = new HibernateTransactionManager();
		tm.setSessionFactory(sf);
		tm.setDataSource(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				public void doInTransactionWithoutResult(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
					SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
					assertEquals(tx1, sessionHolder.getTransaction());
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						public void doInTransactionWithoutResult(TransactionStatus status) {
							status.setRollbackOnly();
							Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
							assertEquals(session, sess);
						}
					});
				}
			});
			fail("Should have thrown UnexpectedRollbackException");
		}
		catch (UnexpectedRollbackException ex) {
			// expected
		}

		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
		assertTrue("Hasn't thread transaction", sessionHolder.getTransaction() == null);
		assertTrue("Not marked rollback-only", !sessionHolder.isRollbackOnly());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
				SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertEquals(tx2, sessionHolder.getTransaction());
				Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				assertEquals(session, sess);
			}
		});

		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
		assertTrue("Hasn't thread transaction", sessionHolder.getTransaction() == null);
		TransactionSynchronizationManager.unbindResource(sf);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(tx1).rollback();
		verify(tx2).commit();
		InOrder ordered = inOrder(session);
		ordered.verify(session).clear();
		ordered.verify(session).setFlushMode(FlushMode.AUTO);
		ordered.verify(session).setFlushMode(FlushMode.MANUAL);
		ordered.verify(session).disconnect();
	}

	@Test
	public void testTransactionRollbackWithHibernateManagedSession() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		final Transaction tx1 = mock(Transaction.class);
		final Transaction tx2 = mock(Transaction.class);

		given(sf.getCurrentSession()).willReturn(session);
		given(session.isOpen()).willReturn(true);
		given(session.getTransaction()).willReturn(tx1, tx2);
		given(session.beginTransaction()).willReturn(tx1, tx2);
		given(session.getFlushMode()).willReturn(FlushMode.MANUAL);

		HibernateTransactionManager tm = new HibernateTransactionManager();
		tm.setSessionFactory(sf);
		tm.setPrepareConnection(false);
		tm.setHibernateManagedSession(true);
		final TransactionTemplate tt = new TransactionTemplate(tm);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				public void doInTransactionWithoutResult(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						public void doInTransactionWithoutResult(TransactionStatus status) {
							status.setRollbackOnly();
							Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
							assertEquals(session, sess);
						}
					});
				}
			});
			fail("Should have thrown UnexpectedRollbackException");
		}
		catch (UnexpectedRollbackException ex) {
			// expected
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				Session sess = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				assertEquals(session, sess);
			}
		});

		verify(tx1).rollback();
		verify(tx2).commit();
		InOrder ordered = inOrder(session);
		ordered.verify(session).setFlushMode(FlushMode.AUTO);
		ordered.verify(session).setFlushMode(FlushMode.MANUAL);
	}

	@Test
	public void testExistingTransactionWithPropagationNestedAndRollback() throws Exception {
		doTestExistingTransactionWithPropagationNestedAndRollback(false);
	}

	@Test
	public void testExistingTransactionWithManualSavepointAndRollback() throws Exception {
		doTestExistingTransactionWithPropagationNestedAndRollback(true);
	}

	private void doTestExistingTransactionWithPropagationNestedAndRollback(final boolean manualSavepoint)
			throws Exception {

		final DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		Savepoint sp = mock(Savepoint.class);
		final SessionFactory sf = mock(SessionFactory.class);
		ImplementingSession session = mock(ImplementingSession.class);
		Transaction tx = mock(Transaction.class);
		Query query = mock(Query.class);

		final List list = new ArrayList();
		list.add("test");
		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);
		given(session.connection()).willReturn(con);
		given(session.isOpen()).willReturn(true);
		given(md.supportsSavepoints()).willReturn(true);
		given(con.getMetaData()).willReturn(md);
		given(con.setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + 1)).willReturn(sp);
		given(session.createQuery("some query string")).willReturn(query);
		given(query.list()).willReturn(list);
		given(session.isConnected()).willReturn(true);

		HibernateTransactionManager tm = new HibernateTransactionManager();
		tm.setNestedTransactionAllowed(true);
		tm.setSessionFactory(sf);
		tm.setDataSource(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
				if (manualSavepoint) {
					Object savepoint = status.createSavepoint();
					status.rollbackToSavepoint(savepoint);
				}
				else {
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
							status.setRollbackOnly();
						}
					});
				}
				Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				return session.createQuery("some query string").list();
			}
		});
		assertTrue("Correct result list", result == list);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(con).setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + 1);
		verify(con).rollback(sp);
		verify(session).close();
		verify(tx).commit();
	}

	@Test
	public void testTransactionCommitWithNonExistingDatabase() throws Exception {
		final DriverManagerDataSource ds = new DriverManagerDataSource();
		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setDataSource(ds);
		Properties props = new Properties();
		props.setProperty("hibernate.dialect", HSQLDialect.class.getName());
		lsfb.setHibernateProperties(props);
		lsfb.afterPropertiesSet();
		final SessionFactory sf = lsfb.getObject();

		HibernateTransactionManager tm = new HibernateTransactionManager();
		tm.setSessionFactory(sf);
		tm.afterPropertiesSet();
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		tt.setTimeout(10);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
					Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
					return session.createQuery("from java.lang.Object").list();
				}
			});
			fail("Should have thrown CannotCreateTransactionException");
		}
		catch (CannotCreateTransactionException ex) {
			// expected
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void testTransactionCommitWithPreBoundSessionAndNonExistingDatabase() throws Exception {
		final DriverManagerDataSource ds = new DriverManagerDataSource();
		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setDataSource(ds);
		Properties props = new Properties();
		props.setProperty("hibernate.dialect", HSQLDialect.class.getName());
		lsfb.setHibernateProperties(props);
		lsfb.afterPropertiesSet();
		final SessionFactory sf = lsfb.getObject();

		HibernateTransactionManager tm = new HibernateTransactionManager();
		tm.setSessionFactory(sf);
		tm.afterPropertiesSet();
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		tt.setTimeout(10);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Session session = sf.openSession();
		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
					Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
					return session.createQuery("from java.lang.Object").list();
				}
			});
			fail("Should have thrown CannotCreateTransactionException");
		}
		catch (CannotCreateTransactionException ex) {
			// expected
			SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
			assertFalse(holder.isSynchronizedWithTransaction());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
			session.close();
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void testTransactionCommitWithNonExistingDatabaseAndLazyConnection() throws Exception {
		DriverManagerDataSource dsTarget = new DriverManagerDataSource();
		final LazyConnectionDataSourceProxy ds = new LazyConnectionDataSourceProxy();
		ds.setTargetDataSource(dsTarget);
		ds.setDefaultAutoCommit(true);
		ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		//ds.setDefaultTransactionIsolationName("TRANSACTION_READ_COMMITTED");

		LocalSessionFactoryBean lsfb = new LocalSessionFactoryBean();
		lsfb.setDataSource(ds);
		Properties props = new Properties();
		props.setProperty("hibernate.dialect", HSQLDialect.class.getName());
		props.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
		lsfb.setHibernateProperties(props);
		lsfb.afterPropertiesSet();
		final SessionFactory sf = lsfb.getObject();

		HibernateTransactionManager tm = new HibernateTransactionManager();
		tm.setSessionFactory(sf);
		tm.afterPropertiesSet();
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		tt.setTimeout(10);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
				Session session = ((SessionHolder) TransactionSynchronizationManager.getResource(sf)).getSession();
				return session.createQuery("from java.lang.Object").list();
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void testTransactionFlush() throws Exception {
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		Transaction tx = mock(Transaction.class);

		given(sf.openSession()).willReturn(session);
		given(session.beginTransaction()).willReturn(tx);

		HibernateTransactionManager tm = new HibernateTransactionManager(sf);
		tm.setPrepareConnection(false);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				status.flush();
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(session).flush();
		verify(tx).commit();
		verify(session).close();
	}

	@Test
	public void testSetJtaTransactionManager() throws Exception {
		DataSource ds = mock(DataSource.class);
		TransactionManager tm = mock(TransactionManager.class);
		UserTransaction ut = mock(UserTransaction.class);
		TransactionSynchronizationRegistry tsr = mock(TransactionSynchronizationRegistry.class);
		JtaTransactionManager jtm = new JtaTransactionManager();
		jtm.setTransactionManager(tm);
		jtm.setUserTransaction(ut);
		jtm.setTransactionSynchronizationRegistry(tsr);
		LocalSessionFactoryBuilder lsfb = new LocalSessionFactoryBuilder(ds);
		lsfb.setJtaTransactionManager(jtm);
		Object jtaPlatform = lsfb.getProperties().get(AvailableSettings.JTA_PLATFORM);
		assertNotNull(jtaPlatform);
		assertSame(tm, jtaPlatform.getClass().getMethod("retrieveTransactionManager").invoke(jtaPlatform));
		assertSame(ut, jtaPlatform.getClass().getMethod("retrieveUserTransaction").invoke(jtaPlatform));
		assertTrue(lsfb.getProperties().get(AvailableSettings.TRANSACTION_STRATEGY) instanceof CMTTransactionFactory);
	}

	@Test
	public void testSetTransactionManager() throws Exception {
		DataSource ds = mock(DataSource.class);
		TransactionManager tm = mock(TransactionManager.class);
		LocalSessionFactoryBuilder lsfb = new LocalSessionFactoryBuilder(ds);
		lsfb.setJtaTransactionManager(tm);
		Object jtaPlatform = lsfb.getProperties().get(AvailableSettings.JTA_PLATFORM);
		assertNotNull(jtaPlatform);
		assertSame(tm, jtaPlatform.getClass().getMethod("retrieveTransactionManager").invoke(jtaPlatform));
		assertTrue(lsfb.getProperties().get(AvailableSettings.TRANSACTION_STRATEGY) instanceof CMTTransactionFactory);
	}


	public interface ImplementingSession extends Session, SessionImplementor {
	}

}

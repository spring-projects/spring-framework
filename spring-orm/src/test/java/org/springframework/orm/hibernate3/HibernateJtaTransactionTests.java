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

import java.util.ArrayList;
import java.util.List;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.junit.After;
import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataAccessException;
import org.springframework.tests.transaction.MockJtaTransaction;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
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
 * @author Phillip Webb
 * @since 05.03.2005
 */
public class HibernateJtaTransactionTests {

	@After
	public void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	@Test
	public void testJtaTransactionCommit() throws Exception {
		doTestJtaTransactionCommit(Status.STATUS_NO_TRANSACTION, false);
	}

	@Test
	public void testJtaTransactionCommitWithReadOnly() throws Exception {
		doTestJtaTransactionCommit(Status.STATUS_NO_TRANSACTION, true);
	}

	@Test
	public void testJtaTransactionCommitWithExisting() throws Exception {
		doTestJtaTransactionCommit(Status.STATUS_ACTIVE, false);
	}

	@Test
	public void testJtaTransactionCommitWithExistingAndReadOnly() throws Exception {
		doTestJtaTransactionCommit(Status.STATUS_ACTIVE, true);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void doTestJtaTransactionCommit(int status, final boolean readOnly) throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		Query query = mock(Query.class);
		if (status == Status.STATUS_NO_TRANSACTION) {
			given(ut.getStatus()).willReturn(status, Status.STATUS_ACTIVE);
		}
		else {
			given(ut.getStatus()).willReturn(status);
		}

		final List list = new ArrayList();
		list.add("test");
		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.isOpen()).willReturn(true);
		given(session.createQuery("some query string")).willReturn(query);
		given(query.list()).willReturn(list);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setReadOnly(readOnly);
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		Object result = tt.execute(new TransactionCallback() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					assertTrue("JTA synchronizations active",
						TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
					HibernateTemplate ht = new HibernateTemplate(sf);
					ht.setExposeNativeSession(true);
					ht.executeFind(new HibernateCallback() {

						@Override
						public Object doInHibernate(org.hibernate.Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return null;
						}
					});
					ht = new HibernateTemplate(sf);
					List htl = ht.executeFind(new HibernateCallback() {

						@Override
						public Object doInHibernate(org.hibernate.Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							return sess.createQuery("some query string").list();
						}
					});
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					return htl;
				}
				catch (Error err) {
					err.printStackTrace();
					throw err;
				}
			}
		});

		assertTrue("Correct result list", result == list);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		if (status == Status.STATUS_NO_TRANSACTION) {
			InOrder ordered = inOrder(ut);
			ordered.verify(ut).begin();
			ordered.verify(ut).commit();
		}

		if (readOnly) {
			verify(session).setFlushMode(FlushMode.MANUAL);
		} else {
			verify(session).flush();
		}
		verify(session).close();
	}

	@Test
	public void testJtaTransactionCommitWithJtaTm() throws Exception {
		doTestJtaTransactionCommitWithJtaTm(Status.STATUS_NO_TRANSACTION);
	}

	@Test
	public void testJtaTransactionCommitWithJtaTmAndExisting() throws Exception {
		doTestJtaTransactionCommitWithJtaTm(Status.STATUS_ACTIVE);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void doTestJtaTransactionCommitWithJtaTm(int status) throws Exception {

		UserTransaction ut = mock(UserTransaction.class);
		if (status == Status.STATUS_NO_TRANSACTION) {
			given(ut.getStatus()).willReturn(status, status, Status.STATUS_ACTIVE);
		} else {
			given(ut.getStatus()).willReturn(status);
		}

		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getTransaction()).willReturn(transaction);

		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final Session session = mock(Session.class);
		given(sf.getTransactionManager()).willReturn(tm);
		given(sf.openSession()).willReturn(session);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		Object result = tt.execute(new TransactionCallback() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					assertTrue("JTA synchronizations active",
						TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
					HibernateTemplate ht = new HibernateTemplate(sf);
					ht.setExposeNativeSession(true);
					List htl = ht.executeFind(new HibernateCallback() {

						@Override
						public Object doInHibernate(org.hibernate.Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return l;
						}
					});
					ht = new HibernateTemplate(sf);
					ht.setExposeNativeSession(true);
					htl = ht.executeFind(new HibernateCallback() {

						@Override
						public Object doInHibernate(org.hibernate.Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return l;
						}
					});
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					return htl;
				}
				catch (Error err) {
					err.printStackTrace();
					throw err;
				}
			}
		});

		assertTrue("Correct result list", result == l);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		if (status == Status.STATUS_NO_TRANSACTION) {
			InOrder ordered = inOrder(ut);
			ordered.verify(ut).begin();
			ordered.verify(ut).commit();
		}
		verify(session).flush();
		verify(session).close();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testJtaTransactionWithFlushFailure() throws Exception {

		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		final List l = new ArrayList();
		l.add("test");
		final HibernateException flushEx = new HibernateException("flush failure");
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		willThrow(flushEx).given(session).flush();

		try {
			tt.execute(new TransactionCallback() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					try {
						assertTrue("JTA synchronizations active",
							TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
						HibernateTemplate ht = new HibernateTemplate(sf);
						ht.setExposeNativeSession(true);
						List htl = ht.executeFind(new HibernateCallback() {

							@Override
							public Object doInHibernate(org.hibernate.Session sess) {
								assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
								assertEquals(session, sess);
								return l;
							}
						});
						ht = new HibernateTemplate(sf);
						ht.setExposeNativeSession(true);
						htl = ht.executeFind(new HibernateCallback() {

							@Override
							public Object doInHibernate(org.hibernate.Session sess) {
								assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
								assertEquals(session, sess);
								return l;
							}
						});
						assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
						return htl;
					}
					catch (Error err) {
						err.printStackTrace();
						throw err;
					}
				}
			});
		}
		catch (DataAccessException ex) {
			// expected
			assertTrue(flushEx == ex.getCause());
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).begin();
		verify(ut).rollback();
		verify(session).close();
	}

	@Test
	public void testJtaTransactionRollback() throws Exception {
		doTestJtaTransactionRollback(false);
	}

	@Test
	public void testJtaTransactionRollbackWithFlush() throws Exception {
		doTestJtaTransactionRollback(true);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void doTestJtaTransactionRollback(final boolean flush) throws Exception {

		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					assertTrue("JTA synchronizations active",
						TransactionSynchronizationManager.isSynchronizationActive());
					HibernateTemplate ht = new HibernateTemplate(sf);
					List htl = ht.executeFind(new HibernateCallback() {

						@Override
						public Object doInHibernate(org.hibernate.Session session) {
							return l;
						}
					});
					if (flush) {
						status.flush();
					}
					status.setRollbackOnly();
					return htl;
				}
				catch (Error err) {
					err.printStackTrace();
					throw err;
				}
			}
		});
		assertTrue("Correct result list", result == l);
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		InOrder ordered = inOrder(ut);
		ordered.verify(ut).begin();
		ordered.verify(ut).rollback();
		if (flush) {
			verify(session).flush();
		}
		verify(session).close();
	}

	@Test
	public void testJtaTransactionCommitWithPreBound() throws Exception {
		doTestJtaTransactionCommitWithPreBound(false, false, false);
	}

	@Test
	public void testJtaTransactionCommitWithPreBoundAndReadOnly() throws Exception {
		doTestJtaTransactionCommitWithPreBound(false, false, true);
	}

	@Test
	public void testJtaTransactionCommitWithPreBoundAndFlushModeNever() throws Exception {
		doTestJtaTransactionCommitWithPreBound(false, true, false);
	}

	@Test
	public void testJtaTransactionCommitWithPreBoundAndFlushModeNeverAndReadOnly() throws Exception {
		doTestJtaTransactionCommitWithPreBound(false, true, true);
	}

	@Test
	public void testJtaTransactionCommitWithJtaTmAndPreBound() throws Exception {
		doTestJtaTransactionCommitWithPreBound(true, false, false);
	}

	@Test
	public void testJtaTransactionCommitWithJtaTmAndPreBoundAndReadOnly() throws Exception {
		doTestJtaTransactionCommitWithPreBound(true, false, true);
	}

	@Test
	public void testJtaTransactionCommitWithJtaTmAndPreBoundAndFlushModeNever() throws Exception {
		doTestJtaTransactionCommitWithPreBound(true, true, false);
	}

	@Test
	public void testJtaTransactionCommitWithJtaTmAndPreBoundAndFlushModeNeverAndReadOnly() throws Exception {
		doTestJtaTransactionCommitWithPreBound(true, true, true);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void doTestJtaTransactionCommitWithPreBound(boolean jtaTm, final boolean flushNever,
			final boolean readOnly) throws Exception {


		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		TransactionManager tm = mock(TransactionManager.class);
		if (jtaTm) {
			MockJtaTransaction transaction = new MockJtaTransaction();
			given(tm.getTransaction()).willReturn(transaction);
		}

		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final ExtendedSession session = mock(ExtendedSession.class);
		given(sf.getTransactionManager()).willReturn(jtaTm ? tm : null);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(flushNever ? FlushMode.MANUAL: FlushMode.AUTO);

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		try {
			JtaTransactionManager ptm = new JtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.setReadOnly(readOnly);
			final List l = new ArrayList();
			l.add("test");
			assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
			assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));

			Object result = tt.execute(new TransactionCallback() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					try {
						assertTrue("JTA synchronizations active",
							TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
						HibernateTemplate ht = new HibernateTemplate(sf);
						ht.setExposeNativeSession(true);
						List htl = null;
						for (int i = 0; i < 5; i++) {
							htl = ht.executeFind(new HibernateCallback() {

								@Override
								public Object doInHibernate(org.hibernate.Session sess) {
									assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
									assertEquals(session, sess);
									return l;
								}
							});
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
						}
						return htl;
					}
					catch (Error err) {
						err.printStackTrace();
						throw err;
					}
				}
			});

			assertTrue("Correct result list", result == l);
			assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
			assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}

		verify(ut).begin();
		verify(ut).commit();

		if (flushNever) {
			if(!readOnly) {
				InOrder ordered = inOrder(session);
				ordered.verify(session).setFlushMode(FlushMode.AUTO);
				ordered.verify(session).setFlushMode(FlushMode.MANUAL);
			}
		}
		if(!flushNever && !readOnly) {
			verify(session).flush();
		}
		verify(session).afterTransactionCompletion(true, null);
		verify(session).disconnect();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testJtaTransactionRollbackWithPreBound() throws Exception {

		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE,
				Status.STATUS_MARKED_ROLLBACK, Status.STATUS_MARKED_ROLLBACK);
		RollbackException rex = new RollbackException();
		willThrow(rex).given(ut).commit();

		final SessionFactory sf = mock(SessionFactory.class);

		final Session session = mock(Session.class);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		try {
			JtaTransactionManager ptm = new JtaTransactionManager(ut);
			final TransactionTemplate tt = new TransactionTemplate(ptm);
			assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
			assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));

			tt.execute(new TransactionCallbackWithoutResult() {

				@Override
				public void doInTransactionWithoutResult(TransactionStatus status) {
					tt.execute(new TransactionCallbackWithoutResult() {

						@Override
						public void doInTransactionWithoutResult(TransactionStatus status) {
							status.setRollbackOnly();
							try {
								assertTrue("JTA synchronizations active",
									TransactionSynchronizationManager.isSynchronizationActive());
								assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
								HibernateTemplate ht = new HibernateTemplate(sf);
								ht.setExposeNativeSession(true);
								for (int i = 0; i < 5; i++) {
									ht.execute(new HibernateCallback() {

										@Override
										public Object doInHibernate(org.hibernate.Session sess) {
											assertTrue("Has thread session",
												TransactionSynchronizationManager.hasResource(sf));
											assertEquals(session, sess);
											return null;
										}
									});
									assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
								}
							}
							catch (Error err) {
								err.printStackTrace();
								throw err;
							}
						}
					});
				}
			});
			fail("Should have thrown UnexpectedRollbackException");
		}
		catch (UnexpectedRollbackException ex) {
			// expected
			assertEquals(rex, ex.getCause());
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}

		verify(ut).begin();
		verify(ut).setRollbackOnly();
		verify(session).flush();
		verify(session).disconnect();
		verify(session).clear();
	}

	@Test
	public void testJtaTransactionCommitWithRequiresNew() throws Exception {
		doTestJtaTransactionWithRequiresNew(false);
	}

	@Test
	public void testJtaTransactionRollbackWithRequiresNew() throws Exception {
		doTestJtaTransactionWithRequiresNew(true);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void doTestJtaTransactionWithRequiresNew(final boolean rollback) throws Exception {

		UserTransaction ut = mock(UserTransaction.class);

		TransactionManager tm = mock(TransactionManager.class);
		javax.transaction.Transaction tx1 = mock(javax.transaction.Transaction.class);

		final SessionFactory sf = mock(SessionFactory.class);
		final Session session1 = mock(Session.class);
		final Session session2 = mock(Session.class);

		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		given(tm.suspend()).willReturn(tx1);
		given(sf.openSession()).willReturn(session1, session2);
		given(session1.getSessionFactory()).willReturn(sf);
		given(session2.getSessionFactory()).willReturn(sf);
		given(session1.isOpen()).willReturn(true);
		given(session2.isOpen()).willReturn(true);
		given(session2.getFlushMode()).willReturn(FlushMode.AUTO);
		if (!rollback) {
			given(session1.getFlushMode()).willReturn(FlushMode.AUTO);
		}

		JtaTransactionManager ptm = new JtaTransactionManager();
		ptm.setUserTransaction(ut);
		ptm.setTransactionManager(tm);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		try {
			tt.execute(new TransactionCallback() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					org.hibernate.Session outerSession = SessionFactoryUtils.getSession(sf, false);
					assertSame(session1, outerSession);
					SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
					assertTrue("Has thread session", holder != null);
					try {
						tt.execute(new TransactionCallback() {

							@Override
							public Object doInTransaction(TransactionStatus status) {
								org.hibernate.Session innerSession = SessionFactoryUtils.getSession(sf, false);
								assertSame(session2, innerSession);
								HibernateTemplate ht = new HibernateTemplate(sf);
								ht.setFlushMode(HibernateTemplate.FLUSH_EAGER);
								return ht.executeFind(new HibernateCallback() {

									@Override
									public Object doInHibernate(org.hibernate.Session innerSession) {
										if (rollback) {
											throw new HibernateException("");
										}
										return null;
									}
								});
							}
						});
						return null;
					}
					finally {
						assertTrue("Same thread session as before",
							outerSession == SessionFactoryUtils.getSession(sf, false));
					}
				}
			});
			if (rollback) {
				fail("Should have thrown DataAccessException");
			}
		}
		catch (DataAccessException ex) {
			if (!rollback) {
				throw ex;
			}
		}
		finally {
			assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		}

		verify(ut, times(2)).begin();
		verify(tm).resume(tx1);
		if (rollback) {
			verify(ut, times(2)).rollback();
		}
		else {
			verify(ut, times(2)).commit();
		}
		verify(session1).disconnect();
		verify(session1).close();
		if(!rollback) {
			verify(session1).flush();
			verify(session2, atLeastOnce()).flush();
		}
		verify(session2).close();
	}

	@Test
	public void testJtaTransactionWithRequiresNewAndSuspendException() throws Exception {
		doTestJtaTransactionWithRequiresNewAndException(true);
	}

	@Test
	public void testJtaTransactionWithRequiresNewAndBeginException() throws Exception {
		doTestJtaTransactionWithRequiresNewAndException(false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void doTestJtaTransactionWithRequiresNewAndException(boolean suspendException) throws Exception {

		UserTransaction ut = mock(UserTransaction.class);

		TransactionManager tm = mock(TransactionManager.class);
		javax.transaction.Transaction tx = mock(javax.transaction.Transaction.class);

		final SessionFactory sf = mock(SessionFactory.class);
		final Session session1 = mock(Session.class);

		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		if (suspendException) {
			given(tm.suspend()).willThrow(new SystemException());
		}
		else {
			given(tm.suspend()).willReturn(tx);
			willDoNothing().willThrow(new SystemException()).given(ut).begin();
		}

		given(sf.openSession()).willReturn(session1);
		given(session1.getSessionFactory()).willReturn(sf);

		JtaTransactionManager ptm = new JtaTransactionManager();
		ptm.setUserTransaction(ut);
		ptm.setTransactionManager(tm);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		try {
			tt.execute(new TransactionCallback() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					org.hibernate.Session outerSession = SessionFactoryUtils.getSession(sf, false);
					assertSame(session1, outerSession);
					SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
					assertTrue("Has thread session", holder != null);
					tt.execute(new TransactionCallback() {

						@Override
						public Object doInTransaction(TransactionStatus status) {
							return null;
						}
					});
					return null;
				}
			});
			fail("Should have thrown TransactionException");
		}
		catch (TransactionException ex) {
			// expected
		}
		finally {
			assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		}

		verify(ut, atLeastOnce()).begin();
		if(!suspendException) {
			verify(tm).resume(tx);
		}
		verify(ut).rollback();
		verify(session1).disconnect();
		verify(session1).close();
	}

	@Test
	public void testJtaTransactionCommitWithRequiresNewAndJtaTm() throws Exception {
		doTestJtaTransactionWithRequiresNewAndJtaTm(false);
	}

	@Test
	public void testJtaTransactionRollbackWithRequiresNewAndJtaTm() throws Exception {
		doTestJtaTransactionWithRequiresNewAndJtaTm(true);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void doTestJtaTransactionWithRequiresNewAndJtaTm(final boolean rollback) throws Exception {

		UserTransaction ut = mock(UserTransaction.class);

		TransactionManager tm = mock(TransactionManager.class);
		javax.transaction.Transaction tx1 = mock(javax.transaction.Transaction.class);

		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final Session session1 = mock(Session.class);
		final Session session2 = mock(Session.class);

		MockJtaTransaction transaction1 = new MockJtaTransaction();
		MockJtaTransaction transaction2 = new MockJtaTransaction();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(transaction1);
		given(tm.suspend()).willReturn(tx1);
		given(tm.getTransaction()).willReturn(transaction2);
		given(sf.getTransactionManager()).willReturn(tm);
		given(sf.openSession()).willReturn(session1, session2);
		given(session1.isOpen()).willReturn(true);
		given(session2.isOpen()).willReturn(true);
		given(session1.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session2.getFlushMode()).willReturn(FlushMode.AUTO);

		JtaTransactionManager ptm = new JtaTransactionManager();
		ptm.setUserTransaction(ut);
		ptm.setTransactionManager(tm);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		try {
			tt.execute(new TransactionCallback() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					org.hibernate.Session outerSession = SessionFactoryUtils.getSession(sf, false);
					assertSame(session1, outerSession);
					SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
					assertTrue("Has thread session", holder != null);
					try {
						tt.execute(new TransactionCallback() {

							@Override
							public Object doInTransaction(TransactionStatus status) {
								org.hibernate.Session innerSession = SessionFactoryUtils.getSession(sf, false);
								assertSame(session2, innerSession);
								HibernateTemplate ht = new HibernateTemplate(sf);
								ht.setFlushMode(HibernateTemplate.FLUSH_EAGER);
								return ht.executeFind(new HibernateCallback() {

									@Override
									public Object doInHibernate(org.hibernate.Session innerSession) {
										if (rollback) {
											throw new HibernateException("");
										}
										return null;
									}
								});
							}
						});
						return null;
					}
					finally {
						assertTrue("Same thread session as before",
							outerSession == SessionFactoryUtils.getSession(sf, false));
					}
				}
			});
			if (rollback) {
				fail("Should have thrown DataAccessException");
			}
		}
		catch (DataAccessException ex) {
			if (!rollback) {
				throw ex;
			}
		}
		finally {
			assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		}

		verify(ut, times(2)).begin();
		verify(tm).resume(tx1);
		if (rollback) {
			verify(ut, times(2)).rollback();
		}
		else {
			verify(ut, times(2)).commit();
			verify(session1).flush();
			verify(session2, times(2)).flush();
		}
		verify(session1).disconnect();
		verify(session1).close();
		verify(session2).close();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testTransactionWithPropagationSupports() throws Exception {

		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.getFlushMode()).willReturn(FlushMode.MANUAL);

		JtaTransactionManager tm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		tt.execute(new TransactionCallback() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				HibernateTemplate ht = new HibernateTemplate(sf);
				ht.setFlushMode(HibernateTemplate.FLUSH_EAGER);
				ht.execute(new HibernateCallback() {

					@Override
					public Object doInHibernate(org.hibernate.Session session) {
						return null;
					}
				});
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				return null;
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		InOrder ordered = inOrder(session);
		ordered.verify(session).setFlushMode(FlushMode.AUTO);
		ordered.verify(session).flush();
		ordered.verify(session).setFlushMode(FlushMode.MANUAL);
		ordered.verify(session).close();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testTransactionWithPropagationSupportsAndInnerTransaction() throws Exception {

		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);

		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sf);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		JtaTransactionManager tm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		final TransactionTemplate tt2 = new TransactionTemplate(tm);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		tt.execute(new TransactionCallback() {

			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				HibernateTemplate ht = new HibernateTemplate(sf);
				ht.setFlushMode(HibernateTemplate.FLUSH_EAGER);
				ht.execute(new HibernateCallback() {

					@Override
					public Object doInHibernate(org.hibernate.Session session) {
						return null;
					}
				});
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				tt2.execute(new TransactionCallback() {

					@Override
					public Object doInTransaction(TransactionStatus status) {
						HibernateTemplate ht = new HibernateTemplate(sf);
						ht.setFlushMode(HibernateTemplate.FLUSH_EAGER);
						return ht.executeFind(new HibernateCallback() {

							@Override
							public Object doInHibernate(org.hibernate.Session session) {
								assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
								// assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
								return null;
							}
						});
					}
				});
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				return null;
			}
		});
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		verify(session, times(3)).flush();
		verify(session).close();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronization() throws Exception {

		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getTransaction()).willReturn(transaction);
		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final Session session = mock(Session.class);
		given(sf.openSession()).willReturn(session);
		given(sf.getTransactionManager()).willReturn(tm);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 5; i++) {
			ht.executeFind(new HibernateCallback() {

				@Override
				public Object doInHibernate(org.hibernate.Session sess) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					assertEquals(session, sess);
					return null;
				}
			});
		}

		Synchronization synchronization = transaction.getSynchronization();
		assertTrue("JTA synchronization registered", synchronization != null);
		synchronization.beforeCompletion();
		synchronization.afterCompletion(Status.STATUS_COMMITTED);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(session).flush();
		verify(session).close();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithRollback() throws Exception {

		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getTransaction()).willReturn(transaction);
		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final Session session = mock(Session.class);
		given(sf.openSession()).willReturn(session);
		given(sf.getTransactionManager()).willReturn(tm);
		given(session.isOpen()).willReturn(true);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 5; i++) {
			ht.executeFind(new HibernateCallback() {

				@Override
				public Object doInHibernate(org.hibernate.Session sess) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					assertEquals(session, sess);
					return null;
				}
			});
		}

		Synchronization synchronization = transaction.getSynchronization();
		assertTrue("JTA synchronization registered", synchronization != null);
		synchronization.afterCompletion(Status.STATUS_ROLLEDBACK);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(session).close();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithRollbackByOtherThread() throws Exception {

		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getTransaction()).willReturn(transaction);
		given(tm.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);
		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final Session session = mock(Session.class);
		given(sf.openSession()).willReturn(session);
		given(sf.getTransactionManager()).willReturn(tm);
		given(session.isOpen()).willReturn(true);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		final HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 5; i++) {
			ht.executeFind(new HibernateCallback() {

				@Override
				public Object doInHibernate(org.hibernate.Session sess) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					assertEquals(session, sess);
					return null;
				}
			});
		}

		final Synchronization synchronization = transaction.getSynchronization();
		assertTrue("JTA synchronization registered", synchronization != null);
		Thread thread = new Thread() {

			@Override
			public void run() {
				synchronization.afterCompletion(Status.STATUS_ROLLEDBACK);
			}
		};
		thread.start();
		thread.join();

		assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		TransactionTemplate tt = new TransactionTemplate(new JtaTransactionManager(tm));
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		tt.setReadOnly(true);
		tt.execute(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
				for (int i = 0; i < 5; i++) {
					ht.executeFind(new HibernateCallback() {

						@Override
						public Object doInHibernate(org.hibernate.Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return null;
						}
					});
				}
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(session).setFlushMode(FlushMode.MANUAL);
		verify(session, times(2)).close();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithFlushFailure() throws Exception {

		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getTransaction()).willReturn(transaction);
		final HibernateException flushEx = new HibernateException("flush failure");
		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final Session session = mock(Session.class);
		given(sf.openSession()).willReturn(session);
		given(sf.getTransactionManager()).willReturn(tm);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		willThrow(flushEx).given(session).flush();

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 5; i++) {
			ht.executeFind(new HibernateCallback() {

				@Override
				public Object doInHibernate(org.hibernate.Session sess) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					assertEquals(session, sess);
					return null;
				}
			});
		}

		Synchronization synchronization = transaction.getSynchronization();
		assertTrue("JTA synchronization registered", synchronization != null);
		try {
			synchronization.beforeCompletion();
			fail("Should have thrown HibernateSystemException");
		}
		catch (HibernateSystemException ex) {
			assertSame(flushEx, ex.getCause());
		}
		synchronization.afterCompletion(Status.STATUS_ROLLEDBACK);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(tm).setRollbackOnly();
		verify(session).close();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithSuspendedTransaction() throws Exception {

		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction1 = new MockJtaTransaction();
		MockJtaTransaction transaction2 = new MockJtaTransaction();
		given(tm.getTransaction()).willReturn(transaction1, transaction1, transaction2, transaction2,
				transaction2);

		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final Session session1 = mock(Session.class);
		final Session session2 = mock(Session.class);
		given(sf.openSession()).willReturn(session1, session2);
		given(sf.getTransactionManager()).willReturn(tm);
		given(session1.getFlushMode()).willReturn(FlushMode.AUTO);
		given(session2.getFlushMode()).willReturn(FlushMode.AUTO);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		ht.executeFind(new HibernateCallback() {

			@Override
			public Object doInHibernate(org.hibernate.Session sess) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertEquals(session1, sess);
				return null;
			}
		});
		ht.executeFind(new HibernateCallback() {

			@Override
			public Object doInHibernate(org.hibernate.Session sess) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertEquals(session2, sess);
				return null;
			}
		});

		Synchronization synchronization2 = transaction2.getSynchronization();
		assertTrue("JTA synchronization registered", synchronization2 != null);
		synchronization2.beforeCompletion();
		synchronization2.afterCompletion(Status.STATUS_COMMITTED);

		Synchronization synchronization1 = transaction1.getSynchronization();
		assertTrue("JTA synchronization registered", synchronization1 != null);
		synchronization1.beforeCompletion();
		synchronization1.afterCompletion(Status.STATUS_COMMITTED);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(session1).flush();
		verify(session2).flush();
		verify(session1).close();
		verify(session2).close();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithNonSessionFactoryImplementor() throws Exception {

		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getTransaction()).willReturn(transaction);
		final SessionFactory sf = mock(SessionFactory.class);
		final Session session = mock(Session.class);
		final SessionFactoryImplementor sfi = mock(SessionFactoryImplementor.class);
		given(sf.openSession()).willReturn(session);
		given(session.getSessionFactory()).willReturn(sfi);
		given(sfi.getTransactionManager()).willReturn(tm);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 5; i++) {
			ht.executeFind(new HibernateCallback() {

				@Override
				public Object doInHibernate(org.hibernate.Session sess) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					assertEquals(session, sess);
					return null;
				}
			});
		}

		Synchronization synchronization = transaction.getSynchronization();
		assertTrue("JTA Synchronization registered", synchronization != null);
		synchronization.beforeCompletion();
		synchronization.afterCompletion(Status.STATUS_COMMITTED);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(session).flush();
		verify(session).close();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithSpringTransactionLaterOn() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);

		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(transaction);
		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final Session session = mock(Session.class);
		given(sf.openSession()).willReturn(session);
		given(sf.getTransactionManager()).willReturn(tm);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		final HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 2; i++) {
			ht.executeFind(new HibernateCallback() {

				@Override
				public Object doInHibernate(org.hibernate.Session sess) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					assertEquals(session, sess);
					return null;
				}
			});
		}

		TransactionTemplate tt = new TransactionTemplate(new JtaTransactionManager(ut));
		tt.execute(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				for (int i = 2; i < 5; i++) {
					ht.executeFind(new HibernateCallback() {

						@Override
						public Object doInHibernate(org.hibernate.Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return null;
						}
					});
				}
			}
		});

		Synchronization synchronization = transaction.getSynchronization();
		assertTrue("JTA synchronization registered", synchronization != null);
		synchronization.beforeCompletion();
		synchronization.afterCompletion(Status.STATUS_COMMITTED);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		verify(session).flush();
		verify(session).close();
	}

	@Test
	public void testJtaSessionSynchronizationWithPreBound() throws Exception {
		doTestJtaSessionSynchronizationWithPreBound(false);
	}

	@Test
	public void testJtaJtaSessionSynchronizationWithPreBoundAndFlushNever() throws Exception {
		doTestJtaSessionSynchronizationWithPreBound(true);
	}

	@SuppressWarnings("rawtypes")
	private void doTestJtaSessionSynchronizationWithPreBound(boolean flushNever) throws Exception {

		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		given(tm.getTransaction()).willReturn(transaction);
		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final Session session = mock(Session.class);
		given(sf.getTransactionManager()).willReturn(tm);
		given(session.isOpen()).willReturn(true);
		if (flushNever) {
			given(session.getFlushMode()).willReturn(FlushMode.MANUAL, FlushMode.AUTO, FlushMode.MANUAL);
		}
		else {
			given(session.getFlushMode()).willReturn(FlushMode.AUTO);
		}

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		try {
			assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
			HibernateTemplate ht = new HibernateTemplate(sf);
			ht.setExposeNativeSession(true);
			for (int i = 0; i < 5; i++) {
				ht.executeFind(new HibernateCallback() {

					@Override
					public Object doInHibernate(org.hibernate.Session sess) {
						assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
						assertEquals(session, sess);
						return null;
					}
				});
			}

			Synchronization synchronization = transaction.getSynchronization();
			assertTrue("JTA synchronization registered", synchronization != null);
			synchronization.beforeCompletion();
			synchronization.afterCompletion(Status.STATUS_COMMITTED);
			assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
		}
		finally {
			TransactionSynchronizationManager.unbindResource(sf);
		}
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		InOrder ordered = inOrder(session);
		if(flushNever) {
			ordered.verify(session).setFlushMode(FlushMode.AUTO);
			ordered.verify(session).setFlushMode(FlushMode.MANUAL);
		} else {
			ordered.verify(session).flush();
		}
		ordered.verify(session).disconnect();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithRemoteTransaction() throws Exception {

		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction transaction = new MockJtaTransaction();
		final SessionFactoryImplementor sf = mock(SessionFactoryImplementor.class);
		final Session session = mock(Session.class);
		given(tm.getTransaction()).willReturn(transaction);
		given(sf.openSession()).willReturn(session);
		given(sf.getTransactionManager()).willReturn(tm);
		given(session.isOpen()).willReturn(true);
		given(session.getFlushMode()).willReturn(FlushMode.AUTO);

		for (int j = 0; j < 2; j++) {

			if (j == 0) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
			}
			else {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
			}

			HibernateTemplate ht = new HibernateTemplate(sf);
			ht.setExposeNativeSession(true);
			for (int i = 0; i < 5; i++) {
				ht.executeFind(new HibernateCallback() {

					@Override
					public Object doInHibernate(org.hibernate.Session sess) {
						assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
						assertEquals(session, sess);
						return null;
					}
				});
			}

			final Synchronization synchronization = transaction.getSynchronization();
			assertTrue("JTA synchronization registered", synchronization != null);

			// Call synchronization in a new thread, to simulate a
			// synchronization
			// triggered by a new remote call from a remote transaction
			// coordinator.
			Thread synch = new Thread() {

				@Override
				public void run() {
					synchronization.beforeCompletion();
					synchronization.afterCompletion(Status.STATUS_COMMITTED);
				}
			};
			synch.start();
			synch.join();

			assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
			SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
			assertTrue("Thread session holder empty", sessionHolder.isEmpty());
			assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		}

		verify(session, times(2)).flush();
		verify(session, times(2)).close();
		TransactionSynchronizationManager.unbindResource(sf);
	}

	/**
	 * Interface that combines Hibernate's Session and SessionImplementor
	 * interface. Necessary for creating a mock that implements both interfaces.
	 * Note: Hibernate 3.1's SessionImplementor interface does not extend
	 * Session anymore.
	 */
	public static interface ExtendedSession extends Session, SessionImplementor {

	}

}

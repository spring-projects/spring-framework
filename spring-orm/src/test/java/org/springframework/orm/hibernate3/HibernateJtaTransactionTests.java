/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.easymock.MockControl;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.MockJtaTransaction;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Juergen Hoeller
 * @since 05.03.2005
 */
@SuppressWarnings("deprecation")
public class HibernateJtaTransactionTests {

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
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		final MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();

		ut.getStatus();
		utControl.setReturnValue(status, 1);
		if (status == Status.STATUS_NO_TRANSACTION) {
			ut.begin();
			utControl.setVoidCallable(1);
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
			ut.commit();
			utControl.setVoidCallable(1);
		}
		else {
			ut.getStatus();
			utControl.setReturnValue(status, 1);
		}

		final List list = new ArrayList();
		list.add("test");
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 1);
		session.createQuery("some query string");
		sessionControl.setReturnValue(query, 1);
		if (readOnly) {
			session.setFlushMode(FlushMode.MANUAL);
			sessionControl.setVoidCallable(1);
		}
		query.list();
		queryControl.setReturnValue(list, 1);

		utControl.replay();
		sfControl.replay();
		sessionControl.replay();
		queryControl.replay();

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setReadOnly(readOnly);
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		Object result = tt.execute(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {
				try {
					assertTrue("JTA synchronizations active",
						TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
					HibernateTemplate ht = new HibernateTemplate(sf);
					ht.setExposeNativeSession(true);
					ht.executeFind(new HibernateCallback() {

						public Object doInHibernate(org.hibernate.Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return null;
						}
					});
					ht = new HibernateTemplate(sf);
					List htl = ht.executeFind(new HibernateCallback() {

						public Object doInHibernate(org.hibernate.Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							return sess.createQuery("some query string").list();
						}
					});
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					sessionControl.verify();
					queryControl.verify();
					sessionControl.reset();
					if (!readOnly) {
						session.getFlushMode();
						sessionControl.setReturnValue(FlushMode.AUTO, 1);
						session.flush();
						sessionControl.setVoidCallable(1);
					}
					session.close();
					sessionControl.setReturnValue(null, 1);
					sessionControl.replay();
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

		utControl.verify();
		sfControl.verify();
		sessionControl.verify();
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
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(status, 2);
		if (status == Status.STATUS_NO_TRANSACTION) {
			ut.begin();
			utControl.setVoidCallable(1);
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
			ut.commit();
			utControl.setVoidCallable(1);
		}

		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 6);

		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		sf.getTransactionManager();
		sfControl.setReturnValue(tm, 1);
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 1);

		utControl.replay();
		tmControl.replay();
		sfControl.replay();
		sessionControl.replay();

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		Object result = tt.execute(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {
				try {
					assertTrue("JTA synchronizations active",
						TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
					HibernateTemplate ht = new HibernateTemplate(sf);
					ht.setExposeNativeSession(true);
					List htl = ht.executeFind(new HibernateCallback() {

						public Object doInHibernate(org.hibernate.Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return l;
						}
					});
					ht = new HibernateTemplate(sf);
					ht.setExposeNativeSession(true);
					htl = ht.executeFind(new HibernateCallback() {

						public Object doInHibernate(org.hibernate.Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return l;
						}
					});
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					sessionControl.verify();
					sessionControl.reset();
					session.getFlushMode();
					sessionControl.setReturnValue(FlushMode.AUTO, 1);
					session.flush();
					sessionControl.setVoidCallable(1);
					session.close();
					sessionControl.setReturnValue(null, 1);
					sessionControl.replay();
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

		utControl.verify();
		sfControl.verify();
		sessionControl.verify();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testJtaTransactionWithFlushFailure() throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		ut.rollback();
		utControl.setVoidCallable(1);
		utControl.replay();

		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 1);
		sfControl.replay();
		sessionControl.replay();

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		final List l = new ArrayList();
		l.add("test");
		final HibernateException flushEx = new HibernateException("flush failure");
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		try {
			tt.execute(new TransactionCallback() {

				public Object doInTransaction(TransactionStatus status) {
					try {
						assertTrue("JTA synchronizations active",
							TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
						HibernateTemplate ht = new HibernateTemplate(sf);
						ht.setExposeNativeSession(true);
						List htl = ht.executeFind(new HibernateCallback() {

							public Object doInHibernate(org.hibernate.Session sess) {
								assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
								assertEquals(session, sess);
								return l;
							}
						});
						ht = new HibernateTemplate(sf);
						ht.setExposeNativeSession(true);
						htl = ht.executeFind(new HibernateCallback() {

							public Object doInHibernate(org.hibernate.Session sess) {
								assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
								assertEquals(session, sess);
								return l;
							}
						});
						assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
						sessionControl.verify();
						sessionControl.reset();
						session.getFlushMode();
						sessionControl.setReturnValue(FlushMode.AUTO, 1);
						session.flush();
						sessionControl.setThrowable(flushEx);
						session.close();
						sessionControl.setReturnValue(null, 1);
						sessionControl.replay();
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

		utControl.verify();
		sfControl.verify();
		sessionControl.verify();
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
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		ut.rollback();
		utControl.setVoidCallable(1);
		utControl.replay();

		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		if (flush) {
			session.flush();
			sessionControl.setVoidCallable(1);
		}
		sfControl.replay();
		sessionControl.replay();

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {
				try {
					assertTrue("JTA synchronizations active",
						TransactionSynchronizationManager.isSynchronizationActive());
					HibernateTemplate ht = new HibernateTemplate(sf);
					List htl = ht.executeFind(new HibernateCallback() {

						public Object doInHibernate(org.hibernate.Session session) {
							return l;
						}
					});
					if (flush) {
						status.flush();
					}
					status.setRollbackOnly();
					sessionControl.verify();
					sessionControl.reset();
					session.close();
					sessionControl.setReturnValue(null, 1);
					sessionControl.replay();
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
		utControl.verify();
		sfControl.verify();
		sessionControl.verify();
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

		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.commit();
		utControl.setVoidCallable(1);

		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		if (jtaTm) {
			MockJtaTransaction transaction = new MockJtaTransaction();
			tm.getTransaction();
			tmControl.setReturnValue(transaction, 1);
		}

		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(ExtendedSession.class);
		final ExtendedSession session = (ExtendedSession) sessionControl.getMock();
		sf.getTransactionManager();
		sfControl.setReturnValue((jtaTm ? tm : null), 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 5);
		session.getFlushMode();
		if (flushNever) {
			sessionControl.setReturnValue(FlushMode.MANUAL, 1);
			if (!readOnly) {
				session.setFlushMode(FlushMode.AUTO);
				sessionControl.setVoidCallable(1);
			}
		}
		else {
			sessionControl.setReturnValue(FlushMode.AUTO, 1);
		}

		utControl.replay();
		tmControl.replay();
		sfControl.replay();
		sessionControl.replay();

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

								public Object doInHibernate(org.hibernate.Session sess) {
									assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
									assertEquals(session, sess);
									return l;
								}
							});
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
						}
						sessionControl.verify();
						sessionControl.reset();
						if (!readOnly) {
							session.getFlushMode();
							sessionControl.setReturnValue(FlushMode.AUTO, 1);
							session.flush();
							sessionControl.setVoidCallable(1);
							if (flushNever) {
								session.setFlushMode(FlushMode.MANUAL);
								sessionControl.setVoidCallable(1);
							}
						}
						session.afterTransactionCompletion(true, null);
						sessionControl.setVoidCallable(1);
						session.disconnect();
						sessionControl.setReturnValue(null, 1);
						sessionControl.replay();
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

		utControl.verify();
		tmControl.verify();
		sfControl.verify();
		sessionControl.verify();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testJtaTransactionRollbackWithPreBound() throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_MARKED_ROLLBACK, 2);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.setRollbackOnly();
		utControl.setVoidCallable(1);
		RollbackException rex = new RollbackException();
		ut.commit();
		utControl.setThrowable(rex, 1);
		utControl.replay();

		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 5);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO, 1);
		sfControl.replay();
		sessionControl.replay();

		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		try {
			JtaTransactionManager ptm = new JtaTransactionManager(ut);
			final TransactionTemplate tt = new TransactionTemplate(ptm);
			assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
			assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));

			tt.execute(new TransactionCallbackWithoutResult() {

				public void doInTransactionWithoutResult(TransactionStatus status) {
					tt.execute(new TransactionCallbackWithoutResult() {

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

										public Object doInHibernate(org.hibernate.Session sess) {
											assertTrue("Has thread session",
												TransactionSynchronizationManager.hasResource(sf));
											assertEquals(session, sess);
											return null;
										}
									});
									assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
								}
								sessionControl.verify();
								sessionControl.reset();
								session.getFlushMode();
								sessionControl.setReturnValue(FlushMode.AUTO, 1);
								session.flush();
								sessionControl.setVoidCallable(1);
								session.disconnect();
								sessionControl.setReturnValue(null, 1);
								session.clear();
								sessionControl.setVoidCallable(1);
								sessionControl.replay();
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

		utControl.verify();
		sfControl.verify();
		sessionControl.verify();
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
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockControl tx1Control = MockControl.createControl(javax.transaction.Transaction.class);
		javax.transaction.Transaction tx1 = (javax.transaction.Transaction) tx1Control.getMock();
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl session1Control = MockControl.createControl(Session.class);
		final Session session1 = (Session) session1Control.getMock();
		MockControl session2Control = MockControl.createControl(Session.class);
		final Session session2 = (Session) session2Control.getMock();

		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 3);
		ut.begin();
		utControl.setVoidCallable(2);
		tm.suspend();
		tmControl.setReturnValue(tx1, 1);
		tm.resume(tx1);
		tmControl.setVoidCallable(1);
		if (rollback) {
			ut.rollback();
		}
		else {
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
			ut.commit();
		}
		utControl.setVoidCallable(2);

		sf.openSession();
		sfControl.setReturnValue(session1, 1);
		sf.openSession();
		sfControl.setReturnValue(session2, 1);
		session1.getSessionFactory();
		session1Control.setReturnValue(sf, 1);
		session2.getSessionFactory();
		session2Control.setReturnValue(sf, 1);
		session1.isOpen();
		session1Control.setReturnValue(true, 1);
		session2.isOpen();
		session2Control.setReturnValue(true, 1);
		session2.getFlushMode();
		session2Control.setReturnValue(FlushMode.AUTO, 1);
		if (!rollback) {
			session1.getFlushMode();
			session1Control.setReturnValue(FlushMode.AUTO, 1);
			session2.getFlushMode();
			session2Control.setReturnValue(FlushMode.AUTO, 1);
			session1.flush();
			session1Control.setVoidCallable(1);
			session2.flush();
			session2Control.setVoidCallable(2);
		}
		session1.disconnect();
		session1Control.setReturnValue(null, 1);
		session1.close();
		session1Control.setReturnValue(null, 1);
		session2.close();
		session2Control.setReturnValue(null, 1);

		utControl.replay();
		tmControl.replay();
		sfControl.replay();
		session1Control.replay();
		session2Control.replay();

		JtaTransactionManager ptm = new JtaTransactionManager();
		ptm.setUserTransaction(ut);
		ptm.setTransactionManager(tm);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		try {
			tt.execute(new TransactionCallback() {

				public Object doInTransaction(TransactionStatus status) {
					org.hibernate.Session outerSession = SessionFactoryUtils.getSession(sf, false);
					assertSame(session1, outerSession);
					SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
					assertTrue("Has thread session", holder != null);
					try {
						tt.execute(new TransactionCallback() {

							public Object doInTransaction(TransactionStatus status) {
								org.hibernate.Session innerSession = SessionFactoryUtils.getSession(sf, false);
								assertSame(session2, innerSession);
								HibernateTemplate ht = new HibernateTemplate(sf);
								ht.setFlushMode(HibernateTemplate.FLUSH_EAGER);
								return ht.executeFind(new HibernateCallback() {

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

		utControl.verify();
		tmControl.verify();
		sfControl.verify();
		session1Control.verify();
		session2Control.verify();
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
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockControl txControl = MockControl.createControl(javax.transaction.Transaction.class);
		javax.transaction.Transaction tx = (javax.transaction.Transaction) txControl.getMock();
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl session1Control = MockControl.createControl(Session.class);
		final Session session1 = (Session) session1Control.getMock();

		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
		ut.begin();
		utControl.setVoidCallable(1);
		if (suspendException) {
			tm.suspend();
			tmControl.setThrowable(new SystemException(), 1);
		}
		else {
			tm.suspend();
			tmControl.setReturnValue(tx, 1);
			ut.begin();
			utControl.setThrowable(new SystemException(), 1);
			tm.resume(tx);
			tmControl.setVoidCallable(1);
		}
		ut.rollback();
		utControl.setVoidCallable(1);

		sf.openSession();
		sfControl.setReturnValue(session1, 1);
		session1.getSessionFactory();
		session1Control.setReturnValue(sf, 1);
		session1.disconnect();
		session1Control.setReturnValue(null, 1);
		session1.close();
		session1Control.setReturnValue(null, 1);

		utControl.replay();
		tmControl.replay();
		sfControl.replay();
		session1Control.replay();

		JtaTransactionManager ptm = new JtaTransactionManager();
		ptm.setUserTransaction(ut);
		ptm.setTransactionManager(tm);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		try {
			tt.execute(new TransactionCallback() {

				public Object doInTransaction(TransactionStatus status) {
					org.hibernate.Session outerSession = SessionFactoryUtils.getSession(sf, false);
					assertSame(session1, outerSession);
					SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
					assertTrue("Has thread session", holder != null);
					tt.execute(new TransactionCallback() {

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

		utControl.verify();
		tmControl.verify();
		sfControl.verify();
		session1Control.verify();
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
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockControl tx1Control = MockControl.createControl(javax.transaction.Transaction.class);
		javax.transaction.Transaction tx1 = (javax.transaction.Transaction) tx1Control.getMock();
		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		MockControl session1Control = MockControl.createControl(Session.class);
		final Session session1 = (Session) session1Control.getMock();
		MockControl session2Control = MockControl.createControl(Session.class);
		final Session session2 = (Session) session2Control.getMock();

		MockJtaTransaction transaction1 = new MockJtaTransaction();
		MockJtaTransaction transaction2 = new MockJtaTransaction();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 3);
		ut.begin();
		utControl.setVoidCallable(2);
		tm.getTransaction();
		tmControl.setReturnValue(transaction1, 1);
		tm.suspend();
		tmControl.setReturnValue(tx1, 1);
		tm.getTransaction();
		tmControl.setReturnValue(transaction2, 1);
		tm.resume(tx1);
		tmControl.setVoidCallable(1);
		if (rollback) {
			ut.rollback();
		}
		else {
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
			ut.commit();
		}
		utControl.setVoidCallable(2);

		sf.getTransactionManager();
		sfControl.setReturnValue(tm, 2);
		sf.openSession();
		sfControl.setReturnValue(session1, 1);
		sf.openSession();
		sfControl.setReturnValue(session2, 1);
		session1.isOpen();
		session1Control.setReturnValue(true, 1);
		session2.isOpen();
		session2Control.setReturnValue(true, 1);
		session2.getFlushMode();
		session2Control.setReturnValue(FlushMode.AUTO, 1);
		if (!rollback) {
			session1.getFlushMode();
			session1Control.setReturnValue(FlushMode.AUTO, 1);
			session2.getFlushMode();
			session2Control.setReturnValue(FlushMode.AUTO, 1);
			session1.flush();
			session1Control.setVoidCallable(1);
			session2.flush();
			session2Control.setVoidCallable(2);
		}
		session1.disconnect();
		session1Control.setReturnValue(null, 1);
		session1.close();
		session1Control.setReturnValue(null, 1);
		session2.close();
		session2Control.setReturnValue(null, 1);

		utControl.replay();
		tmControl.replay();
		sfControl.replay();
		session1Control.replay();
		session2Control.replay();

		JtaTransactionManager ptm = new JtaTransactionManager();
		ptm.setUserTransaction(ut);
		ptm.setTransactionManager(tm);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		try {
			tt.execute(new TransactionCallback() {

				public Object doInTransaction(TransactionStatus status) {
					org.hibernate.Session outerSession = SessionFactoryUtils.getSession(sf, false);
					assertSame(session1, outerSession);
					SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
					assertTrue("Has thread session", holder != null);
					try {
						tt.execute(new TransactionCallback() {

							public Object doInTransaction(TransactionStatus status) {
								org.hibernate.Session innerSession = SessionFactoryUtils.getSession(sf, false);
								assertSame(session2, innerSession);
								HibernateTemplate ht = new HibernateTemplate(sf);
								ht.setFlushMode(HibernateTemplate.FLUSH_EAGER);
								return ht.executeFind(new HibernateCallback() {

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

		utControl.verify();
		tmControl.verify();
		sfControl.verify();
		session1Control.verify();
		session2Control.verify();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testTransactionWithPropagationSupports() throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		ut.rollback();
		utControl.setVoidCallable(1);
		utControl.replay();

		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.MANUAL, 1);
		session.setFlushMode(FlushMode.AUTO);
		sessionControl.setVoidCallable(1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.setFlushMode(FlushMode.MANUAL);
		sessionControl.setVoidCallable(1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.MANUAL, 1);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		JtaTransactionManager tm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		tt.execute(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				HibernateTemplate ht = new HibernateTemplate(sf);
				ht.setFlushMode(HibernateTemplate.FLUSH_EAGER);
				ht.execute(new HibernateCallback() {

					public Object doInHibernate(org.hibernate.Session session) {
						return null;
					}
				});
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				return null;
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		sfControl.verify();
		sessionControl.verify();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testTransactionWithPropagationSupportsAndInnerTransaction() throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		ut.rollback();
		utControl.setVoidCallable(1);
		utControl.replay();

		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sf, 1);
		session.isOpen();
		sessionControl.setReturnValue(true, 1);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO, 3);
		session.flush();
		sessionControl.setVoidCallable(3);
		session.close();
		sessionControl.setReturnValue(null, 1);
		sfControl.replay();
		sessionControl.replay();

		JtaTransactionManager tm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		final TransactionTemplate tt2 = new TransactionTemplate(tm);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		tt.execute(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				HibernateTemplate ht = new HibernateTemplate(sf);
				ht.setFlushMode(HibernateTemplate.FLUSH_EAGER);
				ht.execute(new HibernateCallback() {

					public Object doInHibernate(org.hibernate.Session session) {
						return null;
					}
				});
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				tt2.execute(new TransactionCallback() {

					public Object doInTransaction(TransactionStatus status) {
						HibernateTemplate ht = new HibernateTemplate(sf);
						ht.setFlushMode(HibernateTemplate.FLUSH_EAGER);
						return ht.executeFind(new HibernateCallback() {

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

		sfControl.verify();
		sessionControl.verify();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronization() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 6);

		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		sf.getTransactionManager();
		sfControl.setReturnValue(tm, 6);
		session.isOpen();
		sessionControl.setReturnValue(true, 4);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);

		tmControl.replay();
		sfControl.replay();
		sessionControl.replay();

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 5; i++) {
			ht.executeFind(new HibernateCallback() {

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

		tmControl.verify();
		sfControl.verify();
		sessionControl.verify();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithRollback() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 6);

		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		sf.getTransactionManager();
		sfControl.setReturnValue(tm, 6);
		session.isOpen();
		sessionControl.setReturnValue(true, 4);
		session.close();
		sessionControl.setReturnValue(null, 1);

		tmControl.replay();
		sfControl.replay();
		sessionControl.replay();

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 5; i++) {
			ht.executeFind(new HibernateCallback() {

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

		tmControl.verify();
		sfControl.verify();
		sessionControl.verify();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithRollbackByOtherThread() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 7);
		tm.getStatus();
		tmControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);

		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 2);
		sf.getTransactionManager();
		sfControl.setReturnValue(tm, 7);
		session.isOpen();
		sessionControl.setReturnValue(true, 8);
		session.setFlushMode(FlushMode.MANUAL);
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 2);

		tmControl.replay();
		sfControl.replay();
		sessionControl.replay();

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		final HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 5; i++) {
			ht.executeFind(new HibernateCallback() {

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

			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
				for (int i = 0; i < 5; i++) {
					ht.executeFind(new HibernateCallback() {

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

		tmControl.verify();
		sfControl.verify();
		sessionControl.verify();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithFlushFailure() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 6);
		tm.setRollbackOnly();
		tmControl.setVoidCallable(1);

		final HibernateException flushEx = new HibernateException("flush failure");
		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		sf.getTransactionManager();
		sfControl.setReturnValue(tm, 6);
		session.isOpen();
		sessionControl.setReturnValue(true, 4);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO, 1);
		session.flush();
		sessionControl.setThrowable(flushEx, 1);
		session.close();
		sessionControl.setReturnValue(null, 1);

		tmControl.replay();
		sfControl.replay();
		sessionControl.replay();

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 5; i++) {
			ht.executeFind(new HibernateCallback() {

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

		tmControl.verify();
		sfControl.verify();
		sessionControl.verify();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithSuspendedTransaction() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction1 = new MockJtaTransaction();
		MockJtaTransaction transaction2 = new MockJtaTransaction();
		tm.getTransaction();
		tmControl.setReturnValue(transaction1, 2);
		tm.getTransaction();
		tmControl.setReturnValue(transaction2, 3);

		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		final MockControl session1Control = MockControl.createControl(Session.class);
		final Session session1 = (Session) session1Control.getMock();
		final MockControl session2Control = MockControl.createControl(Session.class);
		final Session session2 = (Session) session2Control.getMock();
		sf.openSession();
		sfControl.setReturnValue(session1, 1);
		sf.openSession();
		sfControl.setReturnValue(session2, 1);
		sf.getTransactionManager();
		sfControl.setReturnValue(tm, 5);
		session1.getFlushMode();
		session1Control.setReturnValue(FlushMode.AUTO, 1);
		session2.getFlushMode();
		session2Control.setReturnValue(FlushMode.AUTO, 1);
		session1.flush();
		session1Control.setVoidCallable(1);
		session2.flush();
		session2Control.setVoidCallable(1);
		session1.close();
		session1Control.setReturnValue(null, 1);
		session2.close();
		session2Control.setReturnValue(null, 1);

		tmControl.replay();
		sfControl.replay();
		session1Control.replay();
		session2Control.replay();

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		ht.executeFind(new HibernateCallback() {

			public Object doInHibernate(org.hibernate.Session sess) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				assertEquals(session1, sess);
				return null;
			}
		});
		ht.executeFind(new HibernateCallback() {

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

		tmControl.verify();
		sfControl.verify();
		session1Control.verify();
		session2Control.verify();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithNonSessionFactoryImplementor() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 6);

		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sf = (SessionFactory) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		MockControl sfiControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sfi = (SessionFactoryImplementor) sfiControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		session.getSessionFactory();
		sessionControl.setReturnValue(sfi, 6);
		sfi.getTransactionManager();
		sfiControl.setReturnValue(tm, 6);
		session.isOpen();
		sessionControl.setReturnValue(true, 4);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);

		tmControl.replay();
		sfControl.replay();
		sessionControl.replay();
		sfiControl.replay();

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 5; i++) {
			ht.executeFind(new HibernateCallback() {

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

		tmControl.verify();
		sfControl.verify();
		sessionControl.verify();
		sfiControl.verify();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithSpringTransactionLaterOn() throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 6);

		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		sf.openSession();
		sfControl.setReturnValue(session, 1);
		sf.getTransactionManager();
		sfControl.setReturnValue(tm, 6);
		session.isOpen();
		sessionControl.setReturnValue(true, 4);
		session.getFlushMode();
		sessionControl.setReturnValue(FlushMode.AUTO, 1);
		session.flush();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setReturnValue(null, 1);

		utControl.replay();
		tmControl.replay();
		sfControl.replay();
		sessionControl.replay();

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		final HibernateTemplate ht = new HibernateTemplate(sf);
		ht.setExposeNativeSession(true);
		for (int i = 0; i < 2; i++) {
			ht.executeFind(new HibernateCallback() {

				public Object doInHibernate(org.hibernate.Session sess) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					assertEquals(session, sess);
					return null;
				}
			});
		}

		TransactionTemplate tt = new TransactionTemplate(new JtaTransactionManager(ut));
		tt.execute(new TransactionCallbackWithoutResult() {

			protected void doInTransactionWithoutResult(TransactionStatus status) {
				for (int i = 2; i < 5; i++) {
					ht.executeFind(new HibernateCallback() {

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

		utControl.verify();
		tmControl.verify();
		sfControl.verify();
		sessionControl.verify();
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
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 6);

		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		sf.getTransactionManager();
		sfControl.setReturnValue(tm, 6);
		session.isOpen();
		sessionControl.setReturnValue(true, 5);
		session.getFlushMode();
		if (flushNever) {
			sessionControl.setReturnValue(FlushMode.MANUAL, 1);
			session.setFlushMode(FlushMode.AUTO);
			sessionControl.setVoidCallable(1);
		}
		else {
			sessionControl.setReturnValue(FlushMode.AUTO, 1);
		}

		tmControl.replay();
		sfControl.replay();
		sessionControl.replay();

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
		try {
			assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
			HibernateTemplate ht = new HibernateTemplate(sf);
			ht.setExposeNativeSession(true);
			for (int i = 0; i < 5; i++) {
				ht.executeFind(new HibernateCallback() {

					public Object doInHibernate(org.hibernate.Session sess) {
						assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
						assertEquals(session, sess);
						return null;
					}
				});
			}

			sessionControl.verify();
			sessionControl.reset();
			session.getFlushMode();
			sessionControl.setReturnValue(FlushMode.AUTO, 1);
			session.flush();
			sessionControl.setVoidCallable(1);
			if (flushNever) {
				session.setFlushMode(FlushMode.MANUAL);
				sessionControl.setVoidCallable(1);
			}
			session.disconnect();
			sessionControl.setReturnValue(null, 1);
			sessionControl.replay();

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

		tmControl.verify();
		sfControl.verify();
		sessionControl.verify();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testJtaSessionSynchronizationWithRemoteTransaction() throws Exception {
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();

		MockControl sfControl = MockControl.createControl(SessionFactoryImplementor.class);
		final SessionFactoryImplementor sf = (SessionFactoryImplementor) sfControl.getMock();
		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		for (int j = 0; j < 2; j++) {
			tmControl.reset();
			sfControl.reset();
			sessionControl.reset();

			tm.getTransaction();
			tmControl.setReturnValue(transaction, 6);

			sf.openSession();
			sfControl.setReturnValue(session, 1);
			sf.getTransactionManager();
			sfControl.setReturnValue(tm, 6);
			session.isOpen();
			sessionControl.setReturnValue(true, 4);
			session.getFlushMode();
			sessionControl.setReturnValue(FlushMode.AUTO, 1);
			session.flush();
			sessionControl.setVoidCallable(1);
			session.close();
			sessionControl.setReturnValue(null, 1);

			tmControl.replay();
			sfControl.replay();
			sessionControl.replay();

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

			tmControl.verify();
			sfControl.verify();
			sessionControl.verify();
		}

		TransactionSynchronizationManager.unbindResource(sf);
	}

	protected void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
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

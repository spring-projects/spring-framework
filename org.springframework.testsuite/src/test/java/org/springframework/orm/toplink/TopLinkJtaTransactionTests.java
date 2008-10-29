/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.orm.toplink;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import junit.framework.TestCase;
import oracle.toplink.exceptions.TopLinkException;
import oracle.toplink.sessions.Session;
import org.easymock.MockControl;

import org.springframework.transaction.MockJtaTransaction;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Juergen Hoeller
 * @author <a href="mailto:james.x.clark@oracle.com">James Clark</a>
 * @since 28.04.2005
 */
public class TopLinkJtaTransactionTests extends TestCase {

	public void testParticipatingJtaTransactionWithWithRequiresNew() throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockControl tx1Control = MockControl.createControl(javax.transaction.Transaction.class);
		javax.transaction.Transaction tx1 = (javax.transaction.Transaction) tx1Control.getMock();

		MockControl session1Control = MockControl.createControl(Session.class);
		Session session1 = (Session) session1Control.getMock();
		MockControl session2Control = MockControl.createControl(Session.class);
		final Session session2 = (Session) session2Control.getMock();
		final MockSessionFactory sf = new MockSessionFactory(session1);

		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 5);
		ut.begin();
		utControl.setVoidCallable(2);
		tm.suspend();
		tmControl.setReturnValue(tx1, 1);
		tm.resume(tx1);
		tmControl.setVoidCallable(1);
		ut.commit();
		utControl.setVoidCallable(2);

//		session1.hasExternalTransactionController();
//		session1Control.setReturnValue(true,1);
		session1.release();
		session1Control.setVoidCallable(1);
//		session2.hasExternalTransactionController();
//		session2Control.setReturnValue(true,1);
		session2.release();
		session2Control.setVoidCallable(1);

		utControl.replay();
		tmControl.replay();
		session1Control.replay();
		session2Control.replay();

		JtaTransactionManager ptm = new JtaTransactionManager();
		ptm.setUserTransaction(ut);
		ptm.setTransactionManager(tm);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				SessionFactoryUtils.getSession(sf, true);
				final SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertTrue("Has thread session", holder != null);
				sf.setSession(session2);

				tt.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						TopLinkTemplate ht = new TopLinkTemplate(sf);
						return ht.executeFind(new TopLinkCallback() {
							public Object doInTopLink(Session session) {
								assertTrue("Not enclosing session", session != holder.getSession());
								return null;
							}
						});
					}
				});
				assertTrue("Same thread session as before",
						holder.getSession() == SessionFactoryUtils.getSession(sf, false));
				return null;
			}
		});
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		utControl.verify();
		tmControl.verify();
		session1Control.verify();
		session2Control.verify();
	}

	public void testJtaTransactionCommit() throws Exception {
		doTestJtaTransactionCommit(Status.STATUS_NO_TRANSACTION);
	}

	public void testJtaTransactionCommitWithExisting() throws Exception {
		doTestJtaTransactionCommit(Status.STATUS_ACTIVE);
	}

	private void doTestJtaTransactionCommit(int status) throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
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
		utControl.replay();

		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		final SessionFactory sf = new SingleSessionFactory(session);

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
					assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
					TopLinkTemplate ht = new TopLinkTemplate(sf);
					List htl = ht.executeFind(new TopLinkCallback() {
						public Object doInTopLink(Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return l;
						}
					});

					ht = new TopLinkTemplate(sf);
					htl = ht.executeFind(new TopLinkCallback() {
						public Object doInTopLink(Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return l;
						}
					});
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));

					sessionControl.verify();
					sessionControl.reset();

					try {
						session.release();
						sessionControl.setVoidCallable(1);
					}
					catch (TopLinkException e) {
					}
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
		sessionControl.verify();
	}

	public void testJtaTransactionCommitWithJtaTm() throws Exception {
		doTestJtaTransactionCommitWithJtaTm(Status.STATUS_NO_TRANSACTION);
	}

	public void testJtaTransactionCommitWithJtaTmAndExisting() throws Exception {
		doTestJtaTransactionCommitWithJtaTm(Status.STATUS_ACTIVE);
	}

	private void doTestJtaTransactionCommitWithJtaTm(int status) throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
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

		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockJtaTransaction transaction = new MockJtaTransaction();
		tm.getStatus();
		tmControl.setReturnValue(Status.STATUS_ACTIVE, 6);
		tm.getTransaction();
		tmControl.setReturnValue(transaction, 6);

		final MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		final SessionFactory sf = new SingleSessionFactory(session);

		utControl.replay();
		tmControl.replay();
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
					assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

					TopLinkTemplate ht = new TopLinkTemplate(sf);
					List htl = ht.executeFind(new TopLinkCallback() {
						public Object doInTopLink(Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return l;
						}
					});

					ht = new TopLinkTemplate(sf);
					htl = ht.executeFind(new TopLinkCallback() {
						public Object doInTopLink(Session sess) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
							assertEquals(session, sess);
							return l;
						}
					});

					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					sessionControl.verify();
					sessionControl.reset();
					try {
						session.release();
						sessionControl.setVoidCallable(1);
					}
					catch (TopLinkException e) {
					}
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
		sessionControl.verify();
	}

	protected void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

}

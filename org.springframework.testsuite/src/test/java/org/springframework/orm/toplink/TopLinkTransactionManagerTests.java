/*
 * Created on Mar 20, 2005
 *
 */

package org.springframework.orm.toplink;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import oracle.toplink.sessions.Session;
import oracle.toplink.sessions.UnitOfWork;
import org.easymock.MockControl;

import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Juergen Hoeller
 * @author <a href="mailto:james.x.clark@oracle.com">James Clark</a>
 * @since 28.04.2005
 */
public class TopLinkTransactionManagerTests extends TestCase {

	public void testTransactionCommit() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		MockControl uowControl = MockControl.createControl(UnitOfWork.class);
		UnitOfWork uow = (UnitOfWork) uowControl.getMock();

		final SessionFactory sf = new MockSessionFactory(session);

		// during commit, TM must get the active UnitOfWork
		session.getActiveUnitOfWork();
		sessionControl.setReturnValue(uow, 2);
		uow.beginEarlyTransaction();
		uowControl.setVoidCallable(1);
		uow.commit();
		uowControl.setVoidCallable();
		// session should be released when it was bound explicitly by the TM
		session.release();
		sessionControl.setVoidCallable();

		sessionControl.replay();
		uowControl.replay();

		TopLinkTransactionManager tm = new TopLinkTransactionManager();
		tm.setJdbcExceptionTranslator(new SQLStateSQLExceptionTranslator());
		tm.setSessionFactory(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		tt.setTimeout(10);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				TopLinkTemplate template = new TopLinkTemplate(sf);
				return template.execute(new TopLinkCallback() {
					public Object doInTopLink(Session session) {
						return null;
					}
				});
			}
		});
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		sessionControl.verify();
		uowControl.verify();
	}

	public void testTransactionRollback() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		MockControl uowControl = MockControl.createControl(UnitOfWork.class);
		UnitOfWork uow = (UnitOfWork) uowControl.getMock();

		final SessionFactory sf = new MockSessionFactory(session);

		session.getActiveUnitOfWork();
		sessionControl.setReturnValue(uow, 1);
		uow.beginEarlyTransaction();
		uowControl.setVoidCallable(1);
		session.release();
		sessionControl.setVoidCallable(1);

		sessionControl.replay();
		uowControl.replay();

		TopLinkTransactionManager tm = new TopLinkTransactionManager();
		tm.setSessionFactory(sf);
		tm.setJdbcExceptionTranslator(new SQLStateSQLExceptionTranslator());
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		tt.setTimeout(10);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			Object result = tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
					TopLinkTemplate template = new TopLinkTemplate(sf);
					return template.execute(new TopLinkCallback() {
						public Object doInTopLink(Session session) {
							throw new RuntimeException("failure");
						}
					});
				}
			});
			fail("Should have propagated RuntimeException");
		}
		catch (RuntimeException ex) {
			assertTrue(ex.getMessage().equals("failure"));
		}
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		sessionControl.verify();
		uowControl.verify();
	}

	public void testTransactionRollbackOnly() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		final SessionFactory sf = new MockSessionFactory(session);
		session.release();
		sessionControl.setVoidCallable();
		sessionControl.replay();

		TopLinkTransactionManager tm = new TopLinkTransactionManager();
		tm.setSessionFactory(sf);
		tm.setLazyDatabaseTransaction(true);
		tm.setJdbcExceptionTranslator(new SQLStateSQLExceptionTranslator());
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		tt.setTimeout(10);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session",
						TransactionSynchronizationManager.hasResource(sf));
				TopLinkTemplate template = new TopLinkTemplate(sf);
				template.execute(new TopLinkCallback() {
					public Object doInTopLink(Session session) {
						return null;
					}
				});
				status.setRollbackOnly();
				return null;
			}
		});
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		sessionControl.verify();
	}

	public void testParticipatingTransactionWithCommit() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		MockControl uowControl = MockControl.createControl(UnitOfWork.class);
		UnitOfWork uow = (UnitOfWork) uowControl.getMock();

		final SessionFactory sf = new MockSessionFactory(session);

		session.getActiveUnitOfWork();
		sessionControl.setReturnValue(uow, 2);
		uow.beginEarlyTransaction();
		uowControl.setVoidCallable(1);
		uow.commit();
		uowControl.setVoidCallable();
		session.release();
		sessionControl.setVoidCallable();

		sessionControl.replay();
		uowControl.replay();

		PlatformTransactionManager tm = new TopLinkTransactionManager(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);

		Object result = tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return tt.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						TopLinkTemplate ht = new TopLinkTemplate(sf);
						return ht.executeFind(new TopLinkCallback() {
							public Object doInTopLink(Session injectedSession) {
								assertTrue(session == injectedSession);
								return null;
							}
						});
					}
				});
			}
		});

		sessionControl.verify();
		uowControl.verify();
	}

	public void testParticipatingTransactionWithRollback() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		final SessionFactory sf = new MockSessionFactory(session);

		session.release();
		sessionControl.setVoidCallable();

		sessionControl.replay();

		TopLinkTransactionManager tm = new TopLinkTransactionManager(sf);
		tm.setLazyDatabaseTransaction(true);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		try {
			tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					return tt.execute(new TransactionCallback() {
						public Object doInTransaction(TransactionStatus status) {
							TopLinkTemplate ht = new TopLinkTemplate(sf);
							return ht.executeFind(new TopLinkCallback() {
								public Object doInTopLink(Session session) {
									throw new RuntimeException("application exception");
								}
							});
						}
					});
				}
			});
			fail("Should not thrown RuntimeException");
		}
		catch (RuntimeException ex) {
			assertTrue(ex.getMessage().equals("application exception"));
		}
		sessionControl.verify();
	}

	public void testParticipatingTransactionWithRollbackOnly() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		final SessionFactory sf = new MockSessionFactory(session);

		session.release();
		sessionControl.setVoidCallable();

		sessionControl.replay();

		TopLinkTransactionManager tm = new TopLinkTransactionManager(sf);
		tm.setLazyDatabaseTransaction(true);
		final TransactionTemplate tt = new TransactionTemplate(tm);

		try {
			tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					tt.execute(new TransactionCallback() {
						public Object doInTransaction(TransactionStatus status) {
							TopLinkTemplate ht = new TopLinkTemplate(sf);
							ht.execute(new TopLinkCallback() {
								public Object doInTopLink(Session session) {
									return null;
								}
							});
							status.setRollbackOnly();
							return null;
						}
					});
					return null;
				}
			});
			fail("Should have thrown UnexpectedRollbackException");
		}
		catch (UnexpectedRollbackException ex) {
			// expected
		}

		sessionControl.verify();
	}

	public void testParticipatingTransactionWithWithRequiresNew() {
		MockControl session1Control = MockControl.createControl(Session.class);
		final Session session1 = (Session) session1Control.getMock();
		MockControl session2Control = MockControl.createControl(Session.class);
		final Session session2 = (Session) session2Control.getMock();

		MockControl uow1Control = MockControl.createControl(UnitOfWork.class);
		UnitOfWork uow1 = (UnitOfWork) uow1Control.getMock();
		MockControl uow2Control = MockControl.createControl(UnitOfWork.class);
		UnitOfWork uow2 = (UnitOfWork) uow2Control.getMock();

		final MockSessionFactory sf = new MockSessionFactory(session1);

		session2.getActiveUnitOfWork();
		session2Control.setReturnValue(uow2, 2);
		uow2.beginEarlyTransaction();
		uow2Control.setVoidCallable(1);
		uow2.commit();
		uow2Control.setVoidCallable();
		session2.release();
		session2Control.setVoidCallable();

		session1.getActiveUnitOfWork();
		session1Control.setReturnValue(uow1, 2);
		uow1.beginEarlyTransaction();
		uow1Control.setVoidCallable(1);
		uow1.commit();
		uow1Control.setVoidCallable();
		session1.release();
		session1Control.setVoidCallable();

		session1Control.replay();
		uow1Control.replay();
		session2Control.replay();
		uow2Control.replay();

		PlatformTransactionManager tm = new TopLinkTransactionManager(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertTrue("Has thread session", holder != null);
				sf.setSession(session2);
				tt.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						TopLinkTemplate ht = new TopLinkTemplate(sf);
						return ht.execute(new TopLinkCallback() {
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

		session1Control.verify();
		session2Control.verify();
		uow1Control.verify();
		uow2Control.verify();
	}

	public void testParticipatingTransactionWithWithNotSupported() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		MockControl uowControl = MockControl.createControl(UnitOfWork.class);
		UnitOfWork uow = (UnitOfWork) uowControl.getMock();

		final SessionFactory sf = new MockSessionFactory(session);

		session.getActiveUnitOfWork();
		sessionControl.setReturnValue(uow, 2);
		uow.beginEarlyTransaction();
		uowControl.setVoidCallable(1);
		uow.commit();
		uowControl.setVoidCallable();
		session.release();
		sessionControl.setVoidCallable(2);

		sessionControl.replay();
		uowControl.replay();

		TopLinkTransactionManager tm = new TopLinkTransactionManager(sf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sf);
				assertTrue("Has thread session", holder != null);
				tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
				tt.execute(new TransactionCallback() {
					public Object doInTransaction(TransactionStatus status) {
						assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
						TopLinkTemplate ht = new TopLinkTemplate(sf);

						return ht.execute(new TopLinkCallback() {
							public Object doInTopLink(Session session) {
								return null;
							}
						});
					}
				});
				assertTrue("Same thread session as before", holder.getSession() == SessionFactoryUtils.getSession(sf, false));
				return null;
			}
		});
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		sessionControl.verify();
		uowControl.verify();
	}

	public void testTransactionWithPropagationSupports() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		final SessionFactory sf = new MockSessionFactory(session);

		// not a new transaction, won't start a new one
		session.release();
		sessionControl.setVoidCallable();

		sessionControl.replay();

		PlatformTransactionManager tm = new TopLinkTransactionManager(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));

		tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				TopLinkTemplate ht = new TopLinkTemplate(sf);
				ht.execute(new TopLinkCallback() {
					public Object doInTopLink(Session session) {
						return null;
					}
				});
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				return null;
			}
		});

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		sessionControl.verify();
	}

	public void testTransactionCommitWithReadOnly() {
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		MockControl uowControl = MockControl.createControl(UnitOfWork.class);
		UnitOfWork uow = (UnitOfWork) uowControl.getMock();

		final SessionFactory sf = new MockSessionFactory(session);

		session.release();
		sessionControl.setVoidCallable();

		sessionControl.replay();
		uowControl.replay();

		TopLinkTransactionManager tm = new TopLinkTransactionManager(sf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setReadOnly(true);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(sf));
				TopLinkTemplate ht = new TopLinkTemplate(sf);
				return ht.executeFind(new TopLinkCallback() {
					public Object doInTopLink(Session session) {
						return l;
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Hasn't thread session", !TransactionSynchronizationManager.hasResource(sf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		sessionControl.verify();
		uowControl.verify();
	}

	protected void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

}

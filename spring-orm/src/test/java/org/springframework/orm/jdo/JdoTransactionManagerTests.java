/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.orm.jdo;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import javax.jdo.JDOFatalDataStoreException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.beans.TestBean;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.SimpleConnectionHandle;
import org.springframework.orm.jdo.support.SpringPersistenceManagerProxyBean;
import org.springframework.orm.jdo.support.StandardPersistenceManagerProxyBean;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.MockJtaTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Juergen Hoeller
 */
public class JdoTransactionManagerTests extends TestCase {

	private MockControl pmfControl, pmControl, txControl;

	private PersistenceManagerFactory pmf;

	private PersistenceManager pm;

	private Transaction tx;


	@Override
	protected void setUp() {
		pmfControl = MockControl.createControl(PersistenceManagerFactory.class);
		pmf = (PersistenceManagerFactory) pmfControl.getMock();
		pmControl = MockControl.createControl(PersistenceManager.class);
		pm = (PersistenceManager) pmControl.getMock();
		txControl = MockControl.createControl(Transaction.class);
		tx = (Transaction) txControl.getMock();
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 1);
	}

	@Override
	protected void tearDown() {
		try {
			pmfControl.verify();
			pmControl.verify();
		}
		catch (IllegalStateException ex) {
			// ignore: test method didn't call replay
		}
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	public void testTransactionCommit() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 3);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pmf.getPersistenceManagerProxy();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 3);
		pm.flush();
		pmControl.setVoidCallable(4);
		pm.close();
		pmControl.setVoidCallable(1);
		tx.begin();
		txControl.setVoidCallable(1);
		tx.getRollbackOnly();
		txControl.setReturnValue(false, 1);
		tx.commit();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();
		txControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));

				TransactionAwarePersistenceManagerFactoryProxy proxyFactory =
						new TransactionAwarePersistenceManagerFactoryProxy();
				proxyFactory.setTargetPersistenceManagerFactory(pmf);
				PersistenceManagerFactory pmfProxy = proxyFactory.getObject();
				assertEquals(pm.toString(), pmfProxy.getPersistenceManager().toString());
				pmfProxy.getPersistenceManager().flush();
				pmfProxy.getPersistenceManager().close();

				SpringPersistenceManagerProxyBean proxyBean = new SpringPersistenceManagerProxyBean();
				proxyBean.setPersistenceManagerFactory(pmf);
				proxyBean.afterPropertiesSet();
				PersistenceManager pmProxy = proxyBean.getObject();
				assertSame(pmf, pmProxy.getPersistenceManagerFactory());
				pmProxy.flush();
				pmProxy.close();

				StandardPersistenceManagerProxyBean stdProxyBean = new StandardPersistenceManagerProxyBean();
				stdProxyBean.setPersistenceManagerFactory(pmf);
				PersistenceManager stdPmProxy = stdProxyBean.getObject();
				stdPmProxy.flush();

				JdoTemplate jt = new JdoTemplate(pmf);
				return jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm2) {
						pm2.flush();
						return l;
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
	}

	public void testTransactionRollback() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 2);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 2);
		pm.close();
		pmControl.setVoidCallable(1);
		tx.begin();
		txControl.setVoidCallable(1);
		tx.isActive();
		txControl.setReturnValue(true, 1);
		tx.rollback();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();
		txControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
					JdoTemplate jt = new JdoTemplate(pmf);
					return jt.execute(new JdoCallback() {
						@Override
						public Object doInJdo(PersistenceManager pm) {
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

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
	}

	public void testTransactionRollbackWithAlreadyRolledBack() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 2);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 2);
		pm.close();
		pmControl.setVoidCallable(1);
		tx.begin();
		txControl.setVoidCallable(1);
		tx.isActive();
		txControl.setReturnValue(false, 1);
		pmfControl.replay();
		pmControl.replay();
		txControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
					JdoTemplate jt = new JdoTemplate(pmf);
					return jt.execute(new JdoCallback() {
						@Override
						public Object doInJdo(PersistenceManager pm) {
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

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
	}

	public void testTransactionRollbackOnly() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 2);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 2);
		pm.flush();
		pmControl.setVoidCallable(1);
		pm.close();
		pmControl.setVoidCallable(1);
		tx.begin();
		txControl.setVoidCallable(1);
		tx.isActive();
		txControl.setReturnValue(true, 1);
		tx.rollback();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();
		txControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
				JdoTemplate jt = new JdoTemplate(pmf);
				jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm2) {
						pm2.flush();
						return null;
					}
				});
				status.setRollbackOnly();
				return null;
			}
		});

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
	}

	public void testParticipatingTransactionWithCommit() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 2);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 3);
		pm.flush();
		pmControl.setVoidCallable(1);
		pm.close();
		pmControl.setVoidCallable(1);
		tx.begin();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();
		txControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				txControl.reset();
				tx.isActive();
				txControl.setReturnValue(true, 1);
				tx.getRollbackOnly();
				txControl.setReturnValue(false, 1);
				tx.commit();
				txControl.setVoidCallable(1);
				txControl.replay();

				return tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						JdoTemplate jt = new JdoTemplate(pmf);
						return jt.execute(new JdoCallback() {
							@Override
							public Object doInJdo(PersistenceManager pm2) {
								pm2.flush();
								return l;
							}
						});
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);
	}

	public void testParticipatingTransactionWithRollback() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 2);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 3);
		pm.close();
		pmControl.setVoidCallable(1);
		tx.isActive();
		txControl.setReturnValue(false, 1);
		tx.begin();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();
		txControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					txControl.reset();
					tx.isActive();
					txControl.setReturnValue(true, 3);
					tx.setRollbackOnly();
					txControl.setVoidCallable(1);
					tx.rollback();
					txControl.setVoidCallable(1);
					txControl.replay();

					return tt.execute(new TransactionCallback() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							JdoTemplate jt = new JdoTemplate(pmf);
							return jt.execute(new JdoCallback() {
								@Override
								public Object doInJdo(PersistenceManager pm) {
									throw new RuntimeException("application exception");
								}
							});
						}
					});
				}
			});
			fail("Should have thrown RuntimeException");
		}
		catch (RuntimeException ex) {
			// expected
		}
	}

	public void testParticipatingTransactionWithRollbackOnly() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 2);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 5);
		pm.flush();
		pmControl.setVoidCallable(1);
		pm.close();
		pmControl.setVoidCallable(1);
		tx.begin();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();
		txControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					txControl.reset();
					tx.isActive();
					txControl.setReturnValue(true, 1);
					tx.setRollbackOnly();
					txControl.setVoidCallable(1);
					tx.getRollbackOnly();
					txControl.setReturnValue(true, 1);
					tx.commit();
					txControl.setThrowable(new JDOFatalDataStoreException(), 1);
					tx.isActive();
					txControl.setReturnValue(false, 1);
					txControl.replay();

					return tt.execute(new TransactionCallback() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							JdoTemplate jt = new JdoTemplate(pmf);
							jt.execute(new JdoCallback() {
								@Override
								public Object doInJdo(PersistenceManager pm2) {
									pm2.flush();
									return l;
								}
							});
							status.setRollbackOnly();
							return null;
						}
					});
				}
			});
			fail("Should have thrown JdoResourceFailureException");
		}
		catch (JdoResourceFailureException ex) {
			// expected
		}
	}

	public void testParticipatingTransactionWithWithRequiresNew() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 2);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 2);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 6);
		tx.begin();
		txControl.setVoidCallable(1);
		pm.flush();
		pmControl.setVoidCallable(1);
		pm.close();
		pmControl.setVoidCallable(2);
		pmfControl.replay();
		pmControl.replay();
		txControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final List l = new ArrayList();
		l.add("test");

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				txControl.verify();
				txControl.reset();
				tx.isActive();
				txControl.setReturnValue(true, 1);
				tx.begin();
				txControl.setVoidCallable(1);
				tx.getRollbackOnly();
				txControl.setReturnValue(false, 2);
				tx.commit();
				txControl.setVoidCallable(2);
				txControl.replay();

				return tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						JdoTemplate jt = new JdoTemplate(pmf);
						return jt.execute(new JdoCallback() {
							@Override
							public Object doInJdo(PersistenceManager pm2) {
								pm2.flush();
								return l;
							}
						});
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);
	}

	public void testParticipatingTransactionWithWithRequiresNewAndPrebound() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 3);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 6);
		tx.begin();
		txControl.setVoidCallable(1);
		pm.flush();
		pmControl.setVoidCallable(1);
		pm.close();
		pmControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();
		txControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(pmf, new PersistenceManagerHolder(pm));
		assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				txControl.verify();
				txControl.reset();
				tx.isActive();
				txControl.setReturnValue(true, 1);
				tx.begin();
				txControl.setVoidCallable(1);
				tx.getRollbackOnly();
				txControl.setReturnValue(false, 2);
				tx.commit();
				txControl.setVoidCallable(2);
				txControl.replay();

				JdoTemplate jt = new JdoTemplate(pmf);
				jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm2) {
						return null;
					}
				});

				return tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						JdoTemplate jt = new JdoTemplate(pmf);
						return jt.execute(new JdoCallback() {
							@Override
							public Object doInJdo(PersistenceManager pm2) {
								pm2.flush();
								return l;
							}
						});
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
		TransactionSynchronizationManager.unbindResource(pmf);
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
	}

	public void testJtaTransactionCommit() throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
		ut.commit();
		utControl.setVoidCallable(1);
		utControl.replay();

		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.flush();
		pmControl.setVoidCallable(2);
		pm.close();
		pmControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
				JdoTemplate jt = new JdoTemplate(pmf);
				jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm2) {
						assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
						pm2.flush();
						return l;
					}
				});
				Object result = jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm2) {
						assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
						pm2.flush();
						return l;
					}
				});
				assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
				return result;
			}
		});
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("Correct result list", result == l);

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		utControl.verify();
	}

	public void testParticipatingJtaTransactionWithWithRequiresNewAndPrebound() throws Exception {
		final MockControl utControl = MockControl.createControl(UserTransaction.class);
		final UserTransaction ut = (UserTransaction) utControl.getMock();
		final MockControl tmControl = MockControl.createControl(TransactionManager.class);
		final TransactionManager tm = (TransactionManager) tmControl.getMock();

		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		utControl.replay();

		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 1);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.flush();
		pmControl.setVoidCallable(1);
		pm.close();
		pmControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();

		JtaTransactionManager ptm = new JtaTransactionManager(ut, tm);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(pmf, new PersistenceManagerHolder(pm));
		assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					utControl.verify();
					utControl.reset();
					ut.getStatus();
					utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
					MockJtaTransaction transaction = new MockJtaTransaction();
					tm.suspend();
					tmControl.setReturnValue(transaction, 1);
					ut.begin();
					utControl.setVoidCallable(1);
					ut.getStatus();
					utControl.setReturnValue(Status.STATUS_ACTIVE, 4);
					ut.commit();
					utControl.setVoidCallable(2);
					tm.resume(transaction);
					tmControl.setVoidCallable(1);
					utControl.replay();
					tmControl.replay();
				}
				catch (Exception ex) {
				}

				JdoTemplate jt = new JdoTemplate(pmf);
				jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm2) {
						return null;
					}
				});

				return tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						JdoTemplate jt = new JdoTemplate(pmf);
						return jt.execute(new JdoCallback() {
							@Override
							public Object doInJdo(PersistenceManager pm2) {
								pm2.flush();
								return l;
							}
						});
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
		TransactionSynchronizationManager.unbindResource(pmf);
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		utControl.verify();
	}


	public void testTransactionCommitWithPropagationSupports() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 2);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.close();
		pmControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				JdoTemplate jt = new JdoTemplate(pmf);
				return jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm) {
						return l;
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
	}

	public void testInvalidIsolation() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 1);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(null, 1);
		pm.close();
		pmControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
				}
			});
			fail("Should have thrown InvalidIsolationLevelException");
		}
		catch (InvalidIsolationLevelException ex) {
			// expected
		}
	}

	public void testTransactionCommitWithPrebound() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 2);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 3);
		tx.isActive();
		txControl.setReturnValue(false, 1);
		tx.begin();
		txControl.setVoidCallable(1);
		tx.getRollbackOnly();
		txControl.setReturnValue(false, 1);
		tx.commit();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();
		txControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		TransactionSynchronizationManager.bindResource(pmf, new PersistenceManagerHolder(pm));
		assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
				JdoTemplate jt = new JdoTemplate(pmf);
				return jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm) {
						return l;
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
		TransactionSynchronizationManager.unbindResource(pmf);
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
	}

	public void testTransactionCommitWithDataSource() throws SQLException {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl dialectControl = MockControl.createControl(JdoDialect.class);
		JdoDialect dialect = (JdoDialect) dialectControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		ConnectionHandle conHandle = new SimpleConnectionHandle(con);

		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 3);
		pm.close();
		pmControl.setVoidCallable(1);
		TransactionTemplate tt = new TransactionTemplate();
		dialect.beginTransaction(tx, tt);
		dialectControl.setReturnValue(null, 1);
		dialect.getJdbcConnection(pm, false);
		dialectControl.setReturnValue(conHandle, 1);
		dialect.releaseJdbcConnection(conHandle, pm);
		dialectControl.setVoidCallable(1);
		dialect.cleanupTransaction(null);
		dialectControl.setVoidCallable(1);
		tx.getRollbackOnly();
		txControl.setReturnValue(false, 1);
		tx.commit();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		dsControl.replay();
		dialectControl.replay();
		pmControl.replay();
		txControl.replay();
		conControl.replay();

		JdoTransactionManager tm = new JdoTransactionManager();
		tm.setPersistenceManagerFactory(pmf);
		tm.setDataSource(ds);
		tm.setJdoDialect(dialect);
		tt.setTransactionManager(tm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
				assertTrue("Has thread con", TransactionSynchronizationManager.hasResource(ds));
				JdoTemplate jt = new JdoTemplate(pmf);
				return jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm) {
						return l;
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("Hasn't thread con", !TransactionSynchronizationManager.hasResource(ds));
		dsControl.verify();
		dialectControl.verify();
		conControl.verify();
	}

	public void testTransactionCommitWithAutoDetectedDataSource() throws SQLException {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl dialectControl = MockControl.createControl(JdoDialect.class);
		JdoDialect dialect = (JdoDialect) dialectControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		ConnectionHandle conHandle = new SimpleConnectionHandle(con);

		pmfControl.reset();
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(ds, 2);
		con.getMetaData();
		conControl.setReturnValue(null, 1);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 3);
		pm.close();
		pmControl.setVoidCallable(1);
		TransactionTemplate tt = new TransactionTemplate();
		dialect.beginTransaction(tx, tt);
		dialectControl.setReturnValue(null, 1);
		dialect.getJdbcConnection(pm, false);
		dialectControl.setReturnValue(conHandle, 1);
		dialect.releaseJdbcConnection(conHandle, pm);
		dialectControl.setVoidCallable(1);
		dialect.cleanupTransaction(null);
		dialectControl.setVoidCallable(1);
		tx.getRollbackOnly();
		txControl.setReturnValue(false, 1);
		tx.commit();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		dsControl.replay();
		dialectControl.replay();
		pmControl.replay();
		txControl.replay();
		conControl.replay();

		JdoTransactionManager tm = new JdoTransactionManager();
		tm.setPersistenceManagerFactory(pmf);
		tm.setJdoDialect(dialect);
		tm.afterPropertiesSet();
		tt.setTransactionManager(tm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
				assertTrue("Has thread con", TransactionSynchronizationManager.hasResource(ds));
				JdoTemplate jt = new JdoTemplate(pmf);
				return jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm) {
						return l;
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("Hasn't thread con", !TransactionSynchronizationManager.hasResource(ds));
		dsControl.verify();
		dialectControl.verify();
		conControl.verify();
	}

	public void testTransactionCommitWithAutoDetectedDataSourceAndNoConnection() throws SQLException {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl dialectControl = MockControl.createControl(JdoDialect.class);
		final JdoDialect dialect = (JdoDialect) dialectControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);

		pmfControl.reset();
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(ds, 1);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 3);
		pm.flush();
		pmControl.setVoidCallable(1);
		pm.close();
		pmControl.setVoidCallable(1);
		TransactionTemplate tt = new TransactionTemplate();
		dialect.beginTransaction(tx, tt);
		dialectControl.setReturnValue(null, 1);
		dialect.getJdbcConnection(pm, false);
		dialectControl.setReturnValue(null, 1);
		dialect.cleanupTransaction(null);
		dialectControl.setVoidCallable(1);
		tx.getRollbackOnly();
		txControl.setReturnValue(false, 1);
		tx.commit();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		dsControl.replay();
		dialectControl.replay();
		pmControl.replay();
		txControl.replay();
		conControl.replay();

		JdoTransactionManager tm = new JdoTransactionManager();
		tm.setPersistenceManagerFactory(pmf);
		tm.setJdoDialect(dialect);
		tm.afterPropertiesSet();
		tt.setTransactionManager(tm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
				assertTrue("Hasn't thread con", !TransactionSynchronizationManager.hasResource(ds));
				JdoTemplate jt = new JdoTemplate();
				jt.setPersistenceManagerFactory(pmf);
				jt.setJdoDialect(dialect);
				return jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm2) {
						pm2.flush();
						return l;
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("Hasn't thread con", !TransactionSynchronizationManager.hasResource(ds));
		dsControl.verify();
		dialectControl.verify();
		conControl.verify();
	}

	public void testExistingTransactionWithPropagationNestedAndRollback() throws SQLException {
		doTestExistingTransactionWithPropagationNestedAndRollback(false);
	}

	public void testExistingTransactionWithManualSavepointAndRollback() throws SQLException {
		doTestExistingTransactionWithPropagationNestedAndRollback(true);
	}

	private void doTestExistingTransactionWithPropagationNestedAndRollback(final boolean manualSavepoint)
			throws SQLException {

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl dialectControl = MockControl.createControl(JdoDialect.class);
		JdoDialect dialect = (JdoDialect) dialectControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		MockControl mdControl = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData md = (DatabaseMetaData) mdControl.getMock();
		MockControl spControl = MockControl.createControl(Savepoint.class);
		Savepoint sp = (Savepoint) spControl.getMock();

		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 3);
		pm.flush();
		pmControl.setVoidCallable(1);
		pm.close();
		pmControl.setVoidCallable(1);
		md.supportsSavepoints();
		mdControl.setReturnValue(true, 1);
		con.getMetaData();
		conControl.setReturnValue(md, 1);
		con.setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + 1);
		conControl.setReturnValue(sp, 1);
		con.rollback(sp);
		conControl.setVoidCallable(1);
		final TransactionTemplate tt = new TransactionTemplate();
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		dialect.beginTransaction(tx, tt);
		dialectControl.setReturnValue(null, 1);
		ConnectionHandle conHandle = new SimpleConnectionHandle(con);
		dialect.getJdbcConnection(pm, false);
		dialectControl.setReturnValue(conHandle, 1);
		dialect.releaseJdbcConnection(conHandle, pm);
		dialectControl.setVoidCallable(1);
		dialect.cleanupTransaction(null);
		dialectControl.setVoidCallable(1);
		if (!manualSavepoint) {
			tx.isActive();
			txControl.setReturnValue(true, 1);
		}
		tx.getRollbackOnly();
		txControl.setReturnValue(false, 1);
		tx.commit();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		dsControl.replay();
		dialectControl.replay();
		pmControl.replay();
		txControl.replay();
		conControl.replay();
		mdControl.replay();
		spControl.replay();

		JdoTransactionManager tm = new JdoTransactionManager();
		tm.setNestedTransactionAllowed(true);
		tm.setPersistenceManagerFactory(pmf);
		tm.setDataSource(ds);
		tm.setJdoDialect(dialect);
		tt.setTransactionManager(tm);
		final List l = new ArrayList();
		l.add("test");
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
				assertTrue("Has thread con", TransactionSynchronizationManager.hasResource(ds));
				if (manualSavepoint) {
					Object savepoint = status.createSavepoint();
					status.rollbackToSavepoint(savepoint);
				}
				else {
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) {
							assertTrue("Has thread session", TransactionSynchronizationManager.hasResource(pmf));
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
							status.setRollbackOnly();
						}
					});
				}
				JdoTemplate jt = new JdoTemplate(pmf);
				return jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm2) {
						pm2.flush();
						return l;
					}
				});
			}
		});
		assertTrue("Correct result list", result == l);

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("Hasn't thread con", !TransactionSynchronizationManager.hasResource(ds));
		dsControl.verify();
		dialectControl.verify();
		conControl.verify();
		mdControl.verify();
		spControl.verify();
	}

	public void testTransactionTimeoutWithJdoDialect() throws SQLException {
		doTestTransactionTimeoutWithJdoDialect(true);
	}

	public void testTransactionTimeoutWithJdoDialectAndPmProxy() throws SQLException {
		doTestTransactionTimeoutWithJdoDialect(false);
	}

	private void doTestTransactionTimeoutWithJdoDialect(final boolean exposeNativePm) throws SQLException {
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();
		MockControl dialectControl = MockControl.createControl(JdoDialect.class);
		final JdoDialect dialect = (JdoDialect) dialectControl.getMock();

		TransactionTemplate tt = new TransactionTemplate();

		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 2);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 3);
		dialect.beginTransaction(tx, tt);
		dialectControl.setReturnValue(null, 1);
		if (!exposeNativePm) {
			dialect.applyQueryTimeout(query, 10);
		}
		dialect.cleanupTransaction(null);
		dialectControl.setVoidCallable(1);
		pm.newQuery(TestBean.class);
		pmControl.setReturnValue(query, 1);
		pm.close();
		pmControl.setVoidCallable(1);
		tx.getRollbackOnly();
		txControl.setReturnValue(false, 1);
		tx.commit();
		txControl.setVoidCallable(1);

		pmfControl.replay();
		pmControl.replay();
		txControl.replay();
		queryControl.replay();
		dialectControl.replay();

		JdoTransactionManager tm = new JdoTransactionManager(pmf);
		tm.setJdoDialect(dialect);
		tt.setTransactionManager(tm);
		tt.setTimeout(10);

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
				JdoTemplate jt = new JdoTemplate(pmf);
				jt.setJdoDialect(dialect);
				if (exposeNativePm) {
					jt.setExposeNativePersistenceManager(true);
				}
				return jt.execute(new JdoCallback() {
					@Override
					public Object doInJdo(PersistenceManager pm2) {
						if (exposeNativePm) {
							assertSame(pm, pm2);
						}
						else {
							assertTrue(Proxy.isProxyClass(pm2.getClass()));
						}
						pm2.newQuery(TestBean.class);
						return null;
					}
				});
			}
		});

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		dialectControl.verify();
		queryControl.verify();
	}

	public void testTransactionFlush() {
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 1);
		pmf.getPersistenceManager();
		pmfControl.setReturnValue(pm, 1);
		pm.currentTransaction();
		pmControl.setReturnValue(tx, 3);
		pm.flush();
		pmControl.setVoidCallable(1);
		pm.close();
		pmControl.setVoidCallable(1);
		tx.begin();
		txControl.setVoidCallable(1);
		tx.getRollbackOnly();
		txControl.setReturnValue(false, 1);
		tx.commit();
		txControl.setVoidCallable(1);
		pmfControl.replay();
		pmControl.replay();
		txControl.replay();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue("Has thread pm", TransactionSynchronizationManager.hasResource(pmf));
				status.flush();
			}
		});

		assertTrue("Hasn't thread pm", !TransactionSynchronizationManager.hasResource(pmf));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
	}

}

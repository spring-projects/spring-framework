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

package org.springframework.orm.jdo;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.Constants;
import javax.jdo.JDOFatalDataStoreException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.datasource.SimpleConnectionHandle;
import org.springframework.orm.jdo.support.SpringPersistenceManagerProxyBean;
import org.springframework.orm.jdo.support.StandardPersistenceManagerProxyBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.tests.transaction.MockJtaTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
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
 */
public class JdoTransactionManagerTests {

	private PersistenceManagerFactory pmf;

	private PersistenceManager pm;

	private Transaction tx;


	@Before
	public void setUp() {
		pmf = mock(PersistenceManagerFactory.class);
		pm = mock(PersistenceManager.class);
		tx = mock(Transaction.class);
	}

	@After
	public void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	@Test
	public void testTransactionCommit() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pmf.getPersistenceManagerProxy()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);

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

		verify(pm, times(4)).flush();
		verify(pm).close();
		verify(tx).begin();
		verify(tx).commit();
	}

	@Test
	public void testTransactionRollback() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);

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

		verify(pm).close();
		verify(tx).begin();
		verify(tx).rollback();
	}

	@Test
	public void testTransactionRollbackWithAlreadyRolledBack() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);

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

		verify(pm).close();
		verify(tx).begin();
	}

	@Test
	public void testTransactionRollbackOnly() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);

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

		verify(pm).flush();
		verify(pm).close();
		verify(tx).begin();
		verify(tx).rollback();
	}

	@Test
	public void testParticipatingTransactionWithCommit() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		final List l = new ArrayList();
		l.add("test");

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

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

		verify(pm).flush();
		verify(pm).close();
		verify(tx).begin();
	}

	@Test
	public void testParticipatingTransactionWithRollback() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
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
		verify(pm).close();
		verify(tx).begin();
		verify(tx).setRollbackOnly();
		verify(tx).rollback();
	}

	@Test
	public void testParticipatingTransactionWithRollbackOnly() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);
		given(tx.getRollbackOnly()).willReturn(true);
		willThrow(new JDOFatalDataStoreException()).given(tx).commit();

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
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
		verify(pm).flush();
		verify(pm).close();
		verify(tx).begin();
		verify(tx).setRollbackOnly();
	}

	@Test
	public void testParticipatingTransactionWithWithRequiresNew() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final List l = new ArrayList();
		l.add("test");

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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
		verify(tx, times(2)).begin();
		verify(tx, times(2)).commit();
		verify(pm).flush();
		verify(pm, times(2)).close();
	}

	@Test
	public void testParticipatingTransactionWithWithRequiresNewAndPrebound() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);

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

		verify(tx, times(2)).begin();
		verify(tx, times(2)).commit();
		verify(pm).flush();
		verify(pm).close();
	}

	@Test
	public void testJtaTransactionCommit() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
		given(pmf.getPersistenceManager()).willReturn(pm);

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

		verify(ut).begin();
		verify(ut).commit();
		verify(pm, times(2)).flush();
		verify(pm, times(2)).close();
	}

	@Test
	public void testParticipatingJtaTransactionWithWithRequiresNewAndPrebound() throws Exception {
		final UserTransaction ut = mock(UserTransaction.class);
		final TransactionManager tm = mock(TransactionManager.class);

		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		given(pmf.getPersistenceManager()).willReturn(pm);

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
					MockJtaTransaction transaction = new MockJtaTransaction();
					given(tm.suspend()).willReturn(transaction);
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

		verify(ut, times(2)).begin();
		verify(pm).flush();
		verify(pm, times(2)).close();
	}

	@Test
	public void testTransactionCommitWithPropagationSupports() {
		given(pmf.getPersistenceManager()).willReturn(pm);

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

		verify(pm, times(2)).close();
	}

	@Test
	public void testIsolationLevel() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);

		PlatformTransactionManager tm = new JdoTransactionManager(pmf);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
			}
		});
		verify(tx).setIsolationLevel(Constants.TX_SERIALIZABLE);
		verify(pm).close();
	}

	@Test
	public void testTransactionCommitWithPrebound() {
		given(pm.currentTransaction()).willReturn(tx);

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

		verify(tx).begin();
		verify(tx).commit();
	}

	@Test
	public void testTransactionCommitWithDataSource() throws SQLException {
		final DataSource ds = mock(DataSource.class);
		JdoDialect dialect = mock(JdoDialect.class);
		final Connection con = mock(Connection.class);
		ConnectionHandle conHandle = new SimpleConnectionHandle(con);

		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		TransactionTemplate tt = new TransactionTemplate();
		given(dialect.getJdbcConnection(pm, false)).willReturn(conHandle);

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

		verify(pm).close();
		verify(dialect).beginTransaction(tx, tt);
		verify(dialect).releaseJdbcConnection(conHandle, pm);
		verify(dialect).cleanupTransaction(null);
		verify(tx).commit();
	}

	@Test
	public void testTransactionCommitWithAutoDetectedDataSource() throws SQLException {
		final DataSource ds = mock(DataSource.class);
		JdoDialect dialect = mock(JdoDialect.class);
		final Connection con = mock(Connection.class);
		ConnectionHandle conHandle = new SimpleConnectionHandle(con);

		given(pmf.getConnectionFactory()).willReturn(ds);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		TransactionTemplate tt = new TransactionTemplate();
		given(dialect.getJdbcConnection(pm, false)).willReturn(conHandle);

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

		verify(pm).close();
		verify(dialect).beginTransaction(tx, tt);
		verify(dialect).releaseJdbcConnection(conHandle, pm);
		verify(dialect).cleanupTransaction(null);
		verify(tx).commit();
	}

	@Test
	public void testTransactionCommitWithAutoDetectedDataSourceAndNoConnection() throws SQLException {
		final DataSource ds = mock(DataSource.class);
		final JdoDialect dialect = mock(JdoDialect.class);

		given(pmf.getConnectionFactory()).willReturn(ds);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		TransactionTemplate tt = new TransactionTemplate();
		given(dialect.getJdbcConnection(pm, false)).willReturn(null);

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

		verify(pm).flush();
		verify(pm).close();
		verify(dialect).beginTransaction(tx, tt);
		verify(dialect).cleanupTransaction(null);
		verify(tx).commit();
	}

	@Test
	public void testExistingTransactionWithPropagationNestedAndRollback() throws SQLException {
		doTestExistingTransactionWithPropagationNestedAndRollback(false);
	}

	@Test
	public void testExistingTransactionWithManualSavepointAndRollback() throws SQLException {
		doTestExistingTransactionWithPropagationNestedAndRollback(true);
	}

	private void doTestExistingTransactionWithPropagationNestedAndRollback(final boolean manualSavepoint)
			throws SQLException {

		final DataSource ds = mock(DataSource.class);
		JdoDialect dialect = mock(JdoDialect.class);
		final Connection con = mock(Connection.class);
		DatabaseMetaData md = mock(DatabaseMetaData.class);
		Savepoint sp = mock(Savepoint.class);

		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		given(md.supportsSavepoints()).willReturn(true);
		given(con.getMetaData()).willReturn(md);
		given(con.setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + 1)).willReturn(sp);
		final TransactionTemplate tt = new TransactionTemplate();
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		ConnectionHandle conHandle = new SimpleConnectionHandle(con);
		given(dialect.getJdbcConnection(pm, false)).willReturn(conHandle);
		given(tx.isActive()).willReturn(!manualSavepoint);

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
		verify(pm).flush();
		verify(pm).close();
		verify(con).setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + 1);
		verify(con).rollback(sp);
		verify(dialect).beginTransaction(tx, tt);
		verify(dialect).releaseJdbcConnection(conHandle, pm);
		verify(dialect).cleanupTransaction(null);
		verify(tx).commit();
	}

	@Test
	public void testTransactionTimeoutWithJdoDialect() throws SQLException {
		doTestTransactionTimeoutWithJdoDialect(true);
	}

	@Test
	public void testTransactionTimeoutWithJdoDialectAndPmProxy() throws SQLException {
		doTestTransactionTimeoutWithJdoDialect(false);
	}

	private void doTestTransactionTimeoutWithJdoDialect(final boolean exposeNativePm) throws SQLException {
		Query query = mock(Query.class);
		final JdoDialect dialect = mock(JdoDialect.class);

		TransactionTemplate tt = new TransactionTemplate();

		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);
		if (!exposeNativePm) {
			dialect.applyQueryTimeout(query, 10);
		}
		given(pm.newQuery(TestBean.class)).willReturn(query);

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

		verify(dialect).beginTransaction(tx, tt);
		verify(dialect).cleanupTransaction(null);
		verify(pm).close();
		verify(tx).getRollbackOnly();
		verify(tx).commit();
	}

	@Test
	public void testTransactionFlush() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.currentTransaction()).willReturn(tx);

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
		verify(pm).flush();
		verify(pm).close();
		verify(tx).begin();
		verify(tx).commit();
	}

}

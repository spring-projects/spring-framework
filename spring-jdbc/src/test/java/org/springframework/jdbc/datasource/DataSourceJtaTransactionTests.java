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

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jdbc.datasource.lookup.BeanFactoryDataSourceLookup;
import org.springframework.jdbc.datasource.lookup.IsolationLevelDataSourceRouter;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.JtaTransactionObject;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Juergen Hoeller
 * @since 17.10.2005
 */
public class DataSourceJtaTransactionTests extends TestCase {

	public void testJtaTransactionCommit() throws Exception {
		doTestJtaTransaction(false);
	}

	public void testJtaTransactionRollback() throws Exception {
		doTestJtaTransaction(true);
	}

	private void doTestJtaTransaction(final boolean rollback) throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		if (rollback) {
			ut.rollback();
		}
		else {
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
			ut.commit();
		}
		utControl.setVoidCallable(1);
		utControl.replay();

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.close();
		conControl.setVoidCallable(1);
		conControl.replay();
		dsControl.replay();

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
				assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue("Is new transaction", status.isNewTransaction());

				Connection c = DataSourceUtils.getConnection(ds);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
				DataSourceUtils.releaseConnection(c, ds);

				c = DataSourceUtils.getConnection(ds);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
				DataSourceUtils.releaseConnection(c, ds);

				if (rollback) {
					status.setRollbackOnly();
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		dsControl.verify();
		conControl.verify();
		utControl.verify();
	}

	public void testJtaTransactionCommitWithPropagationRequiresNew() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, false, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithAccessAfterResume() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, true, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnection() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, true, false, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAccessed() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, true, true, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareDataSource() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, true, true);
	}

	public void testJtaTransactionRollbackWithPropagationRequiresNew() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, false, false);
	}

	public void testJtaTransactionRollbackWithPropagationRequiresNewWithAccessAfterResume() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, true, false);
	}

	public void testJtaTransactionRollbackWithPropagationRequiresNewWithOpenOuterConnection() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, true, false, false);
	}

	public void testJtaTransactionRollbackWithPropagationRequiresNewWithOpenOuterConnectionAccessed() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, true, true, false);
	}

	public void testJtaTransactionRollbackWithPropagationRequiresNewWithTransactionAwareDataSource() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, true, true);
	}

	private void doTestJtaTransactionWithPropagationRequiresNew(
			final boolean rollback, final boolean openOuterConnection, final boolean accessAfterResume,
			final boolean useTransactionAwareDataSource) throws Exception {

		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockControl txControl = MockControl.createControl(Transaction.class);
		Transaction tx = (Transaction) txControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 16);
		tm.suspend();
		tmControl.setReturnValue(tx, 5);
		ut.begin();
		utControl.setVoidCallable(5);
		ut.commit();
		utControl.setVoidCallable(5);
		tm.resume(tx);
		tmControl.setVoidCallable(5);
		if (rollback) {
			ut.rollback();
		}
		else {
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
			ut.commit();
		}
		utControl.setVoidCallable(1);
		utControl.replay();
		tmControl.replay();

		final MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		final MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.isReadOnly();
		conControl.setReturnValue(true, 1);
		if (!openOuterConnection) {
			con.close();
			conControl.setVoidCallable(1);
		}
		conControl.replay();
		dsControl.replay();

		final DataSource dsToUse = useTransactionAwareDataSource ?
				new TransactionAwareDataSourceProxy(ds) : ds;

		JtaTransactionManager ptm = new JtaTransactionManager(ut, tm);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
				assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue("Is new transaction", status.isNewTransaction());

				Connection c = DataSourceUtils.getConnection(dsToUse);
				try {
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
					c.isReadOnly();
					DataSourceUtils.releaseConnection(c, dsToUse);

					c = DataSourceUtils.getConnection(dsToUse);
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
					if (!openOuterConnection) {
						DataSourceUtils.releaseConnection(c, dsToUse);
					}
				}
				catch (SQLException ex) {
				}

				for (int i = 0; i < 5; i++) {

					tt.execute(new TransactionCallbackWithoutResult() {
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
							assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Is new transaction", status.isNewTransaction());

							try {
								dsControl.verify();
								conControl.verify();
								dsControl.reset();
								conControl.reset();
								ds.getConnection();
								dsControl.setReturnValue(con, 1);
								con.isReadOnly();
								conControl.setReturnValue(true, 1);
								con.close();
								conControl.setVoidCallable(1);
								dsControl.replay();
								conControl.replay();

								Connection c = DataSourceUtils.getConnection(dsToUse);
								c.isReadOnly();
								assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
								DataSourceUtils.releaseConnection(c, dsToUse);

								c = DataSourceUtils.getConnection(dsToUse);
								assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
								DataSourceUtils.releaseConnection(c, dsToUse);
							}
							catch (SQLException ex) {
							}
						}
					});

				}

				if (rollback) {
					status.setRollbackOnly();
				}

				if (accessAfterResume) {
					try {
						if (!openOuterConnection) {
							dsControl.verify();
							dsControl.reset();
							ds.getConnection();
							dsControl.setReturnValue(con, 1);
							dsControl.replay();
						}
						conControl.verify();
						conControl.reset();
						con.isReadOnly();
						conControl.setReturnValue(true, 1);
						con.close();
						conControl.setVoidCallable(1);
						conControl.replay();

						if (!openOuterConnection) {
							c = DataSourceUtils.getConnection(dsToUse);
						}
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
						c.isReadOnly();
						DataSourceUtils.releaseConnection(c, dsToUse);

						c = DataSourceUtils.getConnection(dsToUse);
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
						DataSourceUtils.releaseConnection(c, dsToUse);
					}
					catch (SQLException ex) {
					}
				}

				else {
					if (openOuterConnection) {
						try {
							conControl.verify();
							conControl.reset();
							con.close();
							conControl.setVoidCallable(1);
							conControl.replay();
						}
						catch (SQLException ex) {
						}
						DataSourceUtils.releaseConnection(c, dsToUse);
					}
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		dsControl.verify();
		conControl.verify();
		utControl.verify();
		tmControl.verify();
	}

	public void testJtaTransactionCommitWithPropagationRequiredWithinSupports() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(false, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiredWithinNotSupported() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(false, true);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithinSupports() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(true, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithinNotSupported() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(true, true);
	}

	private void doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(
			final boolean requiresNew, boolean notSupported) throws Exception {

		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockControl txControl = MockControl.createControl(Transaction.class);
		Transaction tx = (Transaction) txControl.getMock();
		if (notSupported) {
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
			tm.suspend();
			tmControl.setReturnValue(tx, 1);
		}
		else {
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		}
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
		ut.commit();
		utControl.setVoidCallable(1);
		if (notSupported) {
			tm.resume(tx);
			tmControl.setVoidCallable(1);
		}
		utControl.replay();
		tmControl.replay();
		txControl.replay();

		final MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		final MockControl con1Control = MockControl.createControl(Connection.class);
		final Connection con1 = (Connection) con1Control.getMock();
		final MockControl con2Control = MockControl.createControl(Connection.class);
		final Connection con2 = (Connection) con2Control.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con1, 1);
		ds.getConnection();
		dsControl.setReturnValue(con2, 1);
		con2.close();
		con2Control.setVoidCallable(1);
		con1.close();
		con1Control.setVoidCallable(1);
		dsControl.replay();
		con1Control.replay();
		con2Control.replay();

		final JtaTransactionManager ptm = new JtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(notSupported ?
				TransactionDefinition.PROPAGATION_NOT_SUPPORTED : TransactionDefinition.PROPAGATION_SUPPORTS);

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				assertSame(con1, DataSourceUtils.getConnection(ds));
				assertSame(con1, DataSourceUtils.getConnection(ds));

				TransactionTemplate tt2 = new TransactionTemplate(ptm);
				tt2.setPropagationBehavior(requiresNew ?
						TransactionDefinition.PROPAGATION_REQUIRES_NEW : TransactionDefinition.PROPAGATION_REQUIRED);
				tt2.execute(new TransactionCallbackWithoutResult() {
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						assertSame(con2, DataSourceUtils.getConnection(ds));
						assertSame(con2, DataSourceUtils.getConnection(ds));
					}
				});

				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				assertSame(con1, DataSourceUtils.getConnection(ds));
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		utControl.verify();
		tmControl.verify();
		txControl.verify();
		dsControl.verify();
		con1Control.verify();
		con2Control.verify();
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, false, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, true, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareDataSourceAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, false, true);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndTransactionAwareDataSourceAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, true, true);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, false, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, true, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndTransactionAwareDataSourceAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, true, true);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareDataSourceAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, false, true);
	}

	private void doTestJtaTransactionWithPropagationRequiresNewAndBeginException(boolean suspendException,
			final boolean openOuterConnection, final boolean useTransactionAwareDataSource) throws Exception {

		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockControl txControl = MockControl.createControl(Transaction.class);
		Transaction tx = (Transaction) txControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
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
		utControl.replay();
		tmControl.replay();

		final MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		final MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.isReadOnly();
		conControl.setReturnValue(true, 1);
		if (!openOuterConnection || useTransactionAwareDataSource) {
			con.close();
			conControl.setVoidCallable(1);
		}
		conControl.replay();
		dsControl.replay();

		final DataSource dsToUse = useTransactionAwareDataSource ?
				new TransactionAwareDataSourceProxy(ds) : ds;
		if (dsToUse instanceof TransactionAwareDataSourceProxy) {
			((TransactionAwareDataSourceProxy) dsToUse).setReobtainTransactionalConnections(true);
		}

		JtaTransactionManager ptm = new JtaTransactionManager(ut, tm);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
					assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Is new transaction", status.isNewTransaction());

					Connection c = DataSourceUtils.getConnection(dsToUse);
					try {
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
						c.isReadOnly();
						DataSourceUtils.releaseConnection(c, dsToUse);

						c = DataSourceUtils.getConnection(dsToUse);
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
						if (!openOuterConnection) {
							DataSourceUtils.releaseConnection(c, dsToUse);
						}
					}
					catch (SQLException ex) {
					}

					try {
						tt.execute(new TransactionCallbackWithoutResult() {
							protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
								assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
								assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
								assertTrue("Is new transaction", status.isNewTransaction());

								try {
									dsControl.verify();
									conControl.verify();
									dsControl.reset();
									conControl.reset();
									ds.getConnection();
									dsControl.setReturnValue(con, 1);
									con.close();
									conControl.setVoidCallable(1);
									dsControl.replay();
									conControl.replay();

									Connection c = DataSourceUtils.getConnection(dsToUse);
									assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
									DataSourceUtils.releaseConnection(c, dsToUse);

									c = DataSourceUtils.getConnection(dsToUse);
									assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
									DataSourceUtils.releaseConnection(c, dsToUse);
								}
								catch (SQLException ex) {
								}
							}
						});
					}
					finally {
						if (openOuterConnection) {
							try {
								dsControl.verify();
								dsControl.reset();
								conControl.verify();
								conControl.reset();

								if (useTransactionAwareDataSource) {
									ds.getConnection();
									dsControl.setReturnValue(con, 1);
								}
								con.isReadOnly();
								conControl.setReturnValue(true, 1);
								con.close();
								conControl.setVoidCallable(1);
								dsControl.replay();
								conControl.replay();

								c.isReadOnly();
								DataSourceUtils.releaseConnection(c, dsToUse);
							}
							catch (SQLException ex) {
							}
						}
					}
				}
			});

			fail("Should have thrown TransactionException");
		}
		catch (TransactionException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		dsControl.verify();
		conControl.verify();
		utControl.verify();
		tmControl.verify();
	}

	public void testJtaTransactionWithConnectionHolderStillBound() throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();

		JtaTransactionManager ptm = new JtaTransactionManager(ut) {
			protected void doRegisterAfterCompletionWithJtaTransaction(
					JtaTransactionObject txObject, final List<TransactionSynchronization> synchronizations) {
				Thread async = new Thread() {
					public void run() {
						invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_COMMITTED);
					}
				};
				async.start();
				try {
					async.join();
				}
				catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		};
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		for (int i = 0; i < 3; i++) {
			utControl.reset();
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
			utControl.replay();

			dsControl.reset();
			conControl.reset();
			ds.getConnection();
			dsControl.setReturnValue(con, 1);
			con.close();
			conControl.setVoidCallable(1);
			dsControl.replay();
			conControl.replay();

			final boolean releaseCon = (i != 1);

			tt.execute(new TransactionCallbackWithoutResult() {
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Is existing transaction", !status.isNewTransaction());

					Connection c = DataSourceUtils.getConnection(ds);
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
					DataSourceUtils.releaseConnection(c, ds);

					c = DataSourceUtils.getConnection(ds);
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
					if (releaseCon) {
						DataSourceUtils.releaseConnection(c, ds);
					}
				}
			});

			if (!releaseCon) {
				assertTrue("Still has connection holder", TransactionSynchronizationManager.hasResource(ds));
			}
			else {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
			}
			assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

			conControl.verify();
			dsControl.verify();
			utControl.verify();
		}
	}

	public void testJtaTransactionWithIsolationLevelDataSourceAdapter() throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.commit();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.commit();
		utControl.setVoidCallable(1);
		utControl.replay();

		MockControl ds1Control = MockControl.createControl(DataSource.class);
		final DataSource ds1 = (DataSource) ds1Control.getMock();
		MockControl con1Control = MockControl.createControl(Connection.class);
		final Connection con1 = (Connection) con1Control.getMock();
		ds1.getConnection();
		ds1Control.setReturnValue(con1, 1);
		con1.close();
		con1Control.setVoidCallable(1);
		ds1.getConnection();
		ds1Control.setReturnValue(con1, 1);
		con1.setReadOnly(true);
		con1Control.setVoidCallable(1);
		con1.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
		con1Control.setVoidCallable(1);
		con1.close();
		con1Control.setVoidCallable(1);
		con1Control.replay();
		ds1Control.replay();

		final IsolationLevelDataSourceAdapter dsToUse = new IsolationLevelDataSourceAdapter();
		dsToUse.setTargetDataSource(ds1);
		dsToUse.afterPropertiesSet();

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		ptm.setAllowCustomIsolationLevels(true);

		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Connection c = DataSourceUtils.getConnection(dsToUse);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(con1, c);
				DataSourceUtils.releaseConnection(c, dsToUse);
			}
		});

		tt.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		tt.setReadOnly(true);
		tt.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Connection c = DataSourceUtils.getConnection(dsToUse);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(con1, c);
				DataSourceUtils.releaseConnection(c, dsToUse);
			}
		});

		ds1Control.verify();
		con1Control.verify();
		utControl.verify();
	}

	public void testJtaTransactionWithIsolationLevelDataSourceRouter() throws Exception {
		doTestJtaTransactionWithIsolationLevelDataSourceRouter(false);
	}

	public void testJtaTransactionWithIsolationLevelDataSourceRouterWithDataSourceLookup() throws Exception {
		doTestJtaTransactionWithIsolationLevelDataSourceRouter(true);
	}

	private void doTestJtaTransactionWithIsolationLevelDataSourceRouter(boolean dataSourceLookup) throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.commit();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 2);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.commit();
		utControl.setVoidCallable(1);
		utControl.replay();

		MockControl ds1Control = MockControl.createControl(DataSource.class);
		final DataSource ds1 = (DataSource) ds1Control.getMock();
		MockControl con1Control = MockControl.createControl(Connection.class);
		final Connection con1 = (Connection) con1Control.getMock();
		ds1.getConnection();
		ds1Control.setReturnValue(con1, 1);
		con1.close();
		con1Control.setVoidCallable(1);
		con1Control.replay();
		ds1Control.replay();

		MockControl ds2Control = MockControl.createControl(DataSource.class);
		final DataSource ds2 = (DataSource) ds2Control.getMock();
		MockControl con2Control = MockControl.createControl(Connection.class);
		final Connection con2 = (Connection) con2Control.getMock();
		ds2.getConnection();
		ds2Control.setReturnValue(con2, 1);
		con2.close();
		con2Control.setVoidCallable(1);
		con2Control.replay();
		ds2Control.replay();

		final IsolationLevelDataSourceRouter dsToUse = new IsolationLevelDataSourceRouter();
		Map<Object, Object> targetDataSources = new HashMap<Object, Object>();
		if (dataSourceLookup) {
			targetDataSources.put("ISOLATION_REPEATABLE_READ", "ds2");
			dsToUse.setDefaultTargetDataSource("ds1");
			StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
			beanFactory.addBean("ds1", ds1);
			beanFactory.addBean("ds2", ds2);
			dsToUse.setDataSourceLookup(new BeanFactoryDataSourceLookup(beanFactory));
		}
		else {
			targetDataSources.put("ISOLATION_REPEATABLE_READ", ds2);
			dsToUse.setDefaultTargetDataSource(ds1);
		}
		dsToUse.setTargetDataSources(targetDataSources);
		dsToUse.afterPropertiesSet();

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		ptm.setAllowCustomIsolationLevels(true);

		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Connection c = DataSourceUtils.getConnection(dsToUse);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(con1, c);
				DataSourceUtils.releaseConnection(c, dsToUse);
			}
		});

		tt.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		tt.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Connection c = DataSourceUtils.getConnection(dsToUse);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(con2, c);
				DataSourceUtils.releaseConnection(c, dsToUse);
			}
		});

		ds1Control.verify();
		con1Control.verify();
		ds2Control.verify();
		con2Control.verify();
		utControl.verify();
	}

	protected void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionName());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

}

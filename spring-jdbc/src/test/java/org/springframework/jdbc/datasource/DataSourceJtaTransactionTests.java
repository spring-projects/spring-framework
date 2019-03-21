/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @since 17.10.2005
 */
public class DataSourceJtaTransactionTests {

	private Connection connection;
	private DataSource dataSource;
	private UserTransaction userTransaction;
	private TransactionManager transactionManager;
	private Transaction transaction;

	@Before
	public void setup() throws Exception {
		connection =mock(Connection.class);
		dataSource = mock(DataSource.class);
		userTransaction = mock(UserTransaction.class);
		transactionManager = mock(TransactionManager.class);
		transaction = mock(Transaction.class);
		given(dataSource.getConnection()).willReturn(connection);
	}

	@After
	public void verifyTransactionSynchronizationManagerState() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionName());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

	@Test
	public void testJtaTransactionCommit() throws Exception {
		doTestJtaTransaction(false);
	}

	@Test
	public void testJtaTransactionRollback() throws Exception {
		doTestJtaTransaction(true);
	}

	private void doTestJtaTransaction(final boolean rollback) throws Exception {
		if (rollback) {
			given(userTransaction.getStatus()).willReturn(
					Status.STATUS_NO_TRANSACTION,Status.STATUS_ACTIVE);
		}
		else {
			given(userTransaction.getStatus()).willReturn(
					Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		}

		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dataSource));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dataSource));
				assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue("Is new transaction", status.isNewTransaction());

				Connection c = DataSourceUtils.getConnection(dataSource);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dataSource));
				DataSourceUtils.releaseConnection(c, dataSource);

				c = DataSourceUtils.getConnection(dataSource);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dataSource));
				DataSourceUtils.releaseConnection(c, dataSource);

				if (rollback) {
					status.setRollbackOnly();
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dataSource));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		verify(userTransaction).begin();
		if (rollback) {
			verify(userTransaction).rollback();
		}
		verify(connection).close();
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNew() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, false, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithAccessAfterResume() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, true, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnection() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, true, false, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAccessed() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, true, true, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareDataSource() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, true, true);
	}

	@Test
	public void testJtaTransactionRollbackWithPropagationRequiresNew() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, false, false);
	}

	@Test
	public void testJtaTransactionRollbackWithPropagationRequiresNewWithAccessAfterResume() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, true, false);
	}

	@Test
	public void testJtaTransactionRollbackWithPropagationRequiresNewWithOpenOuterConnection() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, true, false, false);
	}

	@Test
	public void testJtaTransactionRollbackWithPropagationRequiresNewWithOpenOuterConnectionAccessed() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, true, true, false);
	}

	@Test
	public void testJtaTransactionRollbackWithPropagationRequiresNewWithTransactionAwareDataSource() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, true, true);
	}

	private void doTestJtaTransactionWithPropagationRequiresNew(
			final boolean rollback, final boolean openOuterConnection, final boolean accessAfterResume,
			final boolean useTransactionAwareDataSource) throws Exception {

		given(transactionManager.suspend()).willReturn(transaction);
		if (rollback) {
			given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
					Status.STATUS_ACTIVE);
		}
		else {
			given(userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
					Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		}

		given(connection.isReadOnly()).willReturn(true);

		final DataSource dsToUse = useTransactionAwareDataSource ?
				new TransactionAwareDataSourceProxy(dataSource) : dataSource;

		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction, transactionManager);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
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
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
							assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Is new transaction", status.isNewTransaction());

							try {
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
						DataSourceUtils.releaseConnection(c, dsToUse);
					}
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		verify(userTransaction, times(6)).begin();
		verify(transactionManager, times(5)).resume(transaction);
		if (rollback) {
			verify(userTransaction, times(5)).commit();
			verify(userTransaction).rollback();
		}
		else {
			verify(userTransaction, times(6)).commit();
		}
		if (accessAfterResume && !openOuterConnection) {
			verify(connection, times(7)).close();
		}
		else {
			verify(connection, times(6)).close();
		}
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiredWithinSupports() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(false, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiredWithinNotSupported() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(false, true);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithinSupports() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(true, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithinNotSupported() throws Exception {
		doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(true, true);
	}

	private void doTestJtaTransactionCommitWithNewTransactionWithinEmptyTransaction(
			final boolean requiresNew, boolean notSupported) throws Exception {

		if (notSupported) {
			given(userTransaction.getStatus()).willReturn(
					Status.STATUS_ACTIVE,
					Status.STATUS_NO_TRANSACTION,
					Status.STATUS_ACTIVE,
					Status.STATUS_ACTIVE);
			given(transactionManager.suspend()).willReturn(transaction);
		}
		else {
			given(userTransaction.getStatus()).willReturn(
					Status.STATUS_NO_TRANSACTION,
					Status.STATUS_NO_TRANSACTION,
					Status.STATUS_ACTIVE,
					Status.STATUS_ACTIVE);
		}

		final DataSource dataSource = mock(DataSource.class);
		final Connection connection1 = mock(Connection.class);
		final Connection connection2 = mock(Connection.class);
		given(dataSource.getConnection()).willReturn(connection1, connection2);

		final JtaTransactionManager ptm = new JtaTransactionManager(userTransaction, transactionManager);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(notSupported ?
				TransactionDefinition.PROPAGATION_NOT_SUPPORTED : TransactionDefinition.PROPAGATION_SUPPORTS);

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				assertSame(connection1, DataSourceUtils.getConnection(dataSource));
				assertSame(connection1, DataSourceUtils.getConnection(dataSource));

				TransactionTemplate tt2 = new TransactionTemplate(ptm);
				tt2.setPropagationBehavior(requiresNew ?
						TransactionDefinition.PROPAGATION_REQUIRES_NEW : TransactionDefinition.PROPAGATION_REQUIRED);
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						assertSame(connection2, DataSourceUtils.getConnection(dataSource));
						assertSame(connection2, DataSourceUtils.getConnection(dataSource));
					}
				});

				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
				assertSame(connection1, DataSourceUtils.getConnection(dataSource));
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		verify(userTransaction).begin();
		verify(userTransaction).commit();
		if (notSupported) {
			verify(transactionManager).resume(transaction);
		}
		verify(connection2).close();
		verify(connection1).close();
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, false, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, true, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareDataSourceAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, false, true);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndTransactionAwareDataSourceAndSuspendException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(true, true, true);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, false, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, true, false);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAndTransactionAwareDataSourceAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, true, true);
	}

	@Test
	public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareDataSourceAndBeginException() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNewAndBeginException(false, false, true);
	}

	private void doTestJtaTransactionWithPropagationRequiresNewAndBeginException(boolean suspendException,
			final boolean openOuterConnection, final boolean useTransactionAwareDataSource) throws Exception {

		given(userTransaction.getStatus()).willReturn(
				Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE,
				Status.STATUS_ACTIVE);
		if (suspendException) {
			given(transactionManager.suspend()).willThrow(new SystemException());
		}
		else {
			given(transactionManager.suspend()).willReturn(transaction);
			willThrow(new SystemException()).given(userTransaction).begin();
		}

		given(connection.isReadOnly()).willReturn(true);

		final DataSource dsToUse = useTransactionAwareDataSource ?
				new TransactionAwareDataSourceProxy(dataSource) : dataSource;
		if (dsToUse instanceof TransactionAwareDataSourceProxy) {
			((TransactionAwareDataSourceProxy) dsToUse).setReobtainTransactionalConnections(true);
		}

		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction, transactionManager);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
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
							@Override
							protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
								assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
								assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
								assertTrue("Is new transaction", status.isNewTransaction());

								Connection c = DataSourceUtils.getConnection(dsToUse);
								assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
								DataSourceUtils.releaseConnection(c, dsToUse);

								c = DataSourceUtils.getConnection(dsToUse);
								assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
								DataSourceUtils.releaseConnection(c, dsToUse);
							}
						});
					}
					finally {
						if (openOuterConnection) {
							try {
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

		verify(userTransaction).begin();
		if (suspendException) {
			verify(userTransaction).rollback();
		}

		if (suspendException) {
			verify(connection, atLeastOnce()).close();
		}
		else {
			verify(connection, never()).close();
		}
	}

	@Test
	public void testJtaTransactionWithConnectionHolderStillBound() throws Exception {
		@SuppressWarnings("serial")
		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction) {

			@Override
			protected void doRegisterAfterCompletionWithJtaTransaction(
					JtaTransactionObject txObject,
					final List<TransactionSynchronization> synchronizations)
					throws RollbackException, SystemException {
				Thread async = new Thread() {
					@Override
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
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dataSource));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		given(userTransaction.getStatus()).willReturn(Status.STATUS_ACTIVE);
		for (int i = 0; i < 3; i++) {
			final boolean releaseCon = (i != 1);

			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Is existing transaction", !status.isNewTransaction());

					Connection c = DataSourceUtils.getConnection(dataSource);
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dataSource));
					DataSourceUtils.releaseConnection(c, dataSource);

					c = DataSourceUtils.getConnection(dataSource);
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dataSource));
					if (releaseCon) {
						DataSourceUtils.releaseConnection(c, dataSource);
					}
				}
			});

			if (!releaseCon) {
				assertTrue("Still has connection holder", TransactionSynchronizationManager.hasResource(dataSource));
			}
			else {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dataSource));
			}
			assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		}
		verify(connection, times(3)).close();
	}

	@Test
	public void testJtaTransactionWithIsolationLevelDataSourceAdapter() throws Exception {
		given(userTransaction.getStatus()).willReturn(
				Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE,
				Status.STATUS_ACTIVE,
				Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE,
				Status.STATUS_ACTIVE);

		final IsolationLevelDataSourceAdapter dsToUse = new IsolationLevelDataSourceAdapter();
		dsToUse.setTargetDataSource(dataSource);
		dsToUse.afterPropertiesSet();

		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction);
		ptm.setAllowCustomIsolationLevels(true);

		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Connection c = DataSourceUtils.getConnection(dsToUse);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(connection, c);
				DataSourceUtils.releaseConnection(c, dsToUse);
			}
		});

		tt.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		tt.setReadOnly(true);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Connection c = DataSourceUtils.getConnection(dsToUse);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(connection, c);
				DataSourceUtils.releaseConnection(c, dsToUse);
			}
		});

		verify(userTransaction, times(2)).begin();
		verify(userTransaction, times(2)).commit();
		verify(connection).setReadOnly(true);
		verify(connection).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
		verify(connection, times(2)).close();
	}

	@Test
	public void testJtaTransactionWithIsolationLevelDataSourceRouter() throws Exception {
		doTestJtaTransactionWithIsolationLevelDataSourceRouter(false);
	}

	@Test
	public void testJtaTransactionWithIsolationLevelDataSourceRouterWithDataSourceLookup() throws Exception {
		doTestJtaTransactionWithIsolationLevelDataSourceRouter(true);
	}

	private void doTestJtaTransactionWithIsolationLevelDataSourceRouter(boolean dataSourceLookup) throws Exception {
given(		userTransaction.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		final DataSource dataSource1 = mock(DataSource.class);
		final Connection connection1 = mock(Connection.class);
		given(dataSource1.getConnection()).willReturn(connection1);

		final DataSource dataSource2 = mock(DataSource.class);
		final Connection connection2 = mock(Connection.class);
		given(dataSource2.getConnection()).willReturn(connection2);

		final IsolationLevelDataSourceRouter dsToUse = new IsolationLevelDataSourceRouter();
		Map<Object, Object> targetDataSources = new HashMap<>();
		if (dataSourceLookup) {
			targetDataSources.put("ISOLATION_REPEATABLE_READ", "ds2");
			dsToUse.setDefaultTargetDataSource("ds1");
			StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
			beanFactory.addBean("ds1", dataSource1);
			beanFactory.addBean("ds2", dataSource2);
			dsToUse.setDataSourceLookup(new BeanFactoryDataSourceLookup(beanFactory));
		}
		else {
			targetDataSources.put("ISOLATION_REPEATABLE_READ", dataSource2);
			dsToUse.setDefaultTargetDataSource(dataSource1);
		}
		dsToUse.setTargetDataSources(targetDataSources);
		dsToUse.afterPropertiesSet();

		JtaTransactionManager ptm = new JtaTransactionManager(userTransaction);
		ptm.setAllowCustomIsolationLevels(true);

		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Connection c = DataSourceUtils.getConnection(dsToUse);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(connection1, c);
				DataSourceUtils.releaseConnection(c, dsToUse);
			}
		});

		tt.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				Connection c = DataSourceUtils.getConnection(dsToUse);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				assertSame(connection2, c);
				DataSourceUtils.releaseConnection(c, dsToUse);
			}
		});

		verify(userTransaction, times(2)).begin();
		verify(userTransaction, times(2)).commit();
		verify(connection1).close();
		verify(connection2).close();
	}
}

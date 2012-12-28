/*
 * Copyright 2002-2010 the original author or authors.
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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import javax.sql.DataSource;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.support.nativejdbc.SimpleNativeJdbcExtractor;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Juergen Hoeller
 * @since 04.07.2003
 */
public class DataSourceTransactionManagerTests extends TestCase {

	public void testTransactionCommitWithAutoCommitTrue() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(true, false, false);
	}

	public void testTransactionCommitWithAutoCommitFalse() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(false, false, false);
	}

	public void testTransactionCommitWithAutoCommitTrueAndLazyConnection() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(true, true, false);
	}

	public void testTransactionCommitWithAutoCommitFalseAndLazyConnection() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(false, true, false);
	}

	public void testTransactionCommitWithAutoCommitTrueAndLazyConnectionAndStatementCreated() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(true, true, true);
	}

	public void testTransactionCommitWithAutoCommitFalseAndLazyConnectionAndStatementCreated() throws Exception {
		doTestTransactionCommitRestoringAutoCommit(false, true, true);
	}

	private void doTestTransactionCommitRestoringAutoCommit(
			boolean autoCommit, boolean lazyConnection, final boolean createStatement)
			throws Exception {

		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();

		if (lazyConnection) {
			ds.getConnection();
			dsControl.setReturnValue(con, 1);
			if (createStatement) {
				con.getMetaData();
				conControl.setReturnValue(null, 1);
			}
			con.getAutoCommit();
			conControl.setReturnValue(autoCommit, 1);
			con.getTransactionIsolation();
			conControl.setReturnValue(Connection.TRANSACTION_READ_COMMITTED, 1);
			con.close();
			conControl.setVoidCallable(1);
		}

		if (!lazyConnection || createStatement) {
			ds.getConnection();
			dsControl.setReturnValue(con, 1);
			con.getAutoCommit();
			conControl.setReturnValue(autoCommit, 1);
			if (autoCommit) {
				// Must disable autocommit
				con.setAutoCommit(false);
				conControl.setVoidCallable(1);
			}
			if (createStatement) {
				con.createStatement();
				conControl.setReturnValue(null, 1);
			}
			con.commit();
			conControl.setVoidCallable(1);
			con.isReadOnly();
			conControl.setReturnValue(false, 1);
			if (autoCommit) {
				// must restore autoCommit
				con.setAutoCommit(true);
				conControl.setVoidCallable(1);
			}
			con.close();
			conControl.setVoidCallable(1);
		}

		conControl.replay();
		dsControl.replay();

		final DataSource dsToUse = (lazyConnection ? new LazyConnectionDataSourceProxy(ds) : ds);
		PlatformTransactionManager tm = new DataSourceTransactionManager(dsToUse);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue("Is new transaction", status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				Connection tCon = DataSourceUtils.getConnection(dsToUse);
				try {
					if (createStatement) {
						tCon.createStatement();
						assertEquals(con, new SimpleNativeJdbcExtractor().getNativeConnection(tCon));
					}
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		conControl.verify();
		dsControl.verify();
	}

	public void testTransactionRollbackWithAutoCommitTrue() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(true, false, false);
	}

	public void testTransactionRollbackWithAutoCommitFalse() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(false, false, false);
	}

	public void testTransactionRollbackWithAutoCommitTrueAndLazyConnection() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(true, true, false);
	}

	public void testTransactionRollbackWithAutoCommitFalseAndLazyConnection() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(false, true, false);
	}

	public void testTransactionRollbackWithAutoCommitTrueAndLazyConnectionAndCreateStatement() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(true, true, true);
	}

	public void testTransactionRollbackWithAutoCommitFalseAndLazyConnectionAndCreateStatement() throws Exception  {
		doTestTransactionRollbackRestoringAutoCommit(false, true, true);
	}

	private void doTestTransactionRollbackRestoringAutoCommit(
			boolean autoCommit, boolean lazyConnection, final boolean createStatement) throws Exception {

		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();

		if (lazyConnection) {
			ds.getConnection();
			dsControl.setReturnValue(con, 1);
			con.getAutoCommit();
			conControl.setReturnValue(autoCommit, 1);
			con.getTransactionIsolation();
			conControl.setReturnValue(Connection.TRANSACTION_READ_COMMITTED, 1);
			con.close();
			conControl.setVoidCallable(1);
		}

		if (!lazyConnection || createStatement) {
			ds.getConnection();
			dsControl.setReturnValue(con, 1);
			con.getAutoCommit();
			conControl.setReturnValue(autoCommit, 1);
			if (autoCommit) {
				// Must disable autocommit
				con.setAutoCommit(false);
				conControl.setVoidCallable(1);
			}
			if (createStatement) {
				con.createStatement();
				conControl.setReturnValue(null, 1);
			}
			con.rollback();
			conControl.setVoidCallable(1);
			con.isReadOnly();
			conControl.setReturnValue(false, 1);
			if (autoCommit) {
				// Must restore autocommit
				con.setAutoCommit(true);
				conControl.setVoidCallable(1);
			}
			con.close();
			conControl.setVoidCallable(1);
		}

		conControl.replay();
		dsControl.replay();

		final DataSource dsToUse = (lazyConnection ? new LazyConnectionDataSourceProxy(ds) : ds);
		PlatformTransactionManager tm = new DataSourceTransactionManager(dsToUse);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		final RuntimeException ex = new RuntimeException("Application exception");
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
					assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Is new transaction", status.isNewTransaction());
					Connection con = DataSourceUtils.getConnection(dsToUse);
					if (createStatement) {
						try {
							con.createStatement();
						}
						catch (SQLException ex) {
							throw new UncategorizedSQLException("", "", ex);
						}
					}
					throw ex;
				}
			});
			fail("Should have thrown RuntimeException");
		}
		catch (RuntimeException ex2) {
			// expected
			assertTrue("Correct exception thrown", ex2.equals(ex));
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());
		conControl.verify();
		dsControl.verify();
	}

	public void testTransactionRollbackOnly() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		conControl.replay();
		dsControl.replay();

		DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
		tm.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		ConnectionHolder conHolder = new ConnectionHolder(con);
		conHolder.setTransactionActive(true);
		TransactionSynchronizationManager.bindResource(ds, conHolder);
		final RuntimeException ex = new RuntimeException("Application exception");
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
					assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Is existing transaction", !status.isNewTransaction());
					throw ex;
				}
			});
			fail("Should have thrown RuntimeException");
		}
		catch (RuntimeException ex2) {
			// expected
			assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());
			assertEquals("Correct exception thrown", ex, ex2);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(ds);
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	public void testParticipatingTransactionWithRollbackOnly() throws Exception {
		doTestParticipatingTransactionWithRollbackOnly(false);
	}

	public void testParticipatingTransactionWithRollbackOnlyAndFailEarly() throws Exception {
		doTestParticipatingTransactionWithRollbackOnly(true);
	}

	private void doTestParticipatingTransactionWithRollbackOnly(boolean failEarly) throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		con.rollback();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		conControl.replay();
		dsControl.replay();

		DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
		if (failEarly) {
			tm.setFailEarlyOnGlobalRollbackOnly(true);
		}
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		TestTransactionSynchronization synch =
				new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_ROLLED_BACK);
		TransactionSynchronizationManager.registerSynchronization(synch);

		boolean outerTransactionBoundaryReached = false;
		try {
			assertTrue("Is new transaction", ts.isNewTransaction());

			final TransactionTemplate tt = new TransactionTemplate(tm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Is existing transaction", !status.isNewTransaction());
					assertFalse("Is not rollback-only", status.isRollbackOnly());
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
							assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Is existing transaction", !status.isNewTransaction());
							status.setRollbackOnly();
						}
					});
					assertTrue("Is existing transaction", !status.isNewTransaction());
					assertTrue("Is rollback-only", status.isRollbackOnly());
				}
			});

			outerTransactionBoundaryReached = true;
			tm.commit(ts);

			fail("Should have thrown UnexpectedRollbackException");
		}
		catch (UnexpectedRollbackException ex) {
			// expected
			if (!outerTransactionBoundaryReached) {
				tm.rollback(ts);
			}
			if (failEarly) {
				assertFalse(outerTransactionBoundaryReached);
			}
			else {
				assertTrue(outerTransactionBoundaryReached);
			}
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertFalse(synch.beforeCommitCalled);
		assertTrue(synch.beforeCompletionCalled);
		assertFalse(synch.afterCommitCalled);
		assertTrue(synch.afterCompletionCalled);
		conControl.verify();
		dsControl.verify();
	}

	public void testParticipatingTransactionWithIncompatibleIsolationLevel() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		con.rollback();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		conControl.replay();
		dsControl.replay();

		DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
		tm.setValidateExistingTransaction(true);

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			final TransactionTemplate tt = new TransactionTemplate(tm);
			final TransactionTemplate tt2 = new TransactionTemplate(tm);
			tt2.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertFalse("Is not rollback-only", status.isRollbackOnly());
					tt2.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							status.setRollbackOnly();
						}
					});
					assertTrue("Is rollback-only", status.isRollbackOnly());
				}
			});

			fail("Should have thrown IllegalTransactionStateException");
		}
		catch (IllegalTransactionStateException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	public void testParticipatingTransactionWithIncompatibleReadOnly() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		con.rollback();
		conControl.setVoidCallable(1);
		con.setReadOnly(true);
		conControl.setThrowable(new SQLException("read-only not supported"), 1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		conControl.replay();
		dsControl.replay();

		DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
		tm.setValidateExistingTransaction(true);

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			final TransactionTemplate tt = new TransactionTemplate(tm);
			tt.setReadOnly(true);
			final TransactionTemplate tt2 = new TransactionTemplate(tm);
			tt2.setReadOnly(false);

			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertFalse("Is not rollback-only", status.isRollbackOnly());
					tt2.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							status.setRollbackOnly();
						}
					});
					assertTrue("Is rollback-only", status.isRollbackOnly());
				}
			});

			fail("Should have thrown IllegalTransactionStateException");
		}
		catch (IllegalTransactionStateException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	public void testParticipatingTransactionWithTransactionStartedFromSynch() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 2);
		con.commit();
		conControl.setVoidCallable(2);
		con.isReadOnly();
		conControl.setReturnValue(false, 2);
		con.close();
		conControl.setVoidCallable(2);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 2);
		conControl.replay();
		dsControl.replay();

		DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		final TestTransactionSynchronization synch =
				new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_COMMITTED) {
					@Override
					public void afterCompletion(int status) {
						super.afterCompletion(status);
						tt.execute(new TransactionCallbackWithoutResult() {
							@Override
							protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							}
						});
					}
				};

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				TransactionSynchronizationManager.registerSynchronization(synch);
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue(synch.beforeCommitCalled);
		assertTrue(synch.beforeCompletionCalled);
		assertTrue(synch.afterCommitCalled);
		assertTrue(synch.afterCompletionCalled);
		conControl.verify();
		dsControl.verify();
	}

	public void testParticipatingTransactionWithRollbackOnlyAndInnerSynch() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		con.rollback();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		conControl.replay();
		dsControl.replay();

		DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
		tm.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
		DataSourceTransactionManager tm2 = new DataSourceTransactionManager(ds);
		// tm has no synch enabled (used at outer level), tm2 has synch enabled (inner level)

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		final TestTransactionSynchronization synch =
				new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_UNKNOWN);

		try {
			assertTrue("Is new transaction", ts.isNewTransaction());

			final TransactionTemplate tt = new TransactionTemplate(tm2);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Is existing transaction", !status.isNewTransaction());
					assertFalse("Is not rollback-only", status.isRollbackOnly());
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
							assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Is existing transaction", !status.isNewTransaction());
							status.setRollbackOnly();
						}
					});
					assertTrue("Is existing transaction", !status.isNewTransaction());
					assertTrue("Is rollback-only", status.isRollbackOnly());
					TransactionSynchronizationManager.registerSynchronization(synch);
				}
			});

			tm.commit(ts);

			fail("Should have thrown UnexpectedRollbackException");
		}
		catch (UnexpectedRollbackException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertFalse(synch.beforeCommitCalled);
		assertTrue(synch.beforeCompletionCalled);
		assertFalse(synch.afterCommitCalled);
		assertTrue(synch.afterCompletionCalled);
		conControl.verify();
		dsControl.verify();
	}

	public void testPropagationRequiresNewWithExistingTransaction() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 2);
		con.rollback();
		conControl.setVoidCallable(1);
		con.commit();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 2);
		con.close();
		conControl.setVoidCallable(2);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 2);
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Is new transaction", status.isNewTransaction());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						status.setRollbackOnly();
					}
				});
				assertTrue("Is new transaction", status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	public void testPropagationRequiresNewWithExistingTransactionAndUnrelatedDataSource() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		con.commit();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		conControl.replay();
		dsControl.replay();

		MockControl con2Control = MockControl.createControl(Connection.class);
		Connection con2 = (Connection) con2Control.getMock();
		con2.getAutoCommit();
		con2Control.setReturnValue(false, 1);
		con2.rollback();
		con2Control.setVoidCallable(1);
		con2.isReadOnly();
		con2Control.setReturnValue(false, 1);
		con2.close();
		con2Control.setVoidCallable(1);

		MockControl ds2Control = MockControl.createControl(DataSource.class);
		final DataSource ds2 = (DataSource) ds2Control.getMock();
		ds2.getConnection();
		ds2Control.setReturnValue(con2, 1);
		con2Control.replay();
		ds2Control.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		PlatformTransactionManager tm2 = new DataSourceTransactionManager(ds2);
		final TransactionTemplate tt2 = new TransactionTemplate(tm2);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds2));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Is new transaction", status.isNewTransaction());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						status.setRollbackOnly();
					}
				});
				assertTrue("Is new transaction", status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds2));
		conControl.verify();
		dsControl.verify();
		con2Control.verify();
		ds2Control.verify();
	}

	public void testPropagationRequiresNewWithExistingTransactionAndUnrelatedFailingDataSource() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		con.rollback();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		conControl.replay();
		dsControl.replay();

		MockControl ds2Control = MockControl.createControl(DataSource.class);
		final DataSource ds2 = (DataSource) ds2Control.getMock();
		SQLException failure = new SQLException();
		ds2.getConnection();
		ds2Control.setThrowable(failure);
		ds2Control.replay();

		DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		DataSourceTransactionManager tm2 = new DataSourceTransactionManager(ds2);
		tm2.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
		final TransactionTemplate tt2 = new TransactionTemplate(tm2);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds2));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Is new transaction", status.isNewTransaction());
					assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
					assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
					assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
					tt2.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							status.setRollbackOnly();
						}
					});
				}
			});
			fail("Should have thrown CannotCreateTransactionException");
		}
		catch (CannotCreateTransactionException ex) {
			assertSame(failure, ex.getCause());
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds2));
		conControl.verify();
		dsControl.verify();
		ds2Control.verify();
	}

	public void testPropagationNotSupportedWithExistingTransaction() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		con.commit();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Isn't new transaction", !status.isNewTransaction());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
						status.setRollbackOnly();
					}
				});
				assertTrue("Is new transaction", status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	public void testPropagationNeverWithExistingTransaction() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		con.rollback();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("Is new transaction", status.isNewTransaction());
					tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							fail("Should have thrown IllegalTransactionStateException");
						}
					});
					fail("Should have thrown IllegalTransactionStateException");
				}
			});
		}
		catch (IllegalTransactionStateException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	public void testPropagationSupportsAndRequiresNew() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		con.commit();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);

		dsControl.replay();
		conControl.replay();

		final PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
				TransactionTemplate tt2 = new TransactionTemplate(tm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Is new transaction", status.isNewTransaction());
						assertSame(con, DataSourceUtils.getConnection(ds));
						assertSame(con, DataSourceUtils.getConnection(ds));
					}
				});
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		dsControl.verify();
		conControl.verify();
	}

	public void testPropagationSupportsAndRequiresNewWithEarlyAccess() throws Exception {
		MockControl con1Control = MockControl.createControl(Connection.class);
		final Connection con1 = (Connection) con1Control.getMock();
		con1.close();
		con1Control.setVoidCallable(1);

		MockControl con2Control = MockControl.createControl(Connection.class);
		final Connection con2 = (Connection) con2Control.getMock();
		con2.getAutoCommit();
		con2Control.setReturnValue(false, 1);
		con2.commit();
		con2Control.setVoidCallable(1);
		con2.isReadOnly();
		con2Control.setReturnValue(false, 1);
		con2.close();
		con2Control.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con1, 1);
		ds.getConnection();
		dsControl.setReturnValue(con2, 1);

		dsControl.replay();
		con1Control.replay();
		con2Control.replay();

		final PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
				assertSame(con1, DataSourceUtils.getConnection(ds));
				assertSame(con1, DataSourceUtils.getConnection(ds));
				TransactionTemplate tt2 = new TransactionTemplate(tm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Is new transaction", status.isNewTransaction());
						assertSame(con2, DataSourceUtils.getConnection(ds));
						assertSame(con2, DataSourceUtils.getConnection(ds));
					}
				});
				assertSame(con1, DataSourceUtils.getConnection(ds));
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		dsControl.verify();
		con1Control.verify();
		con2Control.verify();
	}

	public void testTransactionWithIsolationAndReadOnly() throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();

		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.getTransactionIsolation();
		conControl.setReturnValue(Connection.TRANSACTION_READ_COMMITTED, 1);
		con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		conControl.setVoidCallable(1);
		con.setReadOnly(true);
		conControl.setVoidCallable(1);
		con.getAutoCommit();
		conControl.setReturnValue(true, 1);
		con.setAutoCommit(false);
		conControl.setVoidCallable(1);
		con.commit();
		conControl.setVoidCallable(1);
		con.setAutoCommit(true);
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		tt.setReadOnly(true);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				// something transactional
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	public void testTransactionWithLongTimeout() throws Exception {
		doTestTransactionWithTimeout(10);
	}

	public void testTransactionWithShortTimeout() throws Exception {
		doTestTransactionWithTimeout(1);
	}

	private void doTestTransactionWithTimeout(int timeout) throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		PreparedStatement ps = (PreparedStatement) psControl.getMock();

		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.getAutoCommit();
		conControl.setReturnValue(true, 1);
		con.setAutoCommit(false);
		conControl.setVoidCallable(1);
		con.prepareStatement("some SQL statement");
		conControl.setReturnValue(ps, 1);
		if (timeout > 1) {
			ps.setQueryTimeout(timeout - 1);
			psControl.setVoidCallable(1);
			con.commit();
		}
		else {
			con.rollback();
		}
		conControl.setVoidCallable(1);
		con.setAutoCommit(true);
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		psControl.replay();
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setTimeout(timeout);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));

		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					try {
						Thread.sleep(1500);
					}
					catch (InterruptedException ex) {
					}
					try {
						Connection con = DataSourceUtils.getConnection(ds);
						PreparedStatement ps = con.prepareStatement("some SQL statement");
						DataSourceUtils.applyTransactionTimeout(ps, ds);
					}
					catch (SQLException ex) {
						throw new DataAccessResourceFailureException("", ex);
					}
				}
			});
			if (timeout <= 1) {
				fail("Should have thrown TransactionTimedOutException");
			}
		}
		catch (TransactionTimedOutException ex) {
			if (timeout <= 1) {
				// expected
			}
			else {
				throw ex;
			}
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
		psControl.verify();
	}

	public void testTransactionAwareDataSourceProxy() throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();

		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.getMetaData();
		conControl.setReturnValue(null, 1);
		con.getAutoCommit();
		conControl.setReturnValue(true, 1);
		con.setAutoCommit(false);
		conControl.setVoidCallable(1);
		con.commit();
		conControl.setVoidCallable(1);
		con.setAutoCommit(true);
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertEquals(con, DataSourceUtils.getConnection(ds));
				TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
				try {
					assertEquals(con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
					assertEquals(con, new SimpleNativeJdbcExtractor().getNativeConnection(dsProxy.getConnection()));
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	public void testTransactionAwareDataSourceProxyWithSuspension() throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();

		ds.getConnection();
		dsControl.setReturnValue(con, 2);
		con.getMetaData();
		conControl.setReturnValue(null, 2);
		con.getAutoCommit();
		conControl.setReturnValue(true, 2);
		con.setAutoCommit(false);
		conControl.setVoidCallable(2);
		con.commit();
		conControl.setVoidCallable(2);
		con.setAutoCommit(true);
		conControl.setVoidCallable(2);
		con.isReadOnly();
		conControl.setReturnValue(false, 2);
		con.close();
		conControl.setVoidCallable(2);

		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertEquals(con, DataSourceUtils.getConnection(ds));
				final TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
				try {
					assertEquals(con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
					assertEquals(con, new SimpleNativeJdbcExtractor().getNativeConnection(dsProxy.getConnection()));
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}

				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						// something transactional
						assertEquals(con, DataSourceUtils.getConnection(ds));
						try {
							assertEquals(con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
							assertEquals(con, new SimpleNativeJdbcExtractor().getNativeConnection(dsProxy.getConnection()));
							// should be ignored
							dsProxy.getConnection().close();
						}
						catch (SQLException ex) {
							throw new UncategorizedSQLException("", "", ex);
						}
					}
				});

				try {
					assertEquals(con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	public void testTransactionAwareDataSourceProxyWithSuspensionAndReobtaining() throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();

		ds.getConnection();
		dsControl.setReturnValue(con, 2);
		con.getMetaData();
		conControl.setReturnValue(null, 2);
		con.getAutoCommit();
		conControl.setReturnValue(true, 2);
		con.setAutoCommit(false);
		conControl.setVoidCallable(2);
		con.commit();
		conControl.setVoidCallable(2);
		con.setAutoCommit(true);
		conControl.setVoidCallable(2);
		con.isReadOnly();
		conControl.setReturnValue(false, 2);
		con.close();
		conControl.setVoidCallable(2);

		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertEquals(con, DataSourceUtils.getConnection(ds));
				final TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
				dsProxy.setReobtainTransactionalConnections(true);
				try {
					assertEquals(con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
					assertEquals(con, new SimpleNativeJdbcExtractor().getNativeConnection(dsProxy.getConnection()));
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}

				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						// something transactional
						assertEquals(con, DataSourceUtils.getConnection(ds));
						try {
							assertEquals(con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
							assertEquals(con, new SimpleNativeJdbcExtractor().getNativeConnection(dsProxy.getConnection()));
							// should be ignored
							dsProxy.getConnection().close();
						}
						catch (SQLException ex) {
							throw new UncategorizedSQLException("", "", ex);
						}
					}
				});

				try {
					assertEquals(con, ((ConnectionProxy) dsProxy.getConnection()).getTargetConnection());
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	/**
	 * Test behavior if the first operation on a connection (getAutoCommit) throws SQLException.
	 */
	public void testTransactionWithExceptionOnBegin() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.getAutoCommit();
		conControl.setThrowable(new SQLException("Cannot begin"));
		con.close();
		conControl.setVoidCallable();
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
			fail("Should have thrown CannotCreateTransactionException");
		}
		catch (CannotCreateTransactionException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
	}

	public void testTransactionWithExceptionOnCommit() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.getAutoCommit();
		// No need to restore it
		conControl.setReturnValue(false, 1);
		con.commit();
		conControl.setThrowable(new SQLException("Cannot commit"), 1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
	}

	public void testTransactionWithExceptionOnCommitAndRollbackOnCommitFailure() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.getAutoCommit();
		// No need to change or restore
		conControl.setReturnValue(false);
		con.commit();
		conControl.setThrowable(new SQLException("Cannot commit"), 1);
		con.rollback();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);
		conControl.replay();
		dsControl.replay();

		DataSourceTransactionManager tm = new DataSourceTransactionManager(ds);
		tm.setRollbackOnCommitFailure(true);
		TransactionTemplate tt = new TransactionTemplate(tm);
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
	}

	public void testTransactionWithExceptionOnRollback() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.getAutoCommit();
		conControl.setReturnValue(true, 1);
		// Must restore
		con.setAutoCommit(false);
		conControl.setVoidCallable(1);

		con.rollback();
		conControl.setThrowable(new SQLException("Cannot rollback"), 1);
		con.setAutoCommit(true);
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					status.setRollbackOnly();
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
		}

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
	}

	public void testTransactionWithPropagationSupports() throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
				assertTrue("Is not new transaction", !status.isNewTransaction());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		dsControl.verify();
	}

	public void testTransactionWithPropagationNotSupported() throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
				assertTrue("Is not new transaction", !status.isNewTransaction());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		dsControl.verify();
	}

	public void testTransactionWithPropagationNever() throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
				assertTrue("Is not new transaction", !status.isNewTransaction());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		dsControl.verify();
	}

	public void testExistingTransactionWithPropagationNested() throws Exception {
		doTestExistingTransactionWithPropagationNested(1);
	}

	public void testExistingTransactionWithPropagationNestedTwice() throws Exception {
		doTestExistingTransactionWithPropagationNested(2);
	}

	private void doTestExistingTransactionWithPropagationNested(final int count) throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl mdControl = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData md = (DatabaseMetaData) mdControl.getMock();
		MockControl spControl = MockControl.createControl(Savepoint.class);
		Savepoint sp = (Savepoint) spControl.getMock();

		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		md.supportsSavepoints();
		mdControl.setReturnValue(true, 1);
		con.getMetaData();
		conControl.setReturnValue(md, 1);
		for (int i = 1; i <= count; i++) {
			con.setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + i);
			conControl.setReturnValue(sp, 1);
			con.releaseSavepoint(sp);
			conControl.setVoidCallable(1);
		}
		con.commit();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);
		ds.getConnection();
		dsControl.setReturnValue(con, 1);

		spControl.replay();
		mdControl.replay();
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
				for (int i = 0; i < count; i++) {
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
							assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Isn't new transaction", !status.isNewTransaction());
							assertTrue("Is nested transaction", status.hasSavepoint());
						}
					});
				}
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		spControl.verify();
		mdControl.verify();
		conControl.verify();
		dsControl.verify();
	}

	public void testExistingTransactionWithPropagationNestedAndRollback() throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl mdControl = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData md = (DatabaseMetaData) mdControl.getMock();
		MockControl spControl = MockControl.createControl(Savepoint.class);
		Savepoint sp = (Savepoint) spControl.getMock();

		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		md.supportsSavepoints();
		mdControl.setReturnValue(true, 1);
		con.getMetaData();
		conControl.setReturnValue(md, 1);
		con.setSavepoint("SAVEPOINT_1");
		conControl.setReturnValue(sp, 1);
		con.rollback(sp);
		conControl.setVoidCallable(1);
		con.commit();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		spControl.replay();
		mdControl.replay();
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
						assertTrue("Synchronization active", TransactionSynchronizationManager.isSynchronizationActive());
						assertTrue("Isn't new transaction", !status.isNewTransaction());
						assertTrue("Is nested transaction", status.hasSavepoint());
						status.setRollbackOnly();
					}
				});
				assertTrue("Is new transaction", status.isNewTransaction());
				assertTrue("Isn't nested transaction", !status.hasSavepoint());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		spControl.verify();
		mdControl.verify();
		conControl.verify();
		dsControl.verify();
	}

	public void testExistingTransactionWithManualSavepoint() throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl mdControl = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData md = (DatabaseMetaData) mdControl.getMock();
		MockControl spControl = MockControl.createControl(Savepoint.class);
		Savepoint sp = (Savepoint) spControl.getMock();

		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		md.supportsSavepoints();
		mdControl.setReturnValue(true, 1);
		con.getMetaData();
		conControl.setReturnValue(md, 1);
		con.setSavepoint("SAVEPOINT_1");
		conControl.setReturnValue(sp, 1);
		con.releaseSavepoint(sp);
		conControl.setVoidCallable(1);
		con.commit();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);
		ds.getConnection();
		dsControl.setReturnValue(con, 1);

		spControl.replay();
		mdControl.replay();
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				Object savepoint = status.createSavepoint();
				status.releaseSavepoint(savepoint);
				assertTrue("Is new transaction", status.isNewTransaction());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		spControl.verify();
		mdControl.verify();
		conControl.verify();
		dsControl.verify();
	}

	public void testExistingTransactionWithManualSavepointAndRollback() throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl mdControl = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData md = (DatabaseMetaData) mdControl.getMock();
		MockControl spControl = MockControl.createControl(Savepoint.class);
		Savepoint sp = (Savepoint) spControl.getMock();

		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		md.supportsSavepoints();
		mdControl.setReturnValue(true, 1);
		con.getMetaData();
		conControl.setReturnValue(md, 1);
		con.setSavepoint("SAVEPOINT_1");
		conControl.setReturnValue(sp, 1);
		con.rollback(sp);
		conControl.setVoidCallable(1);
		con.commit();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		spControl.replay();
		mdControl.replay();
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				Object savepoint = status.createSavepoint();
				status.rollbackToSavepoint(savepoint);
				assertTrue("Is new transaction", status.isNewTransaction());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		spControl.verify();
		mdControl.verify();
		conControl.verify();
		dsControl.verify();
	}

	public void testTransactionWithPropagationNested() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		con.commit();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	public void testTransactionWithPropagationNestedAndRollback() throws Exception {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		con.getAutoCommit();
		conControl.setReturnValue(false, 1);
		con.rollback();
		conControl.setVoidCallable(1);
		con.isReadOnly();
		conControl.setReturnValue(false, 1);
		con.close();
		conControl.setVoidCallable(1);

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		conControl.replay();
		dsControl.replay();

		PlatformTransactionManager tm = new DataSourceTransactionManager(ds);
		final TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("Synchronization not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Is new transaction", status.isNewTransaction());
				status.setRollbackOnly();
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		conControl.verify();
		dsControl.verify();
	}

	@Override
	protected void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}


	private static class TestTransactionSynchronization implements TransactionSynchronization {

		private DataSource dataSource;
		private int status;

		public boolean beforeCommitCalled;
		public boolean beforeCompletionCalled;
		public boolean afterCommitCalled;
		public boolean afterCompletionCalled;

		public TestTransactionSynchronization(DataSource dataSource, int status) {
			this.dataSource = dataSource;
			this.status = status;
		}

		@Override
		public void suspend() {
		}

		@Override
		public void resume() {
		}

		@Override
		public void flush() {
		}

		@Override
		public void beforeCommit(boolean readOnly) {
			if (this.status != TransactionSynchronization.STATUS_COMMITTED) {
				fail("Should never be called");
			}
			assertFalse(this.beforeCommitCalled);
			this.beforeCommitCalled = true;
		}

		@Override
		public void beforeCompletion() {
			assertFalse(this.beforeCompletionCalled);
			this.beforeCompletionCalled = true;
		}

		@Override
		public void afterCommit() {
			if (this.status != TransactionSynchronization.STATUS_COMMITTED) {
				fail("Should never be called");
			}
			assertFalse(this.afterCommitCalled);
			this.afterCommitCalled = true;
		}

		@Override
		public void afterCompletion(int status) {
			assertFalse(this.afterCompletionCalled);
			this.afterCompletionCalled = true;
			assertTrue(status == this.status);
			assertTrue(TransactionSynchronizationManager.hasResource(this.dataSource));
		}
	}

}

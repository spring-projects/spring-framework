/*
 * Copyright 2002-present the original author or authors.
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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * @author Juergen Hoeller
 * @since 04.07.2003
 * @see org.springframework.jdbc.support.JdbcTransactionManagerTests
 */
public class DataSourceTransactionManagerTests {

	protected DataSource ds = mock();

	protected ConnectionProxy con = mock();

	protected DataSourceTransactionManager tm;


	@BeforeEach
	void setup() throws Exception {
		tm = createTransactionManager(ds);
		given(ds.getConnection()).willReturn(con);
		given(con.getTargetConnection()).willThrow(new UnsupportedOperationException());
	}

	protected DataSourceTransactionManager createTransactionManager(DataSource ds) {
		return new DataSourceTransactionManager(ds);
	}

	@AfterEach
	void verifyTransactionSynchronizationManagerState() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}


	@Test
	void transactionCommitWithAutoCommitTrue() throws Exception {
		testTransactionCommitRestoringAutoCommit(true, false, false);
	}

	@Test
	void transactionCommitWithAutoCommitFalse() throws Exception {
		testTransactionCommitRestoringAutoCommit(false, false, false);
	}

	@Test
	void transactionCommitWithAutoCommitTrueAndLazyConnection() throws Exception {
		testTransactionCommitRestoringAutoCommit(true, true, false);
	}

	@Test
	void transactionCommitWithAutoCommitFalseAndLazyConnection() throws Exception {
		testTransactionCommitRestoringAutoCommit(false, true, false);
	}

	@Test
	void transactionCommitWithAutoCommitTrueAndLazyConnectionAndStatementCreated() throws Exception {
		testTransactionCommitRestoringAutoCommit(true, true, true);
	}

	@Test
	void transactionCommitWithAutoCommitFalseAndLazyConnectionAndStatementCreated() throws Exception {
		testTransactionCommitRestoringAutoCommit(false, true, true);
	}

	private void testTransactionCommitRestoringAutoCommit(
			boolean autoCommit, boolean lazyConnection, boolean createStatement) throws Exception {

		given(con.getAutoCommit()).willReturn(autoCommit);

		if (lazyConnection) {
			given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
			given(con.getWarnings()).willThrow(new SQLException());
		}

		DataSource dsToUse = (lazyConnection ? new LazyConnectionDataSourceProxy(ds) : ds);
		tm = createTransactionManager(dsToUse);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			Connection tCon = DataSourceUtils.getConnection(dsToUse);
			try {
				if (createStatement) {
					tCon.createStatement();
				}
				else {
					tCon.getWarnings();
					tCon.clearWarnings();
				}
			}
			catch (SQLException ex) {
				throw new UncategorizedSQLException("", "", ex);
			}
		});

		assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		if (autoCommit && (!lazyConnection || createStatement)) {
			InOrder ordered = inOrder(con);
			ordered.verify(con).setAutoCommit(false);
			ordered.verify(con).commit();
			ordered.verify(con).setAutoCommit(true);
		}
		if (createStatement) {
			verify(con, times(2)).close();
		}
		else {
			verify(con).close();
		}
	}

	@Test
	void transactionRollbackWithAutoCommitTrue() throws Exception {
		testTransactionRollbackRestoringAutoCommit(true, false, false);
	}

	@Test
	void transactionRollbackWithAutoCommitFalse() throws Exception {
		testTransactionRollbackRestoringAutoCommit(false, false, false);
	}

	@Test
	void transactionRollbackWithAutoCommitTrueAndLazyConnection() throws Exception {
		testTransactionRollbackRestoringAutoCommit(true, true, false);
	}

	@Test
	void transactionRollbackWithAutoCommitFalseAndLazyConnection() throws Exception {
		testTransactionRollbackRestoringAutoCommit(false, true, false);
	}

	@Test
	void transactionRollbackWithAutoCommitTrueAndLazyConnectionAndCreateStatement() throws Exception {
		testTransactionRollbackRestoringAutoCommit(true, true, true);
	}

	@Test
	void transactionRollbackWithAutoCommitFalseAndLazyConnectionAndCreateStatement() throws Exception {
		testTransactionRollbackRestoringAutoCommit(false, true, true);
	}

	private void testTransactionRollbackRestoringAutoCommit(
			boolean autoCommit, boolean lazyConnection, boolean createStatement) throws Exception {

		given(con.getAutoCommit()).willReturn(autoCommit);

		if (lazyConnection) {
			given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
		}

		DataSource dsToUse = (lazyConnection ? new LazyConnectionDataSourceProxy(ds) : ds);
		tm = createTransactionManager(dsToUse);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		RuntimeException ex = new RuntimeException("Application exception");
		assertThatRuntimeException().isThrownBy(() ->
				tt.executeWithoutResult(status -> {
					assertThat(TransactionSynchronizationManager.hasResource(dsToUse)).isTrue();
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					assertThat(status.isNewTransaction()).isTrue();
					Connection con = DataSourceUtils.getConnection(dsToUse);
					if (createStatement) {
						try {
							con.createStatement();
						}
						catch (SQLException sqlException) {
							throw new UncategorizedSQLException("", "", sqlException);
						}
					}
					throw ex;
				}))
			.isEqualTo(ex);

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		if (autoCommit && (!lazyConnection || createStatement)) {
			InOrder ordered = inOrder(con);
			ordered.verify(con).setAutoCommit(false);
			ordered.verify(con).rollback();
			ordered.verify(con).setAutoCommit(true);
		}
		if (createStatement) {
			verify(con, times(2)).close();
		}
		else {
			verify(con).close();
		}
	}

	@Test
	void transactionRollbackOnly() {
		tm.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		ConnectionHolder conHolder = new ConnectionHolder(con, true);
		TransactionSynchronizationManager.bindResource(ds, conHolder);
		RuntimeException ex = new RuntimeException("Application exception");
		try {
			tt.executeWithoutResult(status -> {
				assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
				assertThat(status.isNewTransaction()).isFalse();
				throw ex;
			});
			fail("Should have thrown RuntimeException");
		}
		catch (RuntimeException ex2) {
			// expected
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
			assertThat(ex2).as("Correct exception thrown").isEqualTo(ex);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(ds);
		}

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
	}

	@Test
	void participatingTransactionWithRollbackOnly() throws Exception {
		testParticipatingTransactionWithRollbackOnly(false);
	}

	@Test
	void participatingTransactionWithRollbackOnlyAndFailEarly() throws Exception {
		testParticipatingTransactionWithRollbackOnly(true);
	}

	private void testParticipatingTransactionWithRollbackOnly(boolean failEarly) throws Exception {
		given(con.isReadOnly()).willReturn(false);
		if (failEarly) {
			tm.setFailEarlyOnGlobalRollbackOnly(true);
		}
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		TestTransactionSynchronization synch =
				new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_ROLLED_BACK);
		TransactionSynchronizationManager.registerSynchronization(synch);

		boolean outerTransactionBoundaryReached = false;
		try {
			assertThat(ts.isNewTransaction()).isTrue();

			TransactionTemplate tt = new TransactionTemplate(tm);
			tt.executeWithoutResult(status1 -> {
				assertThat(status1.isNewTransaction()).isFalse();
				assertThat(status1.isRollbackOnly()).isFalse();
				tt.executeWithoutResult(status2 -> {
					assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					assertThat(status2.isNewTransaction()).isFalse();
					status2.setRollbackOnly();
				});
				assertThat(status1.isNewTransaction()).isFalse();
				assertThat(status1.isRollbackOnly()).isTrue();
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
				assertThat(outerTransactionBoundaryReached).isFalse();
			}
			else {
				assertThat(outerTransactionBoundaryReached).isTrue();
			}
		}

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(synch.beforeCommitCalled).isFalse();
		assertThat(synch.beforeCompletionCalled).isTrue();
		assertThat(synch.afterCommitCalled).isFalse();
		assertThat(synch.afterCompletionCalled).isTrue();
		verify(con).rollback();
		verify(con).close();
	}

	@Test
	void participatingTransactionWithIncompatibleIsolationLevel() throws Exception {
		tm.setValidateExistingTransaction(true);

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() -> {
				TransactionTemplate tt = new TransactionTemplate(tm);
				TransactionTemplate tt2 = new TransactionTemplate(tm);
				tt2.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

				tt.executeWithoutResult(status -> {
					assertThat(status.isRollbackOnly()).isFalse();
					tt2.executeWithoutResult(status2 -> status2.setRollbackOnly());
					assertThat(status.isRollbackOnly()).isTrue();
				});
			});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).rollback();
		verify(con).close();
	}

	@Test
	void participatingTransactionWithIncompatibleReadOnly() throws Exception {
		willThrow(new SQLException("read-only not supported")).given(con).setReadOnly(true);
		tm.setValidateExistingTransaction(true);

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() -> {
			TransactionTemplate tt = new TransactionTemplate(tm);
			tt.setReadOnly(true);
			TransactionTemplate tt2 = new TransactionTemplate(tm);
			tt2.setReadOnly(false);

			tt.executeWithoutResult(status -> {
				assertThat(status.isRollbackOnly()).isFalse();
				tt2.executeWithoutResult(status2 -> status2.setRollbackOnly());
				assertThat(status.isRollbackOnly()).isTrue();
			});
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).rollback();
		verify(con).close();
	}

	@Test
	void participatingTransactionWithTransactionStartedFromSynch() throws Exception {
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		TestTransactionSynchronization synch =
				new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_COMMITTED) {
					@Override
					protected void doAfterCompletion(int status) {
						super.doAfterCompletion(status);
						tt.executeWithoutResult(status2 -> {
						});
						TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {});
					}
				};

		tt.executeWithoutResult(status ->
				TransactionSynchronizationManager.registerSynchronization(synch));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(synch.beforeCommitCalled).isTrue();
		assertThat(synch.beforeCompletionCalled).isTrue();
		assertThat(synch.afterCommitCalled).isTrue();
		assertThat(synch.afterCompletionCalled).isTrue();
		assertThat(synch.afterCompletionException).isInstanceOf(IllegalStateException.class);
		verify(con, times(2)).commit();
		verify(con, times(2)).close();
	}

	@Test
	void participatingTransactionWithDifferentConnectionObtainedFromSynch() throws Exception {
		DataSource ds2 = mock();
		Connection con2 = mock();
		given(ds2.getConnection()).willReturn(con2);

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		TransactionTemplate tt = new TransactionTemplate(tm);

		TestTransactionSynchronization synch =
				new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_COMMITTED) {
					@Override
					protected void doAfterCompletion(int status) {
						super.doAfterCompletion(status);
						Connection con = DataSourceUtils.getConnection(ds2);
						DataSourceUtils.releaseConnection(con, ds2);
					}
				};

		tt.executeWithoutResult(status ->
				TransactionSynchronizationManager.registerSynchronization(synch));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(synch.beforeCommitCalled).isTrue();
		assertThat(synch.beforeCompletionCalled).isTrue();
		assertThat(synch.afterCommitCalled).isTrue();
		assertThat(synch.afterCompletionCalled).isTrue();
		assertThat(synch.afterCompletionException).isNull();
		verify(con).commit();
		verify(con).close();
		verify(con2).close();
	}

	@Test
	void participatingTransactionWithRollbackOnlyAndInnerSynch() throws Exception {
		tm.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
		DataSourceTransactionManager tm2 = createTransactionManager(ds);
		// tm has no synch enabled (used at outer level), tm2 has synch enabled (inner level)

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		TestTransactionSynchronization synch =
				new TestTransactionSynchronization(ds, TransactionSynchronization.STATUS_UNKNOWN);

		assertThatExceptionOfType(UnexpectedRollbackException.class).isThrownBy(() -> {
			assertThat(ts.isNewTransaction()).isTrue();
			TransactionTemplate tt = new TransactionTemplate(tm2);
			tt.executeWithoutResult(status -> {
				assertThat(status.isNewTransaction()).isFalse();
				assertThat(status.isRollbackOnly()).isFalse();
				tt.executeWithoutResult(status2 -> {
					assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					assertThat(status2.isNewTransaction()).isFalse();
					status2.setRollbackOnly();
				});
				assertThat(status.isNewTransaction()).isFalse();
				assertThat(status.isRollbackOnly()).isTrue();
				TransactionSynchronizationManager.registerSynchronization(synch);
			});

			tm.commit(ts);
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(synch.beforeCommitCalled).isFalse();
		assertThat(synch.beforeCompletionCalled).isTrue();
		assertThat(synch.afterCommitCalled).isFalse();
		assertThat(synch.afterCompletionCalled).isTrue();
		verify(con).rollback();
		verify(con).close();
	}

	@Test
	void propagationRequiresNewWithExistingTransaction() throws Exception {
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			tt.executeWithoutResult(status2 -> {
				assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(status2.isNewTransaction()).isTrue();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				status2.setRollbackOnly();
			});
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).rollback();
		verify(con).commit();
		verify(con, times(2)).close();
	}

	@Test
	void propagationRequiresNewWithExistingTransactionAndUnrelatedDataSource() throws Exception {
		Connection con2 = mock();
		DataSource ds2 = mock();
		given(ds2.getConnection()).willReturn(con2);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		PlatformTransactionManager tm2 = createTransactionManager(ds2);
		TransactionTemplate tt2 = new TransactionTemplate(tm2);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.hasResource(ds2)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			tt2.executeWithoutResult(status2 -> {
				assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(status2.isNewTransaction()).isTrue();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				status2.setRollbackOnly();
			});
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.hasResource(ds2)).isFalse();
		verify(con).commit();
		verify(con).close();
		verify(con2).rollback();
		verify(con2).close();
	}

	@Test
	void propagationRequiresNewWithExistingTransactionAndUnrelatedFailingDataSource() throws Exception {
		DataSource ds2 = mock();
		SQLException failure = new SQLException();
		given(ds2.getConnection()).willThrow(failure);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		DataSourceTransactionManager tm2 = createTransactionManager(ds2);
		tm2.setTransactionSynchronization(DataSourceTransactionManager.SYNCHRONIZATION_NEVER);
		TransactionTemplate tt2 = new TransactionTemplate(tm2);
		tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.hasResource(ds2)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		assertThatExceptionOfType(CannotCreateTransactionException.class).isThrownBy(() ->
			tt.executeWithoutResult(status -> {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(status.hasTransaction()).isTrue();
				assertThat(status.isNewTransaction()).isTrue();
				assertThat(status.isNested()).isFalse();
				assertThat(status.isReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				tt2.executeWithoutResult(status2 -> status2.setRollbackOnly());
		})).withCause(failure);

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.hasResource(ds2)).isFalse();
		verify(con).rollback();
		verify(con).close();
	}

	@Test
	void propagationNotSupportedWithExistingTransaction() throws Exception {
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
			tt.executeWithoutResult(status2 -> {
				assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(status2.hasTransaction()).isFalse();
				assertThat(status2.isNewTransaction()).isFalse();
				assertThat(status2.isNested()).isFalse();
				assertThat(status2.isReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
				status2.setRollbackOnly();
			});
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).commit();
		verify(con).close();
	}

	@Test
	void propagationNeverWithExistingTransaction() throws Exception {
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
			tt.executeWithoutResult(status -> {
				assertThat(status.hasTransaction()).isTrue();
				assertThat(status.isNewTransaction()).isTrue();
				assertThat(status.isNested()).isFalse();
				tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
				tt.executeWithoutResult(status2 ->
						fail("Should have thrown IllegalTransactionStateException"));
				fail("Should have thrown IllegalTransactionStateException");
			}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).rollback();
		verify(con).close();
	}

	@Test
	void propagationSupportsAndRequiresNew() throws Exception {
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
			assertThat(status.hasTransaction()).isFalse();
			assertThat(status.isNewTransaction()).isFalse();
			assertThat(status.isNested()).isFalse();
			TransactionTemplate tt2 = new TransactionTemplate(tm);
			tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			tt2.executeWithoutResult(status2 -> {
				assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(status2.hasTransaction()).isTrue();
				assertThat(status2.isNewTransaction()).isTrue();
				assertThat(status2.isNested()).isFalse();
				assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con);
				assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con);
			});
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).commit();
		verify(con).close();
	}

	@Test
	void propagationSupportsAndRequiresNewWithEarlyAccess() throws Exception {
		Connection con1 = mock();
		Connection con2 = mock();
		given(ds.getConnection()).willReturn(con1, con2);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
			assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con1);
			assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con1);
			assertThat(status.hasTransaction()).isFalse();
			assertThat(status.isNewTransaction()).isFalse();
			assertThat(status.isNested()).isFalse();
			TransactionTemplate tt2 = new TransactionTemplate(tm);
			tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			tt2.executeWithoutResult(status2 -> {
				assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con2);
				assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con2);
				assertThat(status2.hasTransaction()).isTrue();
				assertThat(status2.isNewTransaction()).isTrue();
				assertThat(status2.isNested()).isFalse();
			});
			assertThat(DataSourceUtils.getConnection(ds)).isSameAs(con1);
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con1).close();
		verify(con2).commit();
		verify(con2).close();
	}

	@Test
	void transactionWithIsolationAndReadOnly() throws Exception {
		given(con.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_UNCOMMITTED);
		given(con.getAutoCommit()).willReturn(true);
		given(con.isReadOnly()).willReturn(false);

		testTransactionReadOnly(TransactionDefinition.ISOLATION_REPEATABLE_READ, false);

		InOrder ordered = inOrder(con);
		ordered.verify(con).isReadOnly();
		ordered.verify(con).setReadOnly(true);
		ordered.verify(con).getTransactionIsolation();
		ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
		ordered.verify(con).getAutoCommit();
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).commit();
		ordered.verify(con).setAutoCommit(true);
		ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
		ordered.verify(con).setReadOnly(false);
		ordered.verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void transactionWithDefaultReadOnly() throws Exception {
		given(con.getAutoCommit()).willReturn(true);
		given(con.isReadOnly()).willReturn(true);

		testTransactionReadOnly(TransactionDefinition.ISOLATION_DEFAULT, false);

		InOrder ordered = inOrder(con);
		ordered.verify(con).isReadOnly();
		ordered.verify(con).getAutoCommit();
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).commit();
		ordered.verify(con).setAutoCommit(true);
		ordered.verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void transactionWithEnforceReadOnly() throws Exception {
		tm.setEnforceReadOnly(true);

		given(con.getAutoCommit()).willReturn(true);
		given(con.isReadOnly()).willReturn(false);
		Statement stmt = mock();
		given(con.createStatement()).willReturn(stmt);

		testTransactionReadOnly(TransactionDefinition.ISOLATION_DEFAULT, false);

		InOrder ordered = inOrder(con, stmt);
		ordered.verify(con).isReadOnly();
		ordered.verify(con).setReadOnly(true);
		ordered.verify(con).getAutoCommit();
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).createStatement();
		ordered.verify(stmt).executeUpdate("SET TRANSACTION READ ONLY");
		ordered.verify(stmt).close();
		ordered.verify(con).commit();
		ordered.verify(con).setAutoCommit(true);
		ordered.verify(con).setReadOnly(false);
		ordered.verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void transactionWithLazyConnectionDataSourceAndStatement() throws Exception {
		LazyConnectionDataSourceProxy dsProxy = new LazyConnectionDataSourceProxy();
		dsProxy.setTargetDataSource(ds);
		dsProxy.setDefaultAutoCommit(true);
		dsProxy.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		dsProxy.afterPropertiesSet();
		tm = createTransactionManager(dsProxy);

		try (Connection con = dsProxy.getConnection()) {
			assertThat(con.isReadOnly()).isFalse();
		}
		testTransactionReadOnly(TransactionDefinition.ISOLATION_SERIALIZABLE, true);

		InOrder ordered = inOrder(con);
		ordered.verify(con).setReadOnly(true);
		ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).createStatement();
		ordered.verify(con).commit();
		ordered.verify(con).setAutoCommit(true);
		ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		ordered.verify(con).setReadOnly(false);
		ordered.verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void transactionWithLazyConnectionDataSourceNoStatement() throws Exception {
		LazyConnectionDataSourceProxy dsProxy = new LazyConnectionDataSourceProxy();
		dsProxy.setTargetDataSource(ds);
		dsProxy.setDefaultAutoCommit(true);
		dsProxy.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		dsProxy.afterPropertiesSet();
		tm = createTransactionManager(dsProxy);

		try (Connection con = dsProxy.getConnection()) {
			assertThat(con.isReadOnly()).isFalse();
		}
		testTransactionReadOnly(TransactionDefinition.ISOLATION_SERIALIZABLE, false);

		verifyNoMoreInteractions(con);
	}

	@Test
	void transactionWithReadOnlyDataSourceAndStatement() throws Exception {
		LazyConnectionDataSourceProxy dsProxy = new LazyConnectionDataSourceProxy();
		dsProxy.setReadOnlyDataSource(ds);
		dsProxy.setDefaultAutoCommit(false);
		dsProxy.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		dsProxy.afterPropertiesSet();
		tm = createTransactionManager(dsProxy);

		try (Connection con = dsProxy.getConnection()) {
			assertThat(con.isReadOnly()).isTrue();
		}
		testTransactionReadOnly(TransactionDefinition.ISOLATION_SERIALIZABLE, true);

		InOrder ordered = inOrder(con);
		ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		ordered.verify(con).createStatement();
		ordered.verify(con).commit();
		ordered.verify(con).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		ordered.verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void transactionWithReadOnlyDataSourceNoStatement() throws Exception {
		LazyConnectionDataSourceProxy dsProxy = new LazyConnectionDataSourceProxy();
		dsProxy.setReadOnlyDataSource(ds);
		dsProxy.setDefaultAutoCommit(false);
		dsProxy.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		dsProxy.afterPropertiesSet();
		tm = createTransactionManager(dsProxy);

		try (Connection con = dsProxy.getConnection()) {
			assertThat(con.isReadOnly()).isTrue();
		}
		testTransactionReadOnly(TransactionDefinition.ISOLATION_SERIALIZABLE, false);

		verifyNoMoreInteractions(con);
	}

	private void testTransactionReadOnly(int isolationLevel, boolean withStatement) {
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		tt.setIsolationLevel(isolationLevel);
		tt.setReadOnly(true);
		tt.setName("my-transaction");

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

		tt.executeWithoutResult(status -> {
			// something transactional
			assertThat(status.getTransactionName()).isEqualTo("my-transaction");
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(status.hasSavepoint()).isFalse();
			assertThat(status.isReadOnly()).isTrue();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			assertThat(status.isRollbackOnly()).isFalse();
			assertThat(status.isCompleted()).isFalse();
			if (withStatement) {
				try {
					DataSourceUtils.getConnection(tm.getDataSource()).createStatement();
				}
				catch (SQLException ex) {
					throw new IllegalStateException(ex);
				}
			}
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
	}

	@ParameterizedTest(name = "transaction with {0} second timeout")
	@ValueSource(ints = {1, 10})
	@EnabledForTestGroups(LONG_RUNNING)
	public void transactionWithTimeout(int timeout) throws Exception {
		PreparedStatement ps = mock();
		given(con.getAutoCommit()).willReturn(true);
		given(con.prepareStatement("some SQL statement")).willReturn(ps);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setTimeout(timeout);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

		try {
			tt.executeWithoutResult(status -> {
				try {
					Thread.sleep(1500);
				}
				catch (InterruptedException ex) {
				}
				try {
					Connection con = DataSourceUtils.getConnection(ds);
					PreparedStatement ps2 = con.prepareStatement("some SQL statement");
					DataSourceUtils.applyTransactionTimeout(ps2, ds);
				}
				catch (SQLException ex) {
					throw new DataAccessResourceFailureException("", ex);
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

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		if (timeout > 1) {
			verify(ps).setQueryTimeout(timeout - 1);
			verify(con).commit();
		}
		else {
			verify(con).rollback();
		}
		InOrder ordered = inOrder(con);
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).setAutoCommit(true);
		verify(con).close();
	}

	@Test
	void transactionAwareDataSourceProxy() throws Exception {
		given(con.getAutoCommit()).willReturn(true);
		given(con.getWarnings()).willThrow(new SQLException());

		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		tt.executeWithoutResult(status -> {
			// something transactional
			assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
			TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
			try {
				Connection tCon = dsProxy.getConnection();
				tCon.getWarnings();
				tCon.clearWarnings();
				assertThat(((ConnectionProxy) tCon).getTargetConnection()).isEqualTo(con);
				// should be ignored
				tCon.close();
			}
			catch (SQLException ex) {
				throw new UncategorizedSQLException("", "", ex);
			}
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		InOrder ordered = inOrder(con);
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).commit();
		ordered.verify(con).setAutoCommit(true);
		verify(con).close();
	}

	@Test
	void transactionAwareDataSourceProxyWithLazyFalse() throws Exception {
		given(con.getAutoCommit()).willReturn(true);
		given(con.getWarnings()).willThrow(new SQLException());

		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		tt.executeWithoutResult(status -> {
			// something transactional
			assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
			TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
			dsProxy.setLazyTransactionalConnections(false);
			try {
				Connection tCon = dsProxy.getConnection();
				assertThatExceptionOfType(SQLException.class).isThrownBy(tCon::getWarnings);
				tCon.clearWarnings();
				assertThat(((ConnectionProxy) tCon).getTargetConnection()).isEqualTo(con);
				// should be ignored
				tCon.close();
			}
			catch (SQLException ex) {
				throw new UncategorizedSQLException("", "", ex);
			}
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		InOrder ordered = inOrder(con);
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).commit();
		ordered.verify(con).setAutoCommit(true);
		verify(con).close();
	}

	@Test
	void transactionAwareDataSourceProxyWithEarlyConnection() throws Exception {
		given(ds.getConnection()).willReturn(mock(Connection.class), con);
		given(con.getAutoCommit()).willReturn(true);
		given(con.getWarnings()).willThrow(new SQLException());

		TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
		dsProxy.setLazyTransactionalConnections(false);
		Connection tCon = dsProxy.getConnection();

		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		tt.executeWithoutResult(status -> {
			// something transactional
			assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
			try {
				// should close the early Connection obtained before the transaction
				tCon.close();
			}
			catch (SQLException ex) {
				throw new UncategorizedSQLException("", "", ex);
			}
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

		InOrder ordered = inOrder(con);
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).commit();
		ordered.verify(con).setAutoCommit(true);
		verify(con).close();
	}

	@Test
	void transactionAwareDataSourceProxyWithSuspension() throws Exception {
		given(con.getAutoCommit()).willReturn(true);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

		tt.executeWithoutResult(status -> {
			// something transactional
			assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
			TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
			try {
				assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
				// should be ignored
				dsProxy.getConnection().close();
			}
			catch (SQLException ex) {
				throw new UncategorizedSQLException("", "", ex);
			}

			tt.executeWithoutResult(status2 -> {
				// something transactional
				assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
				try {
					assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}
			});

			try {
				assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
				// should be ignored
				dsProxy.getConnection().close();
			}
			catch (SQLException ex) {
				throw new UncategorizedSQLException("", "", ex);
			}
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		InOrder ordered = inOrder(con);
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).commit();
		ordered.verify(con).setAutoCommit(true);
		verify(con, times(2)).close();
	}

	@Test
	void transactionAwareDataSourceProxyWithSuspensionAndReobtaining() throws Exception {
		given(con.getAutoCommit()).willReturn(true);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

		tt.executeWithoutResult(status -> {
			// something transactional
			assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
			TransactionAwareDataSourceProxy dsProxy = new TransactionAwareDataSourceProxy(ds);
			dsProxy.setReobtainTransactionalConnections(true);
			try {
				assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
				// should be ignored
				dsProxy.getConnection().close();
			}
			catch (SQLException ex) {
				throw new UncategorizedSQLException("", "", ex);
			}

			tt.executeWithoutResult(status2 -> {
				// something transactional
				assertThat(DataSourceUtils.getConnection(ds)).isEqualTo(con);
				try {
					assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
					// should be ignored
					dsProxy.getConnection().close();
				}
				catch (SQLException ex) {
					throw new UncategorizedSQLException("", "", ex);
				}
			});

			try {
				assertThat(((ConnectionProxy) dsProxy.getConnection()).getTargetConnection()).isEqualTo(con);
				// should be ignored
				dsProxy.getConnection().close();
			}
			catch (SQLException ex) {
				throw new UncategorizedSQLException("", "", ex);
			}
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		InOrder ordered = inOrder(con);
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).commit();
		ordered.verify(con).setAutoCommit(true);
		verify(con, times(2)).close();
	}

	/**
	 * Test behavior if the first operation on a connection (getAutoCommit) throws SQLException.
	 */
	@Test
	void transactionWithExceptionOnBegin() throws Exception {
		willThrow(new SQLException("Cannot begin")).given(con).getAutoCommit();

		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThatExceptionOfType(CannotCreateTransactionException.class).isThrownBy(() ->
				tt.executeWithoutResult(status -> {
					// something transactional
				}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).close();
	}

	@Test
	protected void transactionWithExceptionOnCommit() throws Exception {
		willThrow(new SQLException("Cannot commit")).given(con).commit();

		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
				tt.executeWithoutResult(status -> {
					// something transactional
				}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).close();
	}

	@Test
	protected void transactionWithExceptionOnCommitAndRollbackOnCommitFailure() throws Exception {
		willThrow(new SQLException("Cannot commit")).given(con).commit();

		tm.setRollbackOnCommitFailure(true);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
				tt.executeWithoutResult(status -> {
					// something transactional
				}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).rollback();
		verify(con).close();
	}

	@Test
	protected void transactionWithExceptionOnRollback() throws Exception {
		given(con.getAutoCommit()).willReturn(true);
		willThrow(new SQLException("Cannot rollback")).given(con).rollback();

		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
				tt.executeWithoutResult(status -> {
					assertThat(status.getTransactionName()).isEmpty();
					assertThat(status.hasTransaction()).isTrue();
					assertThat(status.isNewTransaction()).isTrue();
					assertThat(status.isNested()).isFalse();
					assertThat(status.hasSavepoint()).isFalse();
					assertThat(status.isReadOnly()).isFalse();
					assertThat(status.isRollbackOnly()).isFalse();
					status.setRollbackOnly();
					assertThat(status.isRollbackOnly()).isTrue();
					assertThat(status.isCompleted()).isFalse();
				}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		InOrder ordered = inOrder(con);
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).rollback();
		ordered.verify(con).setAutoCommit(true);
		verify(con).close();
	}

	@Test
	void transactionWithPropagationSupports() {
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
			assertThat(status.isNewTransaction()).isFalse();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
	}

	@Test
	void transactionWithPropagationNotSupported() {
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
			assertThat(status.isNewTransaction()).isFalse();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
	}

	@Test
	void transactionWithPropagationNever() {
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
			assertThat(status.isNewTransaction()).isFalse();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
	}

	@Test
	void existingTransactionWithPropagationNested() throws Exception {
		testExistingTransactionWithPropagationNested(1);
	}

	@Test
	void existingTransactionWithPropagationNestedTwice() throws Exception {
		testExistingTransactionWithPropagationNested(2);
	}

	private void testExistingTransactionWithPropagationNested(int count) throws Exception {
		DatabaseMetaData md = mock();
		Savepoint sp = mock();

		given(md.supportsSavepoints()).willReturn(true);
		given(con.getMetaData()).willReturn(md);
		for (int i = 1; i <= count; i++) {
			given(con.setSavepoint(ConnectionHolder.SAVEPOINT_NAME_PREFIX + i)).willReturn(sp);
		}

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(status.hasSavepoint()).isFalse();
			TestSavepointSynchronization synch = new TestSavepointSynchronization();
			TransactionSynchronizationManager.registerSynchronization(synch);
			for (int i = 0; i < count; i++) {
				tt.executeWithoutResult(status2 -> {
					assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					assertThat(status2.hasTransaction()).isTrue();
					assertThat(status2.isNewTransaction()).isFalse();
					assertThat(status2.isNested()).isTrue();
					assertThat(status2.hasSavepoint()).isTrue();
					assertThat(synch.savepointCalled).isTrue();
				});
				assertThat(synch.savepointRollbackCalled).isFalse();
				synch.savepointCalled = false;
			}
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(status.hasSavepoint()).isFalse();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con, times(count)).releaseSavepoint(sp);
		verify(con).commit();
		verify(con).close();
	}

	@Test
	void existingTransactionWithPropagationNestedAndRollback() throws Exception {
		DatabaseMetaData md = mock();
		Savepoint sp = mock();

		given(md.supportsSavepoints()).willReturn(true);
		given(con.getMetaData()).willReturn(md);
		given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(status.hasSavepoint()).isFalse();
			TestSavepointSynchronization synch = new TestSavepointSynchronization();
			TransactionSynchronizationManager.registerSynchronization(synch);
			tt.executeWithoutResult(status2 -> {
				assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(status2.hasTransaction()).isTrue();
				assertThat(status2.isNewTransaction()).isFalse();
				assertThat(status2.isNested()).isTrue();
				assertThat(status2.hasSavepoint()).isTrue();
				assertThat(synch.savepointCalled).isTrue();
				assertThat(synch.savepointRollbackCalled).isFalse();
				status2.setRollbackOnly();
			});
			assertThat(synch.savepointRollbackCalled).isTrue();
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(status.hasSavepoint()).isFalse();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).rollback(sp);
		verify(con).releaseSavepoint(sp);
		verify(con).commit();
		verify(con).close();
	}

	@Test
	void existingTransactionWithPropagationNestedAndRequiredRollback() throws Exception {
		DatabaseMetaData md = mock();
		Savepoint sp = mock();

		given(md.supportsSavepoints()).willReturn(true);
		given(con.getMetaData()).willReturn(md);
		given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(status.hasSavepoint()).isFalse();
			TestSavepointSynchronization synch = new TestSavepointSynchronization();
			TransactionSynchronizationManager.registerSynchronization(synch);
			assertThatIllegalStateException().isThrownBy(() ->
					tt.executeWithoutResult(status2 -> {
						assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(status2.hasTransaction()).isTrue();
						assertThat(status2.isNewTransaction()).isFalse();
						assertThat(status2.isNested()).isTrue();
						assertThat(status2.hasSavepoint()).isTrue();
						assertThat(synch.savepointCalled).isTrue();
						assertThat(synch.savepointRollbackCalled).isFalse();
						TransactionTemplate ntt = new TransactionTemplate(tm);
						ntt.executeWithoutResult(status3 -> {
							assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
							assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
							assertThat(status3.hasTransaction()).isTrue();
							assertThat(status3.isNewTransaction()).isFalse();
							assertThat(status3.isNested()).isFalse();
							assertThat(status3.hasSavepoint()).isFalse();
							throw new IllegalStateException();
					});
					}));
			assertThat(synch.savepointRollbackCalled).isTrue();
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(status.hasSavepoint()).isFalse();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).rollback(sp);
		verify(con).releaseSavepoint(sp);
		verify(con).commit();
		verify(con).close();
	}

	@Test
	void existingTransactionWithPropagationNestedAndRequiredRollbackOnly() throws Exception {
		DatabaseMetaData md = mock();
		Savepoint sp = mock();

		given(md.supportsSavepoints()).willReturn(true);
		given(con.getMetaData()).willReturn(md);
		given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status1 -> {
			assertThat(status1.hasTransaction()).isTrue();
			assertThat(status1.isNewTransaction()).isTrue();
			assertThat(status1.isNested()).isFalse();
			assertThat(status1.hasSavepoint()).isFalse();
			TestSavepointSynchronization synch = new TestSavepointSynchronization();
			TransactionSynchronizationManager.registerSynchronization(synch);
			assertThatExceptionOfType(UnexpectedRollbackException.class).isThrownBy(() ->
				tt.executeWithoutResult(status2 -> {
					assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					assertThat(status2.hasTransaction()).isTrue();
					assertThat(status2.isNewTransaction()).isFalse();
					assertThat(status2.isNested()).isTrue();
					assertThat(status2.hasSavepoint()).isTrue();
					assertThat(synch.savepointCalled).isTrue();
					assertThat(synch.savepointRollbackCalled).isFalse();
					TransactionTemplate ntt = new TransactionTemplate(tm);
					ntt.executeWithoutResult(status3 -> {
						assertThat(TransactionSynchronizationManager.hasResource(ds)).isTrue();
						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(status3.hasTransaction()).isTrue();
						assertThat(status3.isNewTransaction()).isFalse();
						assertThat(status3.isNested()).isFalse();
						assertThat(status3.hasSavepoint()).isFalse();
						status3.setRollbackOnly();
					});
				}));
			assertThat(synch.savepointRollbackCalled).isTrue();
				assertThat(status1.hasTransaction()).isTrue();
				assertThat(status1.isNewTransaction()).isTrue();
				assertThat(status1.isNested()).isFalse();
				assertThat(status1.hasSavepoint()).isFalse();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).rollback(sp);
		verify(con).releaseSavepoint(sp);
		verify(con).commit();
		verify(con).close();
	}

	@Test
	void existingTransactionWithManualSavepoint() throws Exception {
		DatabaseMetaData md = mock();
		Savepoint sp = mock();

		given(md.supportsSavepoints()).willReturn(true);
		given(con.getMetaData()).willReturn(md);
		given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(status.hasSavepoint()).isFalse();
			TestSavepointSynchronization synch = new TestSavepointSynchronization();
			TransactionSynchronizationManager.registerSynchronization(synch);
			Object savepoint = status.createSavepoint();
			assertThat(synch.savepointCalled).isTrue();
			status.releaseSavepoint(savepoint);
			assertThat(synch.savepointRollbackCalled).isFalse();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).releaseSavepoint(sp);
		verify(con).commit();
		verify(con).close();
		verify(ds).getConnection();
	}

	@Test
	void existingTransactionWithManualSavepointAndRollback() throws Exception {
		DatabaseMetaData md = mock();
		Savepoint sp = mock();

		given(md.supportsSavepoints()).willReturn(true);
		given(con.getMetaData()).willReturn(md);
		given(con.setSavepoint("SAVEPOINT_1")).willReturn(sp);

		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(status.hasSavepoint()).isFalse();
			TestSavepointSynchronization synch = new TestSavepointSynchronization();
			TransactionSynchronizationManager.registerSynchronization(synch);
			Object savepoint = status.createSavepoint();
			assertThat(synch.savepointCalled).isTrue();
			assertThat(synch.savepointRollbackCalled).isFalse();
			status.rollbackToSavepoint(savepoint);
			assertThat(synch.savepointRollbackCalled).isTrue();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).rollback(sp);
		verify(con).commit();
		verify(con).close();
	}

	@Test
	void transactionWithPropagationNested() throws Exception {
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(status.getTransactionName()).isEmpty();
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(status.hasSavepoint()).isFalse();
			assertThat(status.isReadOnly()).isFalse();
			assertThat(status.isRollbackOnly()).isFalse();
			assertThat(status.isCompleted()).isFalse();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).commit();
		verify(con).close();
	}

	@Test
	void transactionWithPropagationNestedAndRollback() throws Exception {
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(status.getTransactionName()).isEmpty();
			assertThat(status.hasTransaction()).isTrue();
			assertThat(status.isNewTransaction()).isTrue();
			assertThat(status.isNested()).isFalse();
			assertThat(status.hasSavepoint()).isFalse();
			assertThat(status.isReadOnly()).isFalse();
			assertThat(status.isRollbackOnly()).isFalse();
			status.setRollbackOnly();
			assertThat(status.isRollbackOnly()).isTrue();
			assertThat(status.isCompleted()).isFalse();
		});

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).rollback();
		verify(con).close();
	}


	private static class TestTransactionSynchronization implements TransactionSynchronization {

		private DataSource dataSource;

		private int status;

		public boolean beforeCommitCalled;

		public boolean beforeCompletionCalled;

		public boolean afterCommitCalled;

		public boolean afterCompletionCalled;

		public Throwable afterCompletionException;

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
			assertThat(this.beforeCommitCalled).isFalse();
			this.beforeCommitCalled = true;
		}

		@Override
		public void beforeCompletion() {
			assertThat(this.beforeCompletionCalled).isFalse();
			this.beforeCompletionCalled = true;
		}

		@Override
		public void afterCommit() {
			if (this.status != TransactionSynchronization.STATUS_COMMITTED) {
				fail("Should never be called");
			}
			assertThat(this.afterCommitCalled).isFalse();
			this.afterCommitCalled = true;
		}

		@Override
		public void afterCompletion(int status) {
			try {
				doAfterCompletion(status);
			}
			catch (Throwable ex) {
				this.afterCompletionException = ex;
			}
		}

		protected void doAfterCompletion(int status) {
			assertThat(this.afterCompletionCalled).isFalse();
			this.afterCompletionCalled = true;
			assertThat(status).isEqualTo(this.status);
			assertThat(TransactionSynchronizationManager.hasResource(this.dataSource)).isTrue();
		}
	}


	private static class TestSavepointSynchronization implements TransactionSynchronization {

		public boolean savepointCalled;

		public boolean savepointRollbackCalled;

		@Override
		public void savepoint(Object savepoint) {
			assertThat(this.savepointCalled).isFalse();
			this.savepointCalled = true;
		}

		@Override
		public void savepointRollback(Object savepoint) {
			assertThat(this.savepointRollbackCalled).isFalse();
			this.savepointRollbackCalled = true;
		}
	}

}

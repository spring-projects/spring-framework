/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.jdbc.support;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.jdbc.datasource.DataSourceTransactionManagerTests;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 5.3
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManagerTests
 */
class JdbcTransactionManagerTests extends DataSourceTransactionManagerTests {

	@Override
	protected JdbcTransactionManager createTransactionManager(DataSource ds) {
		return new JdbcTransactionManager(ds);
	}


	@Override
	@Test
	protected void testTransactionWithExceptionOnCommit() throws Exception {
		willThrow(new SQLException("Cannot commit")).given(con).commit();
		TransactionTemplate tt = new TransactionTemplate(tm);

		// plain TransactionSystemException
		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
				tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).close();
	}

	@Test
	void testTransactionWithDataAccessExceptionOnCommit() throws Exception {
		willThrow(new SQLException("Cannot commit")).given(con).commit();
		((JdbcTransactionManager) tm).setExceptionTranslator((task, sql, ex) -> new ConcurrencyFailureException(task));
		TransactionTemplate tt = new TransactionTemplate(tm);

		// specific ConcurrencyFailureException
		assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						// something transactional
					}
				}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).close();
	}

	@Test
	void testTransactionWithDataAccessExceptionOnCommitFromLazyExceptionTranslator() throws Exception {
		willThrow(new SQLException("Cannot commit", "40")).given(con).commit();
		TransactionTemplate tt = new TransactionTemplate(tm);

		// specific ConcurrencyFailureException
		assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						// something transactional
					}
				}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).close();
	}

	@Override
	@Test
	protected void testTransactionWithExceptionOnCommitAndRollbackOnCommitFailure() throws Exception {
		willThrow(new SQLException("Cannot commit")).given(con).commit();

		tm.setRollbackOnCommitFailure(true);
		TransactionTemplate tt = new TransactionTemplate(tm);

		// plain TransactionSystemException
		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
				tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		verify(con).rollback();
		verify(con).close();
	}

	@Override
	@Test
	protected void testTransactionWithExceptionOnRollback() throws Exception {
		given(con.getAutoCommit()).willReturn(true);
		willThrow(new SQLException("Cannot rollback")).given(con).rollback();
		TransactionTemplate tt = new TransactionTemplate(tm);

		// plain TransactionSystemException
		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
				tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
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
				}
			}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		InOrder ordered = inOrder(con);
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).rollback();
		ordered.verify(con).setAutoCommit(true);
		verify(con).close();
	}

	@Test
	void testTransactionWithDataAccessExceptionOnRollback() throws Exception {
		given(con.getAutoCommit()).willReturn(true);
		willThrow(new SQLException("Cannot rollback")).given(con).rollback();
		((JdbcTransactionManager) tm).setExceptionTranslator((task, sql, ex) -> new ConcurrencyFailureException(task));
		TransactionTemplate tt = new TransactionTemplate(tm);

		// specific ConcurrencyFailureException
		assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
						status.setRollbackOnly();
					}
				}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		InOrder ordered = inOrder(con);
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).rollback();
		ordered.verify(con).setAutoCommit(true);
		verify(con).close();
	}

	@Test
	void testTransactionWithDataAccessExceptionOnRollbackFromLazyExceptionTranslator() throws Exception {
		given(con.getAutoCommit()).willReturn(true);
		willThrow(new SQLException("Cannot rollback", "40")).given(con).rollback();
		TransactionTemplate tt = new TransactionTemplate(tm);

		// specific ConcurrencyFailureException
		assertThatExceptionOfType(ConcurrencyFailureException.class).isThrownBy(() ->
				tt.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
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
					}
				}));

		assertThat(TransactionSynchronizationManager.hasResource(ds)).isFalse();
		InOrder ordered = inOrder(con);
		ordered.verify(con).setAutoCommit(false);
		ordered.verify(con).rollback();
		ordered.verify(con).setAutoCommit(true);
		verify(con).close();
	}

}

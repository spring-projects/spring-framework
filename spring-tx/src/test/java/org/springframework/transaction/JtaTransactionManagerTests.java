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

package org.springframework.transaction;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.testfixture.TestTransactionExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 12.05.2003
 */
class JtaTransactionManagerTests {

	@Test
	void jtaTransactionManagerWithCommit() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setName("txName");

		assertThat(ptm.getTransactionSynchronization()).isEqualTo(JtaTransactionManager.SYNCHRONIZATION_ALWAYS);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertThat(status.getTransactionName()).isEqualTo("txName");
				assertThat(status.hasTransaction()).isTrue();
				assertThat(status.isNewTransaction()).isTrue();
				assertThat(status.isNested()).isFalse();
				assertThat(status.isReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				TransactionSynchronizationManager.registerSynchronization(synch);
				assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isEqualTo("txName");
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				assertThat(status.isRollbackOnly()).isFalse();
				assertThat(status.isCompleted()).isFalse();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		verify(ut).begin();
		verify(ut).commit();
		verify(synch).beforeCommit(false);
		verify(synch).beforeCompletion();
		verify(synch).afterCommit();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
	}

	@Test
	void jtaTransactionManagerWithCommitAndSynchronizationOnActual() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertThat(status.getTransactionName()).isEmpty();
				assertThat(status.hasTransaction()).isTrue();
				assertThat(status.isNewTransaction()).isTrue();
				assertThat(status.isNested()).isFalse();
				assertThat(status.isReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				TransactionSynchronizationManager.registerSynchronization(synch);
				assertThat(status.isRollbackOnly()).isFalse();
				assertThat(status.isCompleted()).isFalse();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).begin();
		verify(ut).commit();
		verify(synch).beforeCommit(false);
		verify(synch).beforeCompletion();
		verify(synch).afterCommit();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
	}

	@Test
	void jtaTransactionManagerWithCommitAndSynchronizationNever() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(
		Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_NEVER);
		ptm.afterPropertiesSet();

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(status.getTransactionName()).isEmpty();
				assertThat(status.hasTransaction()).isTrue();
				assertThat(status.isNewTransaction()).isTrue();
				assertThat(status.isNested()).isFalse();
				assertThat(status.isReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
				assertThat(status.isRollbackOnly()).isFalse();
				assertThat(status.isCompleted()).isFalse();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).begin();
		verify(ut).commit();
	}

	@Test
	void jtaTransactionManagerWithRollback() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setTimeout(10);
		tt.setName("txName");

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(status.getTransactionName()).isEqualTo("txName");
				assertThat(status.hasTransaction()).isTrue();
				assertThat(status.isNewTransaction()).isTrue();
				assertThat(status.isNested()).isFalse();
				assertThat(status.isReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				TransactionSynchronizationManager.registerSynchronization(synch);
				assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isEqualTo("txName");
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				status.setRollbackOnly();
				assertThat(status.isRollbackOnly()).isTrue();
				assertThat(status.isCompleted()).isFalse();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		verify(ut).setTransactionTimeout(10);
		verify(ut).begin();
		verify(ut).rollback();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
	}

	@Test
	void jtaTransactionManagerWithRollbackAndSynchronizationOnActual() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		tt.setTimeout(10);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(status.getTransactionName()).isEmpty();
				assertThat(status.hasTransaction()).isTrue();
				assertThat(status.isNewTransaction()).isTrue();
				assertThat(status.isNested()).isFalse();
				assertThat(status.isReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
				assertThat(status.isRollbackOnly()).isTrue();
				assertThat(status.isCompleted()).isFalse();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).setTransactionTimeout(10);
		verify(ut).begin();
		verify(ut).rollback();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
	}

	@Test
	void jtaTransactionManagerWithRollbackAndSynchronizationNever() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronizationName("SYNCHRONIZATION_NEVER");
		tt.setTimeout(10);
		ptm.afterPropertiesSet();

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(status.getTransactionName()).isEmpty();
				assertThat(status.hasTransaction()).isTrue();
				assertThat(status.isNewTransaction()).isTrue();
				assertThat(status.isNested()).isFalse();
				assertThat(status.isReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
				assertThat(status.isRollbackOnly()).isFalse();
				status.setRollbackOnly();
				assertThat(status.isRollbackOnly()).isTrue();
				assertThat(status.isCompleted()).isFalse();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).setTransactionTimeout(10);
		verify(ut).begin();
		verify(ut, atLeastOnce()).getStatus();
		verify(ut).rollback();
	}

	@Test
	void jtaTransactionManagerWithExistingTransactionAndRollbackOnly() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	void jtaTransactionManagerWithExistingTransactionAndException() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThatIllegalStateException().isThrownBy(() ->
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					TransactionSynchronizationManager.registerSynchronization(synch);
					throw new IllegalStateException("I want a rollback");
				}
			}));
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	void jtaTransactionManagerWithExistingTransactionAndCommitException() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock();
		willThrow(new OptimisticLockingFailureException("")).given(synch).beforeCommit(false);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() ->
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					TransactionSynchronizationManager.registerSynchronization(synch);
				}
			}));
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	void jtaTransactionManagerWithExistingTransactionAndRollbackOnlyAndNoGlobalRollback() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		ptm.setGlobalRollbackOnParticipationFailure(false);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	void jtaTransactionManagerWithExistingTransactionAndExceptionAndNoGlobalRollback() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		ptm.setGlobalRollbackOnParticipationFailure(false);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThatIllegalStateException().isThrownBy(() ->
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					TransactionSynchronizationManager.registerSynchronization(synch);
					throw new IllegalStateException("I want a rollback");
				}
			}));
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	void jtaTransactionManagerWithExistingTransactionAndJtaSynchronization() throws Exception {
		UserTransaction ut = mock();
		TransactionManager tm = mock();
		MockJtaTransaction tx = new MockJtaTransaction();

		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(tx);

		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(tx.getSynchronization()).isNotNull();
		tx.getSynchronization().beforeCompletion();
		tx.getSynchronization().afterCompletion(Status.STATUS_ROLLEDBACK);

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
	}

	@Test
	void jtaTransactionManagerWithExistingTransactionAndSynchronizationOnActual() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	void jtaTransactionManagerWithExistingTransactionAndSynchronizationNever() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_NEVER);
		ptm.afterPropertiesSet();

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
				status.setRollbackOnly();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).setRollbackOnly();
	}

	@Test
	void jtaTransactionManagerWithExistingAndPropagationSupports() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	void jtaTransactionManagerWithPropagationSupports() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

		final TransactionSynchronization synch = mock();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
	}

	@Test
	void jtaTransactionManagerWithPropagationSupportsAndSynchronizationOnActual() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		ptm.afterPropertiesSet();

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
				status.setRollbackOnly();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
	}

	@Test
	void jtaTransactionManagerWithPropagationSupportsAndSynchronizationNever() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_NEVER);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		ptm.afterPropertiesSet();

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
				status.setRollbackOnly();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
	}

	@Test
	void jtaTransactionManagerWithPropagationNotSupported() throws Exception {
		UserTransaction ut = mock();
		TransactionManager tm = mock();
		Transaction tx = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.suspend()).willReturn(tx);

		JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				status.setRollbackOnly();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(tm).resume(tx);
	}

	@Test
	void jtaTransactionManagerWithPropagationRequiresNew() throws Exception {
		UserTransaction ut = mock();
		TransactionManager tm = mock();
		Transaction tx = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		given(tm.suspend()).willReturn(tx);

		final JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		tt.setName("txName");

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isEqualTo("txName");
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

				TransactionTemplate tt2 = new TransactionTemplate(ptm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				tt2.setReadOnly(true);
				tt2.setName("txName2");
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isEqualTo("txName2");
						assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
					}
				});

				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isEqualTo("txName");
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut, times(2)).begin();
		verify(ut, times(2)).commit();
		verify(tm).resume(tx);
	}

	@Test
	void jtaTransactionManagerWithPropagationRequiresNewWithinSupports() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		final JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

				TransactionTemplate tt2 = new TransactionTemplate(ptm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
						assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
					}
				});

				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).begin();
		verify(ut).commit();
	}

	@Test
	void jtaTransactionManagerWithPropagationRequiresNewAndExisting() throws Exception {
		UserTransaction ut = mock();
		TransactionManager tm = mock();
		Transaction tx = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.suspend()).willReturn(tx);

		JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(ut).begin();
		verify(ut).commit();
		verify(tm).resume(tx);
	}

	@Test
	void jtaTransactionManagerWithPropagationRequiresNewAndExistingWithSuspendException() throws Exception {
		UserTransaction ut = mock();
		TransactionManager tm = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		willThrow(new SystemException()).given(tm).suspend();

		JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				}
			}));
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
	}

	@Test
	void jtaTransactionManagerWithPropagationRequiresNewAndExistingWithBeginException() throws Exception {
		UserTransaction ut = mock();
		TransactionManager tm = mock();
		Transaction tx = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.suspend()).willReturn(tx);
		willThrow(new SystemException()).given(ut).begin();

		JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThatExceptionOfType(CannotCreateTransactionException.class).isThrownBy(() ->
		tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				}
			}));
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		verify(tm).resume(tx);
	}

	@Test
	void jtaTransactionManagerWithPropagationRequiresNewAndAdapter() throws Exception {
		TransactionManager tm = mock();
		Transaction tx = mock();
		given(tm.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.suspend()).willReturn(tx);

		JtaTransactionManager ptm = newJtaTransactionManager(tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
			}
		});
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(tm).begin();
		verify(tm).commit();
		verify(tm).resume(tx);
	}

	@Test
	void jtaTransactionManagerWithPropagationRequiresNewAndSuspensionNotSupported() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThatExceptionOfType(TransactionSuspensionNotSupportedException.class).isThrownBy(() ->
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
				}
			}));
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
	}

	@Test
	void jtaTransactionManagerWithIsolationLevel() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

		assertThatExceptionOfType(InvalidIsolationLevelException.class).isThrownBy(() -> {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
		});
	}

	@Test
	void jtaTransactionManagerWithSystemExceptionOnIsExisting() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willThrow(new SystemException("system exception"));

		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
		});
	}

	@Test
	void jtaTransactionManagerWithNestedBegin() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertThat(status.getTransactionName()).isEmpty();
				assertThat(status.hasTransaction()).isTrue();
				assertThat(status.isNewTransaction()).isTrue();
				assertThat(status.isNested()).isTrue();
				assertThat(status.isReadOnly()).isFalse();
				assertThat(status.isRollbackOnly()).isFalse();
				assertThat(status.isCompleted()).isFalse();
			}
		});

		verify(ut).begin();
		verify(ut).commit();
	}

	@Test
	void jtaTransactionManagerWithNotSupportedExceptionOnNestedBegin() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		willThrow(new NotSupportedException("not supported")).given(ut).begin();

		assertThatExceptionOfType(NestedTransactionNotSupportedException.class).isThrownBy(() -> {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
		});
	}

	@Test
	void jtaTransactionManagerWithUnsupportedOperationExceptionOnNestedBegin() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		willThrow(new UnsupportedOperationException("not supported")).given(ut).begin();

		assertThatExceptionOfType(NestedTransactionNotSupportedException.class).isThrownBy(() -> {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
		});
	}

	@Test
	void jtaTransactionManagerWithSystemExceptionOnBegin() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);
		willThrow(new SystemException("system exception")).given(ut).begin();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		ptm.addListener(tl);

		assertThatExceptionOfType(CannotCreateTransactionException.class).isThrownBy(() -> {
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
		});

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beginFailure).isInstanceOf(CannotCreateTransactionException.class);
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.commitFailure).isNull();
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isFalse();
		assertThat(tl.rollbackFailure).isNull();
	}

	@Test
	void jtaTransactionManagerWithRollbackExceptionOnCommit() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		willThrow(new RollbackException("unexpected rollback")).given(ut).commit();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		ptm.addListener(tl);

		assertThatExceptionOfType(UnexpectedRollbackException.class).isThrownBy(() -> {
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
						@Override
						public void afterCompletion(int status) {
							assertThat(status).as("Correct completion status")
									.isEqualTo(TransactionSynchronization.STATUS_ROLLED_BACK);
						}
					});
				}
			});
		});

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beginFailure).isNull();
		assertThat(tl.beforeCommitCalled).isTrue();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.commitFailure).isNull();
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isTrue();
		assertThat(tl.rollbackFailure).isNull();

		verify(ut).begin();
	}

	@Test
	void jtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly() throws Exception {
		doTestJtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly(false);
	}

	@Test
	void jtaTransactionManagerWithNoExceptionOnGlobalRollbackOnlyAndFailEarly() throws Exception {
		doTestJtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly(true);
	}

	private void doTestJtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly(boolean failEarly) throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_MARKED_ROLLBACK, Status.STATUS_MARKED_ROLLBACK,
				Status.STATUS_MARKED_ROLLBACK);

		JtaTransactionManager tm = newJtaTransactionManager(ut);
		if (failEarly) {
			tm.setFailEarlyOnGlobalRollbackOnly(true);
		}

		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		boolean outerTransactionBoundaryReached = false;
		try {
			assertThat(ts.isNewTransaction()).as("Is new transaction").isTrue();

			TransactionTemplate tt = new TransactionTemplate(tm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
						@Override
						public void afterCompletion(int status) {
							assertThat(status).as("Correct completion status")
									.isEqualTo(TransactionSynchronization.STATUS_ROLLED_BACK);
						}
					});
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
			assertThat(outerTransactionBoundaryReached).isNotEqualTo(failEarly);
		}

		verify(ut).begin();
		if (failEarly) {
			verify(ut).rollback();
		}
		else {
			verify(ut).commit();
		}
	}

	@Test
	void jtaTransactionManagerWithHeuristicMixedExceptionOnCommit() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		willThrow(new HeuristicMixedException("heuristic exception")).given(ut).commit();

		assertThatExceptionOfType(HeuristicCompletionException.class).isThrownBy(() -> {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
						@Override
						public void afterCompletion(int status) {
							assertThat(status == TransactionSynchronization.STATUS_UNKNOWN).as("Correct completion status").isTrue();
						}
					});
				}
			});
		}).satisfies(ex -> assertThat(ex.getOutcomeState()).isEqualTo(HeuristicCompletionException.STATE_MIXED));

		verify(ut).begin();
	}

	@Test
	void jtaTransactionManagerWithHeuristicRollbackExceptionOnCommit() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		willThrow(new HeuristicRollbackException("heuristic exception")).given(ut).commit();

		assertThatExceptionOfType(HeuristicCompletionException.class).isThrownBy(() -> {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
						@Override
						public void afterCompletion(int status) {
							assertThat(status == TransactionSynchronization.STATUS_UNKNOWN).as("Correct completion status").isTrue();
						}
					});
				}
			});
		}).satisfies(ex -> assertThat(ex.getOutcomeState()).isEqualTo(HeuristicCompletionException.STATE_ROLLED_BACK));

		verify(ut).begin();
	}

	@Test
	void jtaTransactionManagerWithSystemExceptionOnCommit() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		willThrow(new SystemException("system exception")).given(ut).commit();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		ptm.addListener(tl);

		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> {
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
						@Override
						public void afterCompletion(int status) {
							assertThat(status).as("Correct completion status")
									.isEqualTo(TransactionSynchronization.STATUS_UNKNOWN);
						}
					});
				}
			});
		});

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beginFailure).isNull();
		assertThat(tl.beforeCommitCalled).isTrue();
		assertThat(tl.afterCommitCalled).isTrue();
		assertThat(tl.commitFailure).isInstanceOf(TransactionSystemException.class);
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isFalse();
		assertThat(tl.rollbackFailure).isNull();

		verify(ut).begin();
	}

	@Test
	void jtaTransactionManagerWithSystemExceptionOnRollback() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
		willThrow(new SystemException("system exception")).given(ut).rollback();

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		ptm.addListener(tl);

		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> {
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
						@Override
						public void afterCompletion(int status) {
							assertThat(status).as("Correct completion status")
									.isEqualTo(TransactionSynchronization.STATUS_UNKNOWN);
						}
					});
					status.setRollbackOnly();
				}
			});
		});

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beginFailure).isNull();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.commitFailure).isNull();
		assertThat(tl.beforeRollbackCalled).isTrue();
		assertThat(tl.afterRollbackCalled).isTrue();
		assertThat(tl.rollbackFailure).isInstanceOf(TransactionSystemException.class);

		verify(ut).begin();
	}

	@Test
	void jtaTransactionManagerWithIllegalStateExceptionOnRollbackOnly() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		willThrow(new IllegalStateException("no existing transaction")).given(ut).setRollbackOnly();

		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					status.setRollbackOnly();
				}
			});
		});
	}

	@Test
	void jtaTransactionManagerWithSystemExceptionOnRollbackOnly() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		willThrow(new SystemException("system exception")).given(ut).setRollbackOnly();

		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() -> {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					status.setRollbackOnly();
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
						@Override
						public void afterCompletion(int status) {
							assertThat(status).as("Correct completion status")
									.isEqualTo(TransactionSynchronization.STATUS_UNKNOWN);
						}
					});
				}
			});
		});
	}

	@Test
	void jtaTransactionManagerWithDoubleCommit() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
		// first commit
		ptm.commit(status);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		// second commit attempt
		assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
				ptm.commit(status));
		verify(ut).begin();
		verify(ut).commit();
	}

	@Test
	void jtaTransactionManagerWithDoubleRollback() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
		// first rollback
		ptm.rollback(status);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		// second rollback attempt
		assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
				ptm.rollback(status));

		verify(ut).begin();
		verify(ut).rollback();
	}

	@Test
	void jtaTransactionManagerWithRollbackAndCommit() throws Exception {
		UserTransaction ut = mock();
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
		// first: rollback
		ptm.rollback(status);
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		// second: commit attempt
		assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
				ptm.commit(status));

		verify(ut).begin();
		verify(ut).rollback();
	}


	protected JtaTransactionManager newJtaTransactionManager(UserTransaction ut) {
		return new JtaTransactionManager(ut);
	}

	protected JtaTransactionManager newJtaTransactionManager(TransactionManager tm) {
		return new JtaTransactionManager(tm);
	}

	protected JtaTransactionManager newJtaTransactionManager(UserTransaction ut, TransactionManager tm) {
		return new JtaTransactionManager(ut, tm);
	}


	/**
	 * Prevent any side effects due to this test modifying ThreadLocals that might
	 * affect subsequent tests when all tests are run in the same JVM, as with Eclipse.
	 */
	@AfterEach
	void tearDown() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()).isNull();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}

}

/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.transaction;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Test;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.tests.transaction.MockJtaTransaction;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @since 12.05.2003
 */
public class JtaTransactionManagerTests {

	@Test
	public void jtaTransactionManagerWithCommit() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setName("txName");

		assertEquals(JtaTransactionManager.SYNCHRONIZATION_ALWAYS, ptm.getTransactionSynchronization());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionName());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				TransactionSynchronizationManager.registerSynchronization(synch);
				assertEquals("txName", TransactionSynchronizationManager.getCurrentTransactionName());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionName());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		verify(ut).begin();
		verify(ut).commit();
		verify(synch).beforeCommit(false);
		verify(synch).beforeCompletion();
		verify(synch).afterCommit();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
	}

	@Test
	public void jtaTransactionManagerWithCommitAndSynchronizationOnActual() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				TransactionSynchronizationManager.registerSynchronization(synch);
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).begin();
		verify(ut).commit();
		verify(synch).beforeCommit(false);
		verify(synch).beforeCompletion();
		verify(synch).afterCommit();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
	}

	@Test
	public void jtaTransactionManagerWithCommitAndSynchronizationNever() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(
		Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_NEVER);
		ptm.afterPropertiesSet();

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).begin();
		verify(ut).commit();
	}

	@Test
	public void jtaTransactionManagerWithRollback() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setTimeout(10);
		tt.setName("txName");

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionName());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				TransactionSynchronizationManager.registerSynchronization(synch);
				assertEquals("txName", TransactionSynchronizationManager.getCurrentTransactionName());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionName());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

		verify(ut).setTransactionTimeout(10);
		verify(ut).begin();
		verify(ut).rollback();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
	}

	@Test
	public void jtaTransactionManagerWithRollbackAndSynchronizationOnActual() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		tt.setTimeout(10);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).setTransactionTimeout(10);
		verify(ut).begin();
		verify(ut).rollback();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
	}

	@Test
	public void jtaTransactionManagerWithRollbackAndSynchronizationNever() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronizationName("SYNCHRONIZATION_NEVER");
		tt.setTimeout(10);
		ptm.afterPropertiesSet();

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).setTransactionTimeout(10);
		verify(ut).begin();
		verify(ut, atLeastOnce()).getStatus();
		verify(ut).rollback();
	}

	@Test
	public void jtaTransactionManagerWithExistingTransactionAndRollbackOnly() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	public void jtaTransactionManagerWithExistingTransactionAndException() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					TransactionSynchronizationManager.registerSynchronization(synch);
					throw new IllegalStateException("I want a rollback");
				}
			});
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	public void jtaTransactionManagerWithExistingTransactionAndCommitException() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock(TransactionSynchronization.class);
		willThrow(new OptimisticLockingFailureException("")).given(synch).beforeCommit(false);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					TransactionSynchronizationManager.registerSynchronization(synch);
				}
			});
			fail("Should have thrown OptimisticLockingFailureException");
		}
		catch (OptimisticLockingFailureException ex) {
			// expected
		}
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	public void jtaTransactionManagerWithExistingTransactionAndRollbackOnlyAndNoGlobalRollback() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		ptm.setGlobalRollbackOnParticipationFailure(false);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	public void jtaTransactionManagerWithExistingTransactionAndExceptionAndNoGlobalRollback() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		ptm.setGlobalRollbackOnParticipationFailure(false);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
					TransactionSynchronizationManager.registerSynchronization(synch);
					throw new IllegalStateException("I want a rollback");
				}
			});
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	public void jtaTransactionManagerWithExistingTransactionAndJtaSynchronization() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		TransactionManager tm = mock(TransactionManager.class);
		MockJtaTransaction tx = new MockJtaTransaction();

		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.getTransaction()).willReturn(tx);

		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNotNull(tx.getSynchronization());
		tx.getSynchronization().beforeCompletion();
		tx.getSynchronization().afterCompletion(Status.STATUS_ROLLEDBACK);

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
	}

	@Test
	public void jtaTransactionManagerWithExistingTransactionAndSynchronizationOnActual() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	public void jtaTransactionManagerWithExistingTransactionAndSynchronizationNever() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_NEVER);
		ptm.afterPropertiesSet();

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).setRollbackOnly();
	}

	@Test
	public void jtaTransactionManagerWithExistingAndPropagationSupports() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).setRollbackOnly();
		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
	}

	@Test
	public void jtaTransactionManagerWithPropagationSupports() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

		final TransactionSynchronization synch = mock(TransactionSynchronization.class);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				TransactionSynchronizationManager.registerSynchronization(synch);
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(synch).beforeCompletion();
		verify(synch).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
	}

	@Test
	public void jtaTransactionManagerWithPropagationSupportsAndSynchronizationOnActual() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		ptm.afterPropertiesSet();

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void jtaTransactionManagerWithPropagationSupportsAndSynchronizationNever() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		ptm.setTransactionSynchronization(JtaTransactionManager.SYNCHRONIZATION_NEVER);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		ptm.afterPropertiesSet();

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void jtaTransactionManagerWithPropagationNotSupported() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		TransactionManager tm = mock(TransactionManager.class);
		Transaction tx = mock(Transaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.suspend()).willReturn(tx);

		JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				status.setRollbackOnly();
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(tm).resume(tx);
	}

	@Test
	public void jtaTransactionManagerWithPropagationRequiresNew() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		TransactionManager tm = mock(TransactionManager.class);
		Transaction tx = mock(Transaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		given(tm.suspend()).willReturn(tx);

		final JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		tt.setName("txName");

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertEquals("txName", TransactionSynchronizationManager.getCurrentTransactionName());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());

				TransactionTemplate tt2 = new TransactionTemplate(ptm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				tt2.setReadOnly(true);
				tt2.setName("txName2");
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
						assertEquals("txName2", TransactionSynchronizationManager.getCurrentTransactionName());
						assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
					}
				});

				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertEquals("txName", TransactionSynchronizationManager.getCurrentTransactionName());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut, times(2)).begin();
		verify(ut, times(2)).commit();
		verify(tm).resume(tx);
	}

	@Test
	public void jtaTransactionManagerWithPropagationRequiresNewWithinSupports() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		final JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());

				TransactionTemplate tt2 = new TransactionTemplate(ptm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				tt2.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
						assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
					}
				});

				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
				assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).begin();
		verify(ut).commit();
	}

	@Test
	public void jtaTransactionManagerWithPropagationRequiresNewAndExisting() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		TransactionManager tm = mock(TransactionManager.class);
		Transaction tx = mock(Transaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.suspend()).willReturn(tx);

		JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(ut).begin();
		verify(ut).commit();
		verify(tm).resume(tx);
	}

	@Test
	public void jtaTransactionManagerWithPropagationRequiresNewAndExistingWithSuspendException() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		TransactionManager tm = mock(TransactionManager.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		willThrow(new SystemException()).given(tm).suspend();

		JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
		}
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void jtaTransactionManagerWithPropagationRequiresNewAndExistingWithBeginException() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		TransactionManager tm = mock(TransactionManager.class);
		Transaction tx = mock(Transaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.suspend()).willReturn(tx);
		willThrow(new SystemException()).given(ut).begin();

		JtaTransactionManager ptm = newJtaTransactionManager(ut, tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
				}
			});
			fail("Should have thrown CannotCreateTransactionException");
		}
		catch (CannotCreateTransactionException ex) {
			// expected
		}
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		verify(tm).resume(tx);
	}

	@Test
	public void jtaTransactionManagerWithPropagationRequiresNewAndAdapter() throws Exception {
		TransactionManager tm = mock(TransactionManager.class);
		Transaction tx = mock(Transaction.class);
		given(tm.getStatus()).willReturn(Status.STATUS_ACTIVE);
		given(tm.suspend()).willReturn(tx);

		JtaTransactionManager ptm = newJtaTransactionManager(tm);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
			}
		});
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());

		verify(tm).begin();
		verify(tm).commit();
		verify(tm).resume(tx);
	}

	@Test
	public void jtaTransactionManagerWithPropagationRequiresNewAndSuspensionNotSupported() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		try {
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
				}
			});
			fail("Should have thrown TransactionSuspensionNotSupportedException");
		}
		catch (TransactionSuspensionNotSupportedException ex) {
			// expected
		}
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

	@Test
	public void jtaTransactionManagerWithIsolationLevel() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
			fail("Should have thrown InvalidIsolationLevelException");
		}
		catch (InvalidIsolationLevelException ex) {
			// expected
		}
	}

	@Test
	public void jtaTransactionManagerWithSystemExceptionOnIsExisting() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willThrow(new SystemException("system exception"));

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
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
	}

	@Test
	public void jtaTransactionManagerWithNestedBegin() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// something transactional
			}
		});

		verify(ut).begin();
		verify(ut).commit();
	}

	@Test
	public void jtaTransactionManagerWithNotSupportedExceptionOnNestedBegin() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		willThrow(new NotSupportedException("not supported")).given(ut).begin();

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
			fail("Should have thrown NestedTransactionNotSupportedException");
		}
		catch (NestedTransactionNotSupportedException ex) {
			// expected
		}
	}

	@Test
	public void jtaTransactionManagerWithUnsupportedOperationExceptionOnNestedBegin() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		willThrow(new UnsupportedOperationException("not supported")).given(ut).begin();

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
				}
			});
			fail("Should have thrown NestedTransactionNotSupportedException");
		}
		catch (NestedTransactionNotSupportedException ex) {
			// expected
		}
	}

	@Test
	public void jtaTransactionManagerWithSystemExceptionOnBegin() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION);
		willThrow(new SystemException("system exception")).given(ut).begin();

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
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
	}

	@Test
	public void jtaTransactionManagerWithRollbackExceptionOnCommit() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		willThrow(new RollbackException("unexpected rollback")).given(ut).commit();

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
						@Override
						public void afterCompletion(int status) {
							assertTrue("Correct completion status", status == TransactionSynchronization.STATUS_ROLLED_BACK);
						}
					});
				}
			});
			fail("Should have thrown UnexpectedRollbackException");
		}
		catch (UnexpectedRollbackException ex) {
			// expected
		}

		verify(ut).begin();
	}

	@Test
	public void jtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly() throws Exception {
		doTestJtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly(false);
	}

	@Test
	public void jtaTransactionManagerWithNoExceptionOnGlobalRollbackOnlyAndFailEarly() throws Exception {
		doTestJtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly(true);
	}

	private void doTestJtaTransactionManagerWithNoExceptionOnGlobalRollbackOnly(boolean failEarly) throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
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
			assertTrue("Is new transaction", ts.isNewTransaction());

			TransactionTemplate tt = new TransactionTemplate(tm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
						@Override
						public void afterCompletion(int status) {
							assertTrue("Correct completion status", status == TransactionSynchronization.STATUS_ROLLED_BACK);
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
			if (failEarly) {
				assertFalse(outerTransactionBoundaryReached);
			}
			else {
				assertTrue(outerTransactionBoundaryReached);
			}
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
	public void jtaTransactionManagerWithHeuristicMixedExceptionOnCommit() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		willThrow(new HeuristicMixedException("heuristic exception")).given(ut).commit();

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
						@Override
						public void afterCompletion(int status) {
							assertTrue("Correct completion status", status == TransactionSynchronization.STATUS_UNKNOWN);
						}
					});
				}
			});
			fail("Should have thrown HeuristicCompletionException");
		}
		catch (HeuristicCompletionException ex) {
			// expected
			assertTrue(ex.getOutcomeState() == HeuristicCompletionException.STATE_MIXED);
		}

		verify(ut).begin();
	}

	@Test
	public void jtaTransactionManagerWithHeuristicRollbackExceptionOnCommit() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		willThrow(new HeuristicRollbackException("heuristic exception")).given(ut).commit();

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
						@Override
						public void afterCompletion(int status) {
							assertTrue("Correct completion status", status == TransactionSynchronization.STATUS_UNKNOWN);
						}
					});
				}
			});
			fail("Should have thrown HeuristicCompletionException");
		}
		catch (HeuristicCompletionException ex) {
			// expected
			assertTrue(ex.getOutcomeState() == HeuristicCompletionException.STATE_ROLLED_BACK);
		}

		verify(ut).begin();
	}

	@Test
	public void jtaTransactionManagerWithSystemExceptionOnCommit() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);
		willThrow(new SystemException("system exception")).given(ut).commit();

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// something transactional
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
						@Override
						public void afterCompletion(int status) {
							assertTrue("Correct completion status", status == TransactionSynchronization.STATUS_UNKNOWN);
						}
					});
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
		}

		verify(ut).begin();
	}

	@Test
	public void jtaTransactionManagerWithSystemExceptionOnRollback() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);
		willThrow(new SystemException("system exception")).given(ut).rollback();

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
						@Override
						public void afterCompletion(int status) {
							assertTrue("Correct completion status", status == TransactionSynchronization.STATUS_UNKNOWN);
						}
					});
					status.setRollbackOnly();
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
		}

		verify(ut).begin();
	}

	@Test
	public void jtaTransactionManagerWithIllegalStateExceptionOnRollbackOnly() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		willThrow(new IllegalStateException("no existing transaction")).given(ut).setRollbackOnly();

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					status.setRollbackOnly();
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
		}
	}

	@Test
	public void jtaTransactionManagerWithSystemExceptionOnRollbackOnly() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_ACTIVE);
		willThrow(new SystemException("system exception")).given(ut).setRollbackOnly();

		try {
			JtaTransactionManager ptm = newJtaTransactionManager(ut);
			TransactionTemplate tt = new TransactionTemplate(ptm);
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					status.setRollbackOnly();
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
						@Override
						public void afterCompletion(int status) {
							assertTrue("Correct completion status", status == TransactionSynchronization.STATUS_UNKNOWN);
						}
					});
				}
			});
			fail("Should have thrown TransactionSystemException");
		}
		catch (TransactionSystemException ex) {
			// expected
		}
	}

	@Test
	public void jtaTransactionManagerWithDoubleCommit() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION,
				Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());
		assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
		// first commit
		ptm.commit(status);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		try {
			// second commit attempt
			ptm.commit(status);
			fail("Should have thrown IllegalTransactionStateException");
		}
		catch (IllegalTransactionStateException ex) {
			// expected
		}

		verify(ut).begin();
		verify(ut).commit();
	}

	@Test
	public void jtaTransactionManagerWithDoubleRollback() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());
		assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
		// first rollback
		ptm.rollback(status);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		try {
			// second rollback attempt
			ptm.rollback(status);
			fail("Should have thrown IllegalTransactionStateException");
		}
		catch (IllegalTransactionStateException ex) {
			// expected
		}

		verify(ut).begin();
		verify(ut).rollback();
	}

	@Test
	public void jtaTransactionManagerWithRollbackAndCommit() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE);

		JtaTransactionManager ptm = newJtaTransactionManager(ut);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());
		assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
		// first: rollback
		ptm.rollback(status);
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		try {
			// second: commit attempt
			ptm.commit(status);
			fail("Should have thrown IllegalTransactionStateException");
		}
		catch (IllegalTransactionStateException ex) {
			// expected
		}

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
	 * Prevent any side-effects due to this test modifying ThreadLocals that might
	 * affect subsequent tests when all tests are run in the same JVM, as with Eclipse.
	 */
	@After
	public void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionName());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

}

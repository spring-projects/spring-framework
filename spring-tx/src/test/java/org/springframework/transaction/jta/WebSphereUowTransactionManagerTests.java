/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.jta;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWException;
import com.ibm.wsspi.uow.UOWManager;
import org.junit.jupiter.api.Test;

import org.springframework.context.testfixture.jndi.ExpectedLookupTemplate;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 */
public class WebSphereUowTransactionManagerTests {

	@Test
	public void uowManagerFoundInJndi() {
		MockUOWManager manager = new MockUOWManager();
		ExpectedLookupTemplate jndiTemplate =
				new ExpectedLookupTemplate(WebSphereUowTransactionManager.DEFAULT_UOW_MANAGER_NAME, manager);
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager();
		ptm.setJndiTemplate(jndiTemplate);
		ptm.afterPropertiesSet();

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		assertThat(ptm.execute(definition, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				return "result";
			}
		})).isEqualTo("result");

		assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
		assertThat(manager.getJoined()).isFalse();
		assertThat(manager.getRollbackOnly()).isFalse();
	}

	@Test
	public void uowManagerAndUserTransactionFoundInJndi() throws Exception {
		UserTransaction ut = mock(UserTransaction.class);
		given(ut.getStatus()).willReturn( Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

		MockUOWManager manager = new MockUOWManager();
		ExpectedLookupTemplate jndiTemplate = new ExpectedLookupTemplate();
		jndiTemplate.addObject(WebSphereUowTransactionManager.DEFAULT_USER_TRANSACTION_NAME, ut);
		jndiTemplate.addObject(WebSphereUowTransactionManager.DEFAULT_UOW_MANAGER_NAME, manager);
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager();
		ptm.setJndiTemplate(jndiTemplate);
		ptm.afterPropertiesSet();

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		TransactionStatus ts = ptm.getTransaction(definition);
		ptm.commit(ts);
		assertThat(ptm.execute(definition, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				return "result";
			}
		})).isEqualTo("result");

		assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
		assertThat(manager.getJoined()).isFalse();
		assertThat(manager.getRollbackOnly()).isFalse();
		verify(ut).begin();
		verify(ut).commit();
	}

	@Test
	public void propagationMandatoryFailsInCaseOfNoExistingTransaction() {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);

		assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
			ptm.execute(definition, new TransactionCallback<String>() {
				@Override
				public String doInTransaction(TransactionStatus status) {
					return "result";
				}
			}));
	}

	@Test
	public void newTransactionSynchronizationUsingPropagationSupports() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_SUPPORTS, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	@Test
	public void newTransactionSynchronizationUsingPropagationNotSupported() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NOT_SUPPORTED, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	@Test
	public void newTransactionSynchronizationUsingPropagationNever() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NEVER, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	@Test
	public void newTransactionSynchronizationUsingPropagationSupportsAndSynchOnActual() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_SUPPORTS, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	@Test
	public void newTransactionSynchronizationUsingPropagationNotSupportedAndSynchOnActual() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NOT_SUPPORTED, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	@Test
	public void newTransactionSynchronizationUsingPropagationNeverAndSynchOnActual() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NEVER, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	@Test
	public void newTransactionSynchronizationUsingPropagationSupportsAndSynchNever() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_SUPPORTS, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	@Test
	public void newTransactionSynchronizationUsingPropagationNotSupportedAndSynchNever() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NOT_SUPPORTED, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	@Test
	public void newTransactionSynchronizationUsingPropagationNeverAndSynchNever() {
		doTestNewTransactionSynchronization(
				TransactionDefinition.PROPAGATION_NEVER, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	private void doTestNewTransactionSynchronization(int propagationBehavior, final int synchMode) {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		ptm.setTransactionSynchronization(synchMode);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(propagationBehavior);
		definition.setReadOnly(true);

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(ptm.execute(definition, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				if (synchMode == WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS) {
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
					assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
				}
				else {
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
					assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
					assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				}
				return "result";
			}
		})).isEqualTo("result");

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(manager.getUOWTimeout()).isEqualTo(0);
		assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_LOCAL_TRANSACTION);
		assertThat(manager.getJoined()).isFalse();
		assertThat(manager.getRollbackOnly()).isFalse();
	}

	@Test
	public void newTransactionWithCommitUsingPropagationRequired() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRED, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	@Test
	public void newTransactionWithCommitUsingPropagationRequiresNew() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRES_NEW, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	@Test
	public void newTransactionWithCommitUsingPropagationNested() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_NESTED, WebSphereUowTransactionManager.SYNCHRONIZATION_ALWAYS);
	}

	@Test
	public void newTransactionWithCommitUsingPropagationRequiredAndSynchOnActual() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRED, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	@Test
	public void newTransactionWithCommitUsingPropagationRequiresNewAndSynchOnActual() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRES_NEW, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	@Test
	public void newTransactionWithCommitUsingPropagationNestedAndSynchOnActual() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_NESTED, WebSphereUowTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	@Test
	public void newTransactionWithCommitUsingPropagationRequiredAndSynchNever() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRED, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	@Test
	public void newTransactionWithCommitUsingPropagationRequiresNewAndSynchNever() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_REQUIRES_NEW, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	@Test
	public void newTransactionWithCommitUsingPropagationNestedAndSynchNever() {
		doTestNewTransactionWithCommit(
				TransactionDefinition.PROPAGATION_NESTED, WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER);
	}

	private void doTestNewTransactionWithCommit(int propagationBehavior, final int synchMode) {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		ptm.setTransactionSynchronization(synchMode);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(propagationBehavior);
		definition.setReadOnly(true);

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(ptm.execute(definition, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				if (synchMode != WebSphereUowTransactionManager.SYNCHRONIZATION_NEVER) {
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
					assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
				}
				else {
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
					assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
					assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				}
				return "result";
			}
		})).isEqualTo("result");

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(manager.getUOWTimeout()).isEqualTo(0);
		assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
		assertThat(manager.getJoined()).isFalse();
		assertThat(manager.getRollbackOnly()).isFalse();
	}

	@Test
	public void newTransactionWithCommitAndTimeout() {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setTimeout(10);
		definition.setReadOnly(true);

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(ptm.execute(definition, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
				return "result";
			}
		})).isEqualTo("result");

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(manager.getUOWTimeout()).isEqualTo(10);
		assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
		assertThat(manager.getJoined()).isFalse();
		assertThat(manager.getRollbackOnly()).isFalse();
	}

	@Test
	public void newTransactionWithCommitException() {
		final RollbackException rex = new RollbackException();
		MockUOWManager manager = new MockUOWManager() {
			@Override
			public void runUnderUOW(int type, boolean join, UOWAction action) throws UOWException {
				throw new UOWException(rex);
			}
		};
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
				ptm.execute(definition, new TransactionCallback<String>() {
					@Override
					public String doInTransaction(TransactionStatus status) {
						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
						assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
						return "result";
					}
				}))
			.withCauseInstanceOf(UOWException.class)
			.satisfies(ex -> {
				assertThat(ex.getRootCause()).isSameAs(rex);
				assertThat(ex.getMostSpecificCause()).isSameAs(rex);
			});

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(manager.getUOWTimeout()).isEqualTo(0);
	}

	@Test
	public void newTransactionWithRollback() {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() ->
			ptm.execute(definition, new TransactionCallback<String>() {
				@Override
				public String doInTransaction(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
					assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
					throw new OptimisticLockingFailureException("");
				}
			}));

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(manager.getUOWTimeout()).isEqualTo(0);
		assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
		assertThat(manager.getJoined()).isFalse();
		assertThat(manager.getRollbackOnly()).isTrue();
	}

	@Test
	public void newTransactionWithRollbackOnly() {
		MockUOWManager manager = new MockUOWManager();
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(ptm.execute(definition, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				status.setRollbackOnly();
				return "result";
			}
		})).isEqualTo("result");

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(manager.getUOWTimeout()).isEqualTo(0);
		assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
		assertThat(manager.getJoined()).isFalse();
		assertThat(manager.getRollbackOnly()).isTrue();
	}

	@Test
	public void existingNonSpringTransaction() {
		MockUOWManager manager = new MockUOWManager();
		manager.setUOWStatus(UOWManager.UOW_STATUS_ACTIVE);
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(ptm.execute(definition, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				return "result";
			}
		})).isEqualTo("result");

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(manager.getUOWTimeout()).isEqualTo(0);
		assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
		assertThat(manager.getJoined()).isTrue();
		assertThat(manager.getRollbackOnly()).isFalse();
	}

	@Test
	public void propagationNeverFailsInCaseOfExistingTransaction() {
		MockUOWManager manager = new MockUOWManager();
		manager.setUOWStatus(UOWManager.UOW_STATUS_ACTIVE);
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);

		assertThatExceptionOfType(IllegalTransactionStateException.class).isThrownBy(() ->
			ptm.execute(definition, new TransactionCallback<String>() {
				@Override
				public String doInTransaction(TransactionStatus status) {
					return "result";
				}
			}));
	}

	@Test
	public void propagationNestedFailsInCaseOfExistingTransaction() {
		MockUOWManager manager = new MockUOWManager();
		manager.setUOWStatus(UOWManager.UOW_STATUS_ACTIVE);
		WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);

		assertThatExceptionOfType(NestedTransactionNotSupportedException.class).isThrownBy(() ->
			ptm.execute(definition, new TransactionCallback<String>() {
				@Override
				public String doInTransaction(TransactionStatus status) {
					return "result";
				}
			}));
	}

	@Test
	public void existingTransactionWithParticipationUsingPropagationRequired() {
		doTestExistingTransactionWithParticipation(TransactionDefinition.PROPAGATION_REQUIRED);
	}

	@Test
	public void existingTransactionWithParticipationUsingPropagationSupports() {
		doTestExistingTransactionWithParticipation(TransactionDefinition.PROPAGATION_SUPPORTS);
	}

	@Test
	public void existingTransactionWithParticipationUsingPropagationMandatory() {
		doTestExistingTransactionWithParticipation(TransactionDefinition.PROPAGATION_MANDATORY);
	}

	private void doTestExistingTransactionWithParticipation(int propagationBehavior) {
		MockUOWManager manager = new MockUOWManager();
		final WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		final DefaultTransactionDefinition definition2 = new DefaultTransactionDefinition();
		definition2.setPropagationBehavior(propagationBehavior);
		definition2.setReadOnly(true);

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(ptm.execute(definition, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				assertThat(ptm.execute(definition2, new TransactionCallback<String>() {
					@Override
					public String doInTransaction(TransactionStatus status1) {
						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
						assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
						return "result2";
					}
				})).isEqualTo("result2");
				return "result";
			}
		})).isEqualTo("result");

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(manager.getUOWTimeout()).isEqualTo(0);
		assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
		assertThat(manager.getJoined()).isTrue();
		assertThat(manager.getRollbackOnly()).isFalse();
	}

	@Test
	public void existingTransactionWithSuspensionUsingPropagationRequiresNew() {
		doTestExistingTransactionWithSuspension(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Test
	public void existingTransactionWithSuspensionUsingPropagationNotSupported() {
		doTestExistingTransactionWithSuspension(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
	}

	private void doTestExistingTransactionWithSuspension(final int propagationBehavior) {
		MockUOWManager manager = new MockUOWManager();
		final WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		final DefaultTransactionDefinition definition2 = new DefaultTransactionDefinition();
		definition2.setPropagationBehavior(propagationBehavior);
		definition2.setReadOnly(true);

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(ptm.execute(definition, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				assertThat(ptm.execute(definition2, new TransactionCallback<String>() {
					@Override
					public String doInTransaction(TransactionStatus status1) {
						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isEqualTo((propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
						assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
						return "result2";
					}
				})).isEqualTo("result2");
				return "result";
			}
		})).isEqualTo("result");

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(manager.getUOWTimeout()).isEqualTo(0);
		if (propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
			assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
		}
		else {
			assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_LOCAL_TRANSACTION);
		}
		assertThat(manager.getJoined()).isFalse();
		assertThat(manager.getRollbackOnly()).isFalse();
	}

	@Test
	public void existingTransactionUsingPropagationNotSupported() {
		MockUOWManager manager = new MockUOWManager();
		final WebSphereUowTransactionManager ptm = new WebSphereUowTransactionManager(manager);
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		final DefaultTransactionDefinition definition2 = new DefaultTransactionDefinition();
		definition2.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
		definition2.setReadOnly(true);

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(ptm.execute(definition, new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				assertThat(ptm.execute(definition2, new TransactionCallback<String>() {
					@Override
					public String doInTransaction(TransactionStatus status1) {
						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
						assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
						return "result2";
					}
				})).isEqualTo("result2");
				return "result";
			}
		})).isEqualTo("result");

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();

		assertThat(manager.getUOWTimeout()).isEqualTo(0);
		assertThat(manager.getUOWType()).isEqualTo(UOWManager.UOW_TYPE_LOCAL_TRANSACTION);
		assertThat(manager.getJoined()).isFalse();
		assertThat(manager.getRollbackOnly()).isFalse();
	}

}

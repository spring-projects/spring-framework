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

package org.springframework.orm.jpa;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link JpaTransactionManager}.
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
class JpaTransactionManagerTests {

	private static final List<String> testList = List.of("test");


	private final EntityManagerFactory factory = mock();

	private final EntityManager manager = mock();

	private final EntityTransaction tx = mock();

	private final JpaTransactionManager tm = new JpaTransactionManager(factory);

	private final TransactionTemplate tt = new TransactionTemplate(tm);


	@BeforeEach
	void setup() {
		given(factory.createEntityManager()).willReturn(manager);
		given(manager.getTransaction()).willReturn(tx);
		given(manager.isOpen()).willReturn(true);
	}

	@AfterEach
	void verifyTransactionSynchronizationManagerState() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}


	@Test
	void transactionCommit() {
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		Object result = tt.execute(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
			return testList;
		});
		assertThat(result).isSameAs(testList);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(tx).commit();
		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	void transactionCommitWithRollbackException() {
		given(tx.getRollbackOnly()).willReturn(true);
		willThrow(new RollbackException()).given(tx).commit();

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		try {
			Object result = tt.execute(status -> {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				return testList;
			});
			assertThat(result).isSameAs(testList);
		}
		catch (TransactionSystemException tse) {
			// expected
			assertThat(tse.getCause()).isInstanceOf(RollbackException.class);
		}

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	void transactionRollback() {
		given(tx.isActive()).willReturn(true);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		assertThatRuntimeException().isThrownBy(() ->
			tt.execute(status -> {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
				throw new RuntimeException("some exception");
			})).withMessage("some exception");

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(tx).rollback();
		verify(manager).close();
	}

	@Test
	void transactionRollbackWithAlreadyRolledBack() {
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		assertThatRuntimeException().isThrownBy(() ->
			tt.execute(status -> {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
				throw new RuntimeException("some exception");
			}));

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(manager).close();
	}

	@Test
	void transactionRollbackOnly() {
		given(tx.isActive()).willReturn(true);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.execute(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

			EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
			status.setRollbackOnly();

			return testList;
		});

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(manager).flush();
		verify(tx).rollback();
		verify(manager).close();
	}

	@Test
	void participatingTransactionWithCommit() {
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.execute(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

			return tt.execute(status1 -> {
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				return testList;
			});
		});

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(manager).flush();
		verify(tx).commit();
		verify(manager).close();
	}

	@Test
	void participatingTransactionWithRollback() {
		given(tx.isActive()).willReturn(true);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		assertThatRuntimeException().isThrownBy(() ->
			tt.execute(status -> {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				return tt.execute(status1 -> {
					EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
					throw new RuntimeException("some exception");
				});
			}));

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(tx).setRollbackOnly();
		verify(tx).rollback();
		verify(manager).close();
	}

	@Test
	void participatingTransactionWithRollbackOnly() {
		given(tx.isActive()).willReturn(true);
		given(tx.getRollbackOnly()).willReturn(true);
		willThrow(new RollbackException()).given(tx).commit();

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
				tt.execute(status -> {
					assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

					return tt.execute(status1 -> {
						EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
						status1.setRollbackOnly();
						return null;
					});
				}))
			.withCauseInstanceOf(RollbackException.class);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(manager).flush();
		verify(tx).setRollbackOnly();
		verify(manager).close();
	}

	@Test
	void participatingTransactionWithRequiresNew() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		Object result = tt.execute(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			return tt.execute(status1 -> {
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				return testList;
			});
		});
		assertThat(result).isSameAs(testList);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(manager).flush();
		verify(manager, times(2)).close();
		verify(tx, times(2)).begin();
	}

	@Test
	void participatingTransactionWithRequiresNewAndPrebound() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			Object result = tt.execute(status -> {
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory);

				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				return tt.execute(status1 -> {
					EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
					return testList;
				});
			});
			assertThat(result).isSameAs(testList);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(tx, times(2)).begin();
		verify(tx, times(2)).commit();
		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	void propagationSupportsAndRequiresNew() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		Object result = tt.execute(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
			TransactionTemplate tt2 = new TransactionTemplate(tm);
			tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			return tt2.execute(status1 -> {
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				return testList;
			});
		});
		assertThat(result).isSameAs(testList);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(tx).commit();
		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	void propagationSupportsAndRequiresNewAndEarlyAccess() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		Object result = tt.execute(status -> {
			EntityManagerFactoryUtils.getTransactionalEntityManager(factory);

			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			TransactionTemplate tt2 = new TransactionTemplate(tm);
			tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			return tt2.execute(status1 -> {
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				return testList;
			});
		});
		assertThat(result).isSameAs(testList);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(tx).commit();
		verify(manager).flush();
		verify(manager, times(2)).close();
	}

	@Test
	void transactionWithRequiresNewInAfterCompletion() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		EntityManager manager2 = mock();
		EntityTransaction tx2 = mock();

		given(factory.createEntityManager()).willReturn(manager, manager2);
		given(manager2.getTransaction()).willReturn(tx2);
		given(manager2.isOpen()).willReturn(true);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.execute(status -> {
			EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					tt.execute(status1 -> {
						EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
						return null;
					});
				}
			});
			return null;
		});

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(tx).commit();
		verify(tx2).begin();
		verify(tx2).commit();
		verify(manager).flush();
		verify(manager).close();
		verify(manager2).flush();
		verify(manager2).close();
	}

	@Test
	void transactionCommitWithPropagationSupports() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		Object result = tt.execute(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
			assertThat(status.isNewTransaction()).isFalse();
			EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
			return testList;
		});
		assertThat(result).isSameAs(testList);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	void transactionRollbackWithPropagationSupports() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.execute(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
			assertThat(status.isNewTransaction()).isFalse();
			EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
			status.setRollbackOnly();
			return null;
		});

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	void transactionCommitWithPrebound() {
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			Object result = tt.execute(status -> {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
				return testList;
			});
			assertThat(result).isSameAs(testList);

			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		verify(tx).begin();
		verify(tx).commit();
	}

	@Test
	void transactionRollbackWithPrebound() {
		given(tx.isActive()).willReturn(true);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			tt.execute(status -> {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
				status.setRollbackOnly();
				return null;
			});

			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		verify(tx).begin();
		verify(tx).rollback();
		verify(manager).clear();
	}

	@Test
	void transactionCommitWithPreboundAndPropagationSupports() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			Object result = tt.execute(status -> {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(status.isNewTransaction()).isFalse();
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				return testList;
			});
			assertThat(result).isSameAs(testList);

			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		verify(manager).flush();
	}

	@Test
	void transactionRollbackWithPreboundAndPropagationSupports() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			tt.execute(status -> {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				assertThat(status.isNewTransaction()).isFalse();
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				status.setRollbackOnly();
				return null;
			});

			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		verify(manager).flush();
		verify(manager).clear();
	}

	@Test
	void invalidIsolation() {
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

		assertThatExceptionOfType(InvalidIsolationLevelException.class).isThrownBy(() ->
				tt.executeWithoutResult(status -> {}));

		verify(manager).close();
	}

	@Test
	void transactionTimeout() {
		tt.setTimeout(1000);
		tt.executeWithoutResult(status -> {});

		verify(tx).setTimeout(1000);
		verify(manager).close();
	}

	@Test
	void transactionFlush() {
		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		tt.executeWithoutResult(status -> {
			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			status.flush();
		});

		assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

		verify(tx).commit();
		verify(manager).flush();
		verify(manager).close();
	}

}

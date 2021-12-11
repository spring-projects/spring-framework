/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class JpaTransactionManagerTests {

	private EntityManagerFactory factory;

	private EntityManager manager;

	private EntityTransaction tx;

	private JpaTransactionManager tm;

	private TransactionTemplate tt;


	@BeforeEach
	public void setup() {
		factory = mock(EntityManagerFactory.class);
		manager = mock(EntityManager.class);
		tx = mock(EntityTransaction.class);

		tm = new JpaTransactionManager(factory);
		tt = new TransactionTemplate(tm);

		given(factory.createEntityManager()).willReturn(manager);
		given(manager.getTransaction()).willReturn(tx);
		given(manager.isOpen()).willReturn(true);
	}

	@AfterEach
	public void verifyTransactionSynchronizationManagerState() {
		assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}


	@Test
	public void testTransactionCommit() {
		given(manager.getTransaction()).willReturn(tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				return l;
			}
		});
		assertThat(result).isSameAs(l);

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(tx).commit();
		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	public void testTransactionCommitWithRollbackException() {
		given(manager.getTransaction()).willReturn(tx);
		given(tx.getRollbackOnly()).willReturn(true);
		willThrow(new RollbackException()).given(tx).commit();

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		try {
			Object result = tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
					EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
					return l;
				}
			});
			assertThat(result).isSameAs(l);
		}
		catch (TransactionSystemException tse) {
			// expected
			boolean condition = tse.getCause() instanceof RollbackException;
			assertThat(condition).isTrue();
		}

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	public void testTransactionRollback() {
		given(manager.getTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
					EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
					throw new RuntimeException("some exception");
				}
			})).withMessage("some exception");

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(tx).rollback();
		verify(manager).close();
	}

	@Test
	public void testTransactionRollbackWithAlreadyRolledBack() {
		given(manager.getTransaction()).willReturn(tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
					EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
					throw new RuntimeException("some exception");
				}
			}));

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(manager).close();
	}

	@Test
	public void testTransactionRollbackOnly() {
		given(manager.getTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				status.setRollbackOnly();

				return l;
			}
		});

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(manager).flush();
		verify(tx).rollback();
		verify(manager).close();
	}

	@Test
	public void testParticipatingTransactionWithCommit() {
		given(manager.getTransaction()).willReturn(tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

				return tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
						return l;
					}
				});
			}
		});

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(manager).flush();
		verify(tx).commit();
		verify(manager).close();
	}

	@Test
	public void testParticipatingTransactionWithRollback() {
		given(manager.getTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
					return tt.execute(new TransactionCallback() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
							throw new RuntimeException("some exception");
						}
					});
				}
			}));

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(tx).setRollbackOnly();
		verify(tx).rollback();
		verify(manager).close();
	}

	@Test
	public void testParticipatingTransactionWithRollbackOnly() {
		given(manager.getTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);
		given(tx.getRollbackOnly()).willReturn(true);
		willThrow(new RollbackException()).given(tx).commit();

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		assertThatExceptionOfType(TransactionSystemException.class).isThrownBy(() ->
				tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();

						return tt.execute(new TransactionCallback() {
							@Override
							public Object doInTransaction(TransactionStatus status) {
								EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
								status.setRollbackOnly();
								return null;
							}
						});
					}
				}))
			.withCauseInstanceOf(RollbackException.class);

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(manager).flush();
		verify(tx).setRollbackOnly();
		verify(manager).close();
	}

	@Test
	public void testParticipatingTransactionWithRequiresNew() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		given(factory.createEntityManager()).willReturn(manager);
		given(manager.getTransaction()).willReturn(tx);
		given(manager.isOpen()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				return tt.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
						return l;
					}
				});
			}
		});
		assertThat(result).isSameAs(l);

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(manager).flush();
		verify(manager, times(2)).close();
		verify(tx, times(2)).begin();
	}

	@Test
	public void testParticipatingTransactionWithRequiresNewAndPrebound() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		given(manager.getTransaction()).willReturn(tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			Object result = tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					EntityManagerFactoryUtils.getTransactionalEntityManager(factory);

					assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
					return tt.execute(new TransactionCallback() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
							return l;
						}
					});
				}
			});
			assertThat(result).isSameAs(l);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(tx, times(2)).begin();
		verify(tx, times(2)).commit();
		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	public void testPropagationSupportsAndRequiresNew() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		given(manager.getTransaction()).willReturn(tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isFalse();
				TransactionTemplate tt2 = new TransactionTemplate(tm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				return tt2.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
						return l;
					}
				});
			}
		});
		assertThat(result).isSameAs(l);

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(tx).commit();
		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	public void testPropagationSupportsAndRequiresNewAndEarlyAccess() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		given(factory.createEntityManager()).willReturn(manager);
		given(manager.getTransaction()).willReturn(tx);
		given(manager.isOpen()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory);

				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				TransactionTemplate tt2 = new TransactionTemplate(tm);
				tt2.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				return tt2.execute(new TransactionCallback() {
					@Override
					public Object doInTransaction(TransactionStatus status) {
						EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
						return l;
					}
				});
			}
		});
		assertThat(result).isSameAs(l);

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(tx).commit();
		verify(manager).flush();
		verify(manager, times(2)).close();
	}

	@Test
	public void testTransactionWithRequiresNewInAfterCompletion() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		EntityManager manager2 = mock(EntityManager.class);
		EntityTransaction tx2 = mock(EntityTransaction.class);

		given(manager.getTransaction()).willReturn(tx);
		given(factory.createEntityManager()).willReturn(manager, manager2);
		given(manager2.getTransaction()).willReturn(tx2);
		given(manager2.isOpen()).willReturn(true);

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void afterCompletion(int status) {
						tt.execute(new TransactionCallback() {
							@Override
							public Object doInTransaction(TransactionStatus status) {
								EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
								return null;
							}
						});
					}
				});
				return null;
			}
		});

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(tx).commit();
		verify(tx2).begin();
		verify(tx2).commit();
		verify(manager).flush();
		verify(manager).close();
		verify(manager2).flush();
		verify(manager2).close();
	}

	@Test
	public void testTransactionCommitWithPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		final List<String> l = new ArrayList<>();
		l.add("test");

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		Object result = tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
				assertThat(condition1).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				boolean condition = !status.isNewTransaction();
				assertThat(condition).isTrue();
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				return l;
			}
		});
		assertThat(result).isSameAs(l);

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	public void testTransactionRollbackWithPropagationSupports() {
		given(manager.isOpen()).willReturn(true);

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		tt.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
				assertThat(condition1).isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
				boolean condition = !status.isNewTransaction();
				assertThat(condition).isTrue();
				EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
				status.setRollbackOnly();
				return null;
			}
		});

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(manager).flush();
		verify(manager).close();
	}

	@Test
	public void testTransactionCommitWithPrebound() {
		given(manager.getTransaction()).willReturn(tx);

		final List<String> l = new ArrayList<>();
		l.add("test");

		boolean condition2 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition2).isTrue();
		boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition1).isTrue();
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			Object result = tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
					return l;
				}
			});
			assertThat(result).isSameAs(l);

			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
			assertThat(condition).isTrue();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		verify(tx).begin();
		verify(tx).commit();
	}

	@Test
	public void testTransactionRollbackWithPrebound() {
		given(manager.getTransaction()).willReturn(tx);
		given(tx.isActive()).willReturn(true);

		boolean condition2 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition2).isTrue();
		boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition1).isTrue();
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
					status.setRollbackOnly();
					return null;
				}
			});

			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
			assertThat(condition).isTrue();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		verify(tx).begin();
		verify(tx).rollback();
		verify(manager).clear();
	}

	@Test
	public void testTransactionCommitWithPreboundAndPropagationSupports() {
		final List<String> l = new ArrayList<>();
		l.add("test");

		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		boolean condition2 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition2).isTrue();
		boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition1).isTrue();
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			Object result = tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					boolean condition = !status.isNewTransaction();
					assertThat(condition).isTrue();
					EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
					return l;
				}
			});
			assertThat(result).isSameAs(l);

			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
			assertThat(condition).isTrue();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		verify(manager).flush();
	}

	@Test
	public void testTransactionRollbackWithPreboundAndPropagationSupports() {
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		boolean condition2 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition2).isTrue();
		boolean condition1 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition1).isTrue();
		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			tt.execute(new TransactionCallback() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					boolean condition = !status.isNewTransaction();
					assertThat(condition).isTrue();
					EntityManagerFactoryUtils.getTransactionalEntityManager(factory).flush();
					status.setRollbackOnly();
					return null;
				}
			});

			assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
			boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
			assertThat(condition).isTrue();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}

		verify(manager).flush();
		verify(manager).clear();
	}

	@Test
	public void testInvalidIsolation() {
		tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

		given(manager.isOpen()).willReturn(true);

		assertThatExceptionOfType(InvalidIsolationLevelException.class).isThrownBy(() ->
			tt.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
				}
			}));

		verify(manager).close();
	}

	@Test
	public void testTransactionFlush() {
		given(manager.getTransaction()).willReturn(tx);

		boolean condition3 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition3).isTrue();
		boolean condition2 = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition2).isTrue();

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				assertThat(TransactionSynchronizationManager.hasResource(factory)).isTrue();
				status.flush();
			}
		});

		boolean condition1 = !TransactionSynchronizationManager.hasResource(factory);
		assertThat(condition1).isTrue();
		boolean condition = !TransactionSynchronizationManager.isSynchronizationActive();
		assertThat(condition).isTrue();

		verify(tx).commit();
		verify(manager).flush();
		verify(manager).close();
	}

}

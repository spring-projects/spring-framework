/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.r2dbc.connection;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionSynchronization;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.reset;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;
import static org.mockito.BDDMockito.when;

/**
 * Unit tests for {@link R2dbcTransactionManager}.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 */
class R2dbcTransactionManagerUnitTests {

	ConnectionFactory connectionFactoryMock = mock();

	Connection connectionMock = mock();

	private R2dbcTransactionManager tm;


	@BeforeEach
	@SuppressWarnings({"unchecked", "rawtypes"})
	void before() {
		when(connectionFactoryMock.create()).thenReturn((Mono) Mono.just(connectionMock));
		when(connectionMock.beginTransaction(any(io.r2dbc.spi.TransactionDefinition.class))).thenReturn(Mono.empty());
		when(connectionMock.close()).thenReturn(Mono.empty());
		tm = new R2dbcTransactionManager(connectionFactoryMock);
	}


	@Test
	void testSimpleTransaction() {
		TestTransactionSynchronization sync = new TestTransactionSynchronization(
				TransactionSynchronization.STATUS_COMMITTED);
		AtomicInteger commits = new AtomicInteger();
		when(connectionMock.commitTransaction()).thenReturn(
				Mono.fromRunnable(commits::incrementAndGet));

		TransactionalOperator operator = TransactionalOperator.create(tm);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock)
				.flatMap(connection -> TransactionSynchronizationManager.forCurrentTransaction()
				.doOnNext(synchronizationManager -> synchronizationManager.registerSynchronization(sync)))
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		assertThat(commits).hasValue(1);
		verify(connectionMock).beginTransaction(any(io.r2dbc.spi.TransactionDefinition.class));
		verify(connectionMock).commitTransaction();
		verify(connectionMock).close();
		verifyNoMoreInteractions(connectionMock);

		assertThat(sync.beforeCommitCalled).isTrue();
		assertThat(sync.afterCommitCalled).isTrue();
		assertThat(sync.beforeCompletionCalled).isTrue();
		assertThat(sync.afterCompletionCalled).isTrue();
	}

	@Test
	void testBeginFails() {
		reset(connectionFactoryMock);
		when(connectionFactoryMock.create()).thenReturn(
				Mono.error(new R2dbcBadGrammarException("fail")));

		when(connectionMock.rollbackTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock)
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectErrorSatisfies(actual -> assertThat(actual).isInstanceOf(
						CannotCreateTransactionException.class).hasCauseInstanceOf(R2dbcBadGrammarException.class))
				.verify();
	}

	@Test
	void appliesTransactionDefinition() {
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setName("my-transaction");
		definition.setTimeout(10);
		definition.setReadOnly(true);
		definition.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);
		operator.execute(tx -> {
			assertThat(tx.getTransactionName()).isEqualTo("my-transaction");
			assertThat(tx.hasTransaction()).isTrue();
			assertThat(tx.isNewTransaction()).isTrue();
			assertThat(tx.isNested()).isFalse();
			assertThat(tx.isReadOnly()).isTrue();
			assertThat(tx.isRollbackOnly()).isFalse();
			assertThat(tx.isCompleted()).isFalse();
			return Mono.empty();
		}).as(StepVerifier::create).verifyComplete();

		ArgumentCaptor<io.r2dbc.spi.TransactionDefinition> txCaptor = ArgumentCaptor.forClass(io.r2dbc.spi.TransactionDefinition.class);
		verify(connectionMock).beginTransaction(txCaptor.capture());
		verify(connectionMock, never()).setTransactionIsolationLevel(any());
		verify(connectionMock).commitTransaction();
		verify(connectionMock).close();

		io.r2dbc.spi.TransactionDefinition def = txCaptor.getValue();
		assertThat(def.getAttribute(io.r2dbc.spi.TransactionDefinition.NAME)).isEqualTo("my-transaction");
		assertThat(def.getAttribute(io.r2dbc.spi.TransactionDefinition.LOCK_WAIT_TIMEOUT)).isEqualTo(Duration.ofSeconds(10));
		assertThat(def.getAttribute(io.r2dbc.spi.TransactionDefinition.READ_ONLY)).isTrue();
		assertThat(def.getAttribute(io.r2dbc.spi.TransactionDefinition.ISOLATION_LEVEL)).isEqualTo(IsolationLevel.SERIALIZABLE);
	}

	@Test
	void doesNotSetIsolationLevelIfMatch() {
		when(connectionMock.getTransactionIsolationLevel()).thenReturn(
				IsolationLevel.READ_COMMITTED);
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock)
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		verify(connectionMock).beginTransaction(any(io.r2dbc.spi.TransactionDefinition.class));
		verify(connectionMock, never()).setTransactionIsolationLevel(any());
		verify(connectionMock).commitTransaction();
	}

	@Test
	void doesNotSetAutoCommitDisabled() {
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock)
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		verify(connectionMock).beginTransaction(any(io.r2dbc.spi.TransactionDefinition.class));
		verify(connectionMock, never()).setAutoCommit(anyBoolean());
		verify(connectionMock).commitTransaction();
	}

	@Test
	void appliesReadOnly() {
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());
		when(connectionMock.setTransactionIsolationLevel(any())).thenReturn(Mono.empty());
		Statement statement = mock();
		when(connectionMock.createStatement(anyString())).thenReturn(statement);
		when(statement.execute()).thenReturn(Mono.empty());
		tm.setEnforceReadOnly(true);

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setReadOnly(true);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock)
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		verify(connectionMock).beginTransaction(any(io.r2dbc.spi.TransactionDefinition.class));
		verify(connectionMock).createStatement("SET TRANSACTION READ ONLY");
		verify(connectionMock).commitTransaction();
		verify(connectionMock).close();
		verifyNoMoreInteractions(connectionMock);
	}

	@Test
	void testCommitFails() {
		when(connectionMock.commitTransaction()).thenReturn(Mono.defer(() -> Mono.error(new R2dbcBadGrammarException("Commit should fail"))));
		when(connectionMock.rollbackTransaction()).thenReturn(Mono.empty());

		TransactionalOperator operator = TransactionalOperator.create(tm);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock)
				.doOnNext(connection -> connection.createStatement("foo")).then()
				.as(operator::transactional)
				.as(StepVerifier::create)
				.verifyError(BadSqlGrammarException.class);

		verify(connectionMock).beginTransaction(any(io.r2dbc.spi.TransactionDefinition.class));
		verify(connectionMock).createStatement("foo");
		verify(connectionMock).commitTransaction();
		verify(connectionMock).rollbackTransaction();
		verify(connectionMock).close();
		verifyNoMoreInteractions(connectionMock);
	}

	@Test
	void testRollback() {
		AtomicInteger commits = new AtomicInteger();
		when(connectionMock.commitTransaction()).thenReturn(
				Mono.fromRunnable(commits::incrementAndGet));

		AtomicInteger rollbacks = new AtomicInteger();
		when(connectionMock.rollbackTransaction()).thenReturn(
				Mono.fromRunnable(rollbacks::incrementAndGet));

		TransactionalOperator operator = TransactionalOperator.create(tm);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock)
				.doOnNext(connection -> { throw new IllegalStateException(); })
				.as(operator::transactional)
				.as(StepVerifier::create).verifyError(IllegalStateException.class);

		assertThat(commits).hasValue(0);
		assertThat(rollbacks).hasValue(1);
		verify(connectionMock).beginTransaction(any(io.r2dbc.spi.TransactionDefinition.class));
		verify(connectionMock).rollbackTransaction();
		verify(connectionMock).close();
		verifyNoMoreInteractions(connectionMock);
	}

	@Test
	@SuppressWarnings("unchecked")
	void testRollbackFails() {
		when(connectionMock.rollbackTransaction()).thenReturn(Mono.defer(() -> Mono.error(new R2dbcBadGrammarException("Commit should fail"))), Mono.empty());

		TransactionalOperator operator = TransactionalOperator.create(tm);
		operator.execute(reactiveTransaction -> {
			reactiveTransaction.setRollbackOnly();
			return ConnectionFactoryUtils.getConnection(connectionFactoryMock)
					.doOnNext(connection -> connection.createStatement("foo")).then();
		}).as(StepVerifier::create).verifyError(BadSqlGrammarException.class);

		verify(connectionMock).beginTransaction(any(io.r2dbc.spi.TransactionDefinition.class));
		verify(connectionMock).createStatement("foo");
		verify(connectionMock, never()).commitTransaction();
		verify(connectionMock).rollbackTransaction();
		verify(connectionMock).close();
		verifyNoMoreInteractions(connectionMock);
	}

	@Test
	@SuppressWarnings("unchecked")
	void testConnectionReleasedWhenRollbackFails() {
		when(connectionMock.rollbackTransaction()).thenReturn(Mono.defer(() -> Mono.error(new R2dbcBadGrammarException("Rollback should fail"))), Mono.empty());
		when(connectionMock.setTransactionIsolationLevel(any())).thenReturn(Mono.empty());

		TransactionalOperator operator = TransactionalOperator.create(tm);
		operator.execute(reactiveTransaction -> ConnectionFactoryUtils.getConnection(connectionFactoryMock)
						.doOnNext(connection -> {
							throw new IllegalStateException("Intentional error to trigger rollback");
						}).then()).as(StepVerifier::create)
				.verifyErrorSatisfies(ex -> assertThat(ex)
						.isInstanceOf(BadSqlGrammarException.class)
						.hasCause(new R2dbcBadGrammarException("Rollback should fail"))
				);

		verify(connectionMock).beginTransaction(any(io.r2dbc.spi.TransactionDefinition.class));
		verify(connectionMock, never()).commitTransaction();
		verify(connectionMock).rollbackTransaction();
		verify(connectionMock).close();
	}

	@Test
	void testTransactionSetRollbackOnly() {
		when(connectionMock.rollbackTransaction()).thenReturn(Mono.empty());
		TestTransactionSynchronization sync = new TestTransactionSynchronization(
				TransactionSynchronization.STATUS_ROLLED_BACK);

		TransactionalOperator operator = TransactionalOperator.create(tm);
		operator.execute(tx -> {
			assertThat(tx.getTransactionName()).isEmpty();
			assertThat(tx.hasTransaction()).isTrue();
			assertThat(tx.isNewTransaction()).isTrue();
			assertThat(tx.isNested()).isFalse();
			assertThat(tx.isReadOnly()).isFalse();
			assertThat(tx.isRollbackOnly()).isFalse();
			tx.setRollbackOnly();
			assertThat(tx.isRollbackOnly()).isTrue();
			assertThat(tx.isCompleted()).isFalse();
			return TransactionSynchronizationManager.forCurrentTransaction().doOnNext(
					synchronizationManager -> {
						assertThat(synchronizationManager.hasResource(connectionFactoryMock)).isTrue();
						synchronizationManager.registerSynchronization(sync);
					}).then();
		}).as(StepVerifier::create).verifyComplete();

		verify(connectionMock).beginTransaction(any(io.r2dbc.spi.TransactionDefinition.class));
		verify(connectionMock).rollbackTransaction();
		verify(connectionMock).close();
		verifyNoMoreInteractions(connectionMock);

		assertThat(sync.beforeCommitCalled).isFalse();
		assertThat(sync.afterCommitCalled).isFalse();
		assertThat(sync.beforeCompletionCalled).isTrue();
		assertThat(sync.afterCompletionCalled).isTrue();
	}

	@Test
	void testPropagationNeverWithExistingTransaction() {
		when(connectionMock.rollbackTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);
		operator.execute(tx1 -> {
			assertThat(tx1.getTransactionName()).isEmpty();
			assertThat(tx1.hasTransaction()).isTrue();
			assertThat(tx1.isNewTransaction()).isTrue();
			assertThat(tx1.isNested()).isFalse();
			assertThat(tx1.isReadOnly()).isFalse();
			assertThat(tx1.isRollbackOnly()).isFalse();
			assertThat(tx1.isCompleted()).isFalse();
			definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
			return operator.execute(tx2 -> {
				fail("Should have thrown IllegalTransactionStateException");
				return Mono.empty();
			});
		}).as(StepVerifier::create).verifyError(IllegalTransactionStateException.class);

		verify(connectionMock).rollbackTransaction();
		verify(connectionMock).close();
	}

	@Test
	void testPropagationNestedWithExistingTransaction() {
		when(connectionMock.createSavepoint("SAVEPOINT_1")).thenReturn(Mono.empty());
		when(connectionMock.releaseSavepoint("SAVEPOINT_1")).thenReturn(Mono.empty());
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);
		operator.execute(tx1 -> {
			assertThat(tx1.hasTransaction()).isTrue();
			assertThat(tx1.isNewTransaction()).isTrue();
			assertThat(tx1.isNested()).isFalse();
			definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
			return operator.execute(tx2 -> {
				assertThat(tx2.hasTransaction()).isTrue();
				assertThat(tx2.isNewTransaction()).isTrue();
				assertThat(tx2.isNested()).isTrue();
				return Mono.empty();
			});
		}).as(StepVerifier::create).verifyComplete();

		verify(connectionMock).createSavepoint("SAVEPOINT_1");
		verify(connectionMock).releaseSavepoint("SAVEPOINT_1");
		verify(connectionMock).commitTransaction();
		verify(connectionMock).close();
	}

	@Test
	void testPropagationNestedWithExistingTransactionAndRollback() {
		when(connectionMock.createSavepoint("SAVEPOINT_1")).thenReturn(Mono.empty());
		when(connectionMock.rollbackTransactionToSavepoint("SAVEPOINT_1")).thenReturn(Mono.empty());
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);
		operator.execute(tx1 -> {
			assertThat(tx1.hasTransaction()).isTrue();
			assertThat(tx1.isNewTransaction()).isTrue();
			assertThat(tx1.isNested()).isFalse();
			definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
			return operator.execute(tx2 -> {
				assertThat(tx2.hasTransaction()).isTrue();
				assertThat(tx2.isNewTransaction()).isTrue();
				assertThat(tx2.isNested()).isTrue();
				assertThat(tx2.isRollbackOnly()).isFalse();
				tx2.setRollbackOnly();
				assertThat(tx2.isRollbackOnly()).isTrue();
				return Mono.empty();
			});
		}).as(StepVerifier::create).verifyComplete();

		verify(connectionMock).createSavepoint("SAVEPOINT_1");
		verify(connectionMock).rollbackTransactionToSavepoint("SAVEPOINT_1");
		verify(connectionMock).commitTransaction();
		verify(connectionMock).close();
	}

	@Test
	void testPropagationSupportsAndNested() {
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);
		operator.execute(tx1 -> {
			assertThat(tx1.hasTransaction()).isFalse();
			assertThat(tx1.isNewTransaction()).isFalse();
			assertThat(tx1.isNested()).isFalse();
			DefaultTransactionDefinition innerDef = new DefaultTransactionDefinition();
			innerDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
			TransactionalOperator inner = TransactionalOperator.create(tm, innerDef);
			return inner.execute(tx2 -> {
				assertThat(tx2.hasTransaction()).isTrue();
				assertThat(tx2.isNewTransaction()).isTrue();
				assertThat(tx2.isNested()).isFalse();
				return Mono.empty();
			});
		}).as(StepVerifier::create).verifyComplete();

		verify(connectionMock).commitTransaction();
		verify(connectionMock).close();
	}

	@Test
	void testPropagationSupportsAndNestedWithRollback() {
		when(connectionMock.rollbackTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);
		operator.execute(tx1 -> {
			assertThat(tx1.hasTransaction()).isFalse();
			assertThat(tx1.isNewTransaction()).isFalse();
			assertThat(tx1.isNested()).isFalse();
			DefaultTransactionDefinition innerDef = new DefaultTransactionDefinition();
			innerDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
			TransactionalOperator inner = TransactionalOperator.create(tm, innerDef);
			return inner.execute(tx2 -> {
				assertThat(tx2.hasTransaction()).isTrue();
				assertThat(tx2.isNewTransaction()).isTrue();
				assertThat(tx2.isNested()).isFalse();
				assertThat(tx2.isRollbackOnly()).isFalse();
				tx2.setRollbackOnly();
				assertThat(tx2.isRollbackOnly()).isTrue();
				return Mono.empty();
			});
		}).as(StepVerifier::create).verifyComplete();

		verify(connectionMock).rollbackTransaction();
		verify(connectionMock).close();
	}

	@Test
	void testPropagationSupportsAndRequiresNew() {
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);
		operator.execute(tx1 -> {
			assertThat(tx1.hasTransaction()).isFalse();
			assertThat(tx1.isNewTransaction()).isFalse();
			assertThat(tx1.isNested()).isFalse();
			DefaultTransactionDefinition innerDef = new DefaultTransactionDefinition();
			innerDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			TransactionalOperator inner = TransactionalOperator.create(tm, innerDef);
			return inner.execute(tx2 -> {
				assertThat(tx2.hasTransaction()).isTrue();
				assertThat(tx2.isNewTransaction()).isTrue();
				assertThat(tx2.isNested()).isFalse();
				return Mono.empty();
			});
		}).as(StepVerifier::create).verifyComplete();

		verify(connectionMock).commitTransaction();
		verify(connectionMock).close();
	}

	@Test
	void testPropagationSupportsAndRequiresNewWithRollback() {
		when(connectionMock.rollbackTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);
		operator.execute(tx1 -> {
			assertThat(tx1.hasTransaction()).isFalse();
			assertThat(tx1.isNewTransaction()).isFalse();
			assertThat(tx1.isNested()).isFalse();
			DefaultTransactionDefinition innerDef = new DefaultTransactionDefinition();
			innerDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			TransactionalOperator inner = TransactionalOperator.create(tm, innerDef);
			return inner.execute(tx2 -> {
				assertThat(tx2.hasTransaction()).isTrue();
				assertThat(tx2.isNewTransaction()).isTrue();
				assertThat(tx2.isNested()).isFalse();
				assertThat(tx2.isRollbackOnly()).isFalse();
				tx2.setRollbackOnly();
				assertThat(tx2.isRollbackOnly()).isTrue();
				return Mono.empty();
			});
		}).as(StepVerifier::create).verifyComplete();

		verify(connectionMock).rollbackTransaction();
		verify(connectionMock).close();
	}


	private static class TestTransactionSynchronization implements TransactionSynchronization {

		private int status;

		public boolean beforeCommitCalled;

		public boolean beforeCompletionCalled;

		public boolean afterCommitCalled;

		public boolean afterCompletionCalled;

		TestTransactionSynchronization(int status) {
			this.status = status;
		}

		@Override
		public Mono<Void> suspend() {
			return Mono.empty();
		}

		@Override
		public Mono<Void> resume() {
			return Mono.empty();
		}

		@Override
		public Mono<Void> beforeCommit(boolean readOnly) {
			if (this.status != TransactionSynchronization.STATUS_COMMITTED) {
				fail("Should never be called");
			}
			return Mono.fromRunnable(() -> {
				assertThat(this.beforeCommitCalled).isFalse();
				this.beforeCommitCalled = true;
			});
		}

		@Override
		public Mono<Void> beforeCompletion() {
			return Mono.fromRunnable(() -> {
				assertThat(this.beforeCompletionCalled).isFalse();
				this.beforeCompletionCalled = true;
			});
		}

		@Override
		public Mono<Void> afterCommit() {
			if (this.status != TransactionSynchronization.STATUS_COMMITTED) {
				fail("Should never be called");
			}
			return Mono.fromRunnable(() -> {
				assertThat(this.afterCommitCalled).isFalse();
				this.afterCommitCalled = true;
			});
		}

		@Override
		public Mono<Void> afterCompletion(int status) {
			try {
				return Mono.fromRunnable(() -> doAfterCompletion(status));
			}
			catch (Throwable ex) {
				// ignore
			}

			return Mono.empty();
		}

		protected void doAfterCompletion(int status) {
			assertThat(this.afterCompletionCalled).isFalse();
			this.afterCompletionCalled = true;
			assertThat(status).isEqualTo(this.status);
		}
	}

}

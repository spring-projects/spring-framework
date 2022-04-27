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

package org.springframework.r2dbc.connection;

import java.util.concurrent.atomic.AtomicInteger;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.IsolationLevel;
import io.r2dbc.spi.R2dbcBadGrammarException;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
 */
class R2dbcTransactionManagerUnitTests {

	ConnectionFactory connectionFactoryMock = mock(ConnectionFactory.class);

	Connection connectionMock = mock(Connection.class);

	private R2dbcTransactionManager tm;

	@BeforeEach
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void before() {
		when(connectionFactoryMock.create()).thenReturn((Mono) Mono.just(connectionMock));
		when(connectionMock.beginTransaction()).thenReturn(Mono.empty());
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
				.doOnNext(synchronizationManager -> synchronizationManager.registerSynchronization(
						sync)))
				.as(operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		assertThat(commits).hasValue(1);
		verify(connectionMock).isAutoCommit();
		verify(connectionMock).beginTransaction();
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

		ConnectionFactoryUtils.getConnection(connectionFactoryMock).as(
				operator::transactional)
				.as(StepVerifier::create)
				.expectErrorSatisfies(actual -> assertThat(actual).isInstanceOf(
						CannotCreateTransactionException.class).hasCauseInstanceOf(
								R2dbcBadGrammarException.class))
				.verify();
	}

	@Test
	void appliesIsolationLevel() {
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());
		when(connectionMock.getTransactionIsolationLevel()).thenReturn(
				IsolationLevel.READ_COMMITTED);
		when(connectionMock.setTransactionIsolationLevel(any())).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock).as(
				operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		verify(connectionMock).beginTransaction();
		verify(connectionMock).setTransactionIsolationLevel(
				IsolationLevel.READ_COMMITTED);
		verify(connectionMock).setTransactionIsolationLevel(IsolationLevel.SERIALIZABLE);
		verify(connectionMock).commitTransaction();
		verify(connectionMock).close();
	}

	@Test
	void doesNotSetIsolationLevelIfMatch() {
		when(connectionMock.getTransactionIsolationLevel()).thenReturn(
				IsolationLevel.READ_COMMITTED);
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock).as(
				operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		verify(connectionMock).beginTransaction();
		verify(connectionMock, never()).setTransactionIsolationLevel(any());
		verify(connectionMock).commitTransaction();
	}

	@Test
	void doesNotSetAutoCommitDisabled() {
		when(connectionMock.isAutoCommit()).thenReturn(false);
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock).as(
				operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		verify(connectionMock).beginTransaction();
		verify(connectionMock, never()).setAutoCommit(anyBoolean());
		verify(connectionMock).commitTransaction();
	}

	@Test
	void restoresAutoCommit() {
		when(connectionMock.isAutoCommit()).thenReturn(true);
		when(connectionMock.setAutoCommit(anyBoolean())).thenReturn(Mono.empty());
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock).as(
				operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		verify(connectionMock).beginTransaction();
		verify(connectionMock).setAutoCommit(false);
		verify(connectionMock).setAutoCommit(true);
		verify(connectionMock).commitTransaction();
		verify(connectionMock).close();
	}

	@Test
	void appliesReadOnly() {
		when(connectionMock.commitTransaction()).thenReturn(Mono.empty());
		when(connectionMock.setTransactionIsolationLevel(any())).thenReturn(Mono.empty());
		Statement statement = mock(Statement.class);
		when(connectionMock.createStatement(anyString())).thenReturn(statement);
		when(statement.execute()).thenReturn(Mono.empty());
		tm.setEnforceReadOnly(true);

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setReadOnly(true);

		TransactionalOperator operator = TransactionalOperator.create(tm, definition);

		ConnectionFactoryUtils.getConnection(connectionFactoryMock).as(
				operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		verify(connectionMock).isAutoCommit();
		verify(connectionMock).beginTransaction();
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
				.verifyError(IllegalTransactionStateException.class);

		verify(connectionMock).isAutoCommit();
		verify(connectionMock).beginTransaction();
		verify(connectionMock).createStatement("foo");
		verify(connectionMock).commitTransaction();
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
				.doOnNext(connection -> {
			throw new IllegalStateException();
		}).as(operator::transactional)
				.as(StepVerifier::create)
				.verifyError(IllegalStateException.class);

		assertThat(commits).hasValue(0);
		assertThat(rollbacks).hasValue(1);
		verify(connectionMock).isAutoCommit();
		verify(connectionMock).beginTransaction();
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
		}).as(StepVerifier::create)
				.verifyError(IllegalTransactionStateException.class);

		verify(connectionMock).isAutoCommit();
		verify(connectionMock).beginTransaction();
		verify(connectionMock).createStatement("foo");
		verify(connectionMock, never()).commitTransaction();
		verify(connectionMock).rollbackTransaction();
		verify(connectionMock).close();
		verifyNoMoreInteractions(connectionMock);
	}

	@Test
	void testTransactionSetRollbackOnly() {
		when(connectionMock.rollbackTransaction()).thenReturn(Mono.empty());
		TestTransactionSynchronization sync = new TestTransactionSynchronization(
				TransactionSynchronization.STATUS_ROLLED_BACK);

		TransactionalOperator operator = TransactionalOperator.create(tm);

		operator.execute(tx -> {

			tx.setRollbackOnly();
			assertThat(tx.isNewTransaction()).isTrue();

			return TransactionSynchronizationManager.forCurrentTransaction().doOnNext(
					synchronizationManager -> {
						assertThat(synchronizationManager.hasResource(connectionFactoryMock)).isTrue();
						synchronizationManager.registerSynchronization(sync);
					}).then();
		}).as(StepVerifier::create)
				.verifyComplete();

		verify(connectionMock).isAutoCommit();
		verify(connectionMock).beginTransaction();
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

			assertThat(tx1.isNewTransaction()).isTrue();

			definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
			return operator.execute(tx2 -> {

				fail("Should have thrown IllegalTransactionStateException");
				return Mono.empty();
			});
		}).as(StepVerifier::create)
				.verifyError(IllegalTransactionStateException.class);

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

			assertThat(tx1.isNewTransaction()).isFalse();

			DefaultTransactionDefinition innerDef = new DefaultTransactionDefinition();
			innerDef.setPropagationBehavior(
					TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			TransactionalOperator inner = TransactionalOperator.create(tm, innerDef);

			return inner.execute(tx2 -> {

				assertThat(tx2.isNewTransaction()).isTrue();
				return Mono.empty();
			});
		}).as(StepVerifier::create)
				.verifyComplete();

		verify(connectionMock).commitTransaction();
		verify(connectionMock).close();
	}


	private static class TestTransactionSynchronization
			implements TransactionSynchronization {

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

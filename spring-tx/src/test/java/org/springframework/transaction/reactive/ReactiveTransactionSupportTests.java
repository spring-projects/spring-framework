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

package org.springframework.transaction.reactive;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.testfixture.TestTransactionExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for transactional support through {@link ReactiveTestTransactionManager}.
 *
 * @author Mark Paluch
 */
class ReactiveTransactionSupportTests {

	@Test
	void noExistingTransaction() {
		ReactiveTransactionManager tm = new ReactiveTestTransactionManager(false, true);

		tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS))
				.contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
				.as(StepVerifier::create).consumeNextWith(actual -> assertThat(actual.hasTransaction()).isFalse()
		).verifyComplete();

		tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED))
				.cast(GenericReactiveTransaction.class).contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create).consumeNextWith(actual -> {
			assertThat(actual.hasTransaction()).isTrue();
			assertThat(actual.isNewTransaction()).isTrue();
		}).verifyComplete();

		tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY))
				.contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
				.as(StepVerifier::create).expectError(IllegalTransactionStateException.class).verify();
	}

	@Test
	void existingTransaction() {
		ReactiveTransactionManager tm = new ReactiveTestTransactionManager(true, true);

		tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS))
				.contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
				.as(StepVerifier::create).consumeNextWith(actual -> {
			assertThat(actual.getTransaction()).isNotNull();
			assertThat(actual.isNewTransaction()).isFalse();
		}).verifyComplete();

		tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED))
				.contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
				.as(StepVerifier::create).consumeNextWith(actual -> {
			assertThat(actual.getTransaction()).isNotNull();
			assertThat(actual.isNewTransaction()).isFalse();
		}).verifyComplete();

		tm.getReactiveTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY))
				.contextWrite(TransactionContextManager.createTransactionContext()).cast(GenericReactiveTransaction.class)
				.as(StepVerifier::create).consumeNextWith(actual -> {
			assertThat(actual.getTransaction()).isNotNull();
			assertThat(actual.isNewTransaction()).isFalse();
		}).verifyComplete();
	}

	@Test
	void commitWithoutExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
				.contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create).verifyComplete();

		assertHasBegan(tm);
		assertHasCommitted(tm);
		assertHasNoRollback(tm);
		assertHasNotSetRollbackOnly(tm);
		assertHasCleanedUp(tm);

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beforeCommitCalled).isTrue();
		assertThat(tl.afterCommitCalled).isTrue();
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isFalse();
	}

	@Test
	void rollbackWithoutExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::rollback)
				.contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
				.verifyComplete();

		assertHasBegan(tm);
		assertHasNotCommitted(tm);
		assertHasRolledBack(tm);
		assertHasNotSetRollbackOnly(tm);
		assertHasCleanedUp(tm);

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.beforeRollbackCalled).isTrue();
		assertThat(tl.afterRollbackCalled).isTrue();
	}

	@Test
	void rollbackOnlyWithoutExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).doOnNext(ReactiveTransaction::setRollbackOnly)
				.flatMap(tm::commit)
				.contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
				.verifyComplete();

		assertHasBegan(tm);
		assertHasNotCommitted(tm);
		assertHasRolledBack(tm);
		assertHasNotSetRollbackOnly(tm);
		assertHasCleanedUp(tm);

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.beforeRollbackCalled).isTrue();
		assertThat(tl.afterRollbackCalled).isTrue();
	}

	@Test
	void commitWithExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
				.contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create).verifyComplete();

		assertHasNotBegan(tm);
		assertHasNotCommitted(tm);
		assertHasNoRollback(tm);
		assertHasNotSetRollbackOnly(tm);
		assertHasNotCleanedUp(tm);

		assertThat(tl.beforeBeginCalled).isFalse();
		assertThat(tl.afterBeginCalled).isFalse();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isFalse();
	}

	@Test
	void rollbackWithExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::rollback)
				.contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
				.verifyComplete();

		assertHasNotBegan(tm);
		assertHasNotCommitted(tm);
		assertHasNoRollback(tm);
		assertHasSetRollbackOnly(tm);
		assertHasNotCleanedUp(tm);

		assertThat(tl.beforeBeginCalled).isFalse();
		assertThat(tl.afterBeginCalled).isFalse();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isFalse();
	}

	@Test
	void rollbackOnlyWithExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).doOnNext(ReactiveTransaction::setRollbackOnly).flatMap(tm::commit)
				.contextWrite(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
				.verifyComplete();

		assertHasNotBegan(tm);
		assertHasNotCommitted(tm);
		assertHasNoRollback(tm);
		assertHasSetRollbackOnly(tm);
		assertHasNotCleanedUp(tm);

		assertThat(tl.beforeBeginCalled).isFalse();
		assertThat(tl.afterBeginCalled).isFalse();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isFalse();
	}

	@Test
	void transactionTemplate() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());

		Flux.just("Walter").as(operator::transactional)
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		assertHasBegan(tm);
		assertHasCommitted(tm);
		assertHasNoRollback(tm);
		assertHasNotSetRollbackOnly(tm);
		assertHasCleanedUp(tm);
	}

	@Test
	void transactionTemplateWithException() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
		TransactionalOperator operator = TransactionalOperator.create(tm, new DefaultTransactionDefinition());
		RuntimeException ex = new RuntimeException("Some application exception");

		Mono.error(ex).as(operator::transactional)
				.as(StepVerifier::create)
				.expectError(RuntimeException.class)
				.verify();

		assertHasBegan(tm);
		assertHasNotCommitted(tm);
		assertHasRolledBack(tm);
		assertHasNotSetRollbackOnly(tm);
		assertHasCleanedUp(tm);
	}

	@Test  // gh-28968
	void errorInCommitDoesInitiateRollbackAfterCommit() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, ConcurrencyFailureException::new, null);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);
		TransactionalOperator rxtx = TransactionalOperator.create(tm);

		StepVerifier.create(rxtx.transactional(Mono.just("bar")))
				.verifyErrorMessage("Forced failure on commit");

		assertHasBegan(tm);
		assertHasCommitted(tm);
		assertHasRolledBack(tm);
		assertHasNotSetRollbackOnly(tm);
		assertHasCleanedUp(tm);

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beforeCommitCalled).isTrue();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isTrue();
	}

	@Test
	void beginFailure() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, false);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
				.contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create).verifyError();

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
	void commitFailure() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, TransactionSystemException::new, null);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
				.contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create).verifyError();

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beginFailure).isNull();
		assertThat(tl.beforeCommitCalled).isTrue();
		assertThat(tl.afterCommitCalled).isTrue();
		assertThat(tl.commitFailure).isInstanceOf(TransactionSystemException.class);
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isFalse();
		assertThat(tl.rollbackFailure).isNull();
	}

	@Test
	void rollbackFailure() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, null, TransactionSystemException::new);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::rollback)
				.contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create).verifyError();

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beginFailure).isNull();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.commitFailure).isNull();
		assertThat(tl.beforeRollbackCalled).isTrue();
		assertThat(tl.afterRollbackCalled).isTrue();
		assertThat(tl.rollbackFailure).isInstanceOf(TransactionSystemException.class);
	}


	private void assertHasBegan(ReactiveTestTransactionManager actual) {
		assertThat(actual.begin).as("Expected <ReactiveTransactionManager.begin()> but was <begin()> was not invoked").isTrue();
	}

	private void assertHasNotBegan(ReactiveTestTransactionManager actual) {
		assertThat(actual.begin).as("Expected to not call <ReactiveTransactionManager.begin()> but was <begin()> was called").isFalse();
	}

	private void assertHasCommitted(ReactiveTestTransactionManager actual) {
		assertThat(actual.commit).as("Expected <ReactiveTransactionManager.commit()> but was <commit()> was not invoked").isTrue();
	}

	private void assertHasNotCommitted(ReactiveTestTransactionManager actual) {
		assertThat(actual.commit).as("Expected to not call <ReactiveTransactionManager.commit()> but was <commit()> was called").isFalse();
	}

	private void assertHasRolledBack(ReactiveTestTransactionManager actual) {
		assertThat(actual.rollback).as("Expected <ReactiveTransactionManager.rollback()> but was <rollback()> was not invoked").isTrue();
	}

	private void assertHasNoRollback(ReactiveTestTransactionManager actual) {
		assertThat(actual.rollback).as("Expected to not call <ReactiveTransactionManager.rollback()> but was <rollback()> was called").isFalse();
	}

	private void assertHasSetRollbackOnly(ReactiveTestTransactionManager actual) {
		assertThat(actual.rollbackOnly).as("Expected <ReactiveTransactionManager.setRollbackOnly()> but was <setRollbackOnly()> was not invoked").isTrue();
	}

	private void assertHasNotSetRollbackOnly(ReactiveTestTransactionManager actual) {
		assertThat(actual.rollbackOnly).as("Expected to not call <ReactiveTransactionManager.setRollbackOnly()> but was <setRollbackOnly()> was called").isFalse();
	}

	private void assertHasCleanedUp(ReactiveTestTransactionManager actual) {
		assertThat(actual.cleanup).as("Expected <ReactiveTransactionManager.doCleanupAfterCompletion()> but was <doCleanupAfterCompletion()> was not invoked").isTrue();
	}

	private void assertHasNotCleanedUp(ReactiveTestTransactionManager actual) {
		assertThat(actual.cleanup).as("Expected to not call <ReactiveTransactionManager.doCleanupAfterCompletion()> but was <doCleanupAfterCompletion()> was called").isFalse();
	}

}

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

package org.springframework.transaction.reactive;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.ReactiveTransactionStatus;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.junit.Assert.*;

/**
 * Unit tests for transactional support through {@link ReactiveTestTransactionManager}.
 *
 * @author Mark Paluch
 */
public class ReactiveTransactionSupportUnitTests {

	@Test
	public void noExistingTransaction() {
		ReactiveTransactionManager tm = new ReactiveTestTransactionManager(false, true);

		tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS))
				.subscriberContext(TransactionContextManager.createTransactionContext()).cast(DefaultReactiveTransactionStatus.class)
				.as(StepVerifier::create).consumeNextWith(actual -> {
					assertFalse(actual.hasTransaction());
				}).verifyComplete();

		tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED))
				.cast(DefaultReactiveTransactionStatus.class).subscriberContext(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create).consumeNextWith(actual -> {
					assertTrue(actual.hasTransaction());
					assertTrue(actual.isNewTransaction());
				}).verifyComplete();

		tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY))
				.subscriberContext(TransactionContextManager.createTransactionContext()).cast(DefaultReactiveTransactionStatus.class)
				.as(StepVerifier::create).expectError(IllegalTransactionStateException.class).verify();
	}

	@Test
	public void existingTransaction() {
		ReactiveTransactionManager tm = new ReactiveTestTransactionManager(true, true);

		tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS))
				.subscriberContext(TransactionContextManager.createTransactionContext()).cast(DefaultReactiveTransactionStatus.class)
				.as(StepVerifier::create).consumeNextWith(actual -> {
					assertNotNull(actual.getTransaction());
					assertFalse(actual.isNewTransaction());
				}).verifyComplete();

		tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED))
				.subscriberContext(TransactionContextManager.createTransactionContext()).cast(DefaultReactiveTransactionStatus.class)
				.as(StepVerifier::create).consumeNextWith(actual -> {
					assertNotNull(actual.getTransaction());
					assertFalse(actual.isNewTransaction());
				}).verifyComplete();

		tm.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY))
				.subscriberContext(TransactionContextManager.createTransactionContext()).cast(DefaultReactiveTransactionStatus.class)
				.as(StepVerifier::create).consumeNextWith(actual -> {
					assertNotNull(actual.getTransaction());
					assertFalse(actual.isNewTransaction());
				}).verifyComplete();
	}

	@Test
	public void commitWithoutExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
		tm.getTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
				.subscriberContext(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create).verifyComplete();

		assertHasBegan(tm);
		assertHasCommitted(tm);
		assertHasNoRollback(tm);
		assertHasNotSetRollbackOnly(tm);
	}

	@Test
	public void rollbackWithoutExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
		tm.getTransaction(new DefaultTransactionDefinition()).flatMap(tm::rollback)
				.subscriberContext(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
				.verifyComplete();

		assertHasBegan(tm);
		assertHasNotCommitted(tm);
		assertHasRolledBack(tm);
		assertHasNotSetRollbackOnly(tm);
	}

	@Test
	public void rollbackOnlyWithoutExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
		tm.getTransaction(new DefaultTransactionDefinition()).doOnNext(ReactiveTransactionStatus::setRollbackOnly)
				.flatMap(tm::commit)
				.subscriberContext(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
				.verifyComplete();

		assertHasBegan(tm);
		assertHasNotCommitted(tm);
		assertHasRolledBack(tm);
		assertHasNotSetRollbackOnly(tm);
	}

	@Test
	public void commitWithExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
		tm.getTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
				.subscriberContext(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create).verifyComplete();

		assertHasNotBegan(tm);
		assertHasNotCommitted(tm);
		assertHasNoRollback(tm);
		assertHasNotSetRollbackOnly(tm);
	}

	@Test
	public void rollbackWithExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
		tm.getTransaction(new DefaultTransactionDefinition()).flatMap(tm::rollback)
				.subscriberContext(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
				.verifyComplete();

		assertHasNotBegan(tm);
		assertHasNotCommitted(tm);
		assertHasNoRollback(tm);
		assertHasSetRollbackOnly(tm);
	}

	@Test
	public void rollbackOnlyWithExistingTransaction() {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(true, true);
		tm.getTransaction(new DefaultTransactionDefinition()).doOnNext(ReactiveTransactionStatus::setRollbackOnly).flatMap(tm::commit)
				.subscriberContext(TransactionContextManager.createTransactionContext()).as(StepVerifier::create)
				.verifyComplete();

		assertHasNotBegan(tm);
		assertHasNotCommitted(tm);
		assertHasNoRollback(tm);
		assertHasSetRollbackOnly(tm);
	}

	@Test
	public void transactionTemplate() {
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
	}

	@Test
	public void transactionTemplateWithException() {
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
	}

	private void assertHasBegan(ReactiveTestTransactionManager actual) {
		assertTrue("Expected <ReactiveTransactionManager.begin()> but was <begin()> was not invoked", actual.begin);
	}

	private void assertHasNotBegan(ReactiveTestTransactionManager actual) {
		assertFalse("Expected to not call <ReactiveTransactionManager.begin()> but was <begin()> was called", actual.begin);
	}

	private void assertHasCommitted(ReactiveTestTransactionManager actual) {
		assertTrue("Expected <ReactiveTransactionManager.commit()> but was <commit()> was not invoked", actual.commit);
	}

	private void assertHasNotCommitted(ReactiveTestTransactionManager actual) {
		assertFalse("Expected to not call <ReactiveTransactionManager.commit()> but was <commit()> was called", actual.commit);
	}

	private void assertHasRolledBack(ReactiveTestTransactionManager actual) {
		assertTrue("Expected <ReactiveTransactionManager.rollback()> but was <rollback()> was not invoked", actual.rollback);
	}

	private void assertHasNoRollback(ReactiveTestTransactionManager actual) {
assertFalse("Expected to not call <ReactiveTransactionManager.rollback()> but was <rollback()> was called", actual.rollback);
	}

	private void assertHasSetRollbackOnly(ReactiveTestTransactionManager actual) {
		assertTrue("Expected <ReactiveTransactionManager.setRollbackOnly()> but was <setRollbackOnly()> was not invoked", actual.rollbackOnly);
	}

	private void assertHasNotSetRollbackOnly(ReactiveTestTransactionManager actual) {
		assertFalse("Expected to not call <ReactiveTransactionManager.setRollbackOnly()> but was <setRollbackOnly()> was called", actual.rollbackOnly);
	}

}

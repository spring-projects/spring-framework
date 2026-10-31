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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveTransactionExecutionListenerTests {

	@ParameterizedTest
	@MethodSource("listenerFailures")
	void listenerFailureDoesNotAffectTransaction(Callback callback, Throwable listenerFailure) {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
		List<Invocation> invocations = addListeners(tm, callback, listenerFailure);
		boolean rollback = (callback == Callback.BEFORE_ROLLBACK || callback == Callback.AFTER_ROLLBACK);

		tm.getReactiveTransaction(new DefaultTransactionDefinition())
				.flatMap(transaction -> rollback ? tm.rollback(transaction) : tm.commit(transaction))
				.contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create)
				.verifyComplete();

		assertThat(tm.begin).isTrue();
		assertThat(tm.commit).isEqualTo(!rollback);
		assertThat(tm.rollback).isEqualTo(rollback);
		assertThat(tm.cleanup).isTrue();
		assertThat(invocations.getFirst().transaction().isCompleted()).isTrue();
		assertCallbackOrder(invocations, Callback.BEFORE_BEGIN, Callback.AFTER_BEGIN,
				rollback ? Callback.BEFORE_ROLLBACK : Callback.BEFORE_COMMIT,
				rollback ? Callback.AFTER_ROLLBACK : Callback.AFTER_COMMIT);
		assertThat(invocations).extracting(Invocation::failure).containsOnlyNulls();
	}

	@ParameterizedTest
	@MethodSource("callbackExceptions")
	void beginFailureIsNotReplacedByListenerFailure(Throwable listenerFailure) {
		CannotCreateTransactionException beginFailure = new CannotCreateTransactionException("begin failed");
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true) {
			@Override
			protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager,
					Object transaction, TransactionDefinition definition) {

				return Mono.error(beginFailure);
			}
		};
		List<Invocation> invocations = addListeners(tm, Callback.AFTER_BEGIN, listenerFailure);

		tm.getReactiveTransaction(new DefaultTransactionDefinition())
				.contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create)
				.verifyErrorSatisfies(ex -> assertThat(ex).isSameAs(beginFailure));

		assertThat(tm.begin).isFalse();
		assertThat(tm.commit).isFalse();
		assertThat(tm.rollback).isFalse();
		assertThat(tm.cleanup).isFalse();
		assertCallbackOrder(invocations, Callback.BEFORE_BEGIN, Callback.AFTER_BEGIN);
		assertCallbackFailure(invocations, Callback.AFTER_BEGIN, beginFailure);
	}

	@ParameterizedTest
	@MethodSource("callbackExceptions")
	void commitFailureIsNotReplacedByListenerFailure(Throwable listenerFailure) {
		TransactionSystemException commitFailure = new TransactionSystemException("commit failed");
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, message -> commitFailure, null);
		List<Invocation> invocations = addListeners(tm, Callback.AFTER_COMMIT, listenerFailure);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
				.contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create)
				.verifyErrorSatisfies(ex -> assertThat(ex).isSameAs(commitFailure));

		assertThat(tm.commit).isTrue();
		assertThat(tm.rollback).isFalse();
		assertThat(tm.cleanup).isTrue();
		assertThat(invocations.getFirst().transaction().isCompleted()).isTrue();
		assertCallbackOrder(invocations, Callback.BEFORE_BEGIN, Callback.AFTER_BEGIN,
				Callback.BEFORE_COMMIT, Callback.AFTER_COMMIT);
		assertCallbackFailure(invocations, Callback.AFTER_COMMIT, commitFailure);
	}

	@ParameterizedTest
	@MethodSource("callbackExceptions")
	void rollbackFailureIsNotReplacedByListenerFailure(Throwable listenerFailure) {
		TransactionSystemException rollbackFailure = new TransactionSystemException("rollback failed");
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, null, message -> rollbackFailure);
		List<Invocation> invocations = addListeners(tm, Callback.AFTER_ROLLBACK, listenerFailure);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::rollback)
				.contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create)
				.verifyErrorSatisfies(ex -> assertThat(ex).isSameAs(rollbackFailure));

		assertThat(tm.commit).isFalse();
		assertThat(tm.rollback).isTrue();
		assertThat(tm.cleanup).isTrue();
		assertThat(invocations.getFirst().transaction().isCompleted()).isTrue();
		assertCallbackOrder(invocations, Callback.BEFORE_BEGIN, Callback.AFTER_BEGIN,
				Callback.BEFORE_ROLLBACK, Callback.AFTER_ROLLBACK);
		assertCallbackFailure(invocations, Callback.AFTER_ROLLBACK, rollbackFailure);
	}

	@ParameterizedTest
	@MethodSource("callbackExceptions")
	void commitExceptionSurvivesRollbackListenerFailure(Throwable listenerFailure) {
		IllegalStateException commitFailure = new IllegalStateException("commit failed");
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, message -> commitFailure, null);
		List<Invocation> invocations = addListeners(tm, Callback.AFTER_ROLLBACK, listenerFailure);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
				.contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create)
				.verifyErrorSatisfies(ex -> assertThat(ex).isSameAs(commitFailure));

		assertThat(tm.commit).isTrue();
		assertThat(tm.rollback).isTrue();
		assertThat(tm.cleanup).isTrue();
		assertCallbackOrder(invocations, Callback.BEFORE_BEGIN, Callback.AFTER_BEGIN,
				Callback.BEFORE_COMMIT, Callback.AFTER_ROLLBACK);
		assertCallbackFailure(invocations, Callback.AFTER_ROLLBACK, null);
	}

	@ParameterizedTest
	@MethodSource("callbackExceptions")
	void rollbackAfterCommitFailureIsNotReplacedByListenerFailure(Throwable listenerFailure) {
		IllegalStateException commitFailure = new IllegalStateException("commit failed");
		TransactionSystemException rollbackFailure = new TransactionSystemException("rollback failed");
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(
				false, message -> commitFailure, message -> rollbackFailure);
		List<Invocation> invocations = addListeners(tm, Callback.AFTER_ROLLBACK, listenerFailure);

		tm.getReactiveTransaction(new DefaultTransactionDefinition()).flatMap(tm::commit)
				.contextWrite(TransactionContextManager.createTransactionContext())
				.as(StepVerifier::create)
				.verifyErrorSatisfies(ex -> assertThat(ex).isSameAs(rollbackFailure));

		assertThat(tm.commit).isTrue();
		assertThat(tm.rollback).isTrue();
		assertThat(tm.cleanup).isTrue();
		assertCallbackOrder(invocations, Callback.BEFORE_BEGIN, Callback.AFTER_BEGIN,
				Callback.BEFORE_COMMIT, Callback.AFTER_ROLLBACK);
		assertCallbackFailure(invocations, Callback.AFTER_ROLLBACK, rollbackFailure);
	}

	@ParameterizedTest
	@MethodSource("beginListenerFailures")
	void requiresNewRestoresOuterTransactionAfterListenerFailure(Callback callback, Throwable listenerFailure) {
		SuspendingTransactionManager tm = new SuspendingTransactionManager(null);
		List<Invocation> invocations = addListeners(tm, callback, listenerFailure);
		DefaultTransactionDefinition definition =
				new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		withOuterTransaction(tm, tm.getReactiveTransaction(definition).flatMap(tm::commit))
				.as(StepVerifier::create)
				.verifyComplete();

		assertThat(tm.begin).isTrue();
		assertThat(tm.commit).isTrue();
		assertThat(tm.rollback).isFalse();
		assertThat(tm.cleanup).isTrue();
		assertThat(tm.suspends).isEqualTo(1);
		assertThat(tm.resumes).isEqualTo(1);
		assertCallbackOrder(invocations, Callback.BEFORE_BEGIN, Callback.AFTER_BEGIN,
				Callback.BEFORE_COMMIT, Callback.AFTER_COMMIT);
		assertThat(invocations).extracting(Invocation::failure).containsOnlyNulls();
	}

	@ParameterizedTest
	@MethodSource("callbackExceptions")
	void requiresNewRestoresOuterTransactionAfterBeginAndListenerFailure(Throwable listenerFailure) {
		CannotCreateTransactionException beginFailure = new CannotCreateTransactionException("begin failed");
		SuspendingTransactionManager tm = new SuspendingTransactionManager(beginFailure);
		List<Invocation> invocations = addListeners(tm, Callback.AFTER_BEGIN, listenerFailure);
		DefaultTransactionDefinition definition =
				new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		withOuterTransaction(tm, tm.getReactiveTransaction(definition).then())
				.as(StepVerifier::create)
				.verifyErrorSatisfies(ex -> assertThat(ex).isSameAs(beginFailure));

		assertThat(tm.begin).isFalse();
		assertThat(tm.commit).isFalse();
		assertThat(tm.rollback).isFalse();
		assertThat(tm.cleanup).isFalse();
		assertThat(tm.suspends).isEqualTo(1);
		assertThat(tm.resumes).isEqualTo(1);
		assertCallbackOrder(invocations, Callback.BEFORE_BEGIN, Callback.AFTER_BEGIN);
		assertCallbackFailure(invocations, Callback.AFTER_BEGIN, beginFailure);
	}

	@ParameterizedTest
	@MethodSource("rollbackListenerFailures")
	void cancellationRollsBackDespiteListenerFailure(Callback callback, Throwable listenerFailure) {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
		List<Invocation> invocations = addListeners(tm, callback, listenerFailure);
		TransactionalOperator operator = TransactionalOperator.create(tm);
		AtomicBoolean subscribed = new AtomicBoolean();
		AtomicBoolean cancelled = new AtomicBoolean();

		Mono.<Void>never()
				.doOnSubscribe(subscription -> subscribed.set(true))
				.doOnCancel(() -> cancelled.set(true))
				.as(operator::transactional)
				.as(StepVerifier::create)
				.then(() -> assertThat(subscribed).isTrue())
				.thenCancel()
				.verify();

		assertThat(cancelled).isTrue();
		assertThat(tm.commit).isFalse();
		assertThat(tm.rollback).isTrue();
		assertThat(tm.cleanup).isTrue();
		assertThat(invocations.getFirst().transaction().isCompleted()).isTrue();
		assertCallbackOrder(invocations, Callback.BEFORE_BEGIN, Callback.AFTER_BEGIN,
				Callback.BEFORE_ROLLBACK, Callback.AFTER_ROLLBACK);
		assertThat(invocations).extracting(Invocation::failure).containsOnlyNulls();
	}

	@ParameterizedTest
	@MethodSource("rollbackListenerFailures")
	void applicationFailureIsNotReplacedByRollbackListenerFailure(Callback callback, Throwable listenerFailure) {
		ReactiveTestTransactionManager tm = new ReactiveTestTransactionManager(false, true);
		List<Invocation> invocations = addListeners(tm, callback, listenerFailure);
		TransactionalOperator operator = TransactionalOperator.create(tm);
		IllegalArgumentException applicationFailure = new IllegalArgumentException("application failed");

		Mono.error(applicationFailure).as(operator::transactional)
				.as(StepVerifier::create)
				.verifyErrorSatisfies(ex -> assertThat(ex).isSameAs(applicationFailure));

		assertThat(tm.commit).isFalse();
		assertThat(tm.rollback).isTrue();
		assertThat(tm.cleanup).isTrue();
		assertCallbackOrder(invocations, Callback.BEFORE_BEGIN, Callback.AFTER_BEGIN,
				Callback.BEFORE_ROLLBACK, Callback.AFTER_ROLLBACK);
		assertThat(invocations).extracting(Invocation::failure).containsOnlyNulls();
	}


	private static Stream<Arguments> listenerFailures() {
		return Arrays.stream(Callback.values())
				.flatMap(callback -> callbackExceptions().map(failure -> Arguments.of(callback, failure)));
	}

	private static Stream<Arguments> beginListenerFailures() {
		return Stream.of(Callback.BEFORE_BEGIN, Callback.AFTER_BEGIN)
				.flatMap(callback -> callbackExceptions().map(failure -> Arguments.of(callback, failure)));
	}

	private static Stream<Arguments> rollbackListenerFailures() {
		return Stream.of(Callback.BEFORE_ROLLBACK, Callback.AFTER_ROLLBACK)
				.flatMap(callback -> callbackExceptions().map(failure -> Arguments.of(callback, failure)));
	}

	private static Stream<Throwable> callbackExceptions() {
		return Stream.of(new IllegalStateException("listener failed"), new AssertionError("listener failed"));
	}

	private List<Invocation> addListeners(
			ReactiveTestTransactionManager tm, Callback callback, Throwable listenerFailure) {

		List<Invocation> invocations = new ArrayList<>();
		tm.addListener(new RecordingListener("throwing", invocations, callback, listenerFailure));
		tm.addListener(new RecordingListener("following", invocations, null, listenerFailure));
		return invocations;
	}

	private void assertCallbackOrder(List<Invocation> invocations, Callback... callbacks) {
		List<String> expected = Arrays.stream(callbacks)
				.flatMap(callback -> Stream.of("throwing:" + callback, "following:" + callback))
				.toList();
		assertThat(invocations).extracting(invocation -> invocation.listener() + ":" + invocation.callback())
				.containsExactlyElementsOf(expected);
		TransactionExecution transaction = invocations.getFirst().transaction();
		assertThat(invocations).allSatisfy(invocation ->
				assertThat(invocation.transaction()).isSameAs(transaction));
	}

	private void assertCallbackFailure(
			List<Invocation> invocations, Callback callback, @Nullable Throwable failure) {

		assertThat(invocations).filteredOn(invocation -> invocation.callback() == callback)
				.hasSize(2)
				.allSatisfy(invocation -> assertThat(invocation.failure()).isSameAs(failure));
		assertThat(invocations).filteredOn(invocation -> invocation.callback() != callback)
				.allSatisfy(invocation -> assertThat(invocation.failure()).isNull());
	}

	private Mono<Void> withOuterTransaction(SuspendingTransactionManager tm, Mono<Void> operation) {
		return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
			synchronizationManager.bindResource(tm.resourceKey, tm.outerResource);
			synchronizationManager.initSynchronization();
			synchronizationManager.setCurrentTransactionName("outer");
			synchronizationManager.setActualTransactionActive(true);
			return operation
					.doOnSuccess(ignore -> assertOuterTransactionRestored(tm, synchronizationManager))
					.doOnError(ex -> assertOuterTransactionRestored(tm, synchronizationManager));
		}).contextWrite(TransactionContextManager.createTransactionContext());
	}

	private void assertOuterTransactionRestored(
			SuspendingTransactionManager tm, TransactionSynchronizationManager synchronizationManager) {

		assertThat(synchronizationManager.getResource(tm.resourceKey)).isSameAs(tm.outerResource);
		assertThat(synchronizationManager.isSynchronizationActive()).isTrue();
		assertThat(synchronizationManager.isActualTransactionActive()).isTrue();
		assertThat(synchronizationManager.getCurrentTransactionName()).isEqualTo("outer");
	}


	private enum Callback {

		BEFORE_BEGIN, AFTER_BEGIN, BEFORE_COMMIT, AFTER_COMMIT, BEFORE_ROLLBACK, AFTER_ROLLBACK
	}


	private record Invocation(String listener, Callback callback,
			TransactionExecution transaction, @Nullable Throwable failure) {
	}


	private static final class RecordingListener implements TransactionExecutionListener {

		private final String name;

		private final List<Invocation> invocations;

		private final @Nullable Callback failOn;

		private final Throwable listenerFailure;

		RecordingListener(String name, List<Invocation> invocations,
				@Nullable Callback failOn, Throwable listenerFailure) {

			this.name = name;
			this.invocations = invocations;
			this.failOn = failOn;
			this.listenerFailure = listenerFailure;
		}

		@Override
		public void beforeBegin(TransactionExecution transaction) {
			invoke(Callback.BEFORE_BEGIN, transaction, null);
		}

		@Override
		public void afterBegin(TransactionExecution transaction, @Nullable Throwable beginFailure) {
			invoke(Callback.AFTER_BEGIN, transaction, beginFailure);
		}

		@Override
		public void beforeCommit(TransactionExecution transaction) {
			invoke(Callback.BEFORE_COMMIT, transaction, null);
		}

		@Override
		public void afterCommit(TransactionExecution transaction, @Nullable Throwable commitFailure) {
			invoke(Callback.AFTER_COMMIT, transaction, commitFailure);
		}

		@Override
		public void beforeRollback(TransactionExecution transaction) {
			invoke(Callback.BEFORE_ROLLBACK, transaction, null);
		}

		@Override
		public void afterRollback(TransactionExecution transaction, @Nullable Throwable rollbackFailure) {
			invoke(Callback.AFTER_ROLLBACK, transaction, rollbackFailure);
		}

		private void invoke(Callback callback, TransactionExecution transaction, @Nullable Throwable failure) {
			this.invocations.add(new Invocation(this.name, callback, transaction, failure));
			if (callback == this.failOn) {
				if (this.listenerFailure instanceof RuntimeException runtimeException) {
					throw runtimeException;
				}
				throw (Error) this.listenerFailure;
			}
		}
	}


	@SuppressWarnings("serial")
	private static final class SuspendingTransactionManager extends ReactiveTestTransactionManager {

		private final Object resourceKey = new Object();

		private final Object outerResource = new Object();

		private final Object innerResource = new Object();

		private final @Nullable RuntimeException beginFailure;

		private int suspends;

		private int resumes;

		SuspendingTransactionManager(@Nullable RuntimeException beginFailure) {
			super(true, true);
			this.beginFailure = beginFailure;
		}

		@Override
		protected Mono<Object> doSuspend(TransactionSynchronizationManager synchronizationManager, Object transaction) {
			return Mono.fromSupplier(() -> {
				this.suspends++;
				return synchronizationManager.unbindResource(this.resourceKey);
			});
		}

		@Override
		protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager,
				Object transaction, TransactionDefinition definition) {

			if (this.beginFailure != null) {
				return Mono.error(this.beginFailure);
			}
			return super.doBegin(synchronizationManager, transaction, definition)
					.doOnSuccess(ignore -> synchronizationManager.bindResource(this.resourceKey, this.innerResource));
		}

		@Override
		protected Mono<Void> doResume(TransactionSynchronizationManager synchronizationManager,
				@Nullable Object transaction, Object suspendedResources) {

			return Mono.fromRunnable(() -> {
				this.resumes++;
				synchronizationManager.bindResource(this.resourceKey, suspendedResources);
			});
		}

		@Override
		protected Mono<Void> doCleanupAfterCompletion(
				TransactionSynchronizationManager synchronizationManager, Object transaction) {

			return super.doCleanupAfterCompletion(synchronizationManager, transaction)
					.doOnSuccess(ignore -> assertThat(synchronizationManager.unbindResource(this.resourceKey))
							.isSameAs(this.innerResource));
		}
	}

}

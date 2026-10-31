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

package org.springframework.transaction.support;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionExecutionListener;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TransactionExecutionListenerTests {

	@AfterEach
	void verifySynchronizationState() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
	}

	@ParameterizedTest
	@MethodSource("failingCallbacks")
	void listenerFailureDoesNotInterruptTransactionOrOtherListeners(String callback, Throwable failure) {
		TrackingTransactionManager manager = new TrackingTransactionManager();
		TransactionExecutionListener first = mock();
		TransactionExecutionListener failing = failingListener(callback, failure);
		TransactionExecutionListener last = mock();
		manager.addListener(first);
		manager.addListener(failing);
		manager.addListener(last);

		TransactionStatus status = manager.getTransaction(null);
		boolean rollback = callback.endsWith("Rollback");
		if (rollback) {
			manager.rollback(status);
		}
		else {
			manager.commit(status);
		}

		assertThat(manager.begin).isTrue();
		assertThat(manager.commit).isEqualTo(!rollback);
		assertThat(manager.rollback).isEqualTo(rollback);
		assertThat(manager.cleanup).isTrue();
		assertThat(status.isCompleted()).isTrue();
		assertCallbacksInOrder(status, rollback, first, failing, last);
	}

	static Stream<Arguments> failingCallbacks() {
		return Stream.of("beforeBegin", "afterBegin", "beforeCommit", "afterCommit", "beforeRollback", "afterRollback")
				.flatMap(callback -> Stream.of(new IllegalStateException("Listener failure"), new AssertionError("Listener failure"))
						.map(failure -> arguments(callback, failure)));
	}

	@Test
	void beginFailureIsNotReplacedByListenerFailure() {
		TrackingTransactionManager manager = new TrackingTransactionManager();
		RuntimeException failure = new TransactionSystemException("Begin failure");
		manager.beginFailure = failure;
		manager.addListener(failingListener("afterBegin", new IllegalStateException("Listener failure")));
		TransactionExecutionListener observer = mock();
		manager.addListener(observer);

		assertThatRuntimeException().isThrownBy(() -> manager.getTransaction(null)).isSameAs(failure);

		verify(observer).afterBegin(any(), same(failure));
		assertThat(manager.commit).isFalse();
		assertThat(manager.rollback).isFalse();
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void commitFailureIsNotReplacedByListenerFailure(boolean rollbackOnCommitFailure) {
		TrackingTransactionManager manager = new TrackingTransactionManager();
		RuntimeException failure = new TransactionSystemException("Commit failure");
		manager.commitFailure = failure;
		manager.setRollbackOnCommitFailure(rollbackOnCommitFailure);
		String callback = (rollbackOnCommitFailure ? "afterRollback" : "afterCommit");
		manager.addListener(failingListener(callback, new IllegalStateException("Listener failure")));
		TransactionExecutionListener observer = mock();
		manager.addListener(observer);
		TransactionStatus status = manager.getTransaction(null);

		assertThatRuntimeException().isThrownBy(() -> manager.commit(status)).isSameAs(failure);

		if (rollbackOnCommitFailure) {
			verify(observer).afterRollback(status, null);
		}
		else {
			verify(observer).afterCommit(status, failure);
		}
		assertThat(manager.rollback).isEqualTo(rollbackOnCommitFailure);
		assertThat(manager.cleanup).isTrue();
		assertThat(status.isCompleted()).isTrue();
	}

	@Test
	void rollbackFailureIsNotReplacedByListenerFailure() {
		TrackingTransactionManager manager = new TrackingTransactionManager();
		RuntimeException failure = new TransactionSystemException("Rollback failure");
		manager.rollbackFailure = failure;
		manager.addListener(failingListener("afterRollback", new IllegalStateException("Listener failure")));
		TransactionExecutionListener observer = mock();
		manager.addListener(observer);
		TransactionStatus status = manager.getTransaction(null);

		assertThatRuntimeException().isThrownBy(() -> manager.rollback(status)).isSameAs(failure);

		verify(observer).afterRollback(status, failure);
		assertThat(manager.cleanup).isTrue();
		assertThat(status.isCompleted()).isTrue();
	}

	@Test
	void synchronizationCanStillPreventCommit() {
		TrackingTransactionManager manager = new TrackingTransactionManager();
		manager.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_ALWAYS);
		RuntimeException failure = new IllegalStateException("Synchronization failure");

		assertThatRuntimeException().isThrownBy(() -> new TransactionTemplate(manager).executeWithoutResult(status ->
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void beforeCommit(boolean readOnly) {
						throw failure;
					}
				}))).isSameAs(failure);

		assertThat(manager.commit).isFalse();
		assertThat(manager.rollback).isTrue();
		assertThat(manager.cleanup).isTrue();
	}

	private static TransactionExecutionListener failingListener(String callback, Throwable failure) {
		return mock(TransactionExecutionListener.class, invocation -> {
			if (invocation.getMethod().getName().equals(callback)) {
				throw failure;
			}
			return null;
		});
	}

	private static void assertCallbacksInOrder(TransactionStatus status, boolean rollback,
			TransactionExecutionListener... listeners) {

		InOrder order = inOrder((Object[]) listeners);
		for (TransactionExecutionListener listener : listeners) {
			order.verify(listener).beforeBegin(status);
		}
		for (TransactionExecutionListener listener : listeners) {
			order.verify(listener).afterBegin(status, null);
		}
		for (TransactionExecutionListener listener : listeners) {
			if (rollback) {
				order.verify(listener).beforeRollback(status);
			}
			else {
				order.verify(listener).beforeCommit(status);
			}
		}
		for (TransactionExecutionListener listener : listeners) {
			if (rollback) {
				order.verify(listener).afterRollback(status, null);
			}
			else {
				order.verify(listener).afterCommit(status, null);
			}
		}
		order.verifyNoMoreInteractions();
	}


	@SuppressWarnings("serial")
	private static class TrackingTransactionManager extends TestTransactionManager {

		@Nullable RuntimeException beginFailure;

		@Nullable RuntimeException commitFailure;

		@Nullable RuntimeException rollbackFailure;

		boolean cleanup;

		TrackingTransactionManager() {
			super(false, true);
		}

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) {
			if (this.beginFailure != null) {
				throw this.beginFailure;
			}
			super.doBegin(transaction, definition);
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) {
			super.doCommit(status);
			if (this.commitFailure != null) {
				throw this.commitFailure;
			}
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) {
			super.doRollback(status);
			if (this.rollbackFailure != null) {
				throw this.rollbackFailure;
			}
		}

		@Override
		protected void doCleanupAfterCompletion(Object transaction) {
			this.cleanup = true;
		}
	}

}

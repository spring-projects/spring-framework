/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.transaction.event;

import org.junit.jupiter.api.Test;

import org.springframework.context.PayloadApplicationEvent;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * @author Juergen Hoeller
 */
class TransactionalApplicationListenerAdapterTests {

	@Test
	void invokesCompletionCallbackOnSuccess() {
		CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
		PayloadApplicationEvent<Object> event = new PayloadApplicationEvent<>(this, new Object());

		TransactionalApplicationListener<PayloadApplicationEvent<Object>> adapter =
				TransactionalApplicationListener.forPayload(p -> {});
		adapter.addCallback(callback);
		runInTransaction(() -> adapter.onApplicationEvent(event));

		assertThat(callback.preEvent).isEqualTo(event);
		assertThat(callback.postEvent).isEqualTo(event);
		assertThat(callback.ex).isNull();
		assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
		assertThat(adapter.getListenerId()).isEmpty();
	}

	@Test
	void invokesExceptionHandlerOnException() {
		CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
		PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "event");
		RuntimeException ex = new RuntimeException("event");

		TransactionalApplicationListener<PayloadApplicationEvent<String>> adapter =
				TransactionalApplicationListener.forPayload(
						TransactionPhase.BEFORE_COMMIT, p -> {throw ex;});
		adapter.addCallback(callback);

		assertThatRuntimeException()
				.isThrownBy(() -> runInTransaction(() -> adapter.onApplicationEvent(event)))
				.withMessage("event");

		assertThat(callback.preEvent).isEqualTo(event);
		assertThat(callback.postEvent).isEqualTo(event);
		assertThat(callback.ex).isEqualTo(ex);
		assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
		assertThat(adapter.getListenerId()).isEmpty();
	}

	@Test
	void useSpecifiedIdentifier() {
		CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
		PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "event");

		TransactionalApplicationListenerAdapter<PayloadApplicationEvent<String>> adapter =
				new TransactionalApplicationListenerAdapter<>(e -> {});
		adapter.setTransactionPhase(TransactionPhase.BEFORE_COMMIT);
		adapter.setListenerId("identifier");
		adapter.addCallback(callback);
		runInTransaction(() -> adapter.onApplicationEvent(event));

		assertThat(callback.preEvent).isEqualTo(event);
		assertThat(callback.postEvent).isEqualTo(event);
		assertThat(callback.ex).isNull();
		assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
		assertThat(adapter.getListenerId()).isEqualTo("identifier");
	}


	private static void runInTransaction(Runnable runnable) {
		TransactionSynchronizationManager.setActualTransactionActive(true);
		TransactionSynchronizationManager.initSynchronization();
		try {
			runnable.run();
			TransactionSynchronizationManager.getSynchronizations().forEach(it -> {
				it.beforeCommit(false);
				it.afterCommit();
				it.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
			});
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
			TransactionSynchronizationManager.setActualTransactionActive(false);
		}
	}

}

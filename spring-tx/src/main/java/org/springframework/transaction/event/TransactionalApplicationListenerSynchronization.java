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

package org.springframework.transaction.event;

import java.util.List;

import org.springframework.context.ApplicationEvent;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * {@link TransactionSynchronization} implementation for event processing with a
 * {@link TransactionalApplicationListener}.
 *
 * @author Juergen Hoeller
 * @since 5.3
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 */
class TransactionalApplicationListenerSynchronization<E extends ApplicationEvent>
		implements TransactionSynchronization {

	private final E event;

	private final TransactionalApplicationListener<E> listener;

	private final List<TransactionalApplicationListener.SynchronizationCallback> callbacks;


	public TransactionalApplicationListenerSynchronization(E event, TransactionalApplicationListener<E> listener,
			List<TransactionalApplicationListener.SynchronizationCallback> callbacks) {

		this.event = event;
		this.listener = listener;
		this.callbacks = callbacks;
	}


	@Override
	public int getOrder() {
		return this.listener.getOrder();
	}

	@Override
	public void beforeCommit(boolean readOnly) {
		if (this.listener.getTransactionPhase() == TransactionPhase.BEFORE_COMMIT) {
			processEventWithCallbacks();
		}
	}

	@Override
	public void afterCompletion(int status) {
		TransactionPhase phase = this.listener.getTransactionPhase();
		if (phase == TransactionPhase.AFTER_COMMIT && status == STATUS_COMMITTED) {
			processEventWithCallbacks();
		}
		else if (phase == TransactionPhase.AFTER_ROLLBACK && status == STATUS_ROLLED_BACK) {
			processEventWithCallbacks();
		}
		else if (phase == TransactionPhase.AFTER_COMPLETION) {
			processEventWithCallbacks();
		}
	}

	private void processEventWithCallbacks() {
		this.callbacks.forEach(callback -> callback.preProcessEvent(this.event));
		try {
			this.listener.processEvent(this.event);
		}
		catch (RuntimeException | Error ex) {
			this.callbacks.forEach(callback -> callback.postProcessEvent(this.event, ex));
			throw ex;
		}
		this.callbacks.forEach(callback -> callback.postProcessEvent(this.event, null));
	}

}

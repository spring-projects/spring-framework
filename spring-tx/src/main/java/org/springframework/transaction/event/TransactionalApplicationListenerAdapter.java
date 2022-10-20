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
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * {@link TransactionalApplicationListener} adapter that delegates the processing of
 * an event to a target {@link ApplicationListener} instance. Supports the exact
 * same features as any regular {@link ApplicationListener} but is aware of the
 * transactional context of the event publisher.
 *
 * <p>For simple {@link org.springframework.context.PayloadApplicationEvent} handling,
 * consider the {@link TransactionalApplicationListener#forPayload} factory methods
 * as a convenient alternative to custom usage of this adapter class.
 *
 * @author Juergen Hoeller
 * @since 5.3
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 * @see TransactionalApplicationListener
 * @see TransactionalEventListener
 * @see TransactionalApplicationListenerMethodAdapter
 */
public class TransactionalApplicationListenerAdapter<E extends ApplicationEvent>
		implements TransactionalApplicationListener<E>, Ordered {

	private final ApplicationListener<E> targetListener;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private TransactionPhase transactionPhase = TransactionPhase.AFTER_COMMIT;

	private String listenerId = "";

	private final List<SynchronizationCallback> callbacks = new CopyOnWriteArrayList<>();


	/**
	 * Construct a new TransactionalApplicationListenerAdapter.
	 * @param targetListener the actual listener to invoke in the specified transaction phase
	 * @see #setTransactionPhase
	 * @see TransactionalApplicationListener#forPayload
	 */
	public TransactionalApplicationListenerAdapter(ApplicationListener<E> targetListener) {
		this.targetListener = targetListener;
	}


	/**
	 * Specify the synchronization order for the listener.
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Return the synchronization order for the listener.
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Specify the transaction phase to invoke the listener in.
	 * <p>The default is {@link TransactionPhase#AFTER_COMMIT}.
	 */
	public void setTransactionPhase(TransactionPhase transactionPhase) {
		this.transactionPhase = transactionPhase;
	}

	/**
	 * Return the transaction phase to invoke the listener in.
	 */
	@Override
	public TransactionPhase getTransactionPhase() {
		return this.transactionPhase;
	}

	/**
	 * Specify an id to identify the listener with.
	 * <p>The default is an empty String.
	 */
	public void setListenerId(String listenerId) {
		this.listenerId = listenerId;
	}

	/**
	 * Return an id to identify the listener with.
	 */
	@Override
	public String getListenerId() {
		return this.listenerId;
	}

	@Override
	public void addCallback(SynchronizationCallback callback) {
		Assert.notNull(callback, "SynchronizationCallback must not be null");
		this.callbacks.add(callback);
	}

	@Override
	public void processEvent(E event) {
		this.targetListener.onApplicationEvent(event);
	}


	@Override
	public void onApplicationEvent(E event) {
		if (TransactionSynchronizationManager.isSynchronizationActive() &&
				TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionalApplicationListenerSynchronization<>(event, this, this.callbacks));
		}
	}

}

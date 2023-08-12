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

package org.springframework.transaction.event;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * {@link GenericApplicationListener} adapter that delegates the processing of
 * an event to a {@link TransactionalEventListener} annotated method. Supports
 * the exact same features as any regular {@link EventListener} annotated method
 * but is aware of the transactional context of the event publisher.
 *
 * <p>Processing of {@link TransactionalEventListener} is enabled automatically
 * when Spring's transaction management is enabled. For other cases, registering
 * a bean of type {@link TransactionalEventListenerFactory} is required.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 5.3
 * @see TransactionalEventListener
 * @see TransactionalApplicationListener
 * @see TransactionalApplicationListenerAdapter
 */
public class TransactionalApplicationListenerMethodAdapter extends ApplicationListenerMethodAdapter
		implements TransactionalApplicationListener<ApplicationEvent> {

	private final TransactionPhase transactionPhase;

	private final boolean fallbackExecution;

	private final List<SynchronizationCallback> callbacks = new CopyOnWriteArrayList<>();


	/**
	 * Construct a new TransactionalApplicationListenerMethodAdapter.
	 * @param beanName the name of the bean to invoke the listener method on
	 * @param targetClass the target class that the method is declared on
	 * @param method the listener method to invoke
	 */
	public TransactionalApplicationListenerMethodAdapter(String beanName, Class<?> targetClass, Method method) {
		super(beanName, targetClass, method);
		TransactionalEventListener eventAnn =
				AnnotatedElementUtils.findMergedAnnotation(getTargetMethod(), TransactionalEventListener.class);
		if (eventAnn == null) {
			throw new IllegalStateException("No TransactionalEventListener annotation found on method: " + method);
		}
		this.transactionPhase = eventAnn.phase();
		this.fallbackExecution = eventAnn.fallbackExecution();
	}


	@Override
	public TransactionPhase getTransactionPhase() {
		return this.transactionPhase;
	}

	@Override
	public void addCallback(SynchronizationCallback callback) {
		Assert.notNull(callback, "SynchronizationCallback must not be null");
		this.callbacks.add(callback);
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (TransactionSynchronizationManager.isSynchronizationActive() &&
				TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionalApplicationListenerSynchronization<>(event, this, this.callbacks));
		}
		else if (this.fallbackExecution) {
			if (getTransactionPhase() == TransactionPhase.AFTER_ROLLBACK && logger.isWarnEnabled()) {
				logger.warn("Processing " + event + " as a fallback execution on AFTER_ROLLBACK phase");
			}
			processEvent(event);
		}
		else {
			// No transactional event execution at all
			if (logger.isDebugEnabled()) {
				logger.debug("No transaction is active - skipping " + event);
			}
		}
	}

}

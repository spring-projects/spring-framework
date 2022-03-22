/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.micrometer.contextpropagation.ContextContainer;
import io.micrometer.contextpropagation.ThreadLocalAccessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * A {@link ThreadLocalAccessor} to put and restore current transactions via {@link TransactionSynchronizationManager}.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public final class TransactionThreadLocalAccessor implements ThreadLocalAccessor {

	private static final Log log = LogFactory.getLog(TransactionThreadLocalAccessor.class);

	private static final String KEY = TransactionSynchronizationManagerHolder.class.getName();

	private static final String PREVIOUS_TRANSACTION_KEY = TransactionSynchronizationManagerHolder.class.getName() + "_PREVIOUS_TRANSACTION_PRESENT";

	@Override
	public void captureValues(ContextContainer container) {
		TransactionSynchronizationManagerHolder value = TransactionSynchronizationManagerHolder.create();
		container.put(KEY, value);
	}

	@Override
	public void restoreValues(ContextContainer container) {
		String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
		if (StringUtils.hasText(transactionName)) {
			container.put(PREVIOUS_TRANSACTION_KEY, true);
			if (log.isDebugEnabled()) {
				log.debug("A transaction with name [" + transactionName + "] has already been set in this thread. We don't want to override it. Will not update the synchronization manager with entries from the container");
			}
			return;
		}
		if (container.containsKey(KEY)) {
			TransactionSynchronizationManagerHolder holder = container.get(KEY);
			holder.putInScope();
		}
	}

	@Override
	public void resetValues(ContextContainer container) {
		Object previousTransaction = container.get(PREVIOUS_TRANSACTION_KEY);
		if (previousTransaction != null && Boolean.parseBoolean(String.valueOf(previousTransaction.toString()))) {
			if (log.isDebugEnabled()) {
				log.debug("There was a transaction when context propagation happened. Will not reset the synchronization manager.");
			}
			return;
		}
		TransactionSynchronizationManager.clear();
	}

	static final class TransactionSynchronizationManagerHolder {

		private final Map<Object, Object> resources;

		@Nullable
		private final Set<TransactionSynchronization> synchronizations;

		@Nullable
		private final String currentTransactionName;

		private final Boolean currentTransactionReadOnly;

		@Nullable
		private final Integer currentTransactionIsolationLevel;

		private final Boolean actualTransactionActive;

		private TransactionSynchronizationManagerHolder(Map<Object, Object> resources, @Nullable Set<TransactionSynchronization> synchronizations, @Nullable String currentTransactionName, Boolean currentTransactionReadOnly, @Nullable Integer currentTransactionIsolationLevel, Boolean actualTransactionActive) {
			this.resources = resources;
			this.synchronizations = synchronizations;
			this.currentTransactionName = currentTransactionName;
			this.currentTransactionReadOnly = currentTransactionReadOnly;
			this.currentTransactionIsolationLevel = currentTransactionIsolationLevel;
			this.actualTransactionActive = actualTransactionActive;
		}

		private static TransactionSynchronizationManagerHolder create() {
			return new TransactionSynchronizationManagerHolder(TransactionSynchronizationManager.resources.get(), TransactionSynchronizationManager.synchronizations.get(), TransactionSynchronizationManager.currentTransactionName.get(), TransactionSynchronizationManager.currentTransactionReadOnly.get(), TransactionSynchronizationManager.currentTransactionIsolationLevel.get(), TransactionSynchronizationManager.actualTransactionActive.get());
		}

		private void putInScope() {
			TransactionSynchronizationManager.resources.set(new HashMap<>(this.resources));
			TransactionSynchronizationManager.synchronizations.set(new HashSet<>(this.synchronizations));
			TransactionSynchronizationManager.currentTransactionName.set(this.currentTransactionName);
			TransactionSynchronizationManager.currentTransactionReadOnly.set(this.currentTransactionReadOnly);
			TransactionSynchronizationManager.currentTransactionIsolationLevel.set(this.currentTransactionIsolationLevel);
			TransactionSynchronizationManager.actualTransactionActive.set(this.actualTransactionActive);
		}
	}
}

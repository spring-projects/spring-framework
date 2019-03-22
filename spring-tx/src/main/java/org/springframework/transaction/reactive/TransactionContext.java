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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Mutable transaction context that encapsulates transactional synchronizations and
 * resources in the scope of a single transaction. Transaction context is typically
 * held by an outer {@link TransactionContextHolder} or referenced directly within
 * from the subscriber context.
 *
 * @author Mark Paluch
 * @since 5.2
 * @see TransactionContextManager
 * @see reactor.util.context.Context
 */
public class TransactionContext {

	private final UUID contextId = UUID.randomUUID();

	private final Map<Object, Object> resources = new LinkedHashMap<>();

	private @Nullable Set<ReactiveTransactionSynchronization> synchronizations;

	private volatile @Nullable String currentTransactionName;

	private volatile boolean currentTransactionReadOnly;

	private volatile @Nullable Integer currentTransactionIsolationLevel;

	private volatile boolean actualTransactionActive;

	private final @Nullable TransactionContext parent;


	TransactionContext() {
		this(null);
	}

	TransactionContext(@Nullable TransactionContext parent) {
		this.parent = parent;
	}


	public void clear() {

		synchronizations = null;
		currentTransactionName = null;
		currentTransactionReadOnly = false;
		currentTransactionIsolationLevel = null;
		actualTransactionActive = false;
	}

	public String getName() {

		if (StringUtils.hasText(currentTransactionName)) {
			return contextId + ": " + currentTransactionName;
		}

		return contextId.toString();
	}

	public UUID getContextId() {
		return contextId;
	}

	public Map<Object, Object> getResources() {
		return resources;
	}

	@Nullable
	public Set<ReactiveTransactionSynchronization> getSynchronizations() {
		return synchronizations;
	}

	public void setSynchronizations(@org.springframework.lang.Nullable Set<ReactiveTransactionSynchronization> synchronizations) {
		this.synchronizations = synchronizations;
	}

	@Nullable
	public String getCurrentTransactionName() {
		return currentTransactionName;
	}

	public void setCurrentTransactionName(@Nullable String currentTransactionName) {
		this.currentTransactionName = currentTransactionName;
	}

	public boolean isCurrentTransactionReadOnly() {
		return currentTransactionReadOnly;
	}

	public void setCurrentTransactionReadOnly(boolean currentTransactionReadOnly) {
		this.currentTransactionReadOnly = currentTransactionReadOnly;
	}

	@Nullable
	public Integer getCurrentTransactionIsolationLevel() {
		return currentTransactionIsolationLevel;
	}

	public void setCurrentTransactionIsolationLevel(@Nullable Integer currentTransactionIsolationLevel) {
		this.currentTransactionIsolationLevel = currentTransactionIsolationLevel;
	}

	public boolean isActualTransactionActive() {
		return actualTransactionActive;
	}

	public void setActualTransactionActive(boolean actualTransactionActive) {
		this.actualTransactionActive = actualTransactionActive;
	}

	@Nullable
	public TransactionContext getParent() {
		return parent;
	}

}

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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Mutable transaction context that encapsulates transactional synchronizations and
 * resources in the scope of a single transaction. Transaction context is typically
 * held by an outer {@link TransactionContextHolder} or referenced directly within
 * from the subscriber context.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see TransactionContextManager
 * @see reactor.util.context.Context
 */
public class TransactionContext {

	private final @Nullable TransactionContext parent;

	private final Map<Object, Object> resources = new LinkedHashMap<>();

	private @Nullable Set<TransactionSynchronization> synchronizations;

	private volatile @Nullable String currentTransactionName;

	private volatile boolean currentTransactionReadOnly;

	private volatile @Nullable Integer currentTransactionIsolationLevel;

	private volatile boolean actualTransactionActive;


	TransactionContext() {
		this(null);
	}

	TransactionContext(@Nullable TransactionContext parent) {
		this.parent = parent;
	}


	public @Nullable TransactionContext getParent() {
		return this.parent;
	}

	public Map<Object, Object> getResources() {
		return this.resources;
	}

	public void setSynchronizations(@Nullable Set<TransactionSynchronization> synchronizations) {
		this.synchronizations = synchronizations;
	}

	public @Nullable Set<TransactionSynchronization> getSynchronizations() {
		return this.synchronizations;
	}

	public void setCurrentTransactionName(@Nullable String currentTransactionName) {
		this.currentTransactionName = currentTransactionName;
	}

	public @Nullable String getCurrentTransactionName() {
		return this.currentTransactionName;
	}

	public void setCurrentTransactionReadOnly(boolean currentTransactionReadOnly) {
		this.currentTransactionReadOnly = currentTransactionReadOnly;
	}

	public boolean isCurrentTransactionReadOnly() {
		return this.currentTransactionReadOnly;
	}

	public void setCurrentTransactionIsolationLevel(@Nullable Integer currentTransactionIsolationLevel) {
		this.currentTransactionIsolationLevel = currentTransactionIsolationLevel;
	}

	public @Nullable Integer getCurrentTransactionIsolationLevel() {
		return this.currentTransactionIsolationLevel;
	}

	public void setActualTransactionActive(boolean actualTransactionActive) {
		this.actualTransactionActive = actualTransactionActive;
	}

	public boolean isActualTransactionActive() {
		return this.actualTransactionActive;
	}


	public void clear() {
		this.synchronizations = null;
		this.currentTransactionName = null;
		this.currentTransactionReadOnly = false;
		this.currentTransactionIsolationLevel = null;
		this.actualTransactionActive = false;
	}

}

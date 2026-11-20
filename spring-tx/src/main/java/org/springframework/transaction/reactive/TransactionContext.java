/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

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

package org.springframework.transaction.reactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reactor.core.publisher.Mono;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.transaction.NoTransactionException;
import org.springframework.util.Assert;

/**
 * Central delegate that manages resources and transaction synchronizations per
 * subscriber context. To be used by resource management code but not by typical
 * application code.
 *
 * <p>Supports one resource per key without overwriting, that is, a resource needs
 * to be removed before a new one can be set for the same key.
 * Supports a list of transaction synchronizations if synchronization is active.
 *
 * <p>Resource management code should check for context-bound resources, e.g.
 * database connections, via {@code getResource}. Such code is normally not
 * supposed to bind resources to units of work, as this is the responsibility
 * of transaction managers. A further option is to lazily bind on first use if
 * transaction synchronization is active, for performing transactions that span
 * an arbitrary number of resources.
 *
 * <p>Transaction synchronization must be activated and deactivated by a transaction
 * manager via {@link #initSynchronization()} and {@link #clearSynchronization()}.
 * This is automatically supported by {@link AbstractReactiveTransactionManager},
 * and thus by all standard Spring transaction managers.
 *
 * <p>Resource management code should only register synchronizations when this
 * manager is active, which can be checked via {@link #isSynchronizationActive};
 * it should perform immediate resource cleanup else. If transaction synchronization
 * isn't active, there is either no current transaction, or the transaction manager
 * doesn't support transaction synchronization.
 *
 * <p>Synchronization is for example used to always return the same resources within
 * a transaction, e.g. a database connection for any given connection factory.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see #isSynchronizationActive
 * @see #registerSynchronization
 * @see TransactionSynchronization
 */
public class TransactionSynchronizationManager {

	private final TransactionContext transactionContext;


	public TransactionSynchronizationManager(TransactionContext transactionContext) {
		Assert.notNull(transactionContext, "TransactionContext must not be null");
		this.transactionContext = transactionContext;
	}


	/**
	 * Get the {@link TransactionSynchronizationManager} that is associated with
	 * the current transaction context.
	 * <p>Mainly intended for code that wants to bind resources or synchronizations.
	 * @throws NoTransactionException if the transaction info cannot be found &mdash;
	 * for example, because the method was invoked outside a managed transaction
	 */
	public static Mono<TransactionSynchronizationManager> forCurrentTransaction() {
		return TransactionContextManager.currentContext().map(TransactionSynchronizationManager::new);
	}


	/**
	 * Check if there is a resource for the given key bound to the current context.
	 * @param key the key to check (usually the resource factory)
	 * @return if there is a value bound to the current context
	 */
	public boolean hasResource(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doGetResource(actualKey);
		return (value != null);
	}

	/**
	 * Retrieve a resource for the given key that is bound to the current context.
	 * @param key the key to check (usually the resource factory)
	 * @return a value bound to the current context (usually the active
	 * resource object), or {@code null} if none
	 */
	@Nullable
	public Object getResource(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		return doGetResource(actualKey);
	}

	/**
	 * Actually check the value of the resource that is bound for the given key.
	 */
	@Nullable
	private Object doGetResource(Object actualKey) {
		return this.transactionContext.getResources().get(actualKey);
	}

	/**
	 * Bind the given resource for the given key to the current context.
	 * @param key the key to bind the value to (usually the resource factory)
	 * @param value the value to bind (usually the active resource object)
	 * @throws IllegalStateException if there is already a value bound to the context
	 */
	public void bindResource(Object key, Object value) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Assert.notNull(value, "Value must not be null");
		Map<Object, Object> map = this.transactionContext.getResources();
		Object oldValue = map.put(actualKey, value);
		if (oldValue != null) {
			throw new IllegalStateException(
					"Already value [" + oldValue + "] for key [" + actualKey + "] bound to context");
		}
	}

	/**
	 * Unbind a resource for the given key from the current context.
	 * @param key the key to unbind (usually the resource factory)
	 * @return the previously bound value (usually the active resource object)
	 * @throws IllegalStateException if there is no value bound to the context
	 */
	public Object unbindResource(Object key) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doUnbindResource(actualKey);
		if (value == null) {
			throw new IllegalStateException("No value for key [" + actualKey + "] bound to context");
		}
		return value;
	}

	/**
	 * Unbind a resource for the given key from the current context.
	 * @param key the key to unbind (usually the resource factory)
	 * @return the previously bound value, or {@code null} if none bound
	 */
	@Nullable
	public Object unbindResourceIfPossible(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		return doUnbindResource(actualKey);
	}

	/**
	 * Actually remove the value of the resource that is bound for the given key.
	 */
	@Nullable
	private Object doUnbindResource(Object actualKey) {
		Map<Object, Object> map = this.transactionContext.getResources();
		return map.remove(actualKey);
	}


	//-------------------------------------------------------------------------
	// Management of transaction synchronizations
	//-------------------------------------------------------------------------

	/**
	 * Return if transaction synchronization is active for the current context.
	 * Can be called before register to avoid unnecessary instance creation.
	 * @see #registerSynchronization
	 */
	public boolean isSynchronizationActive() {
		return (this.transactionContext.getSynchronizations() != null);
	}

	/**
	 * Activate transaction synchronization for the current context.
	 * Called by a transaction manager on transaction begin.
	 * @throws IllegalStateException if synchronization is already active
	 */
	public void initSynchronization() throws IllegalStateException {
		if (isSynchronizationActive()) {
			throw new IllegalStateException("Cannot activate transaction synchronization - already active");
		}
		this.transactionContext.setSynchronizations(new LinkedHashSet<>());
	}

	/**
	 * Register a new transaction synchronization for the current context.
	 * Typically called by resource management code.
	 * <p>Note that synchronizations can implement the
	 * {@link org.springframework.core.Ordered} interface.
	 * They will be executed in an order according to their order value (if any).
	 * @param synchronization the synchronization object to register
	 * @throws IllegalStateException if transaction synchronization is not active
	 * @see org.springframework.core.Ordered
	 */
	public void registerSynchronization(TransactionSynchronization synchronization)
			throws IllegalStateException {

		Assert.notNull(synchronization, "TransactionSynchronization must not be null");
		Set<TransactionSynchronization> synchs = this.transactionContext.getSynchronizations();
		if (synchs == null) {
			throw new IllegalStateException("Transaction synchronization is not active");
		}
		synchs.add(synchronization);
	}

	/**
	 * Return an unmodifiable snapshot list of all registered synchronizations
	 * for the current context.
	 * @return unmodifiable List of TransactionSynchronization instances
	 * @throws IllegalStateException if synchronization is not active
	 * @see TransactionSynchronization
	 */
	public List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
		Set<TransactionSynchronization> synchs = this.transactionContext.getSynchronizations();
		if (synchs == null) {
			throw new IllegalStateException("Transaction synchronization is not active");
		}
		// Return unmodifiable snapshot, to avoid ConcurrentModificationExceptions
		// while iterating and invoking synchronization callbacks that in turn
		// might register further synchronizations.
		if (synchs.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			// Sort lazily here, not in registerSynchronization.
			List<TransactionSynchronization> sortedSynchs = new ArrayList<>(synchs);
			AnnotationAwareOrderComparator.sort(sortedSynchs);
			return Collections.unmodifiableList(sortedSynchs);
		}
	}

	/**
	 * Deactivate transaction synchronization for the current context.
	 * Called by the transaction manager on transaction cleanup.
	 * @throws IllegalStateException if synchronization is not active
	 */
	public void clearSynchronization() throws IllegalStateException {
		if (!isSynchronizationActive()) {
			throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
		}
		this.transactionContext.setSynchronizations(null);
	}


	//-------------------------------------------------------------------------
	// Exposure of transaction characteristics
	//-------------------------------------------------------------------------

	/**
	 * Expose the name of the current transaction, if any.
	 * Called by the transaction manager on transaction begin and on cleanup.
	 * @param name the name of the transaction, or {@code null} to reset it
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	public void setCurrentTransactionName(@Nullable String name) {
		this.transactionContext.setCurrentTransactionName(name);
	}

	/**
	 * Return the name of the current transaction, or {@code null} if none set.
	 * To be called by resource management code for optimizations per use case,
	 * for example to optimize fetch strategies for specific named transactions.
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	@Nullable
	public String getCurrentTransactionName() {
		return this.transactionContext.getCurrentTransactionName();
	}

	/**
	 * Expose a read-only flag for the current transaction.
	 * Called by the transaction manager on transaction begin and on cleanup.
	 * @param readOnly {@code true} to mark the current transaction
	 * as read-only; {@code false} to reset such a read-only marker
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
	 */
	public void setCurrentTransactionReadOnly(boolean readOnly) {
		this.transactionContext.setCurrentTransactionReadOnly(readOnly);
	}

	/**
	 * Return whether the current transaction is marked as read-only.
	 * To be called by resource management code when preparing a newly
	 * created resource.
	 * <p>Note that transaction synchronizations receive the read-only flag
	 * as argument for the {@code beforeCommit} callback, to be able
	 * to suppress change detection on commit. The present method is meant
	 * to be used for earlier read-only checks.
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
	 * @see TransactionSynchronization#beforeCommit(boolean)
	 */
	public boolean isCurrentTransactionReadOnly() {
		return this.transactionContext.isCurrentTransactionReadOnly();
	}

	/**
	 * Expose an isolation level for the current transaction.
	 * Called by the transaction manager on transaction begin and on cleanup.
	 * @param isolationLevel the isolation level to expose, according to the
	 * R2DBC Connection constants (equivalent to the corresponding Spring
	 * TransactionDefinition constants), or {@code null} to reset it
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 */
	public void setCurrentTransactionIsolationLevel(@Nullable Integer isolationLevel) {
		this.transactionContext.setCurrentTransactionIsolationLevel(isolationLevel);
	}

	/**
	 * Return the isolation level for the current transaction, if any.
	 * To be called by resource management code when preparing a newly
	 * created resource (for example, a R2DBC Connection).
	 * @return the currently exposed isolation level, according to the
	 * R2DBC Connection constants (equivalent to the corresponding Spring
	 * TransactionDefinition constants), or {@code null} if none
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 */
	@Nullable
	public Integer getCurrentTransactionIsolationLevel() {
		return this.transactionContext.getCurrentTransactionIsolationLevel();
	}

	/**
	 * Expose whether there currently is an actual transaction active.
	 * Called by the transaction manager on transaction begin and on cleanup.
	 * @param active {@code true} to mark the current context as being associated
	 * with an actual transaction; {@code false} to reset that marker
	 */
	public void setActualTransactionActive(boolean active) {
		this.transactionContext.setActualTransactionActive(active);
	}

	/**
	 * Return whether there currently is an actual transaction active.
	 * This indicates whether the current context is associated with an actual
	 * transaction rather than just with active transaction synchronization.
	 * <p>To be called by resource management code that wants to discriminate
	 * between active transaction synchronization (with or without backing
	 * resource transaction; also on PROPAGATION_SUPPORTS) and an actual
	 * transaction being active (with backing resource transaction;
	 * on PROPAGATION_REQUIRED, PROPAGATION_REQUIRES_NEW, etc).
	 * @see #isSynchronizationActive()
	 */
	public boolean isActualTransactionActive() {
		return this.transactionContext.isActualTransactionActive();
	}

	/**
	 * Clear the entire transaction synchronization state:
	 * registered synchronizations as well as the various transaction characteristics.
	 * @see #clearSynchronization()
	 * @see #setCurrentTransactionName
	 * @see #setCurrentTransactionReadOnly
	 * @see #setCurrentTransactionIsolationLevel
	 * @see #setActualTransactionActive
	 */
	public void clear() {
		this.transactionContext.clear();
	}

}

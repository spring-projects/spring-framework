/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.UnexpectedRollbackException;

/**
 * Abstract base class that implements Spring's standard reactive transaction workflow,
 * serving as basis for concrete platform transaction managers.
 *
 * <p>This base class provides the following workflow handling:
 * <ul>
 * <li>determines if there is an existing transaction;
 * <li>applies the appropriate propagation behavior;
 * <li>suspends and resumes transactions if necessary;
 * <li>checks the rollback-only flag on commit;
 * <li>applies the appropriate modification on rollback
 * (actual rollback or setting rollback-only);
 * <li>triggers registered synchronization callbacks.
 * </ul>
 *
 * <p>Subclasses have to implement specific template methods for specific
 * states of a transaction, e.g.: begin, suspend, resume, commit, rollback.
 * The most important of them are abstract and must be provided by a concrete
 * implementation; for the rest, defaults are provided, so overriding is optional.
 *
 * <p>Transaction synchronization is a generic mechanism for registering callbacks
 * that get invoked at transaction completion time. This is mainly used internally
 * by the data access support classes for R2DBC, MongoDB, etc. The same mechanism can
 * also be leveraged for custom synchronization needs in an application.
 *
 * <p>The state of this class is serializable, to allow for serializing the
 * transaction strategy along with proxies that carry a transaction interceptor.
 * It is up to subclasses if they wish to make their state to be serializable too.
 * They should implement the {@code java.io.Serializable} marker interface in
 * that case, and potentially a private {@code readObject()} method (according
 * to Java serialization rules) if they need to restore any transient state.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see TransactionSynchronizationManager
 */
@SuppressWarnings("serial")
public abstract class AbstractReactiveTransactionManager implements ReactiveTransactionManager, Serializable {

	protected transient Log logger = LogFactory.getLog(getClass());


	//---------------------------------------------------------------------
	// Implementation of ReactiveTransactionManager
	//---------------------------------------------------------------------

	/**
	 * This implementation handles propagation behavior. Delegates to
	 * {@code doGetTransaction}, {@code isExistingTransaction}
	 * and {@code doBegin}.
	 * @see #doGetTransaction
	 * @see #isExistingTransaction
	 * @see #doBegin
	 */
	@Override
	public final Mono<ReactiveTransaction> getReactiveTransaction(@Nullable TransactionDefinition definition)
			throws TransactionException {

		// Use defaults if no transaction definition given.
		TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());

		return TransactionSynchronizationManager.forCurrentTransaction()
				.flatMap(synchronizationManager -> {

			Object transaction = doGetTransaction(synchronizationManager);

			// Cache debug flag to avoid repeated checks.
			boolean debugEnabled = logger.isDebugEnabled();

			if (isExistingTransaction(transaction)) {
				// Existing transaction found -> check propagation behavior to find out how to behave.
				return handleExistingTransaction(synchronizationManager, def, transaction, debugEnabled);
			}

			// Check definition settings for new transaction.
			if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
				return Mono.error(new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout()));
			}

			// No existing transaction found -> check propagation behavior to find out how to proceed.
			if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
				return Mono.error(new IllegalTransactionStateException(
						"No existing transaction found for transaction marked with propagation 'mandatory'"));
			}
			else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
					def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
					def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {

				return TransactionContextManager.currentContext()
						.map(TransactionSynchronizationManager::new)
						.flatMap(nestedSynchronizationManager ->
								suspend(nestedSynchronizationManager, null)
								.map(Optional::of)
								.defaultIfEmpty(Optional.empty())
								.flatMap(suspendedResources -> {
							if (debugEnabled) {
								logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
							}
							return Mono.defer(() -> {
								GenericReactiveTransaction status = newReactiveTransaction(
										nestedSynchronizationManager, def, transaction, true,
										debugEnabled, suspendedResources.orElse(null));
								return doBegin(nestedSynchronizationManager, transaction, def)
										.doOnSuccess(ignore -> prepareSynchronization(nestedSynchronizationManager, status, def))
										.thenReturn(status);
							}).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR,
									ex -> resume(nestedSynchronizationManager, null, suspendedResources.orElse(null))
									.then(Mono.error(ex)));
						}));
			}
			else {
				// Create "empty" transaction: no actual transaction, but potentially synchronization.
				if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
					logger.warn("Custom isolation level specified but no actual transaction initiated; " +
							"isolation level will effectively be ignored: " + def);
				}
				return Mono.just(prepareReactiveTransaction(synchronizationManager, def, null, true, debugEnabled, null));
			}
		});
	}

	/**
	 * Create a ReactiveTransaction for an existing transaction.
	 */
	private Mono<ReactiveTransaction> handleExistingTransaction(TransactionSynchronizationManager synchronizationManager,
			TransactionDefinition definition, Object transaction, boolean debugEnabled) throws TransactionException {

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
			return Mono.error(new IllegalTransactionStateException(
					"Existing transaction found for transaction marked with propagation 'never'"));
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction");
			}
			Mono<SuspendedResourcesHolder> suspend = suspend(synchronizationManager, transaction);
			return suspend.map(suspendedResources -> prepareReactiveTransaction(synchronizationManager,
					definition, null, false, debugEnabled, suspendedResources)) //
					.switchIfEmpty(Mono.fromSupplier(() -> prepareReactiveTransaction(synchronizationManager,
							definition, null, false, debugEnabled, null)))
					.cast(ReactiveTransaction.class);
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction, creating new transaction with name [" +
						definition.getName() + "]");
			}
			Mono<SuspendedResourcesHolder> suspendedResources = suspend(synchronizationManager, transaction);
			return suspendedResources.flatMap(suspendedResourcesHolder -> {
				GenericReactiveTransaction status = newReactiveTransaction(synchronizationManager,
						definition, transaction, true, debugEnabled, suspendedResourcesHolder);
				return doBegin(synchronizationManager, transaction, definition).doOnSuccess(ignore ->
						prepareSynchronization(synchronizationManager, status, definition)).thenReturn(status)
						.onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR, beginEx ->
								resumeAfterBeginException(synchronizationManager, transaction, suspendedResourcesHolder, beginEx)
										.then(Mono.error(beginEx)));
			});
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			if (debugEnabled) {
				logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
			}
			// Nested transaction through nested begin and commit/rollback calls.
			GenericReactiveTransaction status = newReactiveTransaction(synchronizationManager,
					definition, transaction, true, debugEnabled, null);
			return doBegin(synchronizationManager, transaction, definition).doOnSuccess(ignore ->
					prepareSynchronization(synchronizationManager, status, definition)).thenReturn(status);
		}

		// Assumably PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED.
		if (debugEnabled) {
			logger.debug("Participating in existing transaction");
		}
		return Mono.just(prepareReactiveTransaction(synchronizationManager, definition, transaction, false, debugEnabled, null));
	}

	/**
	 * Create a new ReactiveTransaction for the given arguments,
	 * also initializing transaction synchronization as appropriate.
	 * @see #newReactiveTransaction
	 * @see #prepareReactiveTransaction
	 */
	private GenericReactiveTransaction prepareReactiveTransaction(
			TransactionSynchronizationManager synchronizationManager, TransactionDefinition definition,
			@Nullable Object transaction, boolean newTransaction, boolean debug, @Nullable Object suspendedResources) {

		GenericReactiveTransaction status = newReactiveTransaction(synchronizationManager,
				definition, transaction, newTransaction, debug, suspendedResources);
		prepareSynchronization(synchronizationManager, status, definition);
		return status;
	}

	/**
	 * Create a ReactiveTransaction instance for the given arguments.
	 */
	private GenericReactiveTransaction newReactiveTransaction(
			TransactionSynchronizationManager synchronizationManager, TransactionDefinition definition,
			@Nullable Object transaction, boolean newTransaction, boolean debug, @Nullable Object suspendedResources) {

		return new GenericReactiveTransaction(transaction, newTransaction,
				!synchronizationManager.isSynchronizationActive(),
				definition.isReadOnly(), debug, suspendedResources);
	}

	/**
	 * Initialize transaction synchronization as appropriate.
	 */
	private void prepareSynchronization(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status, TransactionDefinition definition) {

		if (status.isNewSynchronization()) {
			synchronizationManager.setActualTransactionActive(status.hasTransaction());
			synchronizationManager.setCurrentTransactionIsolationLevel(
					definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ?
							definition.getIsolationLevel() : null);
			synchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
			synchronizationManager.setCurrentTransactionName(definition.getName());
			synchronizationManager.initSynchronization();
		}
	}


	/**
	 * Suspend the given transaction. Suspends transaction synchronization first,
	 * then delegates to the {@code doSuspend} template method.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the current transaction object
	 * (or {@code null} to just suspend active synchronizations, if any)
	 * @return an object that holds suspended resources
	 * (or {@code null} if neither transaction nor synchronization active)
	 * @see #doSuspend
	 * @see #resume
	 */
	private Mono<SuspendedResourcesHolder> suspend(TransactionSynchronizationManager synchronizationManager,
			@Nullable Object transaction) throws TransactionException {

		if (synchronizationManager.isSynchronizationActive()) {
			Mono<List<TransactionSynchronization>> suspendedSynchronizations = doSuspendSynchronization(synchronizationManager);
			return suspendedSynchronizations.flatMap(synchronizations -> {
				Mono<Optional<Object>> suspendedResources = (transaction != null ?
						doSuspend(synchronizationManager, transaction).map(Optional::of).defaultIfEmpty(Optional.empty()) :
						Mono.just(Optional.empty()));
				return suspendedResources.map(it -> {
					String name = synchronizationManager.getCurrentTransactionName();
					synchronizationManager.setCurrentTransactionName(null);
					boolean readOnly = synchronizationManager.isCurrentTransactionReadOnly();
					synchronizationManager.setCurrentTransactionReadOnly(false);
					Integer isolationLevel = synchronizationManager.getCurrentTransactionIsolationLevel();
					synchronizationManager.setCurrentTransactionIsolationLevel(null);
					boolean wasActive = synchronizationManager.isActualTransactionActive();
					synchronizationManager.setActualTransactionActive(false);
					return new SuspendedResourcesHolder(
							it.orElse(null), synchronizations, name, readOnly, isolationLevel, wasActive);
				}).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR,
						ex -> doResumeSynchronization(synchronizationManager, synchronizations)
								.cast(SuspendedResourcesHolder.class));
			});
		}
		else if (transaction != null) {
			// Transaction active but no synchronization active.
			Mono<Optional<Object>> suspendedResources =
					doSuspend(synchronizationManager, transaction).map(Optional::of).defaultIfEmpty(Optional.empty());
			return suspendedResources.map(it -> new SuspendedResourcesHolder(it.orElse(null)));
		}
		else {
			// Neither transaction nor synchronization active.
			return Mono.empty();
		}
	}

	/**
	 * Resume the given transaction. Delegates to the {@code doResume}
	 * template method first, then resuming transaction synchronization.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the current transaction object
	 * @param resourcesHolder the object that holds suspended resources,
	 * as returned by {@code suspend} (or {@code null} to just
	 * resume synchronizations, if any)
	 * @see #doResume
	 * @see #suspend
	 */
	private Mono<Void> resume(TransactionSynchronizationManager synchronizationManager,
			@Nullable Object transaction, @Nullable SuspendedResourcesHolder resourcesHolder)
			throws TransactionException {

		Mono<Void> resume = Mono.empty();

		if (resourcesHolder != null) {
			Object suspendedResources = resourcesHolder.suspendedResources;
			if (suspendedResources != null) {
				resume =  doResume(synchronizationManager, transaction, suspendedResources);
			}
			List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
			if (suspendedSynchronizations != null) {
				synchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
				synchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
				synchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
				synchronizationManager.setCurrentTransactionName(resourcesHolder.name);
				return resume.then(doResumeSynchronization(synchronizationManager, suspendedSynchronizations));
			}
		}

		return resume;
	}

	/**
	 * Resume outer transaction after inner transaction begin failed.
	 */
	private Mono<Void> resumeAfterBeginException(TransactionSynchronizationManager synchronizationManager,
			Object transaction, @Nullable SuspendedResourcesHolder suspendedResources, Throwable beginEx) {

		String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";
		return resume(synchronizationManager, transaction, suspendedResources).doOnError(ErrorPredicates.RUNTIME_OR_ERROR,
				ex -> logger.error(exMessage, beginEx));
	}

	/**
	 * Suspend all current synchronizations and deactivate transaction
	 * synchronization for the current transaction context.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @return the List of suspended TransactionSynchronization objects
	 */
	private Mono<List<TransactionSynchronization>> doSuspendSynchronization(
			TransactionSynchronizationManager synchronizationManager) {

		List<TransactionSynchronization> suspendedSynchronizations = synchronizationManager.getSynchronizations();
		return Flux.fromIterable(suspendedSynchronizations)
				.concatMap(TransactionSynchronization::suspend)
				.then(Mono.defer(() -> {
					synchronizationManager.clearSynchronization();
					return Mono.just(suspendedSynchronizations);
				}));
	}

	/**
	 * Reactivate transaction synchronization for the current transaction context
	 * and resume all given synchronizations.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param suspendedSynchronizations a List of TransactionSynchronization objects
	 */
	private Mono<Void> doResumeSynchronization(TransactionSynchronizationManager synchronizationManager,
			List<TransactionSynchronization> suspendedSynchronizations) {

		synchronizationManager.initSynchronization();
		return Flux.fromIterable(suspendedSynchronizations)
				.concatMap(synchronization -> synchronization.resume()
						.doOnSuccess(ignore -> synchronizationManager.registerSynchronization(synchronization))).then();
	}


	/**
	 * This implementation of commit handles participating in existing
	 * transactions and programmatic rollback requests.
	 * Delegates to {@code isRollbackOnly}, {@code doCommit}
	 * and {@code rollback}.
	 * @see ReactiveTransaction#isRollbackOnly()
	 * @see #doCommit
	 * @see #rollback
	 */
	@Override
	public final Mono<Void> commit(ReactiveTransaction transaction) throws TransactionException {
		if (transaction.isCompleted()) {
			return Mono.error(new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction"));
		}

		return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
			GenericReactiveTransaction reactiveTx = (GenericReactiveTransaction) transaction;
			if (reactiveTx.isRollbackOnly()) {
				if (reactiveTx.isDebug()) {
					logger.debug("Transactional code has requested rollback");
				}
				return processRollback(synchronizationManager, reactiveTx);
			}
			return processCommit(synchronizationManager, reactiveTx);
		});
	}

	/**
	 * Process an actual commit.
	 * Rollback-only flags have already been checked and applied.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @throws TransactionException in case of commit failure
	 */
	private Mono<Void> processCommit(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) throws TransactionException {

		AtomicBoolean beforeCompletionInvoked = new AtomicBoolean();

		Mono<Object> commit = prepareForCommit(synchronizationManager, status)
				.then(triggerBeforeCommit(synchronizationManager, status))
				.then(triggerBeforeCompletion(synchronizationManager, status))
				.then(Mono.defer(() -> {
					beforeCompletionInvoked.set(true);
					if (status.isNewTransaction()) {
						if (status.isDebug()) {
							logger.debug("Initiating transaction commit");
						}
						return doCommit(synchronizationManager, status);
					}
					return Mono.empty();
				})).then(Mono.empty().onErrorResume(ex -> {
					Mono<Object> propagateException = Mono.error(ex);
					// Store result in a local variable in order to appease the
					// Eclipse compiler with regard to inferred generics.
					Mono<Object> result = propagateException;
					if (ErrorPredicates.UNEXPECTED_ROLLBACK.test(ex)) {
						result = triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_ROLLED_BACK)
								.then(propagateException);
					}
					else if (ErrorPredicates.TRANSACTION_EXCEPTION.test(ex)) {
						result = triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_UNKNOWN)
								.then(propagateException);
					}
					else if (ErrorPredicates.RUNTIME_OR_ERROR.test(ex)) {
						Mono<Void> mono;
						if (!beforeCompletionInvoked.get()) {
							mono = triggerBeforeCompletion(synchronizationManager, status);
						}
						else {
							mono = Mono.empty();
						}
						result = mono.then(doRollbackOnCommitException(synchronizationManager, status, ex))
								.then(propagateException);
					}

					return result;
				})).then(Mono.defer(() -> triggerAfterCommit(synchronizationManager, status).onErrorResume(ex ->
						triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_COMMITTED).then(Mono.error(ex)))
						.then(triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_COMMITTED))));

		return commit
				.onErrorResume(ex -> cleanupAfterCompletion(synchronizationManager, status)
						.then(Mono.error(ex))).then(cleanupAfterCompletion(synchronizationManager, status));
	}

	/**
	 * This implementation of rollback handles participating in existing transactions.
	 * Delegates to {@code doRollback} and {@code doSetRollbackOnly}.
	 * @see #doRollback
	 * @see #doSetRollbackOnly
	 */
	@Override
	public final Mono<Void> rollback(ReactiveTransaction transaction) throws TransactionException {
		if (transaction.isCompleted()) {
			return Mono.error(new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction"));
		}
		return TransactionSynchronizationManager.forCurrentTransaction().flatMap(synchronizationManager -> {
			GenericReactiveTransaction reactiveTx = (GenericReactiveTransaction) transaction;
			return processRollback(synchronizationManager, reactiveTx);
		});
	}

	/**
	 * Process an actual rollback.
	 * The completed flag has already been checked.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @throws TransactionException in case of rollback failure
	 */
	private Mono<Void> processRollback(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		return triggerBeforeCompletion(synchronizationManager, status).then(Mono.defer(() -> {
			if (status.isNewTransaction()) {
				if (status.isDebug()) {
					logger.debug("Initiating transaction rollback");
				}
				return doRollback(synchronizationManager, status);
			}
			else {
				Mono<Void> beforeCompletion = Mono.empty();
				// Participating in larger transaction
				if (status.hasTransaction()) {
					if (status.isDebug()) {
						logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
					}
					beforeCompletion = doSetRollbackOnly(synchronizationManager, status);
				}
				else {
					logger.debug("Should roll back transaction but cannot - no transaction available");
				}
				return beforeCompletion;
			}
		})).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR, ex -> triggerAfterCompletion(
				synchronizationManager, status, TransactionSynchronization.STATUS_UNKNOWN)
				.then(Mono.error(ex)))
				.then(Mono.defer(() -> triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_ROLLED_BACK)))
				.onErrorResume(ex -> cleanupAfterCompletion(synchronizationManager, status).then(Mono.error(ex)))
			.then(cleanupAfterCompletion(synchronizationManager, status));
	}

	/**
	 * Invoke {@code doRollback}, handling rollback exceptions properly.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @param ex the thrown application exception or error
	 * @throws TransactionException in case of rollback failure
	 * @see #doRollback
	 */
	private Mono<Void> doRollbackOnCommitException(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status, Throwable ex) throws TransactionException {

		return Mono.defer(() -> {
			if (status.isNewTransaction()) {
				if (status.isDebug()) {
					logger.debug("Initiating transaction rollback after commit exception", ex);
				}
				return doRollback(synchronizationManager, status);
			}
			else if (status.hasTransaction()) {
				if (status.isDebug()) {
					logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
				}
				return doSetRollbackOnly(synchronizationManager, status);
			}
			return Mono.empty();
		}).onErrorResume(ErrorPredicates.RUNTIME_OR_ERROR, rbex -> {
			logger.error("Commit exception overridden by rollback exception", ex);
			return triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_UNKNOWN)
				.then(Mono.error(rbex));
		}).then(triggerAfterCompletion(synchronizationManager, status, TransactionSynchronization.STATUS_ROLLED_BACK));
	}


	/**
	 * Trigger {@code beforeCommit} callbacks.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 */
	private Mono<Void> triggerBeforeCommit(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		if (status.isNewSynchronization()) {
			return TransactionSynchronizationUtils.triggerBeforeCommit(
					synchronizationManager.getSynchronizations(), status.isReadOnly());
		}
		return Mono.empty();
	}

	/**
	 * Trigger {@code beforeCompletion} callbacks.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 */
	private Mono<Void> triggerBeforeCompletion(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		if (status.isNewSynchronization()) {
			return TransactionSynchronizationUtils.triggerBeforeCompletion(synchronizationManager.getSynchronizations());
		}
		return Mono.empty();
	}

	/**
	 * Trigger {@code afterCommit} callbacks.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 */
	private Mono<Void> triggerAfterCommit(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		if (status.isNewSynchronization()) {
			return TransactionSynchronizationUtils.invokeAfterCommit(synchronizationManager.getSynchronizations());
		}
		return Mono.empty();
	}

	/**
	 * Trigger {@code afterCompletion} callbacks.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @param completionStatus completion status according to TransactionSynchronization constants
	 */
	private Mono<Void> triggerAfterCompletion(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status, int completionStatus) {

		if (status.isNewSynchronization()) {
			List<TransactionSynchronization> synchronizations = synchronizationManager.getSynchronizations();
			synchronizationManager.clearSynchronization();
			if (!status.hasTransaction() || status.isNewTransaction()) {
				// No transaction or new transaction for the current scope ->
				// invoke the afterCompletion callbacks immediately
				return invokeAfterCompletion(synchronizationManager, synchronizations, completionStatus);
			}
			else if (!synchronizations.isEmpty()) {
				// Existing transaction that we participate in, controlled outside
				// of the scope of this Spring transaction manager -> try to register
				// an afterCompletion callback with the existing (JTA) transaction.
				return registerAfterCompletionWithExistingTransaction(
						synchronizationManager, status.getTransaction(), synchronizations);
			}
		}
		return Mono.empty();
	}

	/**
	 * Actually invoke the {@code afterCompletion} methods of the
	 * given TransactionSynchronization objects.
	 * <p>To be called by this abstract manager itself, or by special implementations
	 * of the {@code registerAfterCompletionWithExistingTransaction} callback.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param synchronizations a List of TransactionSynchronization objects
	 * @param completionStatus the completion status according to the
	 * constants in the TransactionSynchronization interface
	 * @see #registerAfterCompletionWithExistingTransaction(TransactionSynchronizationManager, Object, List)
	 * @see TransactionSynchronization#STATUS_COMMITTED
	 * @see TransactionSynchronization#STATUS_ROLLED_BACK
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	private Mono<Void> invokeAfterCompletion(TransactionSynchronizationManager synchronizationManager,
			List<TransactionSynchronization> synchronizations, int completionStatus) {

		return TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
	}

	/**
	 * Clean up after completion, clearing synchronization if necessary,
	 * and invoking doCleanupAfterCompletion.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @see #doCleanupAfterCompletion
	 */
	private Mono<Void> cleanupAfterCompletion(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		return Mono.defer(() -> {
			status.setCompleted();
			if (status.isNewSynchronization()) {
				synchronizationManager.clear();
			}
			Mono<Void> cleanup = Mono.empty();
			if (status.isNewTransaction()) {
				cleanup = doCleanupAfterCompletion(synchronizationManager, status.getTransaction());
			}
			if (status.getSuspendedResources() != null) {
				if (status.isDebug()) {
					logger.debug("Resuming suspended transaction after completion of inner transaction");
				}
				Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
				return cleanup.then(resume(synchronizationManager, transaction,
						(SuspendedResourcesHolder) status.getSuspendedResources()));
			}
			return cleanup;
		});
	}


	//---------------------------------------------------------------------
	// Template methods to be implemented in subclasses
	//---------------------------------------------------------------------

	/**
	 * Return a transaction object for the current transaction state.
	 * <p>The returned object will usually be specific to the concrete transaction
	 * manager implementation, carrying corresponding transaction state in a
	 * modifiable fashion. This object will be passed into the other template
	 * methods (e.g. doBegin and doCommit), either directly or as part of a
	 * DefaultReactiveTransactionStatus instance.
	 * <p>The returned object should contain information about any existing
	 * transaction, that is, a transaction that has already started before the
	 * current {@code getTransaction} call on the transaction manager.
	 * Consequently, a {@code doGetTransaction} implementation will usually
	 * look for an existing transaction and store corresponding state in the
	 * returned transaction object.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @return the current transaction object
	 * @throws org.springframework.transaction.CannotCreateTransactionException
	 * if transaction support is not available
	 * @throws TransactionException in case of lookup or system errors
	 * @see #doBegin
	 * @see #doCommit
	 * @see #doRollback
	 * @see GenericReactiveTransaction#getTransaction
	 */
	protected abstract Object doGetTransaction(TransactionSynchronizationManager synchronizationManager)
			throws TransactionException;

	/**
	 * Check if the given transaction object indicates an existing transaction
	 * (that is, a transaction which has already started).
	 * <p>The result will be evaluated according to the specified propagation
	 * behavior for the new transaction. An existing transaction might get
	 * suspended (in case of PROPAGATION_REQUIRES_NEW), or the new transaction
	 * might participate in the existing one (in case of PROPAGATION_REQUIRED).
	 * <p>The default implementation returns {@code false}, assuming that
	 * participating in existing transactions is generally not supported.
	 * Subclasses are of course encouraged to provide such support.
	 * @param transaction the transaction object returned by doGetTransaction
	 * @return if there is an existing transaction
	 * @throws TransactionException in case of system errors
	 * @see #doGetTransaction
	 */
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
		return false;
	}

	/**
	 * Begin a new transaction with semantics according to the given transaction
	 * definition. Does not have to care about applying the propagation behavior,
	 * as this has already been handled by this abstract manager.
	 * <p>This method gets called when the transaction manager has decided to actually
	 * start a new transaction. Either there wasn't any transaction before, or the
	 * previous transaction has been suspended.
	 * <p>A special scenario is a nested transaction: This method will be called to
	 * start a nested transaction when necessary. In such a context, there will be an
	 * active transaction: The implementation of this method has to detect this and
	 * start an appropriate nested transaction.
	 * @param synchronizationManager the synchronization manager bound to the new transaction
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @param definition a TransactionDefinition instance, describing propagation
	 * behavior, isolation level, read-only flag, timeout, and transaction name
	 * @throws TransactionException in case of creation or system errors
	 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
	 * if the underlying transaction does not support nesting (e.g. through savepoints)
	 */
	protected abstract Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager,
			Object transaction, TransactionDefinition definition) throws TransactionException;

	/**
	 * Suspend the resources of the current transaction.
	 * Transaction synchronization will already have been suspended.
	 * <p>The default implementation throws a TransactionSuspensionNotSupportedException,
	 * assuming that transaction suspension is generally not supported.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @return an object that holds suspended resources
	 * (will be kept unexamined for passing it into doResume)
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
	 * if suspending is not supported by the transaction manager implementation
	 * @throws TransactionException in case of system errors
	 * @see #doResume
	 */
	protected Mono<Object> doSuspend(TransactionSynchronizationManager synchronizationManager,
			Object transaction) throws TransactionException {

		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * Resume the resources of the current transaction.
	 * Transaction synchronization will be resumed afterwards.
	 * <p>The default implementation throws a TransactionSuspensionNotSupportedException,
	 * assuming that transaction suspension is generally not supported.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @param suspendedResources the object that holds suspended resources,
	 * as returned by doSuspend
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException
	 * if suspending is not supported by the transaction manager implementation
	 * @throws TransactionException in case of system errors
	 * @see #doSuspend
	 */
	protected Mono<Void> doResume(TransactionSynchronizationManager synchronizationManager,
			@Nullable Object transaction, Object suspendedResources) throws TransactionException {

		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * Make preparations for commit, to be performed before the
	 * {@code beforeCommit} synchronization callbacks occur.
	 * <p>Note that exceptions will get propagated to the commit caller
	 * and cause a rollback of the transaction.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status the status representation of the transaction
	 * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
	 * (note: do not throw TransactionException subclasses here!)
	 */
	protected Mono<Void> prepareForCommit(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) {

		return Mono.empty();
	}

	/**
	 * Perform an actual commit of the given transaction.
	 * <p>An implementation does not need to check the "new transaction" flag
	 * or the rollback-only flag; this will already have been handled before.
	 * Usually, a straight commit will be performed on the transaction object
	 * contained in the passed-in status.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status the status representation of the transaction
	 * @throws TransactionException in case of commit or system errors
	 * @see GenericReactiveTransaction#getTransaction
	 */
	protected abstract Mono<Void> doCommit(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) throws TransactionException;

	/**
	 * Perform an actual rollback of the given transaction.
	 * <p>An implementation does not need to check the "new transaction" flag;
	 * this will already have been handled before. Usually, a straight rollback
	 * will be performed on the transaction object contained in the passed-in status.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status the status representation of the transaction
	 * @throws TransactionException in case of system errors
	 * @see GenericReactiveTransaction#getTransaction
	 */
	protected abstract Mono<Void> doRollback(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) throws TransactionException;

	/**
	 * Set the given transaction rollback-only. Only called on rollback
	 * if the current transaction participates in an existing one.
	 * <p>The default implementation throws an IllegalTransactionStateException,
	 * assuming that participating in existing transactions is generally not
	 * supported. Subclasses are of course encouraged to provide such support.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status the status representation of the transaction
	 * @throws TransactionException in case of system errors
	 */
	protected Mono<Void> doSetRollbackOnly(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction status) throws TransactionException {

		throw new IllegalTransactionStateException(
				"Participating in existing transactions is not supported - when 'isExistingTransaction' " +
				"returns true, appropriate 'doSetRollbackOnly' behavior must be provided");
	}

	/**
	 * Register the given list of transaction synchronizations with the existing transaction.
	 * <p>Invoked when the control of the Spring transaction manager and thus all Spring
	 * transaction synchronizations end, without the transaction being completed yet. This
	 * is for example the case when participating in an existing JTA or EJB CMT transaction.
	 * <p>The default implementation simply invokes the {@code afterCompletion} methods
	 * immediately, passing in "STATUS_UNKNOWN". This is the best we can do if there's no
	 * chance to determine the actual outcome of the outer transaction.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 * @param synchronizations a List of TransactionSynchronization objects
	 * @throws TransactionException in case of system errors
	 * @see #invokeAfterCompletion(TransactionSynchronizationManager, List, int)
	 * @see TransactionSynchronization#afterCompletion(int)
	 * @see TransactionSynchronization#STATUS_UNKNOWN
	 */
	protected Mono<Void> registerAfterCompletionWithExistingTransaction(TransactionSynchronizationManager synchronizationManager,
			Object transaction, List<TransactionSynchronization> synchronizations) throws TransactionException {

		logger.debug("Cannot register Spring after-completion synchronization with existing transaction - " +
				"processing Spring after-completion callbacks immediately, with outcome status 'unknown'");
		return invokeAfterCompletion(synchronizationManager, synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
	}

	/**
	 * Cleanup resources after transaction completion.
	 * <p>Called after {@code doCommit} and {@code doRollback} execution,
	 * on any outcome. The default implementation does nothing.
	 * <p>Should not throw any exceptions but just issue warnings on errors.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction the transaction object returned by {@code doGetTransaction}
	 */
	protected Mono<Void> doCleanupAfterCompletion(TransactionSynchronizationManager synchronizationManager,
			Object transaction) {

		return Mono.empty();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		this.logger = LogFactory.getLog(getClass());
	}


	/**
	 * Holder for suspended resources.
	 * Used internally by {@code suspend} and {@code resume}.
	 */
	protected static final class SuspendedResourcesHolder {

		@Nullable
		private final Object suspendedResources;

		@Nullable
		private List<TransactionSynchronization> suspendedSynchronizations;

		@Nullable
		private String name;

		private boolean readOnly;

		@Nullable
		private Integer isolationLevel;

		private boolean wasActive;

		private SuspendedResourcesHolder(@Nullable Object suspendedResources) {
			this.suspendedResources = suspendedResources;
		}

		private SuspendedResourcesHolder(
				@Nullable Object suspendedResources, List<TransactionSynchronization> suspendedSynchronizations,
				@Nullable String name, boolean readOnly, @Nullable Integer isolationLevel, boolean wasActive) {

			this.suspendedResources = suspendedResources;
			this.suspendedSynchronizations = suspendedSynchronizations;
			this.name = name;
			this.readOnly = readOnly;
			this.isolationLevel = isolationLevel;
			this.wasActive = wasActive;
		}
	}


	/**
	 * Predicates for exception types that transactional error handling applies to.
	 */
	private enum ErrorPredicates implements Predicate<Throwable> {

		/**
		 * Predicate matching {@link RuntimeException} or {@link Error}.
		 */
		RUNTIME_OR_ERROR {
			@Override
			public boolean test(Throwable throwable) {
				return throwable instanceof RuntimeException || throwable instanceof Error;
			}
		},

		/**
		 * Predicate matching {@link TransactionException}.
		 */
		TRANSACTION_EXCEPTION {
			@Override
			public boolean test(Throwable throwable) {
				return throwable instanceof TransactionException;
			}
		},

		/**
		 * Predicate matching {@link UnexpectedRollbackException}.
		 */
		UNEXPECTED_ROLLBACK {
			@Override
			public boolean test(Throwable throwable) {
				return throwable instanceof UnexpectedRollbackException;
			}
		};

		@Override
		public abstract boolean test(Throwable throwable);
	}

}

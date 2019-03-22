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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.ReactiveTransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;


/**
 * Abstract base class that implements Spring's standard reactive transaction workflow,
 * serving as basis for concrete platform transaction managers.
 * <p>This base class provides the following workflow handling:
 * <ul>
 * <li>determines if there is an existing transaction;
 * <li>applies the appropriate propagation behavior;
 * <li>suspends and resumes transactions if necessary;
 * <li>checks the rollback-only flag on commit;
 * <li>applies the appropriate modification on rollback
 * (actual rollback or setting rollback-only);
 * <li>triggers registered synchronization callbacks
 * (if transaction synchronization is active).
 * </ul>
 * <p>Subclasses have to implement specific template methods for specific
 * states of a transaction, e.g.: begin, suspend, resume, commit, rollback.
 * The most important of them are abstract and must be provided by a concrete
 * implementation; for the rest, defaults are provided, so overriding is optional.
 * <p>Transaction synchronization is a generic mechanism for registering callbacks
 * that get invoked at transaction completion time. This is mainly used internally
 * by the data access support classes for R2DBC, MongoDB, etc. The same mechanism can
 * also be leveraged for custom synchronization needs in an application.
 * <p>The state of this class is serializable, to allow for serializing the
 * transaction strategy along with proxies that carry a transaction interceptor.
 * It is up to subclasses if they wish to make their state to be serializable too.
 * They should implement the {@code java.io.Serializable} marker interface in
 * that case, and potentially a private {@code readObject()} method (according
 * to Java serialization rules) if they need to restore any transient state.
 *
 * @author Mark Paluch
 * @since 5.2
 * @see #setTransactionSynchronization
 * @see ReactiveTransactionSynchronizationManager
 */
@SuppressWarnings({"serial", "WeakerAccess"})
public abstract class AbstractReactiveTransactionManager implements ReactiveTransactionManager, Serializable {

	/**
	 * Always activate transaction synchronization, even for "empty" transactions
	 * that result from PROPAGATION_SUPPORTS with no existing backend transaction.
	 *
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_SUPPORTS
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NOT_SUPPORTED
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NEVER
	 */
	public static final int SYNCHRONIZATION_ALWAYS = 0;

	/**
	 * Activate transaction synchronization only for actual transactions,
	 * that is, not for empty ones that result from PROPAGATION_SUPPORTS with
	 * no existing backend transaction.
	 *
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_MANDATORY
	 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRES_NEW
	 */
	public static final int SYNCHRONIZATION_ON_ACTUAL_TRANSACTION = 1;

	/**
	 * Never active transaction synchronization, not even for actual transactions.
	 */
	public static final int SYNCHRONIZATION_NEVER = 2;


	/**
	 * Constants instance for AbstractReactiveTransactionManager.
	 */
	private static final Constants constants = new Constants(AbstractReactiveTransactionManager.class);


	protected transient Log logger = LogFactory.getLog(getClass());

	private int transactionSynchronization = SYNCHRONIZATION_ALWAYS;

	private Duration defaultTimeout = Duration.ofSeconds(TransactionDefinition.TIMEOUT_DEFAULT);

	private boolean nestedTransactionAllowed = false;

	private boolean validateExistingTransaction = false;

	private boolean globalRollbackOnParticipationFailure = true;

	private boolean failEarlyOnGlobalRollbackOnly = false;

	private boolean rollbackOnCommitFailure = false;


	/**
	 * Set the transaction synchronization by the name of the corresponding constant
	 * in this class, e.g. "SYNCHRONIZATION_ALWAYS".
	 * @param constantName name of the constant
	 * @see #SYNCHRONIZATION_ALWAYS
	 */
	public final void setTransactionSynchronizationName(String constantName) {
		setTransactionSynchronization(constants.asNumber(constantName).intValue());
	}

	/**
	 * Set when this transaction manager should activate the subscriber context-bound
	 * transaction synchronization support. Default is "always".
	 * <p>Note that transaction synchronization isn't supported for
	 * multiple concurrent transactions by different transaction managers.
	 * Only one transaction manager is allowed to activate it at any time.
	 * @see #SYNCHRONIZATION_ALWAYS
	 * @see #SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
	 * @see #SYNCHRONIZATION_NEVER
	 * @see ReactiveTransactionSynchronizationManager
	 * @see ReactiveTransactionSynchronization
	 */
	public final void setTransactionSynchronization(int transactionSynchronization) {
		this.transactionSynchronization = transactionSynchronization;
	}

	/**
	 * Return if this transaction manager should activate the subscriber context-bound
	 * transaction synchronization support.
	 */
	public final int getTransactionSynchronization() {
		return this.transactionSynchronization;
	}

	/**
	 * Specify the default timeout that this transaction manager should apply
	 * if there is no timeout specified at the transaction level, in seconds.
	 * <p>Default is the underlying transaction infrastructure's default timeout,
	 * e.g. typically 30 seconds in case of a JTA provider, indicated by the
	 * {@code TransactionDefinition.TIMEOUT_DEFAULT} value.
	 * @see org.springframework.transaction.TransactionDefinition#TIMEOUT_DEFAULT
	 */
	public final void setDefaultTimeout(Duration defaultTimeout) {
		Assert.notNull(defaultTimeout, "Default timeout must not be null");
		if (defaultTimeout.getSeconds() < TransactionDefinition.TIMEOUT_DEFAULT) {
			throw new InvalidTimeoutException("Invalid default timeout", (int) defaultTimeout.getSeconds());
		}
		this.defaultTimeout = defaultTimeout;
	}

	/**
	 * Return the default timeout that this transaction manager should apply
	 * if there is no timeout specified at the transaction level, in seconds.
	 * <p>Returns {@code TransactionDefinition.TIMEOUT_DEFAULT} to indicate
	 * the underlying transaction infrastructure's default timeout.
	 */
	public final Duration getDefaultTimeout() {
		return this.defaultTimeout;
	}

	/**
	 * Set whether nested transactions are allowed. Default is "false".
	 * <p>Typically initialized with an appropriate default by the
	 * concrete transaction manager subclass.
	 */
	public final void setNestedTransactionAllowed(boolean nestedTransactionAllowed) {
		this.nestedTransactionAllowed = nestedTransactionAllowed;
	}

	/**
	 * Return whether nested transactions are allowed.
	 */
	public final boolean isNestedTransactionAllowed() {
		return this.nestedTransactionAllowed;
	}

	/**
	 * Set whether existing transactions should be validated before participating
	 * in them.
	 * <p>When participating in an existing transaction (e.g. with
	 * PROPAGATION_REQUIRED or PROPAGATION_SUPPORTS encountering an existing
	 * transaction), this outer transaction's characteristics will apply even
	 * to the inner transaction scope. Validation will detect incompatible
	 * isolation level and read-only settings on the inner transaction definition
	 * and reject participation accordingly through throwing a corresponding exception.
	 * <p>Default is "false", leniently ignoring inner transaction settings,
	 * simply overriding them with the outer transaction's characteristics.
	 * Switch this flag to "true" in order to enforce strict validation.
	 */
	public final void setValidateExistingTransaction(boolean validateExistingTransaction) {
		this.validateExistingTransaction = validateExistingTransaction;
	}

	/**
	 * Return whether existing transactions should be validated before participating
	 * in them.
	 */
	public final boolean isValidateExistingTransaction() {
		return this.validateExistingTransaction;
	}

	/**
	 * Set whether to globally mark an existing transaction as rollback-only
	 * after a participating transaction failed.
	 * <p>Default is "true": If a participating transaction (e.g. with
	 * PROPAGATION_REQUIRED or PROPAGATION_SUPPORTS encountering an existing
	 * transaction) fails, the transaction will be globally marked as rollback-only.
	 * The only possible outcome of such a transaction is a rollback: The
	 * transaction originator <i>cannot</i> make the transaction commit anymore.
	 * <p>Switch this to "false" to let the transaction originator make the rollback
	 * decision. If a participating transaction fails with an exception, the caller
	 * can still decide to continue with a different path within the transaction.
	 * However, note that this will only work as long as all participating resources
	 * are capable of continuing towards a transaction commit even after a data access
	 * failure: This is generally not the case for a Hibernate Session, for example;
	 * neither is it for a sequence of R2DBC insert/update/delete operations.
	 * <p><b>Note:</b>This flag only applies to an explicit rollback attempt for a
	 * subtransaction, typically caused by an exception thrown by a data access operation
	 * (where TransactionInterceptor will trigger a {@code ReactiveTransactionManager.rollback()}
	 * call according to a rollback rule). If the flag is off, the caller can handle the exception
	 * and decide on a rollback, independent of the rollback rules of the subtransaction.
	 * This flag does, however, <i>not</i> apply to explicit {@code setRollbackOnly}
	 * calls on a {@code TransactionStatus}, which will always cause an eventual
	 * global rollback (as it might not throw an exception after the rollback-only call).
	 * <p>The recommended solution for handling failure of a subtransaction
	 * is a "nested transaction", where the global transaction can be rolled
	 * back to a savepoint taken at the beginning of the subtransaction.
	 * PROPAGATION_NESTED provides exactly those semantics; however, it will
	 * only work when nested transaction support is available. This is the case
	 * with DataSourceTransactionManager, but not with JtaTransactionManager.
	 * @see #setNestedTransactionAllowed
	 */
	public final void setGlobalRollbackOnParticipationFailure(boolean globalRollbackOnParticipationFailure) {
		this.globalRollbackOnParticipationFailure = globalRollbackOnParticipationFailure;
	}

	/**
	 * Return whether to globally mark an existing transaction as rollback-only
	 * after a participating transaction failed.
	 */
	public final boolean isGlobalRollbackOnParticipationFailure() {
		return this.globalRollbackOnParticipationFailure;
	}

	/**
	 * Set whether to fail early in case of the transaction being globally marked
	 * as rollback-only.
	 * <p>Default is "false", only causing an UnexpectedRollbackException at the
	 * outermost transaction boundary. Switch this flag on to cause an
	 * UnexpectedRollbackException as early as the global rollback-only marker
	 * has been first detected, even from within an inner transaction boundary.
	 * @see org.springframework.transaction.UnexpectedRollbackException
	 */
	public final void setFailEarlyOnGlobalRollbackOnly(boolean failEarlyOnGlobalRollbackOnly) {
		this.failEarlyOnGlobalRollbackOnly = failEarlyOnGlobalRollbackOnly;
	}

	/**
	 * Return whether to fail early in case of the transaction being globally marked
	 * as rollback-only.
	 */
	public final boolean isFailEarlyOnGlobalRollbackOnly() {
		return this.failEarlyOnGlobalRollbackOnly;
	}

	/**
	 * Set whether {@code doRollback} should be performed on failure of the
	 * {@code doCommit} call. Typically not necessary and thus to be avoided,
	 * as it can potentially override the commit exception with a subsequent
	 * rollback exception.
	 * <p>Default is "false".
	 * @see #doCommit
	 * @see #doRollback
	 */
	public final void setRollbackOnCommitFailure(boolean rollbackOnCommitFailure) {
		this.rollbackOnCommitFailure = rollbackOnCommitFailure;
	}

	/**
	 * Return whether {@code doRollback} should be performed on failure of the
	 * {@code doCommit} call.
	 */
	public final boolean isRollbackOnCommitFailure() {
		return this.rollbackOnCommitFailure;
	}

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
	public final Mono<ReactiveTransactionStatus> getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {

		if (definition == null) {
			// Use defaults if no transaction definition given.
			definition = new DefaultTransactionDefinition();
		}

		TransactionDefinition definitionToUse = definition;

		return ReactiveTransactionSynchronizationManager.currentTransaction()
				.flatMap(synchronizationManager -> {

			Object transaction = doGetTransaction(synchronizationManager);

			// Cache debug flag to avoid repeated checks.
			boolean debugEnabled = logger.isDebugEnabled();

			if (isExistingTransaction(transaction)) {
				// Existing transaction found -> check propagation behavior to find out how to behave.
				return handleExistingTransaction(synchronizationManager, definitionToUse, transaction, debugEnabled);
			}

			// Check definition settings for new transaction.
			if (definitionToUse.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
				return Mono.error(new InvalidTimeoutException("Invalid transaction timeout", definitionToUse.getTimeout()));
			}

			// No existing transaction found -> check propagation behavior to find out how to proceed.
			if (definitionToUse.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
				return Mono.error(new IllegalTransactionStateException(
						"No existing transaction found for transaction marked with propagation 'mandatory'"));
			} else if (definitionToUse.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
					definitionToUse.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
					definitionToUse.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {

				return TransactionContextManager.currentContext()
						.map(ReactiveTransactionSynchronizationManager::new)
						.flatMap(nestedSynchronizationManager -> {

					return suspend(nestedSynchronizationManager, null)
							.map(Optional::of)
							.defaultIfEmpty(Optional.empty())
							.flatMap(suspendedResources -> {

						if (debugEnabled) {
							logger.debug("Creating new transaction with name [" + definitionToUse.getName() + "]: " + definitionToUse);
						}

						return Mono.defer(() -> {
							boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
							DefaultReactiveTransactionStatus status = newTransactionStatus(
									nestedSynchronizationManager, definitionToUse, transaction, true,
									newSynchronization, debugEnabled, suspendedResources.orElse(null));

							return doBegin(nestedSynchronizationManager, transaction, definitionToUse)
									.doOnSuccess(ignore -> prepareSynchronization(nestedSynchronizationManager, status, definitionToUse))
									.thenReturn(status);
						}).onErrorResume(ErrorPredicates.RuntimeOrError, e -> {
							return resume(nestedSynchronizationManager, null, suspendedResources.orElse(null))
									.then(Mono.error(e));
						});
					});
				});
			} else {
				// Create "empty" transaction: no actual transaction, but potentially synchronization.
				if (definitionToUse.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
					logger.warn("Custom isolation level specified but no actual transaction initiated; " +
							"isolation level will effectively be ignored: " + definitionToUse);
				}
				boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
				return Mono.just(prepareTransactionStatus(synchronizationManager, definitionToUse, null, true, newSynchronization, debugEnabled, null));
			}
		});
	}

	/**
	 * Create a TransactionStatus for an existing transaction.
	 */
	private Mono<ReactiveTransactionStatus> handleExistingTransaction(ReactiveTransactionSynchronizationManager synchronizationManager,
															  TransactionDefinition definition, Object transaction, boolean debugEnabled)
			throws TransactionException {

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
			return Mono.error(new IllegalTransactionStateException(
					"Existing transaction found for transaction marked with propagation 'never'"));
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction");
			}
			Mono<SuspendedResourcesHolder> suspend = suspend(synchronizationManager, transaction);
			boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);

			return suspend.map(suspendedResources -> prepareTransactionStatus(synchronizationManager,
					definition, null, false, newSynchronization, debugEnabled, suspendedResources)) //
					.switchIfEmpty(Mono.fromSupplier(() -> prepareTransactionStatus(synchronizationManager,
							definition, null, false, newSynchronization, debugEnabled, null)))
					.cast(ReactiveTransactionStatus.class);
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
			if (debugEnabled) {
				logger.debug("Suspending current transaction, creating new transaction with name [" +
						definition.getName() + "]");
			}
			Mono<SuspendedResourcesHolder> suspendedResources = suspend(synchronizationManager, transaction);
			boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);

			return suspendedResources.flatMap(suspendedResourcesHolder -> {

				DefaultReactiveTransactionStatus status = newTransactionStatus(synchronizationManager,
						definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);
				return doBegin(synchronizationManager, transaction, definition).doOnSuccess(ignore -> {
					prepareSynchronization(synchronizationManager, status, definition);
				}).thenReturn(status).

						onErrorResume(ErrorPredicates.RuntimeOrError, beginEx -> {
							return resumeAfterBeginException(synchronizationManager, transaction, suspendedResourcesHolder, beginEx).then(Mono.error(beginEx));
						});
			});
		}

		if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
			if (!isNestedTransactionAllowed()) {
				return Mono.error(new NestedTransactionNotSupportedException(
						"Transaction manager does not allow nested transactions by default - " +
								"specify 'nestedTransactionAllowed' property with value 'true'"));
			}
			if (debugEnabled) {
				logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
			}

			// Nested transaction through nested begin and commit/rollback calls.
			boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
			DefaultReactiveTransactionStatus status = newTransactionStatus(synchronizationManager,
					definition, transaction, true, newSynchronization, debugEnabled, null);

			return doBegin(synchronizationManager, transaction, definition).doOnSuccess(ignore -> {
				prepareSynchronization(synchronizationManager, status, definition);
			}).thenReturn(status);
		}

		// Assumably PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED.
		if (debugEnabled) {
			logger.debug("Participating in existing transaction");
		}
		if (isValidateExistingTransaction()) {
			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
				Integer currentIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
				if (currentIsolationLevel == null || currentIsolationLevel != definition.getIsolationLevel()) {
					Constants isoConstants = new Constants(TransactionDefinition.class);
					return Mono.error(new IllegalTransactionStateException("Participating transaction with definition [" +
							definition + "] specifies isolation level which is incompatible with existing transaction: " +
							(currentIsolationLevel != null ?
									isoConstants.toCode(currentIsolationLevel, DefaultTransactionDefinition.PREFIX_ISOLATION) :
									"(unknown)")));
				}
			}
			if (!definition.isReadOnly()) {
				if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
					return Mono.error(new IllegalTransactionStateException("Participating transaction with definition [" +
							definition + "] is not marked as read-only but existing transaction is"));
				}
			}
		}
		boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
		return Mono.just(prepareTransactionStatus(synchronizationManager, definition, transaction, false, newSynchronization, debugEnabled, null));
	}

	/**
	 * Create a new TransactionStatus for the given arguments,
	 * also initializing transaction synchronization as appropriate.
	 *
	 * @see #newTransactionStatus
	 * @see #prepareTransactionStatus
	 */
	protected final DefaultReactiveTransactionStatus prepareTransactionStatus(
			ReactiveTransactionSynchronizationManager synchronizationManager, TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
			boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {

		DefaultReactiveTransactionStatus status = newTransactionStatus(synchronizationManager,
				definition, transaction, newTransaction, newSynchronization, debug, suspendedResources);
		prepareSynchronization(synchronizationManager, status, definition);
		return status;
	}

	/**
	 * Create a TransactionStatus instance for the given arguments.
	 */
	protected DefaultReactiveTransactionStatus newTransactionStatus(
			ReactiveTransactionSynchronizationManager synchronizationManager, TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
			boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {

		boolean actualNewSynchronization = newSynchronization &&
				!synchronizationManager.isSynchronizationActive();
		return new DefaultReactiveTransactionStatus(
				transaction, newTransaction, actualNewSynchronization,
				definition.isReadOnly(), debug, suspendedResources);
	}

	/**
	 * Initialize transaction synchronization as appropriate.
	 */
	protected void prepareSynchronization(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status, TransactionDefinition definition) {
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
	 * Determine the actual timeout to use for the given definition.
	 * Will fall back to this manager's default timeout if the
	 * transaction definition doesn't specify a non-default value.
	 * @param definition the transaction definition
	 * @return the actual timeout to use
	 * @see org.springframework.transaction.TransactionDefinition#getTimeout()
	 * @see #setDefaultTimeout
	 */
	protected Duration determineTimeout(TransactionDefinition definition) {
		if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
			return Duration.ofSeconds(definition.getTimeout());
		}
		return this.defaultTimeout;
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
	protected final Mono<SuspendedResourcesHolder> suspend(ReactiveTransactionSynchronizationManager synchronizationManager, @Nullable Object transaction) throws TransactionException {
		if (synchronizationManager.isSynchronizationActive()) {
			Mono<List<ReactiveTransactionSynchronization>> suspendedSynchronizations = doSuspendSynchronization(synchronizationManager);

			return suspendedSynchronizations.flatMap(synchronizations -> {

				Mono<Optional<Object>> suspendedResources = transaction != null ? doSuspend(synchronizationManager, transaction).map(Optional::of).defaultIfEmpty(Optional.empty()) : Mono.just(Optional.empty());

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
				}).onErrorResume(ErrorPredicates.RuntimeOrError, t -> doResumeSynchronization(synchronizationManager, synchronizations).cast(SuspendedResourcesHolder.class));
			});
		} else if (transaction != null) {
			// Transaction active but no synchronization active.
			Mono<Optional<Object>> suspendedResources = doSuspend(synchronizationManager, transaction).map(Optional::of).defaultIfEmpty(Optional.empty());
			return suspendedResources.map(it -> new SuspendedResourcesHolder(it.orElse(null)));
		} else {
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
	protected final Mono<Void> resume(ReactiveTransactionSynchronizationManager synchronizationManager, @Nullable Object transaction, @Nullable SuspendedResourcesHolder resourcesHolder)
			throws TransactionException {

		if (resourcesHolder != null) {
			Object suspendedResources = resourcesHolder.suspendedResources;
			if (suspendedResources != null) {
				return doResume(synchronizationManager, transaction, suspendedResources);
			}
			List<ReactiveTransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
			if (suspendedSynchronizations != null) {
				synchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
				synchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
				synchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
				synchronizationManager.setCurrentTransactionName(resourcesHolder.name);
				return doResumeSynchronization(synchronizationManager, suspendedSynchronizations);
			}
		}

		return Mono.empty();
	}

	/**
	 * Resume outer transaction after inner transaction begin failed.
	 */
	private Mono<Void> resumeAfterBeginException(ReactiveTransactionSynchronizationManager synchronizationManager,
												 Object transaction, @Nullable SuspendedResourcesHolder suspendedResources, Throwable beginEx) {

		String exMessage = "Inner transaction begin exception overridden by outer transaction resume exception";
		return resume(synchronizationManager, transaction, suspendedResources).doOnError(ErrorPredicates.RuntimeOrError, t -> logger.error(exMessage, beginEx));
	}

	/**
	 * Suspend all current synchronizations and deactivate transaction
	 * synchronization for the current transaction context.
	 *
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @return the List of suspended ReactiveTransactionSynchronization objects
	 */
	private Mono<List<ReactiveTransactionSynchronization>> doSuspendSynchronization(ReactiveTransactionSynchronizationManager synchronizationManager) {
		List<ReactiveTransactionSynchronization> suspendedSynchronizations =
				synchronizationManager.getSynchronizations();

		return Flux.fromIterable(suspendedSynchronizations)
				.concatMap(ReactiveTransactionSynchronization::suspend)
				.then(Mono.defer(() -> {
					synchronizationManager.clearSynchronization();
					return Mono.just(suspendedSynchronizations);
				}));
	}

	/**
	 * Reactivate transaction synchronization for the current transaction context
	 * and resume all given synchronizations.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param suspendedSynchronizations a List of ReactiveTransactionSynchronization objects
	 */
	private Mono<Void> doResumeSynchronization(ReactiveTransactionSynchronizationManager synchronizationManager, List<ReactiveTransactionSynchronization> suspendedSynchronizations) {
		synchronizationManager.initSynchronization();

		return Flux.fromIterable(suspendedSynchronizations)
				.concatMap(synchronization -> {
					return synchronization.resume()
							.doOnSuccess(ignore -> synchronizationManager.registerSynchronization(synchronization));
				}).then();
	}


	/**
	 * This implementation of commit handles participating in existing
	 * transactions and programmatic rollback requests.
	 * Delegates to {@code isRollbackOnly}, {@code doCommit}
	 * and {@code rollback}.
	 * @see ReactiveTransactionStatus#isRollbackOnly()
	 * @see #doCommit
	 * @see #rollback
	 */
	@Override
	public final Mono<Void> commit(ReactiveTransactionStatus status) throws TransactionException {
		if (status.isCompleted()) {
			return Mono.error(new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction"));
		}

		return ReactiveTransactionSynchronizationManager.currentTransaction().flatMap(synchronizationManager -> {

			DefaultReactiveTransactionStatus defStatus = (DefaultReactiveTransactionStatus) status;
			if (defStatus.isLocalRollbackOnly()) {
				if (defStatus.isDebug()) {
					logger.debug("Transactional code has requested rollback");
				}
				return processRollback(synchronizationManager, defStatus, false);
			}

			if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
				if (defStatus.isDebug()) {
					logger.debug("Global transaction is marked as rollback-only but transactional code requested commit");
				}
				return processRollback(synchronizationManager, defStatus, true);
			}

			return processCommit(synchronizationManager, defStatus);
		});
	}

	/**
	 * Process an actual commit.
	 * Rollback-only flags have already been checked and applied.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @throws TransactionException in case of commit failure
	 */
	private Mono<Void> processCommit(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status) throws TransactionException {

		AtomicBoolean beforeCompletionInvoked = new AtomicBoolean(false);
		AtomicBoolean unexpectedRollback = new AtomicBoolean(false);

		Mono<Object> commit = prepareForCommit(synchronizationManager, status)
				.then(triggerBeforeCommit(synchronizationManager, status))
				.then(triggerBeforeCompletion(synchronizationManager, status))
				.then(Mono.defer(() -> {

					beforeCompletionInvoked.set(true);

					if (status.isNewTransaction()) {
						if (status.isDebug()) {
							logger.debug("Initiating transaction commit");
						}
						unexpectedRollback.set(status.isGlobalRollbackOnly());
						return doCommit(synchronizationManager, status);
					} else if (isFailEarlyOnGlobalRollbackOnly()) {
						unexpectedRollback.set(status.isGlobalRollbackOnly());
					}

					return Mono.empty();
				})).then(Mono.defer(() -> {

					// Throw UnexpectedRollbackException if we have a global rollback-only
					// marker but still didn't get a corresponding exception from commit.
					if (unexpectedRollback.get()) {
						return Mono.error(new UnexpectedRollbackException(
								"Transaction silently rolled back because it has been marked as rollback-only"));
					}

					return Mono.empty();
				}).onErrorResume(e -> {

					Mono<Object> propagateException = Mono.error(e);
					if (ErrorPredicates.UnexpectedRollback.test(e)) {
						return triggerAfterCompletion(synchronizationManager, status, ReactiveTransactionSynchronization.STATUS_ROLLED_BACK).then(propagateException);
					}

					if (ErrorPredicates.TransactionException.test(e)) {

						Mono<Void> mono;
						// can only be caused by doCommit
						if (isRollbackOnCommitFailure()) {
							mono = doRollbackOnCommitException(synchronizationManager, status, e);
						} else {
							mono = triggerAfterCompletion(synchronizationManager, status, ReactiveTransactionSynchronization.STATUS_UNKNOWN);
						}
						return mono.then(propagateException);
					}

					if (ErrorPredicates.RuntimeOrError.test(e)) {

						Mono<Void> mono;
						if (!beforeCompletionInvoked.get()) {
							mono = triggerBeforeCompletion(synchronizationManager, status);
						} else {
							mono = Mono.empty();
						}
						return mono.then(doRollbackOnCommitException(synchronizationManager, status, e)).then(propagateException);
					}

					return propagateException;
				})).then(Mono.defer((() -> {

					// Trigger afterCommit callbacks, with an exception thrown there
					// propagated to callers but the transaction still considered as committed.

					return triggerAfterCommit(synchronizationManager, status).onErrorResume(e -> {
						return triggerAfterCompletion(synchronizationManager, status, ReactiveTransactionSynchronization.STATUS_COMMITTED).then(Mono.error(e));
					}).then(triggerAfterCompletion(synchronizationManager, status, ReactiveTransactionSynchronization.STATUS_COMMITTED));
				})));

		return commit
				.onErrorResume((e) -> {
					return cleanupAfterCompletion(synchronizationManager, status).then(Mono.error(e));
				}).then(cleanupAfterCompletion(synchronizationManager, status));
	}

	/**
	 * This implementation of rollback handles participating in existing
	 * transactions. Delegates to {@code doRollback} and
	 * {@code doSetRollbackOnly}.
	 * @see #doRollback
	 * @see #doSetRollbackOnly
	 */
	@Override
	public final Mono<Void> rollback(ReactiveTransactionStatus status) throws TransactionException {
		if (status.isCompleted()) {
			return Mono.error(new IllegalTransactionStateException(
					"Transaction is already completed - do not call commit or rollback more than once per transaction"));
		}

		return ReactiveTransactionSynchronizationManager.currentTransaction().flatMap(synchronizationManager -> {
			DefaultReactiveTransactionStatus defStatus = (DefaultReactiveTransactionStatus) status;
			return processRollback(synchronizationManager, defStatus, false);
		});
	}

	/**
	 * Process an actual rollback.
	 * The completed flag has already been checked.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @throws TransactionException in case of rollback failure
	 */
	private Mono<Void> processRollback(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status, boolean unexpected) {
		AtomicBoolean unexpectedRollback = new AtomicBoolean(unexpected);

		return triggerBeforeCompletion(synchronizationManager, status)
				.then(Mono.defer(() -> {

			if (status.isNewTransaction()) {
				if (status.isDebug()) {
					logger.debug("Initiating transaction rollback");
				}
				return doRollback(synchronizationManager, status);
			} else {

				Mono<Void> beforeCompletion = Mono.empty();
				// Participating in larger transaction
				if (status.hasTransaction()) {
					if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {
						if (status.isDebug()) {
							logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
						}
						beforeCompletion = doSetRollbackOnly(synchronizationManager, status);
					} else {
						if (status.isDebug()) {
							logger.debug("Participating transaction failed - letting transaction originator decide on rollback");
						}
					}
				} else {
					logger.debug("Should roll back transaction but cannot - no transaction available");
				}

				return beforeCompletion.doOnSuccess(ignore -> {

					// Unexpected rollback only matters here if we're asked to fail early
					if (!isFailEarlyOnGlobalRollbackOnly()) {
						unexpectedRollback.set(false);
					}
				});
			}
		})).onErrorResume(ErrorPredicates.RuntimeOrError, e -> triggerAfterCompletion(synchronizationManager,
				status, ReactiveTransactionSynchronization.STATUS_UNKNOWN)
				.then(Mono.error(e)))
				.then(Mono.defer(() -> {

			Mono<Void> afterCompletion = triggerAfterCompletion(synchronizationManager, status, ReactiveTransactionSynchronization.STATUS_ROLLED_BACK);

			// Raise UnexpectedRollbackException if we had a global rollback-only marker
			if (unexpectedRollback.get()) {
				return afterCompletion.then(Mono.error(new UnexpectedRollbackException(
						"Transaction rolled back because it has been marked as rollback-only")));
			}
			return afterCompletion;
		})).onErrorResume((e) -> cleanupAfterCompletion(synchronizationManager, status)
						.then(Mono.error(e)))
			.then(cleanupAfterCompletion(synchronizationManager, status));
	}

	/**
	 * Invoke {@code doRollback}, handling rollback exceptions properly.
	 *
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @param ex the thrown application exception or error
	 * @throws TransactionException in case of rollback failure
	 * @see #doRollback
	 */
	private Mono<Void> doRollbackOnCommitException(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status, Throwable ex) throws TransactionException {

		return Mono.defer(() -> {

			if (status.isNewTransaction()) {
				if (status.isDebug()) {
					logger.debug("Initiating transaction rollback after commit exception", ex);
				}
				return doRollback(synchronizationManager, status);
			} else if (status.hasTransaction() && isGlobalRollbackOnParticipationFailure()) {
				if (status.isDebug()) {
					logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
				}
				return doSetRollbackOnly(synchronizationManager, status);
			}

			return Mono.empty();
		}).onErrorResume(ErrorPredicates.RuntimeOrError, (rbex) -> {

			logger.error("Commit exception overridden by rollback exception", ex);
			return triggerAfterCompletion(synchronizationManager, status, ReactiveTransactionSynchronization.STATUS_UNKNOWN)
				.then(Mono.error(rbex));
		}).then(triggerAfterCompletion(synchronizationManager, status, ReactiveTransactionSynchronization.STATUS_ROLLED_BACK));
	}


	/**
	 * Trigger {@code beforeCommit} callbacks.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 */
	protected final Mono<Void> triggerBeforeCommit(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status) {
		if (status.isNewSynchronization()) {
			if (status.isDebug()) {
				logger.trace("Triggering beforeCommit synchronization");
			}
			return ReactiveTransactionSynchronizationUtils.triggerBeforeCommit(synchronizationManager.getSynchronizations(), status.isReadOnly());
		}

		return Mono.empty();
	}

	/**
	 * Trigger {@code beforeCompletion} callbacks.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 */
	protected final Mono<Void> triggerBeforeCompletion(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status) {
		if (status.isNewSynchronization()) {
			if (status.isDebug()) {
				logger.trace("Triggering beforeCompletion synchronization");
			}
			return ReactiveTransactionSynchronizationUtils.triggerBeforeCompletion(synchronizationManager.getSynchronizations());
		}

		return Mono.empty();
	}

	/**
	 * Trigger {@code afterCommit} callbacks.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 */
	private Mono<Void> triggerAfterCommit(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status) {
		if (status.isNewSynchronization()) {
			if (status.isDebug()) {
				logger.trace("Triggering afterCommit synchronization");
			}
			return ReactiveTransactionSynchronizationUtils.invokeAfterCommit(synchronizationManager.getSynchronizations());
		}

		return Mono.empty();
	}

	/**
	 * Trigger {@code afterCompletion} callbacks.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @param completionStatus completion status according to ReactiveTransactionSynchronization constants
	 */
	private Mono<Void> triggerAfterCompletion(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status, int completionStatus) {
		if (status.isNewSynchronization()) {
			List<ReactiveTransactionSynchronization> synchronizations = synchronizationManager.getSynchronizations();
			synchronizationManager.clearSynchronization();
			if (!status.hasTransaction() || status.isNewTransaction()) {
				if (status.isDebug()) {
					logger.trace("Triggering afterCompletion synchronization");
				}
				// No transaction or new transaction for the current scope ->
				// invoke the afterCompletion callbacks immediately
				return invokeAfterCompletion(synchronizationManager, synchronizations, completionStatus);
			} else if (!synchronizations.isEmpty()) {
				// Existing transaction that we participate in, controlled outside
				// of the scope of this Spring transaction manager -> try to register
				// an afterCompletion callback with the existing (JTA) transaction.
				return registerAfterCompletionWithExistingTransaction(synchronizationManager, status.getTransaction(), synchronizations);
			}
		}

		return Mono.empty();
	}

	/**
	 * Actually invoke the {@code afterCompletion} methods of the
	 * given Spring ReactiveTransactionSynchronization objects.
	 * <p>To be called by this abstract manager itself, or by special implementations
	 * of the {@code registerAfterCompletionWithExistingTransaction} callback.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param synchronizations a List of ReactiveTransactionSynchronization objects
	 * @param completionStatus the completion status according to the
	 * constants in the ReactiveTransactionSynchronization interface
	 * @see #registerAfterCompletionWithExistingTransaction(ReactiveTransactionSynchronizationManager, Object, List)
	 * @see ReactiveTransactionSynchronization#STATUS_COMMITTED
	 * @see ReactiveTransactionSynchronization#STATUS_ROLLED_BACK
	 * @see ReactiveTransactionSynchronization#STATUS_UNKNOWN
	 */
	protected final Mono<Void> invokeAfterCompletion(ReactiveTransactionSynchronizationManager synchronizationManager, List<ReactiveTransactionSynchronization> synchronizations, int completionStatus) {
		return ReactiveTransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
	}

	/**
	 * Clean up after completion, clearing synchronization if necessary,
	 * and invoking doCleanupAfterCompletion.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status object representing the transaction
	 * @see #doCleanupAfterCompletion
	 */
	private Mono<Void> cleanupAfterCompletion(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status) {

		return Mono.defer(() -> {

			status.setCompleted();
			if (status.isNewSynchronization()) {
				synchronizationManager.clear();
			}
			if (status.isNewTransaction()) {
				doCleanupAfterCompletion(synchronizationManager, status.getTransaction());
			}
			if (status.getSuspendedResources() != null) {
				if (status.isDebug()) {
					logger.debug("Resuming suspended transaction after completion of inner transaction");
				}
				Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
				return resume(synchronizationManager, transaction, (SuspendedResourcesHolder) status.getSuspendedResources());
			}

			return Mono.empty();
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
	 * @throws org.springframework.transaction.CannotCreateTransactionException if transaction support is not available
	 * @throws TransactionException in case of lookup or system errors
	 * @see #doBegin
	 * @see #doCommit
	 * @see #doRollback
	 * @see DefaultReactiveTransactionStatus#getTransaction
	 */
	protected abstract Object doGetTransaction(ReactiveTransactionSynchronizationManager synchronizationManager) throws TransactionException;

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
	 * @param transaction transaction object returned by doGetTransaction
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
	 * <p>A special scenario is a nested transaction without savepoint: If
	 * {@code useSavepointForNestedTransaction()} returns "false", this method
	 * will be called to start a nested transaction when necessary. In such a context,
	 * there will be an active transaction: The implementation of this method has
	 * to detect this and start an appropriate nested transaction.
	 * @param synchronizationManager the synchronization manager bound to the new transaction
	 * @param transaction transaction object returned by {@code doGetTransaction}
	 * @param definition a TransactionDefinition instance, describing propagation
	 * behavior, isolation level, read-only flag, timeout, and transaction name
	 * @throws TransactionException in case of creation or system errors
	 */
	protected abstract Mono<Void> doBegin(ReactiveTransactionSynchronizationManager synchronizationManager, Object transaction, TransactionDefinition definition)
			throws TransactionException;

	/**
	 * Suspend the resources of the current transaction.
	 * Transaction synchronization will already have been suspended.
	 * <p>The default implementation throws a TransactionSuspensionNotSupportedException,
	 * assuming that transaction suspension is generally not supported.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction transaction object returned by {@code doGetTransaction}
	 * @return an object that holds suspended resources
	 * (will be kept unexamined for passing it into doResume)
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException if suspending is not supported by the transaction manager implementation
	 * @throws TransactionException in case of system errors
	 * @see #doResume
	 */
	protected Mono<Object> doSuspend(ReactiveTransactionSynchronizationManager synchronizationManager, Object transaction) throws TransactionException {
		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * Resume the resources of the current transaction.
	 * Transaction synchronization will be resumed afterwards.
	 * <p>The default implementation throws a TransactionSuspensionNotSupportedException,
	 * assuming that transaction suspension is generally not supported.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction transaction object returned by {@code doGetTransaction}
	 * @param suspendedResources the object that holds suspended resources,
	 * as returned by doSuspend
	 * @throws org.springframework.transaction.TransactionSuspensionNotSupportedException if resuming is not supported by the transaction manager implementation
	 * @throws TransactionException in case of system errors
	 * @see #doSuspend
	 */
	protected Mono<Void> doResume(ReactiveTransactionSynchronizationManager synchronizationManager, @Nullable Object transaction, Object suspendedResources) throws TransactionException {
		throw new TransactionSuspensionNotSupportedException(
				"Transaction manager [" + getClass().getName() + "] does not support transaction suspension");
	}

	/**
	 * Return whether to call {@code doCommit} on a transaction that has been
	 * marked as rollback-only in a global fashion.
	 * <p>Does not apply if an application locally sets the transaction to rollback-only
	 * via the TransactionStatus, but only to the transaction itself being marked as
	 * rollback-only by the transaction coordinator.
	 * <p>Default is "false": Local transaction strategies usually don't hold the rollback-only
	 * marker in the transaction itself, therefore they can't handle rollback-only transactions
	 * as part of transaction commit. Hence, AbstractReactiveTransactionManager will trigger
	 * a rollback in that case, throwing an UnexpectedRollbackException afterwards.
	 * <p>Override this to return "true" if the concrete transaction manager expects a
	 * {@code doCommit} call even for a rollback-only transaction, allowing for
	 * special handling there. This will, for example, be the case for JTA, where
	 * {@code UserTransaction.commit} will check the read-only flag itself and
	 * throw a corresponding RollbackException, which might include the specific reason
	 * (such as a transaction timeout).
	 * <p>If this method returns "true" but the {@code doCommit} implementation does not
	 * throw an exception, this transaction manager will throw an UnexpectedRollbackException
	 * itself.
	 * @see #doCommit
	 * @see DefaultReactiveTransactionStatus#isGlobalRollbackOnly()
	 * @see DefaultReactiveTransactionStatus#isLocalRollbackOnly()
	 * @see ReactiveTransactionStatus#setRollbackOnly()
	 * @see org.springframework.transaction.UnexpectedRollbackException
	 */
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return false;
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
	protected Mono<Void> prepareForCommit(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status) {
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
	 * @see DefaultReactiveTransactionStatus#getTransaction
	 */
	protected abstract Mono<Void> doCommit(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status) throws TransactionException;

	/**
	 * Perform an actual rollback of the given transaction.
	 * <p>An implementation does not need to check the "new transaction" flag;
	 * this will already have been handled before. Usually, a straight rollback
	 * will be performed on the transaction object contained in the passed-in status.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param status the status representation of the transaction
	 * @throws TransactionException in case of system errors
	 * @see DefaultReactiveTransactionStatus#getTransaction
	 */
	protected abstract Mono<Void> doRollback(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status) throws TransactionException;

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
	protected Mono<Void> doSetRollbackOnly(ReactiveTransactionSynchronizationManager synchronizationManager, DefaultReactiveTransactionStatus status) throws TransactionException {
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
	 * @param transaction transaction object returned by {@code doGetTransaction}
	 * @param synchronizations a List of ReactiveTransactionSynchronization objects
	 * @throws TransactionException in case of system errors
	 * @see #invokeAfterCompletion(ReactiveTransactionSynchronizationManager, List, int)
	 * @see ReactiveTransactionSynchronization#afterCompletion(int)
	 * @see ReactiveTransactionSynchronization#STATUS_UNKNOWN
	 */
	protected Mono<Void> registerAfterCompletionWithExistingTransaction(ReactiveTransactionSynchronizationManager synchronizationManager,
																		Object transaction, List<ReactiveTransactionSynchronization> synchronizations) throws TransactionException {

		logger.debug("Cannot register Spring after-completion synchronization with existing transaction - " +
				"processing Spring after-completion callbacks immediately, with outcome status 'unknown'");
		return invokeAfterCompletion(synchronizationManager, synchronizations, ReactiveTransactionSynchronization.STATUS_UNKNOWN);
	}

	/**
	 * Cleanup resources after transaction completion.
	 * <p>Called after {@code doCommit} and {@code doRollback} execution,
	 * on any outcome. The default implementation does nothing.
	 * <p>Should not throw any exceptions but just issue warnings on errors.
	 * @param synchronizationManager the synchronization manager bound to the current transaction
	 * @param transaction transaction object returned by {@code doGetTransaction}
	 */
	protected Mono<Void> doCleanupAfterCompletion(ReactiveTransactionSynchronizationManager synchronizationManager, Object transaction) {
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
		private List<ReactiveTransactionSynchronization> suspendedSynchronizations;

		@Nullable
		private String name;

		private boolean readOnly;

		@Nullable
		private Integer isolationLevel;

		private boolean wasActive;

		private SuspendedResourcesHolder(Object suspendedResources) {
			this.suspendedResources = suspendedResources;
		}

		private SuspendedResourcesHolder(
				@Nullable Object suspendedResources, List<ReactiveTransactionSynchronization> suspendedSynchronizations,
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
	enum ErrorPredicates implements Predicate<Throwable> {

		/**
		 * Predicate matching {@link RuntimeException} or {@link Error}.
		 */
		RuntimeOrError {
			@Override
			public boolean test(Throwable throwable) {
				return throwable instanceof RuntimeException || throwable instanceof Error;
			}
		},

		/**
		 * Predicate matching {@link TransactionException}.
		 */
		TransactionException {
			@Override
			public boolean test(Throwable throwable) {
				return throwable instanceof TransactionException;
			}
		},

		/**
		 * Predicate matching {@link UnexpectedRollbackException}.
		 */
		UnexpectedRollback {
			@Override
			public boolean test(Throwable throwable) {
				return throwable instanceof UnexpectedRollbackException;
			}
		};

		@Override
		public abstract boolean test(Throwable throwable);
	}

}

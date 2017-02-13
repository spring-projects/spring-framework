/*
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.transaction.chained;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.HeuristicCompletionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.util.Assert;

/**
 * {@link PlatformTransactionManager} implementation that orchestrates transaction creation, commits and rollbacks to a
 * list of delegates. Using this implementation assumes that errors causing a transaction rollback will usually happen
 * before the transaction completion or during the commit of the most inner {@link PlatformTransactionManager}.
 * <p />
 * The configured instances will start transactions in the order given and commit/rollback in <em>reverse</em> order,
 * which means the {@link PlatformTransactionManager} most likely to break the transaction should be the <em>last</em>
 * in the list configured. A {@link PlatformTransactionManager} throwing an exception during commit will automatically
 * cause the remaining transaction managers to roll back instead of committing.
 *
 * @author Michael Hunger
 * @author Oliver Gierke
 * @since 1.6
 */
public class ChainedTransactionManager implements PlatformTransactionManager {

	protected transient Log logger = LogFactory.getLog(getClass());

	private final List<PlatformTransactionManager> transactionManagers;
	private final SynchronizationManager synchronizationManager;

	/**
	 * Creates a new {@link ChainedTransactionManager} delegating to the given {@link PlatformTransactionManager}s.
	 *
	 * @param transactionManagers must not be {@literal null} or empty.
	 */
	public ChainedTransactionManager(PlatformTransactionManager... transactionManagers) {
		this(SpringTransactionSynchronizationManager.INSTANCE, transactionManagers);
	}

	/**
	 * Creates a new {@link ChainedTransactionManager} using the given {@link SynchronizationManager} and
	 * {@link PlatformTransactionManager}s.
	 *
	 * @param synchronizationManager must not be {@literal null}.
	 * @param transactionManagers must not be {@literal null} or empty.
	 */
	ChainedTransactionManager(SynchronizationManager synchronizationManager,
			PlatformTransactionManager... transactionManagers) {

		Assert.notNull(synchronizationManager, "SynchronizationManager must not be null!");
		Assert.notNull(transactionManagers, "Transaction managers must not be null!");
		Assert.isTrue(transactionManagers.length > 0, "At least one PlatformTransactionManager must be given!");

		this.synchronizationManager = synchronizationManager;
		this.transactionManagers = asList(transactionManagers);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#getTransaction(org.springframework.transaction.TransactionDefinition)
	 */
	public MultiTransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {

		MultiTransactionStatus mts = new MultiTransactionStatus(transactionManagers.get(0));

		if (!synchronizationManager.isSynchronizationActive()) {
			synchronizationManager.initSynchronization();
			mts.setNewSynchonization();
		}

		try {

			for (PlatformTransactionManager transactionManager : transactionManagers) {
				mts.registerTransactionManager(definition, transactionManager);
			}

		} catch (Exception ex) {

			Map<PlatformTransactionManager, TransactionStatus> transactionStatuses = mts.getTransactionStatuses();

			for (PlatformTransactionManager transactionManager : transactionManagers) {
				try {
					if (transactionStatuses.get(transactionManager) != null) {
						transactionManager.rollback(transactionStatuses.get(transactionManager));
					}
				} catch (Exception ex2) {
					logger.warn("Rollback exception (" + transactionManager + ") " + ex2.getMessage(), ex2);
				}
			}

			if (mts.isNewSynchonization()) {
				synchronizationManager.clearSynchronization();
			}

			throw new CannotCreateTransactionException(ex.getMessage(), ex);
		}

		return mts;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#commit(org.springframework.transaction.TransactionStatus)
	 */
	public void commit(TransactionStatus status) throws TransactionException {

		MultiTransactionStatus multiTransactionStatus = (MultiTransactionStatus) status;

		boolean commit = true;
		Exception commitException = null;
		PlatformTransactionManager commitExceptionTransactionManager = null;

		for (PlatformTransactionManager transactionManager : reverse(transactionManagers)) {

			if (commit) {

				try {
					multiTransactionStatus.commit(transactionManager);
				} catch (Exception ex) {
					commit = false;
					commitException = ex;
					commitExceptionTransactionManager = transactionManager;
				}

			} else {

				// after unsucessfull commit we must try to rollback remaining transaction managers

				try {
					multiTransactionStatus.rollback(transactionManager);
				} catch (Exception ex) {
					logger.warn("Rollback exception (after commit) (" + transactionManager + ") " + ex.getMessage(), ex);
				}
			}
		}

		if (multiTransactionStatus.isNewSynchonization()) {
			synchronizationManager.clearSynchronization();
		}

		if (commitException != null) {
			boolean firstTransactionManagerFailed = commitExceptionTransactionManager == getLastTransactionManager();
			int transactionState = firstTransactionManagerFailed ? HeuristicCompletionException.STATE_ROLLED_BACK
					: HeuristicCompletionException.STATE_MIXED;
			throw new HeuristicCompletionException(transactionState, commitException);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.transaction.PlatformTransactionManager#rollback(org.springframework.transaction.TransactionStatus)
	 */
	public void rollback(TransactionStatus status) throws TransactionException {

		Exception rollbackException = null;
		PlatformTransactionManager rollbackExceptionTransactionManager = null;

		MultiTransactionStatus multiTransactionStatus = (MultiTransactionStatus) status;

		for (PlatformTransactionManager transactionManager : reverse(transactionManagers)) {
			try {
				multiTransactionStatus.rollback(transactionManager);
			} catch (Exception ex) {
				if (rollbackException == null) {
					rollbackException = ex;
					rollbackExceptionTransactionManager = transactionManager;
				} else {
					logger.warn("Rollback exception (" + transactionManager + ") " + ex.getMessage(), ex);
				}
			}
		}

		if (multiTransactionStatus.isNewSynchonization()) {
			synchronizationManager.clearSynchronization();
		}

		if (rollbackException != null) {
			throw new UnexpectedRollbackException("Rollback exception, originated at (" + rollbackExceptionTransactionManager
					+ ") " + rollbackException.getMessage(), rollbackException);
		}
	}

	private <T> Iterable<T> reverse(Collection<T> collection) {

		List<T> list = new ArrayList<T>(collection);
		Collections.reverse(list);
		return list;
	}

	private PlatformTransactionManager getLastTransactionManager() {
		return transactionManagers.get(lastTransactionManagerIndex());
	}

	private int lastTransactionManagerIndex() {
		return transactionManagers.size() - 1;
	}
}

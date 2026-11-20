/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.support;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.transaction.TransactionStatus;

/**
 * Simple convenience class for TransactionCallback implementation.
 * Allows for implementing a doInTransaction version without result,
 * i.e. without the need for a return statement.
 *
 * @author Juergen Hoeller
 * @since 28.03.2003
 * @see TransactionTemplate
 * @deprecated as of 7.0, superseded by {@link TransactionOperations#executeWithoutResult(Consumer)}
 */
@Deprecated(since = "7.0")
public abstract class TransactionCallbackWithoutResult implements TransactionCallback<@Nullable Object> {

	@Override
	public final @Nullable Object doInTransaction(TransactionStatus status) {
		doInTransactionWithoutResult(status);
		return null;
	}

	/**
	 * Gets called by {@code TransactionTemplate.execute} within a transactional
	 * context. Does not need to care about transactions itself, although it can retrieve
	 * and influence the status of the current transaction via the given status object,
	 * for example, setting rollback-only.
	 * <p>A RuntimeException thrown by the callback is treated as application
	 * exception that enforces a rollback. An exception gets propagated to the
	 * caller of the template.
	 * <p>Note when using JTA: JTA transactions only work with transactional
	 * JNDI resources, so implementations need to use such resources if they
	 * want transaction support.
	 * @param status associated transaction status
	 * @see TransactionTemplate#execute
	 */
	protected abstract void doInTransactionWithoutResult(TransactionStatus status);

}

/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.transaction.TransactionStatus;

/**
 * Simple convenience class for TransactionCallback implementation.
 * Allows for implementing a doInTransaction version without result,
 * i.e. without the need for a return statement.
 *
 * @author Juergen Hoeller
 * @since 28.03.2003
 * @see TransactionTemplate
 */
public abstract class TransactionCallbackWithoutResult implements TransactionCallback<Object> {

	@Override
	public final Object doInTransaction(TransactionStatus status) {
		doInTransactionWithoutResult(status);
		return null;
	}

	/**
	 * Gets called by {@code TransactionTemplate.execute} within a transactional
	 * context. Does not need to care about transactions itself, although it can retrieve
	 * and influence the status of the current transaction via the given status object,
	 * e.g. setting rollback-only.
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

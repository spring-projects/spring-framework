/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.Flushable;

/**
 * Interface to be implemented by transaction objects that are able to
 * return an internal rollback-only marker, typically from another
 * transaction that has participated and marked it as rollback-only.
 *
 * <p>Autodetected by DefaultTransactionStatus, to always return a
 * current rollbackOnly flag even if not resulting from the current
 * TransactionStatus.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see DefaultTransactionStatus#isRollbackOnly
 */
public interface SmartTransactionObject extends Flushable {

	/**
	 * Return whether the transaction is internally marked as rollback-only.
	 * Can, for example, check the JTA UserTransaction.
	 * @see javax.transaction.UserTransaction#getStatus
	 * @see javax.transaction.Status#STATUS_MARKED_ROLLBACK
	 */
	boolean isRollbackOnly();

	/**
	 * Flush the underlying sessions to the datastore, if applicable:
	 * for example, all affected Hibernate/JPA sessions.
	 */
	@Override
	void flush();

}

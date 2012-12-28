/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.jta;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * JTA transaction object, representing a {@link javax.transaction.UserTransaction}.
 * Used as transaction object by Spring's {@link JtaTransactionManager}.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see JtaTransactionManager
 * @see javax.transaction.UserTransaction
 */
public class JtaTransactionObject implements SmartTransactionObject {

	private final UserTransaction userTransaction;


	/**
	 * Create a new JtaTransactionObject for the given JTA UserTransaction.
	 * @param userTransaction the JTA UserTransaction for the current transaction
	 * (either a shared object or retrieved through a fresh per-transaction lookuip)
	 */
	public JtaTransactionObject(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	/**
	 * Return the JTA UserTransaction object for the current transaction.
	 */
	public final UserTransaction getUserTransaction() {
		return this.userTransaction;
	}


	/**
	 * This implementation checks the UserTransaction's rollback-only flag.
	 */
	@Override
	public boolean isRollbackOnly() {
		if (this.userTransaction == null) {
			return false;
		}
		try {
			int jtaStatus = this.userTransaction.getStatus();
			return (jtaStatus == Status.STATUS_MARKED_ROLLBACK || jtaStatus == Status.STATUS_ROLLEDBACK);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on getStatus", ex);
		}
	}

	/**
	 * This implementation triggers flush callbacks,
	 * assuming that they will flush all affected ORM sessions.
	 * @see org.springframework.transaction.support.TransactionSynchronization#flush()
	 */
	@Override
	public void flush() {
		TransactionSynchronizationUtils.triggerFlush();
	}

}

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

package org.springframework.orm.hibernate3;

import java.util.Properties;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Transaction;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.transaction.JDBCTransaction;
import org.hibernate.transaction.TransactionFactory;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Spring-aware implementation of the Hibernate TransactionFactory interface, aware of
 * Spring-synchronized transactions (in particular Spring-managed JTA transactions)
 * and asking for default release mode ON_CLOSE. Otherwise identical to Hibernate's
 * default {@link org.hibernate.transaction.JDBCTransactionFactory} implementation.
 *
 * @author Juergen Hoeller
 * @since 2.5.4
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 * @see org.hibernate.transaction.JDBCTransactionFactory
 */
public class SpringTransactionFactory implements TransactionFactory {

	/**
	 * Sets connection release mode "on_close" as default.
	 * <p>This was the case for Hibernate 3.0; Hibernate 3.1 changed
	 * it to "auto" (i.e. "after_statement" or "after_transaction").
	 * However, for Spring's resource management (in particular for
	 * HibernateTransactionManager), "on_close" is the better default.
	 */
	@Override
	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.ON_CLOSE;
	}

	@Override
	public Transaction createTransaction(JDBCContext jdbcContext, Context transactionContext) {
		return new JDBCTransaction(jdbcContext, transactionContext);
	}

	@Override
	public void configure(Properties props) {
	}

	@Override
	public boolean isTransactionManagerRequired() {
		return false;
	}

	@Override
	public boolean areCallbacksLocalToHibernateTransactions() {
		return true;
	}

	@Override
	public boolean isTransactionInProgress(
			JDBCContext jdbcContext, Context transactionContext, Transaction transaction) {

		return (transaction != null && transaction.isActive()) ||
				TransactionSynchronizationManager.isActualTransactionActive();
	}

}

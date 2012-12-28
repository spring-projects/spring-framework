/*
 * Copyright 2002-2008 the original author or authors.
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

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.transaction.TransactionManagerLookup;

/**
 * Implementation of Hibernate's {@link TransactionManagerLookup} interface
 * that returns a Spring-managed JTA {@link TransactionManager}, determined
 * by LocalSessionFactoryBean's "jtaTransactionManager" property.
 *
 * <p>The main advantage of this TransactionManagerLookup is that it avoids
 * double configuration of JTA specifics. A single TransactionManager bean can
 * be used for both JtaTransactionManager and LocalSessionFactoryBean, with no
 * JTA setup in Hibernate configuration.
 *
 * <p>Alternatively, use Hibernate's own TransactionManagerLookup implementations:
 * Spring's JtaTransactionManager only requires a TransactionManager for suspending
 * and resuming transactions, so you might not need to apply such special Spring
 * configuration at all.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see LocalSessionFactoryBean#setJtaTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
 */
public class LocalTransactionManagerLookup implements TransactionManagerLookup {

	private final TransactionManager transactionManager;


	public LocalTransactionManagerLookup() {
		TransactionManager tm = LocalSessionFactoryBean.getConfigTimeTransactionManager();
		// absolutely needs thread-bound TransactionManager to initialize
		if (tm == null) {
			throw new IllegalStateException("No JTA TransactionManager found - " +
				"'jtaTransactionManager' property must be set on LocalSessionFactoryBean");
		}
		this.transactionManager = tm;
	}

	public TransactionManager getTransactionManager(Properties props) {
		return this.transactionManager;
	}

	public String getUserTransactionName() {
		return null;
	}

	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}

}

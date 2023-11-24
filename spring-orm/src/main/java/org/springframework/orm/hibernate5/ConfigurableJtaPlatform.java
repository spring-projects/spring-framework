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

package org.springframework.orm.hibernate5;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

import org.springframework.lang.Nullable;
import org.springframework.transaction.jta.UserTransactionAdapter;
import org.springframework.util.Assert;

/**
 * Implementation of Hibernate 5's JtaPlatform SPI, exposing passed-in {@link TransactionManager},
 * {@link UserTransaction} and {@link TransactionSynchronizationRegistry} references.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
@SuppressWarnings("serial")
class ConfigurableJtaPlatform implements JtaPlatform {

	private final TransactionManager transactionManager;

	private final UserTransaction userTransaction;

	@Nullable
	private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;


	/**
	 * Create a new ConfigurableJtaPlatform instance with the given
	 * JTA TransactionManager and optionally a given UserTransaction.
	 * @param tm the JTA TransactionManager reference (required)
	 * @param ut the JTA UserTransaction reference (optional)
	 * @param tsr the JTA 1.1 TransactionSynchronizationRegistry (optional)
	 */
	public ConfigurableJtaPlatform(TransactionManager tm, @Nullable UserTransaction ut,
			@Nullable TransactionSynchronizationRegistry tsr) {

		Assert.notNull(tm, "TransactionManager reference must not be null");
		this.transactionManager = tm;
		this.userTransaction = (ut != null ? ut : new UserTransactionAdapter(tm));
		this.transactionSynchronizationRegistry = tsr;
	}


	@Override
	public TransactionManager retrieveTransactionManager() {
		return this.transactionManager;
	}

	@Override
	public UserTransaction retrieveUserTransaction() {
		return this.userTransaction;
	}

	@Override
	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}

	@Override
	public boolean canRegisterSynchronization() {
		try {
			return (this.transactionManager.getStatus() == Status.STATUS_ACTIVE);
		}
		catch (SystemException ex) {
			throw new TransactionException("Could not determine JTA transaction status", ex);
		}
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		if (this.transactionSynchronizationRegistry != null) {
			this.transactionSynchronizationRegistry.registerInterposedSynchronization(synchronization);
		}
		else {
			try {
				this.transactionManager.getTransaction().registerSynchronization(synchronization);
			}
			catch (Exception ex) {
				throw new TransactionException("Could not access JTA Transaction to register synchronization", ex);
			}
		}
	}

	@Override
	public int getCurrentStatus() throws SystemException {
		return this.transactionManager.getStatus();
	}

}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.hibernate;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.jspecify.annotations.Nullable;

import org.springframework.transaction.jta.UserTransactionAdapter;
import org.springframework.util.Assert;

/**
 * Implementation of Hibernate's JtaPlatform SPI, exposing passed-in {@link TransactionManager},
 * {@link UserTransaction} and {@link TransactionSynchronizationRegistry} references.
 *
 * @author Juergen Hoeller
 * @since 7.0.4
 */
@SuppressWarnings("serial")
public class ConfigurableJtaPlatform implements JtaPlatform {

	private final TransactionManager transactionManager;

	private final UserTransaction userTransaction;

	private final @Nullable TransactionSynchronizationRegistry transactionSynchronizationRegistry;


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

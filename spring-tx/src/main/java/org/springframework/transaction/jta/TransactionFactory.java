/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.jta;

import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for creating JTA {@link jakarta.transaction.Transaction}
 * objects based on specified transactional characteristics.
 *
 * <p>The default implementation, {@link SimpleTransactionFactory}, simply
 * wraps a standard JTA {@link jakarta.transaction.TransactionManager}.
 * This strategy interface allows for more sophisticated implementations
 * that adapt to vendor-specific JTA extensions.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see jakarta.transaction.TransactionManager#getTransaction()
 * @see SimpleTransactionFactory
 * @see JtaTransactionManager
 */
public interface TransactionFactory {

	/**
	 * Create an active Transaction object based on the given name and timeout.
	 * @param name the transaction name (may be {@code null})
	 * @param timeout the transaction timeout (may be -1 for the default timeout)
	 * @return the active Transaction object (never {@code null})
	 * @throws NotSupportedException if the transaction manager does not support
	 * a transaction of the specified type
	 * @throws SystemException if the transaction manager failed to create the
	 * transaction
	 */
	Transaction createTransaction(@Nullable String name, int timeout) throws NotSupportedException, SystemException;

	/**
	 * Determine whether the underlying transaction manager supports XA transactions
	 * managed by a resource adapter (i.e. without explicit XA resource enlistment).
	 * <p>Typically {@code false}. Checked by
	 * {@link org.springframework.jca.endpoint.AbstractMessageEndpointFactory}
	 * in order to differentiate between invalid configuration and valid
	 * ResourceAdapter-managed transactions.
	 * @see jakarta.resource.spi.ResourceAdapter#endpointActivation
	 * @see jakarta.resource.spi.endpoint.MessageEndpointFactory#isDeliveryTransacted
	 */
	boolean supportsResourceAdapterManagedTransactions();

}

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

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

/**
 * Strategy interface for creating JTA {@link javax.transaction.Transaction}
 * objects based on specified transactional characteristics.
 *
 * <p>The default implementation, {@link SimpleTransactionFactory}, simply
 * wraps a standard JTA {@link javax.transaction.TransactionManager}.
 * This strategy interface allows for more sophisticated implementations
 * that adapt to vendor-specific JTA extensions.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see javax.transaction.TransactionManager#getTransaction()
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
	Transaction createTransaction(String name, int timeout) throws NotSupportedException, SystemException;

	/**
	 * Determine whether the underlying transaction manager supports XA transactions
	 * managed by a resource adapter (i.e. without explicit XA resource enlistment).
	 * <p>Typically {@code false}. Checked by
	 * {@link org.springframework.jca.endpoint.AbstractMessageEndpointFactory}
	 * in order to differentiate between invalid configuration and valid
	 * ResourceAdapter-managed transactions.
	 * @see javax.resource.spi.ResourceAdapter#endpointActivation
	 * @see javax.resource.spi.endpoint.MessageEndpointFactory#isDeliveryTransacted
	 */
	boolean supportsResourceAdapterManagedTransactions();

}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.support;

import org.springframework.transaction.TransactionDefinition;

/**
 * Extended variant of {@link TransactionDefinition}, indicating a resource transaction
 * and in particular whether the transactional resource is ready for local optimizations.
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @see ResourceTransactionManager
 */
public interface ResourceTransactionDefinition extends TransactionDefinition {

	/**
	 * Determine whether the transactional resource is ready for local optimizations.
	 * @return {@code true} if the resource is known to be entirely transaction-local,
	 * not affecting any operations outside the scope of the current transaction
	 * @see #isReadOnly()
	 */
	boolean isLocalResource();

}

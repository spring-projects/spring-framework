/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction;

/**
 * A static unmodifiable transaction definition.
 *
 * @author Juergen Hoeller
 * @since 5.2
 * @see TransactionDefinition#withDefaults()
 */
final class StaticTransactionDefinition implements TransactionDefinition {

	static final StaticTransactionDefinition INSTANCE = new StaticTransactionDefinition();

	private StaticTransactionDefinition() {
	}

}

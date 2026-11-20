/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.annotation;

/**
 * An enum for global rollback-on behavior.
 *
 * <p>Note that the default behavior matches the traditional behavior in
 * EJB CMT and JTA, with the latter having rollback rules similar to Spring.
 * A global switch to trigger a rollback on any exception affects Spring's
 * {@link Transactional} as well as {@link jakarta.transaction.Transactional}
 * but leaves the non-rule-based {@link jakarta.ejb.TransactionAttribute} as-is.
 *
 * @author Juergen Hoeller
 * @since 6.2
 * @see EnableTransactionManagement#rollbackOn()
 * @see org.springframework.transaction.interceptor.RuleBasedTransactionAttribute
 */
public enum RollbackOn {

	/**
	 * The default rollback-on behavior: rollback on
	 * {@link RuntimeException RuntimeExceptions} as well as {@link Error Errors}.
	 * @see org.springframework.transaction.interceptor.RollbackRuleAttribute#ROLLBACK_ON_RUNTIME_EXCEPTIONS
	 */
	RUNTIME_EXCEPTIONS,

	/**
	 * The alternative mode: rollback on all exceptions, including any checked
	 * {@link Exception}.
	 * @see org.springframework.transaction.interceptor.RollbackRuleAttribute#ROLLBACK_ON_ALL_EXCEPTIONS
	 */
	ALL_EXCEPTIONS

}

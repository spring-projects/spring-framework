/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.config;

/**
 * Configuration constants for internal sharing across subpackages.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 */
public abstract class TransactionManagementConfigUtils {

	/**
	 * The bean name of the internally managed transaction advisor (used when mode == PROXY).
	 */
	public static final String TRANSACTION_ADVISOR_BEAN_NAME =
			"org.springframework.transaction.config.internalTransactionAdvisor";

	/**
	 * The bean name of the internally managed transaction aspect (used when mode == ASPECTJ).
	 */
	public static final String TRANSACTION_ASPECT_BEAN_NAME =
			"org.springframework.transaction.config.internalTransactionAspect";

	/**
	 * The class name of the AspectJ transaction management aspect.
	 */
	public static final String TRANSACTION_ASPECT_CLASS_NAME =
			"org.springframework.transaction.aspectj.AnnotationTransactionAspect";

	/**
	 * The name of the AspectJ transaction management @{@code Configuration} class.
	 */
	public static final String TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.transaction.aspectj.AspectJTransactionManagementConfiguration";

	/**
	 * The bean name of the internally managed JTA transaction aspect (used when mode == ASPECTJ).
	 * @since 5.1
	 */
	public static final String JTA_TRANSACTION_ASPECT_BEAN_NAME =
			"org.springframework.transaction.config.internalJtaTransactionAspect";

	/**
	 * The class name of the AspectJ transaction management aspect.
	 * @since 5.1
	 */
	public static final String JTA_TRANSACTION_ASPECT_CLASS_NAME =
			"org.springframework.transaction.aspectj.JtaAnnotationTransactionAspect";

	/**
	 * The name of the AspectJ transaction management @{@code Configuration} class for JTA.
	 * @since 5.1
	 */
	public static final String JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.transaction.aspectj.AspectJJtaTransactionManagementConfiguration";

	/**
	 * The bean name of the internally managed TransactionalEventListenerFactory.
	 */
	public static final String TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME =
			"org.springframework.transaction.config.internalTransactionalEventListenerFactory";

}

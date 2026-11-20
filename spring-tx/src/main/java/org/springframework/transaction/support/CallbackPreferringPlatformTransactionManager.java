/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.support;

import org.jspecify.annotations.Nullable;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * Extension of the {@link org.springframework.transaction.PlatformTransactionManager}
 * interface, exposing a method for executing a given callback within a transaction.
 *
 * <p>Implementors of this interface automatically express a preference for
 * callbacks over programmatic {@code getTransaction}, {@code commit}
 * and {@code rollback} calls. Calling code may check whether a given
 * transaction manager implements this interface to choose to prepare a
 * callback instead of explicit transaction demarcation control.
 *
 * <p>Spring's {@link TransactionTemplate} and
 * {@link org.springframework.transaction.interceptor.TransactionInterceptor}
 * detect and use this PlatformTransactionManager variant automatically.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see TransactionTemplate
 * @see org.springframework.transaction.interceptor.TransactionInterceptor
 */
public interface CallbackPreferringPlatformTransactionManager extends PlatformTransactionManager {

	/**
	 * Execute the action specified by the given callback object within a transaction.
	 * <p>Allows for returning a result object created within the transaction, that is,
	 * a domain object or a collection of domain objects. A RuntimeException thrown
	 * by the callback is treated as a fatal exception that enforces a rollback.
	 * Such an exception gets propagated to the caller of the template.
	 * @param definition the definition for the transaction to wrap the callback in
	 * @param callback the callback object that specifies the transactional action
	 * @return a result object returned by the callback, or {@code null} if none
	 * @throws TransactionException in case of initialization, rollback, or system errors
	 * @throws RuntimeException if thrown by the TransactionCallback
	 */
	<T extends @Nullable Object> T execute(@Nullable TransactionDefinition definition, TransactionCallback<T> callback)
			throws TransactionException;

}

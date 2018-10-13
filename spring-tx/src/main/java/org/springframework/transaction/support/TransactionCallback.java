/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.transaction.support;

import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionStatus;

/**
 * Callback interface for transactional code. Used with {@link TransactionTemplate}'s
 * {@code execute} method, often as anonymous class within a method implementation.
 *
 * <p>Typically used to assemble various calls to transaction-unaware data access
 * services into a higher-level service method with transaction demarcation. As an
 * alternative, consider the use of declarative transaction demarcation (e.g. through
 * Spring's {@link org.springframework.transaction.annotation.Transactional} annotation).
 *
 * @author Juergen Hoeller
 * @since 17.03.2003
 * @see TransactionTemplate
 * @see CallbackPreferringPlatformTransactionManager
 * @param <T> the result type
 */
@FunctionalInterface
public interface TransactionCallback<T> {

	/**
	 * Gets called by {@link TransactionTemplate#execute} within a transactional context.
	 * Does not need to care about transactions itself, although it can retrieve and
	 * influence the status of the current transaction via the given status object,
	 * e.g. setting rollback-only.
	 * <p>Allows for returning a result object created within the transaction, i.e. a
	 * domain object or a collection of domain objects. A RuntimeException thrown by the
	 * callback is treated as application exception that enforces a rollback. Any such
	 * exception will be propagated to the caller of the template, unless there is a
	 * problem rolling back, in which case a TransactionException will be thrown.
	 * @param status associated transaction status
	 * @return a result object, or {@code null}
	 * @see TransactionTemplate#execute
	 * @see CallbackPreferringPlatformTransactionManager#execute
	 */
	@Nullable
	T doInTransaction(TransactionStatus status);

}

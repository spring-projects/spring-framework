/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.transaction;

/**
 * Representation of an ongoing {@link ReactiveTransactionManager} transaction.
 * This is currently a marker interface extending {@link TransactionExecution}
 * but may acquire further methods in a future revision.
 *
 * <p>Transactional code can use this to retrieve status information,
 * and to programmatically request a rollback (instead of throwing
 * an exception that causes an implicit rollback).
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.2
 * @see #setRollbackOnly()
 * @see ReactiveTransactionManager#getReactiveTransaction
 * @see org.springframework.transaction.reactive.TransactionCallback#doInTransaction
 */
public interface ReactiveTransaction extends TransactionExecution {

}

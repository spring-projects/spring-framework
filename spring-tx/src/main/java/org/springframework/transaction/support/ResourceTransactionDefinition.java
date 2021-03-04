/*
 * Copyright 2002-2018 the original author or authors.
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
	 * not affecting any operations outside of the scope of the current transaction
	 * @see #isReadOnly()
	 */
	boolean isLocalResource();

}

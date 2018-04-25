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

package org.springframework.cache.transaction;

import org.springframework.cache.support.AbstractCacheManager;

/**
 * Base class for CacheManager implementations that want to support built-in
 * awareness of Spring-managed transactions. This usually needs to be switched
 * on explicitly through the {@link TransactionAwareCacheDecorator#transactionAware} bean property.
 *
 * @author Juergen Hoeller
 * @see TransactionAwareCache
 * @see TransactionAwareCacheManagerProxy
 * @since 3.2
 */
public abstract class AbstractTransactionSupportingCacheManager extends AbstractCacheManager {

	/**
	 * Constructor that creates CacheManager with transactionAware flag set to false.
	 */
	public AbstractTransactionSupportingCacheManager() {
		this(false);
	}

	/**
	 * Constructor that creates CacheManager with the given transactionAware flag.
	 *
	 * @param transactionAware Set whether this CacheManager should expose transaction-aware Cache objects.
	 *                         <p>Default is "false". Set this to "true" to synchronize cache put/evict
	 *                         operations with ongoing Spring-managed transactions, performing the actual cache
	 *                         put/evict operation only in the after-commit phase of a successful transaction.
	 */
	public AbstractTransactionSupportingCacheManager(boolean transactionAware) {
		super.addCacheDecorator(new TransactionAwareCacheDecorator(transactionAware));
	}
}

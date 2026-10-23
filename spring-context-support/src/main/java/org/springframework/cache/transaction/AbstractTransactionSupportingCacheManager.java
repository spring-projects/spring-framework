/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.cache.transaction;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.AbstractCacheManager;

/**
 * Base class for CacheManager implementations that want to support built-in
 * awareness of Spring-managed transactions. This usually needs to be switched
 * on explicitly through the {@link #setTransactionAware} bean property.
 *
 * @author Juergen Hoeller
 * @author Seonghun Lee
 * @since 3.2
 * @see #setTransactionAware
 * @see TransactionAwareCacheDecorator
 * @see TransactionAwareCacheManagerProxy
 */
public abstract class AbstractTransactionSupportingCacheManager extends AbstractCacheManager {

	private boolean transactionAware = false;

	private @Nullable CacheErrorHandler errorHandler;


	/**
	 * Set whether this CacheManager should expose transaction-aware Cache objects.
	 * <p>Default is "false". Set this to "true" to synchronize cache put/evict
	 * operations with ongoing Spring-managed transactions, performing the actual cache
	 * put/evict operation only in the after-commit phase of a successful transaction.
	 */
	public void setTransactionAware(boolean transactionAware) {
		this.transactionAware = transactionAware;
	}

	/**
	 * Return whether this CacheManager has been configured to be transaction-aware.
	 */
	public boolean isTransactionAware() {
		return this.transactionAware;
	}

	/**
	 * Set the {@link CacheErrorHandler} for {@link Cache#put}, {@link Cache#evict}
	 * and {@link Cache#clear} failures in the after-commit phase of a transaction,
	 * applied when this CacheManager is {@linkplain #setTransactionAware
	 * transaction-aware}.
	 * <p>By default, such failures are propagated to the caller of the transaction
	 * commit, bypassing any error handler configured at the cache interception
	 * level, since the deferred operation runs outside the intercepted cache
	 * invocation.
	 * @since 7.1
	 * @see TransactionAwareCacheDecorator
	 */
	public void setErrorHandler(@Nullable CacheErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Return the {@link CacheErrorHandler} for after-commit cache operation
	 * failures, if any.
	 * @since 7.1
	 */
	public @Nullable CacheErrorHandler getErrorHandler() {
		return this.errorHandler;
	}


	@Override
	protected Cache decorateCache(Cache cache) {
		return (isTransactionAware() ? new TransactionAwareCacheDecorator(cache, getErrorHandler()) : cache);
	}

}

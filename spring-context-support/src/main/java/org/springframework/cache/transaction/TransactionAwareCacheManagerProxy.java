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

package org.springframework.cache.transaction;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.CacheManagerProxy;

/**
 * Proxy for a target {@link CacheManager}, exposing transaction-aware {@link Cache} objects
 * which synchronize their {@link Cache#put} operations with Spring-managed transactions
 * (through Spring's {@link org.springframework.transaction.support.TransactionSynchronizationManager},
 * performing the actual cache put operation only in the after-commit phase of a successful transaction.
 * If no transaction is active, {@link Cache#put} operations will be performed immediately, as usual.
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see #setTargetCacheManager
 * @see TransactionAwareCacheDecorator
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public class TransactionAwareCacheManagerProxy extends CacheManagerProxy {

	/**
	 * Create a new TransactionAwareCacheManagerProxy, setting the target CacheManager
	 * through the {@link #setTargetCacheManager} bean property.
	 */
	public TransactionAwareCacheManagerProxy() {
		setCacheDecoratorFactory(TransactionAwareCacheDecorator::new);
	}

	/**
	 * Create a new TransactionAwareCacheManagerProxy for the given target CacheManager.
	 * @param targetCacheManager the target CacheManager to proxy
	 */
	public TransactionAwareCacheManagerProxy(CacheManager targetCacheManager) {
		super(targetCacheManager, TransactionAwareCacheDecorator::new);
	}
}

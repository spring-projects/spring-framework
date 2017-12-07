/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Collection;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

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
public class TransactionAwareCacheManagerProxy implements CacheManager, InitializingBean {

	@Nullable
	private CacheManager targetCacheManager;


	/**
	 * Create a new TransactionAwareCacheManagerProxy, setting the target CacheManager
	 * through the {@link #setTargetCacheManager} bean property.
	 */
	public TransactionAwareCacheManagerProxy() {
	}

	/**
	 * Create a new TransactionAwareCacheManagerProxy for the given target CacheManager.
	 * @param targetCacheManager the target CacheManager to proxy
	 */
	public TransactionAwareCacheManagerProxy(CacheManager targetCacheManager) {
		Assert.notNull(targetCacheManager, "Target CacheManager must not be null");
		this.targetCacheManager = targetCacheManager;
	}


	/**
	 * Set the target CacheManager to proxy.
	 */
	public void setTargetCacheManager(CacheManager targetCacheManager) {
		this.targetCacheManager = targetCacheManager;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.targetCacheManager == null) {
			throw new IllegalArgumentException("Property 'targetCacheManager' is required");
		}
	}


	@Override
	@Nullable
	public Cache getCache(String name) {
		Assert.state(this.targetCacheManager != null, "No target CacheManager set");
		Cache targetCache = this.targetCacheManager.getCache(name);
		return (targetCache != null ? new TransactionAwareCacheDecorator(targetCache) : null);
	}

	@Override
	public Collection<String> getCacheNames() {
		Assert.state(this.targetCacheManager != null, "No target CacheManager set");
		return this.targetCacheManager.getCacheNames();
	}

}

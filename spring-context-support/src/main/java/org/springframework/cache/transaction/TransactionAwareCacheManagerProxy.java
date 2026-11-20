/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.transaction;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * Proxy for a target {@link CacheManager}, exposing transaction-aware {@link Cache} objects
 * which synchronize their {@link Cache#put} operations with Spring-managed transactions
 * (through Spring's {@link org.springframework.transaction.support.TransactionSynchronizationManager}),
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

	private @Nullable CacheManager targetCacheManager;


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
	public @Nullable Cache getCache(String name) {
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

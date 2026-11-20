/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared tests for {@link CacheManager} that inherit from
 * {@link AbstractTransactionSupportingCacheManager}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public abstract class AbstractTransactionSupportingCacheManagerTests<T extends CacheManager> {

	public static final String CACHE_NAME = "testCacheManager";


	protected String cacheName;

	@BeforeEach
	void trackCacheName(TestInfo testInfo) {
		this.cacheName = testInfo.getTestMethod().get().getName();
	}


	/**
	 * Returns the {@link CacheManager} to use.
	 * @param transactionAware if the requested cache manager should be aware
	 * of the transaction
	 * @return the cache manager to use
	 * @see org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager#setTransactionAware
	 */
	protected abstract T getCacheManager(boolean transactionAware);

	/**
	 * Returns the expected concrete type of the cache.
	 */
	protected abstract Class<? extends Cache> getCacheType();

	/**
	 * Adds a cache with the specified name to the native manager.
	 */
	protected abstract void addNativeCache(String cacheName);

	/**
	 * Removes the cache with the specified name from the native manager.
	 */
	protected abstract void removeNativeCache(String cacheName);


	@Test
	void getOnExistingCache() {
		assertThat(getCacheManager(false).getCache(CACHE_NAME)).isInstanceOf(getCacheType());
	}

	@Test
	void getOnNewCache() {
		T cacheManager = getCacheManager(false);
		addNativeCache(this.cacheName);
		assertThat(cacheManager.getCacheNames()).doesNotContain(this.cacheName);
		try {
			assertThat(cacheManager.getCache(this.cacheName)).isInstanceOf(getCacheType());
			assertThat(cacheManager.getCacheNames()).contains(this.cacheName);
		}
		finally {
			removeNativeCache(this.cacheName);
		}
	}

	@Test
	void getOnUnknownCache() {
		T cacheManager = getCacheManager(false);
		assertThat(cacheManager.getCacheNames()).doesNotContain(this.cacheName);
		assertThat(cacheManager.getCache(this.cacheName)).isNull();
	}

	@Test
	void getTransactionalOnExistingCache() {
		assertThat(getCacheManager(true).getCache(CACHE_NAME))
				.isInstanceOf(TransactionAwareCacheDecorator.class);
	}

	@Test
	void getTransactionalOnNewCache() {
		T cacheManager = getCacheManager(true);
		assertThat(cacheManager.getCacheNames()).doesNotContain(this.cacheName);
		addNativeCache(this.cacheName);
		try {
			assertThat(cacheManager.getCache(this.cacheName))
					.isInstanceOf(TransactionAwareCacheDecorator.class);
			assertThat(cacheManager.getCacheNames()).contains(this.cacheName);
		}
		finally {
			removeNativeCache(this.cacheName);
		}
	}

}

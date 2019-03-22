/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Shared tests for {@link CacheManager} that inherit from
 * {@link AbstractTransactionSupportingCacheManager}.
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractTransactionSupportingCacheManagerTests<T extends CacheManager> {

	public static final String CACHE_NAME = "testCacheManager";

	@Rule
	public final TestName name = new TestName();


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
	public void getOnExistingCache() {
		assertThat(getCacheManager(false).getCache(CACHE_NAME), is(instanceOf(getCacheType())));
	}

	@Test
	public void getOnNewCache() {
		T cacheManager = getCacheManager(false);
		String cacheName = name.getMethodName();
		addNativeCache(cacheName);
		assertFalse(cacheManager.getCacheNames().contains(cacheName));
		try {
			assertThat(cacheManager.getCache(cacheName), is(instanceOf(getCacheType())));
			assertTrue(cacheManager.getCacheNames().contains(cacheName));
		}
		finally {
			removeNativeCache(cacheName);
		}
	}

	@Test
	public void getOnUnknownCache() {
		T cacheManager = getCacheManager(false);
		String cacheName = name.getMethodName();
		assertFalse(cacheManager.getCacheNames().contains(cacheName));
		assertThat(cacheManager.getCache(cacheName), nullValue());
	}

	@Test
	public void getTransactionalOnExistingCache() {
		assertThat(getCacheManager(true).getCache(CACHE_NAME),
				is(instanceOf(TransactionAwareCacheDecorator.class)));
	}

	@Test
	public void getTransactionalOnNewCache() {
		String cacheName = name.getMethodName();
		T cacheManager = getCacheManager(true);
		assertFalse(cacheManager.getCacheNames().contains(cacheName));
		addNativeCache(cacheName);
		try {
			assertThat(cacheManager.getCache(cacheName),
					is(instanceOf(TransactionAwareCacheDecorator.class)));
			assertTrue(cacheManager.getCacheNames().contains(cacheName));
		}
		finally {
			removeNativeCache(cacheName);
		}
	}

}

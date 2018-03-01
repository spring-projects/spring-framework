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

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * An alternative strategy to {@link TransactionAwareCacheDecorator} for use with
 * {@link TransactionAwareCacheManagerProxy}.
 *
 * This decorator collects cache updates during a transaction and writes them to the
 * target cache on commit, or discards them on rollback. It differs from the default
 * decorator used by the TransactionAwareCacheManagerProxy in that updates are visible
 * immediately (i.e callers within the scope of the transaction can read the changes
 * made before commit).
 * <p>
 * If there is no transaction in progress all operations are performed immediately.
 * <p>
 * <b>Note:</b> When a transaction is active {@link Cache#get(Object, Callable)} and
 * {@link Cache#putIfAbsent} are implemented as a get and, if required, a
 * subsequent deferred put to the target cache; they are not performed atomically.
 *
 * @author William Hoyle
 * @see TransactionAwareCacheManagerProxy#setCacheDecoratorFactory
 * @see TransactionalCacheDecoratorFactory
 */
public class TransactionalCacheDecorator implements Cache {

	private final Cache targetCache;
	private String name;


	/**
	 * Create a new TransactionalCacheDecorator for the given target Cache, defaulting
	 * the cache name to the name of the target cache with the suffix {@code ".tx"}.
	 *
	 * @param targetCache the target Cache to decorate
	 */
	public TransactionalCacheDecorator(Cache targetCache) {
		this(targetCache, targetCache.getName() + ".tx");
	}

	/**
	 * Create a new TransactionalCacheDecorator for the given target Cache.
	 *
	 * @param targetCache the target Cache to decorate
	 * @param name the name used by the CacheManager to refer to the cache
	 */
	public TransactionalCacheDecorator(Cache targetCache, String name) {
		this.name = name;
		this.targetCache = targetCache;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * @return null
	 */
	@Override
	@Nullable
	public Object getNativeCache() {
		return null;
	}

	/**
	 * @return the cache that this decorator delegates to
	 */
	public Cache getTargetCache() {
		return targetCache;
	}

	@Override
	public ValueWrapper get(Object key) {
		return getCache().get(key);
	}

	@Override
	public <T> T get(Object key, @Nullable Class<T> type) {
		return getCache().get(key, type);
	}

	/**
	 * @implNote If a transaction is in progress this operation is implemented as a get
	 * and, if required, a subsequent put on commit to the target cache; it will not
	 * be performed atomically. If there is no transaction the operation will be delegated
	 * directly to the target cache an may be performed atomically if the implementation
	 * supports it.
	 */
	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		return getCache().get(key, valueLoader);
	}

	@Override
	public void put(Object key, @Nullable Object value) {
		getCache().put(key, value);
	}

	/**
	 * @implNote If a transaction is in progress this operation is implemented as a get
	 * and, if required, a subsequent put on commit to the target cache; it will not
	 * be performed atomically. If there is no transaction the operation will be delegated
	 * directly to the target cache an may be performed atomically if the implementation
	 * supports it.
	 */
	@Override
	public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		return getCache().putIfAbsent(key, value);
	}

	@Override
	public void evict(Object key) {
		getCache().evict(key);
	}

	@Override
	public void clear() {
		getCache().clear();
	}

	private Cache getCache() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return targetCache;
		}
		else {
			BufferedCache cache = (BufferedCache) TransactionSynchronizationManager.getResource(this);
			if (cache == null) {
				cache = new BufferedCache(targetCache);
				TransactionSynchronizationManager.bindResource(this, cache);
				TransactionSynchronizationManager.registerSynchronization(new CacheSynchronization());
			}
			return cache;
		}
	}

	/**
	 * On commit, flushes changes to the target cache.
	 * <p>
	 * Also responsible for resource cleanup at the end of the transaction.
	 */
	private class CacheSynchronization extends TransactionSynchronizationAdapter {

		@Nullable
		private BufferedCache transactionLocalCache;

		@Override
		public void suspend() {
			if (transactionLocalCache != null) {
				throw new IllegalStateException();
			}
			transactionLocalCache = (BufferedCache) TransactionSynchronizationManager
					.unbindResource(TransactionalCacheDecorator.this);
		}

		@Override
		public void resume() {
			if (transactionLocalCache == null) {
				throw new IllegalStateException();
			}
			TransactionSynchronizationManager
					.bindResource(TransactionalCacheDecorator.this, transactionLocalCache);
			transactionLocalCache = null;
		}


		@Override
		public void afterCommit() {
			BufferedCache cache = (BufferedCache) TransactionSynchronizationManager
					.getResource(TransactionalCacheDecorator.this);
			if (cache == null) {
				throw new IllegalStateException();
			}
			cache.commit();
		}

		@Override
		public void afterCompletion(int status) {
			TransactionSynchronizationManager
					.unbindResourceIfPossible(TransactionalCacheDecorator.this);
		}
	}
}

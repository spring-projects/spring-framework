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

import org.springframework.cache.Cache;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Cache decorator which synchronizes its {@link #put} and {@link #evict} operations with
 * Spring-managed transactions (through Spring's {@link TransactionSynchronizationManager},
 * performing the actual cache put/evict operation only in the after-commit phase of a
 * successful transaction. If no transaction is active, {@link #put} and {@link #evict}
 * operations will be performed immediately, as usual.
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see TransactionAwareCacheManagerProxy
 */
public class TransactionAwareCacheDecorator implements Cache {

	private final Cache targetCache;


	/**
	 * Create a new TransactionAwareCache for the given target Cache.
	 * @param targetCache the target Cache to decorate
	 */
	public TransactionAwareCacheDecorator(Cache targetCache) {
		Assert.notNull(targetCache, "Target Cache must not be null");
		this.targetCache = targetCache;
	}


	@Override
	public String getName() {
		return this.targetCache.getName();
	}

	@Override
	public Object getNativeCache() {
		return this.targetCache.getNativeCache();
	}

	@Override
	public ValueWrapper get(Object key) {
		return this.targetCache.get(key);
	}

	@Override
	public void put(final Object key, final Object value) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void afterCommit() {
					targetCache.put(key, value);
				}
			});
		}
		else {
			this.targetCache.put(key, value);
		}
	}

	@Override
	public void evict(final Object key) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void afterCommit() {
					targetCache.evict(key);
				}
			});
		}
		else {
			this.targetCache.evict(key);
		}
	}

	@Override
	public void clear() {
		this.targetCache.clear();
	}

}

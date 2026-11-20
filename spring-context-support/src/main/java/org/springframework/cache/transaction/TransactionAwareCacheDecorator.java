/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.transaction;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Cache decorator which synchronizes its {@link #put}, {@link #evict} and
 * {@link #clear} operations with Spring-managed transactions (through Spring's
 * {@link TransactionSynchronizationManager}), performing the actual cache
 * put/evict/clear operation only in the after-commit phase of a successful
 * transaction. If no transaction is active, {@link #put}, {@link #evict} and
 * {@link #clear} operations will be performed immediately, as usual.
 *
 * <p><b>Note:</b> Use of immediate operations such as {@link #putIfAbsent} and
 * {@link #evictIfPresent} cannot be deferred to the after-commit phase of a
 * running transaction. Use these with care in a transactional environment.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Stas Volsky
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


	/**
	 * Return the target Cache that this Cache should delegate to.
	 */
	public Cache getTargetCache() {
		return this.targetCache;
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
	public @Nullable ValueWrapper get(Object key) {
		return this.targetCache.get(key);
	}

	@Override
	public <T> @Nullable T get(Object key, @Nullable Class<T> type) {
		return this.targetCache.get(key, type);
	}

	@Override
	public <T> @Nullable T get(Object key, Callable<T> valueLoader) {
		return this.targetCache.get(key, valueLoader);
	}

	@Override
	public @Nullable CompletableFuture<?> retrieve(Object key) {
		return this.targetCache.retrieve(key);
	}

	@Override
	public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
		return this.targetCache.retrieve(key, valueLoader);
	}

	@Override
	public void put(final Object key, final @Nullable Object value) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					TransactionAwareCacheDecorator.this.targetCache.put(key, value);
				}
			});
		}
		else {
			this.targetCache.put(key, value);
		}
	}

	@Override
	public @Nullable ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		return this.targetCache.putIfAbsent(key, value);
	}

	@Override
	public void evict(final Object key) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					TransactionAwareCacheDecorator.this.targetCache.evict(key);
				}
			});
		}
		else {
			this.targetCache.evict(key);
		}
	}

	@Override
	public boolean evictIfPresent(Object key) {
		return this.targetCache.evictIfPresent(key);
	}

	@Override
	public void clear() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					targetCache.clear();
				}
			});
		}
		else {
			this.targetCache.clear();
		}
	}

	@Override
	public boolean invalidate() {
		return this.targetCache.invalidate();
	}

}

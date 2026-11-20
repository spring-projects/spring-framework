/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.support;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.util.Assert;

/**
 * A no operation {@link Cache} implementation suitable for disabling caching.
 *
 * <p>Will simply accept any items into the cache not actually storing them.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @since 4.3.4
 * @see NoOpCacheManager
 */
public class NoOpCache implements Cache {

	private final String name;


	/**
	 * Create a {@link NoOpCache} instance with the specified name.
	 * @param name the name of the cache
	 */
	public NoOpCache(String name) {
		Assert.notNull(name, "Cache name must not be null");
		this.name = name;
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object getNativeCache() {
		return this;
	}

	@Override
	public @Nullable ValueWrapper get(Object key) {
		return null;
	}

	@Override
	public <T> @Nullable T get(Object key, @Nullable Class<T> type) {
		return null;
	}

	@Override
	public <T> @Nullable T get(Object key, Callable<T> valueLoader) {
		try {
			return valueLoader.call();
		}
		catch (Exception ex) {
			throw new ValueRetrievalException(key, valueLoader, ex);
		}
	}

	@Override
	public @Nullable CompletableFuture<?> retrieve(Object key) {
		return null;
	}

	@Override
	public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
		return valueLoader.get();
	}

	@Override
	public void put(Object key, @Nullable Object value) {
	}

	@Override
	public @Nullable ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		return null;
	}

	@Override
	public void evict(Object key) {
	}

	@Override
	public boolean evictIfPresent(Object key) {
		return false;
	}

	@Override
	public void clear() {
	}

	@Override
	public boolean invalidate() {
		return false;
	}

}

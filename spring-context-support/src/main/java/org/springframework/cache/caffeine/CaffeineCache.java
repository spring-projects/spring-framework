/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.cache.caffeine;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.LoadingCache;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Spring {@link org.springframework.cache.Cache} adapter implementation
 * on top of a Caffeine {@link com.github.benmanes.caffeine.cache.Cache} instance.
 *
 * <p>Supports the {@link #retrieve(Object)} and {@link #retrieve(Object, Supplier)}
 * operations through Caffeine's {@link AsyncCache}, when provided via the
 * {@link #CaffeineCache(String, AsyncCache, boolean)} constructor.
 *
 * <p>Requires Caffeine 3.0 or higher, as of Spring Framework 6.1.
 *
 * @author Ben Manes
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 4.3
 * @see CaffeineCacheManager
 */
public class CaffeineCache extends AbstractValueAdaptingCache {

	private final String name;

	private final com.github.benmanes.caffeine.cache.Cache<Object, Object> cache;

	@Nullable
	private AsyncCache<Object, Object> asyncCache;


	/**
	 * Create a {@link CaffeineCache} instance with the specified name and the
	 * given internal {@link com.github.benmanes.caffeine.cache.Cache} to use.
	 * @param name the name of the cache
	 * @param cache the backing Caffeine Cache instance
	 */
	public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
		this(name, cache, true);
	}

	/**
	 * Create a {@link CaffeineCache} instance with the specified name and the
	 * given internal {@link com.github.benmanes.caffeine.cache.Cache} to use.
	 * @param name the name of the cache
	 * @param cache the backing Caffeine Cache instance
	 * @param allowNullValues whether to accept and convert {@code null} values
	 * for this cache
	 */
	public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache,
			boolean allowNullValues) {

		super(allowNullValues);
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(cache, "Cache must not be null");
		this.name = name;
		this.cache = cache;
	}

	/**
	 * Create a {@link CaffeineCache} instance with the specified name and the
	 * given internal {@link AsyncCache} to use.
	 * @param name the name of the cache
	 * @param cache the backing Caffeine AsyncCache instance
	 * @param allowNullValues whether to accept and convert {@code null} values
	 * for this cache
	 * @since 6.1
	 */
	public CaffeineCache(String name, AsyncCache<Object, Object> cache, boolean allowNullValues) {
		super(allowNullValues);
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(cache, "Cache must not be null");
		this.name = name;
		this.cache = cache.synchronous();
		this.asyncCache = cache;
	}


	@Override
	public final String getName() {
		return this.name;
	}

	/**
	 * Return the internal Caffeine Cache
	 * (possibly an adapter on top of an {@link #getAsyncCache()}).
	 */
	@Override
	public final com.github.benmanes.caffeine.cache.Cache<Object, Object> getNativeCache() {
		return this.cache;
	}

	/**
	 * Return the internal Caffeine AsyncCache.
	 * @throws IllegalStateException if no AsyncCache is available
	 * @since 6.1
	 * @see #CaffeineCache(String, AsyncCache, boolean)
	 * @see CaffeineCacheManager#setAsyncCacheMode
	 */
	public final AsyncCache<Object, Object> getAsyncCache() {
		Assert.state(this.asyncCache != null,
				"No Caffeine AsyncCache available: set CaffeineCacheManager.setAsyncCacheMode(true)");
		return this.asyncCache;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T get(Object key, Callable<T> valueLoader) {
		return (T) fromStoreValue(this.cache.get(key, new LoadFunction(valueLoader)));
	}

	@Override
	@Nullable
	public CompletableFuture<?> retrieve(Object key) {
		CompletableFuture<?> result = getAsyncCache().getIfPresent(key);
		if (result != null && isAllowNullValues()) {
			result = result.thenApply(this::toValueWrapper);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
		if (isAllowNullValues()) {
			return (CompletableFuture<T>) getAsyncCache()
					.get(key, (k, e) -> valueLoader.get().thenApply(this::toStoreValue))
					.thenApply(this::fromStoreValue);
		}
		else {
			return (CompletableFuture<T>) getAsyncCache().get(key, (k, e) -> valueLoader.get());
		}
	}

	@Override
	@Nullable
	protected Object lookup(Object key) {
		if (this.cache instanceof LoadingCache<Object, Object> loadingCache) {
			return loadingCache.get(key);
		}
		return this.cache.getIfPresent(key);
	}

	@Override
	public void put(Object key, @Nullable Object value) {
		this.cache.put(key, toStoreValue(value));
	}

	@Override
	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		PutIfAbsentFunction callable = new PutIfAbsentFunction(value);
		Object result = this.cache.get(key, callable);
		return (callable.called ? null : toValueWrapper(result));
	}

	@Override
	public void evict(Object key) {
		this.cache.invalidate(key);
	}

	@Override
	public boolean evictIfPresent(Object key) {
		return (this.cache.asMap().remove(key) != null);
	}

	@Override
	public void clear() {
		this.cache.invalidateAll();
	}

	@Override
	public boolean invalidate() {
		boolean notEmpty = !this.cache.asMap().isEmpty();
		this.cache.invalidateAll();
		return notEmpty;
	}


	private class PutIfAbsentFunction implements Function<Object, Object> {

		@Nullable
		private final Object value;

		boolean called;

		public PutIfAbsentFunction(@Nullable Object value) {
			this.value = value;
		}

		@Override
		public Object apply(Object key) {
			this.called = true;
			return toStoreValue(this.value);
		}
	}


	private class LoadFunction implements Function<Object, Object> {

		private final Callable<?> valueLoader;

		public LoadFunction(Callable<?> valueLoader) {
			Assert.notNull(valueLoader, "Callable must not be null");
			this.valueLoader = valueLoader;
		}

		@Override
		public Object apply(Object key) {
			try {
				return toStoreValue(this.valueLoader.call());
			}
			catch (Exception ex) {
				throw new ValueRetrievalException(key, this.valueLoader, ex);
			}
		}
	}

}

/*
 * Copyright 2018-2018 the original author or authors.
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

package org.springframework.cache.cache2k;

import org.cache2k.SimpleCacheEntry;
import org.cache2k.integration.CacheLoaderException;
import org.cache2k.processor.EntryProcessor;
import org.cache2k.processor.MutableCacheEntry;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;

/**
 * Spring {@link org.springframework.cache.Cache} adapter implementation
 * for a cache2k {@link org.cache2k.Cache} instance.
 *
 * @author Jens Wilke
 */
public class Cache2kCache implements Cache {

	private final org.cache2k.Cache<Object,Object> cache;

	private boolean loaderPresent = false;

	/**
	 * Create an adapter instance for the given cache2k instance.
	 *
	 * @param cache the cache2k cache instance to adapt
	 * values for this cache
	 */
	public Cache2kCache(org.cache2k.Cache<Object,Object> cache) {
		Assert.notNull(cache, "Cache must not be null");
		this.cache = cache;
	}

	/**
	 * Create an adapter instance for the given cache2k instance.
	 *
	 * @param loaderPresent the cache is constructed with a loader
	 * @param cache the cache2k cache instance to adapt
	 * values for this cache
	 */
	public Cache2kCache(org.cache2k.Cache<Object,Object> cache, boolean loaderPresent) {
		Assert.notNull(cache, "Cache must not be null");
		this.cache = cache;
		this.loaderPresent = loaderPresent;
	}

	@Override
	public String getName() {
		return cache.getName();
	}

	@Override
	public org.cache2k.Cache<Object,Object> getNativeCache() {
		return cache;
	}

	@Nullable
	@Override
	public ValueWrapper get(final Object key) {
		final SimpleCacheEntry<Object,Object> entry = cache.getSimpleEntry(key);
		if (entry == null) {
			return null;
		}
		return returnWrappedValue(entry);
	}

	private ValueWrapper returnWrappedValue(final SimpleCacheEntry<Object, Object> entry) {
		return new ValueWrapper() {
			@Nullable
			@Override
			public Object get() {
				return entry.getValue();
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public <T> T get(final Object key, @Nullable final Class<T> type) {
		Object value = cache.get(key);
		if (value != null && type != null && !type.isInstance(value)) {
			throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
		}
		return (T) value;
	}

	/**
	 * cache2k will throw an exception if the callable yields and exception.
	 * Future versions may throw a {@code RuntimeException} directly so wrap this also.
	 *
	 * <p>Ignore the {@code valueLoader} parameter in case a loader is present. This makes
	 * sure the loader is consistently used and the cache2k features refresh ahead and resilience work
	 * as expected.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public <T> T get(final Object key, final Callable<T> valueLoader) {
		if (loaderPresent) {
			return (T) cache.get(key);
		} else {
			try {
				return (T) cache.computeIfAbsent(key, (Callable<Object>) valueLoader);
			} catch (CacheLoaderException ex) {
				throw new ValueRetrievalException(key, valueLoader, ex.getCause());
			} catch (RuntimeException ex) {
				throw new ValueRetrievalException(key, valueLoader, ex);
			}
		}
	}

	@Override
	public void put(final Object key, @Nullable final Object value) {
		cache.put(key, value);
	}

	@Nullable
	@Override
	public ValueWrapper putIfAbsent(final Object key, @Nullable final Object value) {
		return cache.invoke(key, new EntryProcessor<Object, Object, ValueWrapper>() {
			@Override
			public ValueWrapper process(final MutableCacheEntry<Object, Object> e) throws Exception {
				if (e.exists()) {
					return returnWrappedValue(e);
				}
				e.setValue(value);
				return null;
			}
		});
	}

	@Override
	public void evict(final Object key) {
		cache.remove(key);
	}

	@Override
	public void clear() {
		cache.clear();
	}

	public boolean isLoaderPresent() {
		return loaderPresent;
	}

}

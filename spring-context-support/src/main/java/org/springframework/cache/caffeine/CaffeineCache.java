/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.cache.caffeine;

import java.util.function.Function;

import com.github.benmanes.caffeine.cache.LoadingCache;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.util.Assert;

/**
 * Spring {@link org.springframework.cache.Cache} adapter implementation
 * on top of a Caffeine {@link com.github.benmanes.caffeine.cache.Cache} instance.
 *
 * <p>Requires Caffeine 2.0 or higher.
 *
 * @author Ben Manes
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 4.3
 */
public class CaffeineCache extends AbstractValueAdaptingCache {

	private final String name;

	private final com.github.benmanes.caffeine.cache.Cache<Object, Object> cache;


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
	 * @param allowNullValues whether to accept and convert {@code null}
	 * values for this cache
	 */
	public CaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache,
			boolean allowNullValues) {
		super(allowNullValues);
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(cache, "Cache must not be null");
		this.name = name;
		this.cache = cache;
	}


	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public final com.github.benmanes.caffeine.cache.Cache<Object, Object> getNativeCache() {
		return this.cache;
	}

	@Override
	public ValueWrapper get(Object key) {
		if (this.cache instanceof LoadingCache) {
			Object value = ((LoadingCache<Object, Object>) this.cache).get(key);
			return toValueWrapper(value);
		}
		return super.get(key);
	}

	@Override
	protected Object lookup(Object key) {
		return this.cache.getIfPresent(key);
	}

	@Override
	public void put(Object key, Object value) {
		this.cache.put(key, toStoreValue(value));
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, final Object value) {
		PutIfAbsentFunction callable = new PutIfAbsentFunction(value);
		Object result = this.cache.get(key, callable);
		return (callable.called ? null : toValueWrapper(result));
	}

	@Override
	public void evict(Object key) {
		this.cache.invalidate(key);
	}

	@Override
	public void clear() {
		this.cache.invalidateAll();
	}


	private class PutIfAbsentFunction implements Function<Object, Object> {

		private final Object value;

		private boolean called;

		public PutIfAbsentFunction(Object value) {
			this.value = value;
		}

		@Override
		public Object apply(Object key) {
			this.called = true;
			return toStoreValue(this.value);
		}
	}

}

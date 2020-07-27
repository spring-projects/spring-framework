/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.cache.jcache;

import java.util.concurrent.Callable;

import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.cache.Cache} implementation on top of a
 * {@link Cache javax.cache.Cache} instance.
 *
 * <p>Note: This class has been updated for JCache 1.0, as of Spring 4.0.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.2
 * @see JCacheCacheManager
 */
public class JCacheCache extends AbstractValueAdaptingCache {

	private final Cache<Object, Object> cache;


	/**
	 * Create a {@code JCacheCache} instance.
	 * @param jcache backing JCache Cache instance
	 */
	public JCacheCache(Cache<Object, Object> jcache) {
		this(jcache, true);
	}

	/**
	 * Create a {@code JCacheCache} instance.
	 * @param jcache backing JCache Cache instance
	 * @param allowNullValues whether to accept and convert null values for this cache
	 */
	public JCacheCache(Cache<Object, Object> jcache, boolean allowNullValues) {
		super(allowNullValues);
		Assert.notNull(jcache, "Cache must not be null");
		this.cache = jcache;
	}


	@Override
	public final String getName() {
		return this.cache.getName();
	}

	@Override
	public final Cache<Object, Object> getNativeCache() {
		return this.cache;
	}

	@Override
	@Nullable
	protected Object lookup(Object key) {
		return this.cache.get(key);
	}

	@Override
	@Nullable
	public <T> T get(Object key, Callable<T> valueLoader) {
		try {
			return this.cache.invoke(key, new ValueLoaderEntryProcessor<T>(), valueLoader);
		}
		catch (EntryProcessorException ex) {
			throw new ValueRetrievalException(key, valueLoader, ex.getCause());
		}
	}

	@Override
	public void put(Object key, @Nullable Object value) {
		this.cache.put(key, toStoreValue(value));
	}

	@Override
	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		boolean set = this.cache.putIfAbsent(key, toStoreValue(value));
		return (set ? null : get(key));
	}

	@Override
	public void evict(Object key) {
		this.cache.remove(key);
	}

	@Override
	public boolean evictIfPresent(Object key) {
		return this.cache.remove(key);
	}

	@Override
	public void clear() {
		this.cache.removeAll();
	}

	@Override
	public boolean invalidate() {
		boolean notEmpty = this.cache.iterator().hasNext();
		this.cache.removeAll();
		return notEmpty;
	}


	private class ValueLoaderEntryProcessor<T> implements EntryProcessor<Object, Object, T> {

		@SuppressWarnings("unchecked")
		@Override
		@Nullable
		public T process(MutableEntry<Object, Object> entry, Object... arguments) throws EntryProcessorException {
			Callable<T> valueLoader = (Callable<T>) arguments[0];
			if (entry.exists()) {
				return (T) fromStoreValue(entry.getValue());
			}
			else {
				T value;
				try {
					value = valueLoader.call();
				}
				catch (Exception ex) {
					throw new EntryProcessorException("Value loader '" + valueLoader + "' failed " +
							"to compute value for key '" + entry.getKey() + "'", ex);
				}
				entry.setValue(toStoreValue(value));
				return value;
			}
		}
	}

}

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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.Nullable;

/**
 * Buffers write ops for batched {@link #commit} to the target cache.
 * <p>
 * Get operations read through the buffer first then from the target cache if no local
 * value is found.
 *
 * @implNote operations are not synchronized; this is a utility class that is only used by
 * instances of {@link TransactionalCacheDecorator} in a thread local context.
 *
 * @author William Hoyle
 * @see TransactionalCacheDecorator
 */
class BufferedCache extends AbstractValueAdaptingCache {

	/**
	 * Sentinel value associated with evicted keys in the buffer.
	 */
	private static final Object EVICTED = new Object();

	private final String name;
	private final Cache targetCache;
	private final Map<Object, Object> buffer = new HashMap<>();

	/**
	 * True if a {@link #clear()} has been issued.
	 */
	private boolean cleared;


	/**
	 * Create a new BufferedCache for the given target Cache.
	 *
	 * @param targetCache the target Cache to decorate
	 */
	public BufferedCache(Cache targetCache) {
		super(true);
		Objects.requireNonNull(targetCache);
		this.name = targetCache.getName() + ".buffer";
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

	@Override
	@Nullable
	protected Object lookup(Object key) {
		ValueWrapper wrapper = delegatingGet(key);
		return (wrapper != null ? toStoreValue(wrapper.get()) : null);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T get(Object key, Callable<T> valueLoader) {
		T value;
		ValueWrapper wrapper = delegatingGet(key);
		if (wrapper == null) {
			try {
				value = valueLoader.call();
				put(key, value);
			}
			catch (Exception e) {
				throw new ValueRetrievalException(key, valueLoader, e);
			}
		}
		else {
			value = (T) wrapper.get();
		}
		return value;
	}

	@Override
	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		ValueWrapper wrapper = delegatingGet(key);
		if (wrapper == null) {
			put(key, value);
		}
		return wrapper;
	}

	@Override
	public void put(Object key, @Nullable Object value) {
		buffer.put(key, toStoreValue(value));
	}

	@Override
	public void evict(Object key) {
		buffer.put(key, EVICTED);
	}

	@Override
	public void clear() {
		buffer.clear();
		cleared = true;
	}

	/**
	 * Write all changes since the last commit to the underlying cache.
	 */
	public void commit() {
		if (cleared) {
			targetCache.clear();
		}
		for (Map.Entry<Object, Object> entry : buffer.entrySet()) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (value == EVICTED) {
				targetCache.evict(key);
			}
			else {
				targetCache.put(key, fromStoreValue(value));
			}
		}
		clearBuffer();
	}

	/**
	 * Discard all changes since the last commit without writing them to the underlying
	 * cache.
	 */
	public void rollback() {
		clearBuffer();
	}

	private void clearBuffer() {
		buffer.clear();
		cleared = false;
	}

	/**
	 * Get a value from the buffer, or fall back to get the value from the target cache if
	 * the value isn't in the buffer.
	 * <p>
	 * If the key has been evicted or cleared from the buffer this method will return null
	 * rather than fetching the value from the underlying cache.
	 */
	@Nullable
	private ValueWrapper delegatingGet(Object key) {
		Object value = buffer.get(key);
		ValueWrapper wrapper;
		if (value != null) {
			wrapper = (value != EVICTED ? toValueWrapper(value) : null);
		}
		else if (cleared) {
			wrapper = null;
		}
		else {
			wrapper = targetCache.get(key);
		}
		return wrapper;
	}
}

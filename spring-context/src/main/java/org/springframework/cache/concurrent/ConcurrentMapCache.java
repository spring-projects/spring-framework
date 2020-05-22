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

package org.springframework.cache.concurrent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.core.serializer.support.SerializationDelegate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple {@link org.springframework.cache.Cache} implementation based on the
 * core JDK {@code java.util.concurrent} package.
 *
 * <p>Useful for testing or simple caching scenarios, typically in combination
 * with {@link org.springframework.cache.support.SimpleCacheManager} or
 * dynamically through {@link ConcurrentMapCacheManager}.
 *
 * <p><b>Note:</b> As {@link ConcurrentHashMap} (the default implementation used)
 * does not allow for {@code null} values to be stored, this class will replace
 * them with a predefined internal object. This behavior can be changed through the
 * {@link #ConcurrentMapCache(String, ConcurrentMap, boolean)} constructor.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public class ConcurrentMapCache extends AbstractValueAdaptingCache {

	private final String name;

	private final ConcurrentMap<Object, Object> store;

	@Nullable
	private final SerializationDelegate serialization;


	/**
	 * Create a new ConcurrentMapCache with the specified name.
	 * @param name the name of the cache
	 */
	public ConcurrentMapCache(String name) {
		this(name, new ConcurrentHashMap<>(256), true);
	}

	/**
	 * Create a new ConcurrentMapCache with the specified name.
	 * @param name the name of the cache
	 * @param allowNullValues whether to accept and convert {@code null}
	 * values for this cache
	 */
	public ConcurrentMapCache(String name, boolean allowNullValues) {
		this(name, new ConcurrentHashMap<>(256), allowNullValues);
	}

	/**
	 * Create a new ConcurrentMapCache with the specified name and the
	 * given internal {@link ConcurrentMap} to use.
	 * @param name the name of the cache
	 * @param store the ConcurrentMap to use as an internal store
	 * @param allowNullValues whether to allow {@code null} values
	 * (adapting them to an internal null holder value)
	 */
	public ConcurrentMapCache(String name, ConcurrentMap<Object, Object> store, boolean allowNullValues) {
		this(name, store, allowNullValues, null);
	}

	/**
	 * Create a new ConcurrentMapCache with the specified name and the
	 * given internal {@link ConcurrentMap} to use. If the
	 * {@link SerializationDelegate} is specified,
	 * {@link #isStoreByValue() store-by-value} is enabled
	 * @param name the name of the cache
	 * @param store the ConcurrentMap to use as an internal store
	 * @param allowNullValues whether to allow {@code null} values
	 * (adapting them to an internal null holder value)
	 * @param serialization the {@link SerializationDelegate} to use
	 * to serialize cache entry or {@code null} to store the reference
	 * @since 4.3
	 */
	protected ConcurrentMapCache(String name, ConcurrentMap<Object, Object> store,
			boolean allowNullValues, @Nullable SerializationDelegate serialization) {

		super(allowNullValues);
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(store, "Store must not be null");
		this.name = name;
		this.store = store;
		this.serialization = serialization;
	}


	/**
	 * Return whether this cache stores a copy of each entry ({@code true}) or
	 * a reference ({@code false}, default). If store by value is enabled, each
	 * entry in the cache must be serializable.
	 * @since 4.3
	 */
	public final boolean isStoreByValue() {
		return (this.serialization != null);
	}

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public final ConcurrentMap<Object, Object> getNativeCache() {
		return this.store;
	}

	@Override
	@Nullable
	protected Object lookup(Object key) {
		return this.store.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T get(Object key, Callable<T> valueLoader) {
		return (T) fromStoreValue(this.store.computeIfAbsent(key, k -> {
			try {
				return toStoreValue(valueLoader.call());
			}
			catch (Throwable ex) {
				throw new ValueRetrievalException(key, valueLoader, ex);
			}
		}));
	}

	@Override
	public void put(Object key, @Nullable Object value) {
		this.store.put(key, toStoreValue(value));
	}

	@Override
	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		Object existing = this.store.putIfAbsent(key, toStoreValue(value));
		return toValueWrapper(existing);
	}

	@Override
	public void evict(Object key) {
		this.store.remove(key);
	}

	@Override
	public void clear() {
		this.store.clear();
	}

	@Override
	protected Object toStoreValue(@Nullable Object userValue) {
		Object storeValue = super.toStoreValue(userValue);
		if (this.serialization != null) {
			try {
				return serializeValue(this.serialization, storeValue);
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Failed to serialize cache value '" + userValue +
						"'. Does it implement Serializable?", ex);
			}
		}
		else {
			return storeValue;
		}
	}

	private Object serializeValue(SerializationDelegate serialization, Object storeValue) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		try {
			serialization.serialize(storeValue, out);
			return out.toByteArray();
		}
		finally {
			out.close();
		}
	}

	@Override
	protected Object fromStoreValue(@Nullable Object storeValue) {
		if (storeValue != null && this.serialization != null) {
			try {
				return super.fromStoreValue(deserializeValue(this.serialization, storeValue));
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Failed to deserialize cache value '" + storeValue + "'", ex);
			}
		}
		else {
			return super.fromStoreValue(storeValue);
		}

	}

	private Object deserializeValue(SerializationDelegate serialization, Object storeValue) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream((byte[]) storeValue);
		try {
			return serialization.deserialize(in);
		}
		finally {
			in.close();
		}
	}

}

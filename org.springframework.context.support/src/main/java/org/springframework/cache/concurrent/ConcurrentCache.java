/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.cache.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractDelegatingCache;

/** 
 * Simple {@link Cache} implementation based on the JDK 1.5+ java.util.concurrent package.
 * Useful for testing or simple caching scenarios.
 * 
 * @author Costin Leau
 */
public class ConcurrentCache<K, V> extends AbstractDelegatingCache<K, V> {

	private final ConcurrentMap<K, V> store;
	private final String name;

	public ConcurrentCache() {
		this("");
	}

	public ConcurrentCache(String name) {
		this(new ConcurrentHashMap<K, V>(), name);
	}

	public ConcurrentCache(ConcurrentMap<K, V> delegate, String name) {
		super(delegate);
		this.store = delegate;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public ConcurrentMap<K, V> getNativeCache() {
		return store;
	}

	public V putIfAbsent(K key, V value) {
		return store.putIfAbsent(key, value);
	}

	public boolean remove(Object key, Object value) {
		return store.remove(key, value);
	}

	public boolean replace(K key, V oldValue, V newValue) {
		return store.replace(key, oldValue, newValue);
	}

	public V replace(K key, V value) {
		return store.replace(key, value);
	}
}
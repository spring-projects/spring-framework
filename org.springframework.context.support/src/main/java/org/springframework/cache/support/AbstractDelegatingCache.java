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

package org.springframework.cache.support;

import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.util.Assert;

/**
 * Abstract base class delegating most of the {@link Map}-like methods
 * to the underlying cache.
 * 
 * @author Costin Leau
 */
public abstract class AbstractDelegatingCache<K, V> implements Cache<K, V> {

	private final Map<K, V> delegate;

	public <D extends Map<K, V>> AbstractDelegatingCache(D delegate) {
		Assert.notNull(delegate);
		this.delegate = delegate;
	}

	public void clear() {
		delegate.clear();
	}

	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	public V get(Object key) {
		return delegate.get(key);
	}

	public V put(K key, V value) {
		return delegate.put(key, value);
	}

	public V remove(Object key) {
		return delegate.remove(key);
	}
}
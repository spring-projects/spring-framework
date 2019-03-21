/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cache.ehcache;

import java.util.concurrent.Callable;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.util.Assert;

/**
 * {@link Cache} implementation on top of an {@link Ehcache} instance.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public class EhCacheCache implements Cache {

	private final Ehcache cache;


	/**
	 * Create an {@link EhCacheCache} instance.
	 * @param ehcache the backing Ehcache instance
	 */
	public EhCacheCache(Ehcache ehcache) {
		Assert.notNull(ehcache, "Ehcache must not be null");
		Status status = ehcache.getStatus();
		if (!Status.STATUS_ALIVE.equals(status)) {
			throw new IllegalArgumentException(
					"An 'alive' Ehcache is required - current cache is " + status.toString());
		}
		this.cache = ehcache;
	}


	@Override
	public final String getName() {
		return this.cache.getName();
	}

	@Override
	public final Ehcache getNativeCache() {
		return this.cache;
	}

	@Override
	public ValueWrapper get(Object key) {
		Element element = lookup(key);
		return toValueWrapper(element);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		Element element = lookup(key);
		if (element != null) {
			return (T) element.getObjectValue();
		}
		else {
			this.cache.acquireWriteLockOnKey(key);
			try {
				element = lookup(key);  // one more attempt with the write lock
				if (element != null) {
					return (T) element.getObjectValue();
				}
				else {
					return loadValue(key, valueLoader);
				}
			}
			finally {
				this.cache.releaseWriteLockOnKey(key);
			}
		}

	}

	private <T> T loadValue(Object key, Callable<T> valueLoader) {
		T value;
		try {
			value = valueLoader.call();
		}
		catch (Throwable ex) {
			throw new ValueRetrievalException(key, valueLoader, ex);
		}
		put(key, value);
		return value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Class<T> type) {
		Element element = this.cache.get(key);
		Object value = (element != null ? element.getObjectValue() : null);
		if (value != null && type != null && !type.isInstance(value)) {
			throw new IllegalStateException(
					"Cached value is not of required type [" + type.getName() + "]: " + value);
		}
		return (T) value;
	}

	@Override
	public void put(Object key, Object value) {
		this.cache.put(new Element(key, value));
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		Element existingElement = this.cache.putIfAbsent(new Element(key, value));
		return toValueWrapper(existingElement);
	}

	@Override
	public void evict(Object key) {
		this.cache.remove(key);
	}

	@Override
	public void clear() {
		this.cache.removeAll();
	}


	private Element lookup(Object key) {
		return this.cache.get(key);
	}

	private ValueWrapper toValueWrapper(Element element) {
		return (element != null ? new SimpleValueWrapper(element.getObjectValue()) : null);
	}

}

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

package org.springframework.cache.ehcache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;

import org.springframework.cache.Cache;
import org.springframework.util.Assert;

/**
 * {@link Cache} implementation on top of an {@link Ehcache} instance.
 * 
 * @author Costin Leau
 */
public class EhCacheCache implements Cache<Object, Object> {

	private final Ehcache cache;

	/**
	 * Creates a {@link EhCacheCache} instance.
	 * 
	 * @param ehcache backing Ehcache instance
	 */
	public EhCacheCache(Ehcache ehcache) {
		Assert.notNull(ehcache, "non null ehcache required");
		Status status = ehcache.getStatus();
		Assert.isTrue(Status.STATUS_ALIVE.equals(status), "an 'alive' ehcache is required - current cache is "
				+ status.toString());
		this.cache = ehcache;
	}

	public String getName() {
		return cache.getName();
	}

	public Ehcache getNativeCache() {
		return cache;
	}

	public void clear() {
		cache.removeAll();
	}

	public boolean containsKey(Object key) {
		return cache.isKeyInCache(key);
	}

	public boolean containsValue(Object value) {
		return cache.isValueInCache(value);
	}

	public Object get(Object key) {
		Element element = cache.get(key);
		return (element != null ? element.getObjectValue() : null);
	}

	public Object put(Object key, Object value) {
		Element previous = cache.getQuiet(key);
		cache.put(new Element(key, value));
		return (previous != null ? previous.getValue() : null);
	}

	public Object remove(Object key) {
		Object value = null;
		if (cache.isKeyInCache(key)) {
			Element element = cache.getQuiet(key);
			value = (element != null ? element.getObjectValue() : null);
		}
		cache.remove(key);
		return value;
	}

	public Object putIfAbsent(Object key, Object value) {
		// putIfAbsent supported only from Ehcache 2.1
		// return cache.putIfAbsent(new Element(key, value));
		Element existing = cache.getQuiet(key);
		if (existing == null) {
			cache.put(new Element(key, value));
			return null;
		}
		return existing.getObjectValue();
	}

	public boolean remove(Object key, Object value) {
		// remove(Element) supported only from Ehcache 2.1
		// return cache.removeElement(new Element(key, value));
		Element existing = cache.getQuiet(key);
		
		if (existing != null && existing.getObjectValue().equals(value)) {
	         cache.remove(key);
	         return true;
	    }
		
		return false;
	}

	public Object replace(Object key, Object value) {
		// replace(Object, Object) supported only from Ehcache 2.1
		// return cache.replace(new Element(key, value));
		Element existing = cache.getQuiet(key);

		if (existing != null) {
			cache.put(new Element(key, value));
			return existing.getObjectValue();
		}

		return null;
	}

	public boolean replace(Object key, Object oldValue, Object newValue) {
		// replace(Object, Object, Object) supported only from Ehcache 2.1
		// return cache.replace(new Element(key, oldValue), new Element(key,
		// newValue));
		Element existing = cache.getQuiet(key);

		if (existing != null && existing.getObjectValue().equals(oldValue)) {
			cache.put(new Element(key, newValue));
			return true;
		}
		return false;
	}
}
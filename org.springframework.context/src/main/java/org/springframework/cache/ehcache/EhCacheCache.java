/*
 * Copyright 2010-2011 the original author or authors.
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
import org.springframework.cache.interceptor.DefaultValueWrapper;
import org.springframework.util.Assert;

/**
 * {@link Cache} implementation on top of an {@link Ehcache} instance.
 * 
 * @author Costin Leau
 */
public class EhCacheCache implements Cache {

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

	public ValueWrapper get(Object key) {
		Element element = cache.get(key);
		return (element != null ? new DefaultValueWrapper<Object>(element.getObjectValue()) : null);
	}

	public void put(Object key, Object value) {
		cache.put(new Element(key, value));
	}

	public void evict(Object key) {
		cache.remove(key);
	}
}
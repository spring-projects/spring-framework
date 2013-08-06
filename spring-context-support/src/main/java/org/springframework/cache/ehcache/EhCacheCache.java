/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.util.Assert;

/**
 * {@link Cache} implementation on top of an {@link Ehcache} instance.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
public class EhCacheCache implements Cache {

	private final Ehcache cache;


	/**
	 * Create an {@link EhCacheCache} instance.
	 * @param ehcache backing Ehcache instance
	 */
	public EhCacheCache(Ehcache ehcache) {
		Assert.notNull(ehcache, "Ehcache must not be null");
		Status status = ehcache.getStatus();
		Assert.isTrue(Status.STATUS_ALIVE.equals(status),
				"An 'alive' Ehcache is required - current cache is " + status.toString());
		this.cache = ehcache;
	}


	@Override
	public String getName() {
		return this.cache.getName();
	}

	@Override
	public Ehcache getNativeCache() {
		return this.cache;
	}

	@Override
	public ValueWrapper get(Object key) {
		Element element = this.cache.get(key);
		return (element != null ? new SimpleValueWrapper(element.getObjectValue()) : null);
	}

	@Override
	public void put(Object key, Object value) {
		this.cache.put(new Element(key, value));
	}

	@Override
	public void evict(Object key) {
		this.cache.remove(key);
	}

	@Override
	public void clear() {
		this.cache.removeAll();
	}

}

/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Collection;
import java.util.LinkedHashSet;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;

import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;
import org.springframework.util.Assert;

/**
 * CacheManager backed by an EhCache {@link net.sf.ehcache.CacheManager}.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public class EhCacheCacheManager extends AbstractTransactionSupportingCacheManager {

	private net.sf.ehcache.CacheManager cacheManager;


	/**
	 * Create a new EhCacheCacheManager, setting the target EhCache CacheManager
	 * through the {@link #setCacheManager} bean property.
	 */
	public EhCacheCacheManager() {
	}

	/**
	 * Create a new EhCacheCacheManager for the given backing EhCache CacheManager.
	 * @param cacheManager the backing EhCache {@link net.sf.ehcache.CacheManager}
	 */
	public EhCacheCacheManager(net.sf.ehcache.CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}


	/**
	 * Set the backing EhCache {@link net.sf.ehcache.CacheManager}.
	 */
	public void setCacheManager(net.sf.ehcache.CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * Return the backing EhCache {@link net.sf.ehcache.CacheManager}.
	 */
	public net.sf.ehcache.CacheManager getCacheManager() {
		return this.cacheManager;
	}

	@Override
	public void afterPropertiesSet() {
		if (getCacheManager() == null) {
			setCacheManager(EhCacheManagerUtils.buildCacheManager());
		}
		super.afterPropertiesSet();
	}


	@Override
	protected Collection<Cache> loadCaches() {
		Status status = getCacheManager().getStatus();
		Assert.isTrue(Status.STATUS_ALIVE.equals(status),
				"An 'alive' EhCache CacheManager is required - current cache is " + status.toString());

		String[] names = getCacheManager().getCacheNames();
		Collection<Cache> caches = new LinkedHashSet<Cache>(names.length);
		for (String name : names) {
			caches.add(new EhCacheCache(getCacheManager().getEhcache(name)));
		}
		return caches;
	}

	@Override
	protected Cache getMissingCache(String name) {
		// check the EhCache cache again
		// (in case the cache was added at runtime)
		Ehcache ehcache = getCacheManager().getEhcache(name);
		if (ehcache != null) {
			return new EhCacheCache(ehcache);
		}
		return null;
	}

}

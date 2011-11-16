/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * Composite {@link CacheManager} implementation that iterates
 * over a given collection of {@link CacheManager} instances.
 *
 * Allows {@link NoOpCacheManager} to be automatically added to the list for handling
 * the cache declarations without a backing store.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
public class CompositeCacheManager implements InitializingBean, CacheManager {

	private List<CacheManager> cacheManagers;

	private boolean fallbackToNoOpCache = false;


	public void setCacheManagers(Collection<CacheManager> cacheManagers) {
		Assert.notEmpty(cacheManagers, "cacheManagers Collection must not be empty");
		this.cacheManagers = new ArrayList<CacheManager>();
		this.cacheManagers.addAll(cacheManagers);
	}

	/**
	 * Indicate whether a {@link NoOpCacheManager} should be added at the end of the manager lists.
	 * In this case, any {@code getCache} requests not handled by the configured cache managers will
	 * be automatically handled by the {@link NoOpCacheManager} (and hence never return {@code null}).
	 */
	public void setFallbackToNoOpCache(boolean fallbackToNoOpCache) {
		this.fallbackToNoOpCache = fallbackToNoOpCache;
	}

	public void afterPropertiesSet() {
		if (this.fallbackToNoOpCache) {
			this.cacheManagers.add(new NoOpCacheManager());
		}
	}


	public Cache getCache(String name) {
		for (CacheManager cacheManager : this.cacheManagers) {
			Cache cache = cacheManager.getCache(name);
			if (cache != null) {
				return cache;
			}
		}
		return null;
	}

	public Collection<String> getCacheNames() {
		List<String> names = new ArrayList<String>();
		for (CacheManager manager : this.cacheManagers) {
			names.addAll(manager.getCacheNames());
		}
		return Collections.unmodifiableList(names);
	}

}

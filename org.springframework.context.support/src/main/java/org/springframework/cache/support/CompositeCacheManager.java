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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * Composite {@link CacheManager} implementation that iterates
 * over a given collection of {@link CacheManager} instances.
 * 
 * @author Costin Leau
 */
public class CompositeCacheManager implements CacheManager {

	private CacheManager[] cacheManagers;

	public <K, V> Cache<K, V> getCache(String name) {
		Cache<K, V> cache = null;
		for (CacheManager cacheManager : cacheManagers) {
			cache = cacheManager.getCache(name);
			if (cache != null) {
				return cache;
			}
		}

		return cache;
	}

	public Collection<String> getCacheNames() {
		List<String> names = new ArrayList<String>();
		for (CacheManager manager : cacheManagers) {
			names.addAll(manager.getCacheNames());
		}
		return Collections.unmodifiableCollection(names);
	}

	public void setCacheManagers(CacheManager[] cacheManagers) {
		Assert.notEmpty(cacheManagers, "non-null/empty array required");
		this.cacheManagers = cacheManagers.clone();
	}
}
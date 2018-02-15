/*
 * Copyright 2018-2018 the original author or authors.
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

package org.springframework.cache.cache2k;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.CacheEntry;
import org.cache2k.integration.AdvancedCacheLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.AbstractCacheTests;
import static org.junit.Assert.*;

/**
 * @author Jens Wilke
 */
public class Cache2kCacheTests extends AbstractCacheTests<Cache2kCache> {

	private Cache nativeCache;
	private Cache2kCache cache;

	@Before
	public void setUp() {
		Cache2kBuilder builder = Cache2kCacheManager.configureCache(Cache2kBuilder.forUnknownTypes(), CACHE_NAME);
		cache = Cache2kCacheManager.buildAndWrap(builder);
		nativeCache = cache.getNativeCache();
	}

	@After
	public void tearDown() {
		nativeCache.close();
	}

	@Override
	protected Cache2kCache getCache() {
		return cache;
	}

	@Override
	protected Cache getNativeCache() {
		return nativeCache;
	}

	@Test
	public void testNoLoadingCache() {
		assertFalse(cache.isLoaderPresent());
	}

	@Test
	public void testLoadingCache() {
		Cache2kBuilder builder =
				Cache2kCacheManager.configureCache(Cache2kBuilder.forUnknownTypes(), CACHE_NAME + "-withloader")
				.loader(key -> "123");
		Cache2kCache cacheWithLoader = Cache2kCacheManager.buildAndWrap(builder);
		assertTrue(cacheWithLoader.isLoaderPresent());
		cacheWithLoader.getNativeCache().close();
	}

	@Test
	public void testLoadingCacheAdvancedLoader() {
		Cache2kBuilder builder =
				Cache2kCacheManager.configureCache(Cache2kBuilder.forUnknownTypes(), CACHE_NAME + "-withloader")
						.loader(new AdvancedCacheLoader() {
							@Override
							public Object load(
									final Object key, final long currentTime, final CacheEntry currentEntry) {
								return "123";
							}
						});
		Cache2kCache cacheWithLoader = Cache2kCacheManager.buildAndWrap(builder);
		assertTrue(cacheWithLoader.isLoaderPresent());
		cacheWithLoader.getNativeCache().close();
	}

}

/*
 * Copyright 2002-2015 the original author or authors.
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

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cache.AbstractCacheTests;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;

import static org.junit.Assert.*;

/**
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class EhCacheCacheTests extends AbstractCacheTests<EhCacheCache> {

	private CacheManager cacheManager;
	private Ehcache nativeCache;
	private EhCacheCache cache;

	@Before
	public void setUp() {
		cacheManager = new CacheManager(new Configuration().name("EhCacheCacheTests")
				.defaultCache(new CacheConfiguration("default", 100)));
		nativeCache = new net.sf.ehcache.Cache(new CacheConfiguration(CACHE_NAME, 100));
		cacheManager.addCache(nativeCache);

		cache = new EhCacheCache(nativeCache);
	}

	@After
	public void tearDown() {
		cacheManager.shutdown();
	}

	@Override
	protected EhCacheCache getCache() {
		return cache;
	}

	@Override
	protected Ehcache getNativeCache() {
		return nativeCache;
	}

	@Test
	public void testExpiredElements() throws Exception {
		Assume.group(TestGroup.LONG_RUNNING);
		String key = "brancusi";
		String value = "constantin";
		Element brancusi = new Element(key, value);
		// ttl = 10s
		brancusi.setTimeToLive(3);
		nativeCache.put(brancusi);

		assertEquals(value, cache.get(key).get());
		// wait for the entry to expire
		Thread.sleep(5 * 1000);
		assertNull(cache.get(key));
	}

}

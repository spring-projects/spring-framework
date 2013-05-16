/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.cache.infinispan;

import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

/**
 * @author Philippe Marschall
 */
public class InfinispanCacheTests {

	protected final static String CACHE_NAME = "testCache";

	protected org.infinispan.Cache nativeCache;

	protected EmbeddedCacheManager cacheManager;

	protected Cache cache;


	@Before
	public void setUp() throws Exception {
		cacheManager = new DefaultCacheManager("org/springframework/cache/infinispan/testInfinispan.xml", true);
		nativeCache = cacheManager.getCache(CACHE_NAME, true);
		cache = new InfinispanCache(nativeCache);
		cache.clear();
	}

	@After
	public void tearDown() {
		nativeCache.stop();
		cacheManager.stop();
	}


	@Test
	public void testCacheName() {
		assertEquals(CACHE_NAME, cache.getName());
	}

	@Test
	public void testNativeCache() {
		assertSame(nativeCache, cache.getNativeCache());
	}

	@Test
	public void testCachePut() {
		Object key = "enescu";
		Object value = "george";

		assertNull(cache.get(key));
		cache.put(key, value);
		assertEquals(value, cache.get(key).get());
	}

	@Test
	public void testCacheRemove() {
		Object key = "enescu";
		Object value = "george";

		assertNull(cache.get(key));
		cache.put(key, value);
	}

	@Test
	public void testCacheClear() {
		assertNull(cache.get("enescu"));
		cache.put("enescu", "george");
		assertNull(cache.get("vlaicu"));
		cache.put("vlaicu", "aurel");
		cache.clear();
		assertNull(cache.get("vlaicu"));
		assertNull(cache.get("enescu"));
	}

	@Test
	public void testExpiredElements() throws Exception {
		Assume.group(TestGroup.LONG_RUNNING);
		String key = "brancusi";
		String value = "constantin";
		// ttl = 10s
		nativeCache.put(key, value, 3, SECONDS);

		assertEquals(value, cache.get(key).get());
		// wait for the entry to expire
		Thread.sleep(5 * 1000);
		assertNull(cache.get(key));
	}

}

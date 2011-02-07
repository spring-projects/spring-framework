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

package org.springframework.cache.vendor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.Cache;

/**
 * Test for native cache implementations.
 * 
 * @author Costin Leau
 */
public abstract class AbstractNativeCacheTest<T> {

	private T nativeCache;
	private Cache cache;
	protected final static String CACHE_NAME = "testCache";

	@Before
	public void setUp() throws Exception {
		nativeCache = createNativeCache();
		cache = createCache(nativeCache);
		cache.clear();
	}


	protected abstract T createNativeCache() throws Exception;

	protected abstract Cache createCache(T nativeCache);


	@Test
	public void testCacheName() throws Exception {
		assertEquals(CACHE_NAME, cache.getName());
	}

	@Test
	public void testNativeCache() throws Exception {
		assertSame(nativeCache, cache.getNativeCache());
	}

	@Test
	public void testCachePut() throws Exception {

		Object key = "enescu";
		Object value = "george";

		assertNull(cache.get(key));
		cache.put(key, value);
		assertEquals(value, cache.get(key));
	}

	@Test
	public void testCacheRemove() throws Exception {
		Object key = "enescu";
		Object value = "george";

		assertNull(cache.get(key));
		cache.put(key, value);
		assertEquals(value, cache.remove(key));
		assertNull(cache.get(key));
	}

	@Test
	public void testCacheClear() throws Exception {
		assertNull(cache.get("enescu"));
		cache.put("enescu", "george");
		assertNull(cache.get("vlaicu"));
		cache.put("vlaicu", "aurel");
		cache.clear();
		assertNull(cache.get("vlaicu"));
		assertNull(cache.get("enescu"));
	}

	// concurrent map tests
	@Test
	public void testPutIfAbsent() throws Exception {
		Object key = "enescu";
		Object value1 = "george";
		Object value2 = "geo";

		assertNull(cache.get("enescu"));
		cache.put(key, value1);
		cache.putIfAbsent(key, value2);
		assertEquals(value1, cache.get(key));
	}

	@Test
	public void testConcurrentRemove() throws Exception {
		Object key = "enescu";
		Object value1 = "george";
		Object value2 = "geo";

		assertNull(cache.get("enescu"));
		cache.put(key, value1);
		// no remove
		cache.remove(key, value2);
		assertEquals(value1, cache.get(key));
		// one remove
		cache.remove(key, value1);
		assertNull(cache.get("enescu"));
	}

	@Test
	public void testConcurrentReplace() throws Exception {
		Object key = "enescu";
		Object value1 = "george";
		Object value2 = "geo";

		assertNull(cache.get("enescu"));
		cache.put(key, value1);
		cache.replace(key, value2);
		assertEquals(value2, cache.get(key));
		cache.remove(key);
		cache.replace(key, value1);
		assertNull(cache.get("enescu"));
	}

	@Test
	public void testConcurrentReplaceIfEqual() throws Exception {
		Object key = "enescu";
		Object value1 = "george";
		Object value2 = "geo";

		assertNull(cache.get("enescu"));
		cache.put(key, value1);
		assertEquals(value1, cache.get(key));
		// no replace
		cache.replace(key, value2, value1);
		assertEquals(value1, cache.get(key));
		cache.replace(key, value1, value2);
		assertEquals(value2, cache.get(key));
		cache.replace(key, value2, value1);
		assertEquals(value1, cache.get(key));
	}
}
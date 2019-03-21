/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.concurrent;

import org.junit.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class ConcurrentMapCacheManagerTests {

	@Test
	public void testDynamicMode() {
		CacheManager cm = new ConcurrentMapCacheManager();
		Cache cache1 = cm.getCache("c1");
		assertTrue(cache1 instanceof ConcurrentMapCache);
		Cache cache1again = cm.getCache("c1");
		assertSame(cache1again, cache1);
		Cache cache2 = cm.getCache("c2");
		assertTrue(cache2 instanceof ConcurrentMapCache);
		Cache cache2again = cm.getCache("c2");
		assertSame(cache2again, cache2);
		Cache cache3 = cm.getCache("c3");
		assertTrue(cache3 instanceof ConcurrentMapCache);
		Cache cache3again = cm.getCache("c3");
		assertSame(cache3again, cache3);

		cache1.put("key1", "value1");
		assertEquals("value1", cache1.get("key1").get());
		cache1.put("key2", 2);
		assertEquals(2, cache1.get("key2").get());
		cache1.put("key3", null);
		assertNull(cache1.get("key3").get());
		cache1.put("key3", null);
		assertNull(cache1.get("key3").get());
		cache1.evict("key3");
		assertNull(cache1.get("key3"));

		assertEquals("value1", cache1.putIfAbsent("key1", "value1x").get());
		assertEquals("value1", cache1.get("key1").get());
		assertEquals(2, cache1.putIfAbsent("key2", 2.1).get());
		assertNull(cache1.putIfAbsent("key3", null));
		assertNull(cache1.get("key3").get());
		assertNull(cache1.putIfAbsent("key3", null).get());
		assertNull(cache1.get("key3").get());
		cache1.evict("key3");
		assertNull(cache1.get("key3"));
	}

	@Test
	public void testStaticMode() {
		ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager("c1", "c2");
		Cache cache1 = cm.getCache("c1");
		assertTrue(cache1 instanceof ConcurrentMapCache);
		Cache cache1again = cm.getCache("c1");
		assertSame(cache1again, cache1);
		Cache cache2 = cm.getCache("c2");
		assertTrue(cache2 instanceof ConcurrentMapCache);
		Cache cache2again = cm.getCache("c2");
		assertSame(cache2again, cache2);
		Cache cache3 = cm.getCache("c3");
		assertNull(cache3);

		cache1.put("key1", "value1");
		assertEquals("value1", cache1.get("key1").get());
		cache1.put("key2", 2);
		assertEquals(2, cache1.get("key2").get());
		cache1.put("key3", null);
		assertNull(cache1.get("key3").get());
		cache1.evict("key3");
		assertNull(cache1.get("key3"));

		cm.setAllowNullValues(false);
		Cache cache1x = cm.getCache("c1");
		assertTrue(cache1x instanceof ConcurrentMapCache);
		assertTrue(cache1x != cache1);
		Cache cache2x = cm.getCache("c2");
		assertTrue(cache2x instanceof ConcurrentMapCache);
		assertTrue(cache2x != cache2);
		Cache cache3x = cm.getCache("c3");
		assertNull(cache3x);

		cache1x.put("key1", "value1");
		assertEquals("value1", cache1x.get("key1").get());
		cache1x.put("key2", 2);
		assertEquals(2, cache1x.get("key2").get());
		try {
			cache1x.put("key3", null);
			fail("Should have thrown NullPointerException");
		}
		catch (NullPointerException ex) {
			// expected
		}

		cm.setAllowNullValues(true);
		Cache cache1y = cm.getCache("c1");

		cache1y.put("key3", null);
		assertNull(cache1y.get("key3").get());
		cache1y.evict("key3");
		assertNull(cache1y.get("key3"));
	}

	@Test
	public void testChangeStoreByValue() {
		ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager("c1", "c2");
		assertFalse(cm.isStoreByValue());
		Cache cache1 = cm.getCache("c1");
		assertTrue(cache1 instanceof ConcurrentMapCache);
		assertFalse(((ConcurrentMapCache)cache1).isStoreByValue());
		cache1.put("key", "value");

		cm.setStoreByValue(true);
		assertTrue(cm.isStoreByValue());
		Cache cache1x = cm.getCache("c1");
		assertTrue(cache1x instanceof ConcurrentMapCache);
		assertTrue(cache1x != cache1);
		assertNull(cache1x.get("key"));
	}

}

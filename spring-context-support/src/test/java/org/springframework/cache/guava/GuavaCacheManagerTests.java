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

package org.springframework.cache.guava;

import org.junit.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class GuavaCacheManagerTests {

	@Test
	public void testDynamicMode() {
		CacheManager cm = new GuavaCacheManager();
		Cache cache1 = cm.getCache("c1");
		assertTrue(cache1 instanceof GuavaCache);
		Cache cache1again = cm.getCache("c1");
		assertSame(cache1again, cache1);
		Cache cache2 = cm.getCache("c2");
		assertTrue(cache2 instanceof GuavaCache);
		Cache cache2again = cm.getCache("c2");
		assertSame(cache2again, cache2);
		Cache cache3 = cm.getCache("c3");
		assertTrue(cache3 instanceof GuavaCache);
		Cache cache3again = cm.getCache("c3");
		assertSame(cache3again, cache3);

		cache1.put("key1", "value1");
		assertEquals("value1", cache1.get("key1").get());
		cache1.put("key2", 2);
		assertEquals(2, cache1.get("key2").get());
		cache1.put("key3", null);
		assertNull(cache1.get("key3").get());
		cache1.evict("key3");
		assertNull(cache1.get("key3"));
	}

	@Test
	public void testStaticMode() {
		CacheManager cm = new GuavaCacheManager("c1", "c2");
		Cache cache1 = cm.getCache("c1");
		assertTrue(cache1 instanceof GuavaCache);
		Cache cache1again = cm.getCache("c1");
		assertSame(cache1again, cache1);
		Cache cache2 = cm.getCache("c2");
		assertTrue(cache2 instanceof GuavaCache);
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
	}

}

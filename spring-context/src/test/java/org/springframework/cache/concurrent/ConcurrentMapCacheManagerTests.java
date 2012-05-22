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

package org.springframework.cache.concurrent;

import org.junit.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
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
	}

}

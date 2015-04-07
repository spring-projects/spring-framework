/*
 * Copyright 2010-2013 the original author or authors.
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

package org.springframework.cache;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cache.support.NoOpCacheManager;

import static org.junit.Assert.*;

public class NoOpCacheManagerTests {

	private CacheManager manager;

	@Before
	public void setup() {
		manager = new NoOpCacheManager();
	}

	@Test
	public void testGetCache() throws Exception {
		Cache cache = manager.getCache("bucket");
		assertNotNull(cache);
		assertSame(cache, manager.getCache("bucket"));
	}

	@Test
	public void testNoOpCache() throws Exception {
		String name = UUID.randomUUID().toString();
		Cache cache = manager.getCache(name);
		assertEquals(name, cache.getName());
		Object key = new Object();
		cache.put(key, new Object());
		assertNull(cache.get(key));
		assertNull(cache.get(key, Object.class));
		assertNull(cache.getNativeCache());
	}

	@Test
	public void testCacheName() throws Exception {
		String name = "bucket";
		assertFalse(manager.getCacheNames().contains(name));
		manager.getCache(name);
		assertTrue(manager.getCacheNames().contains(name));
	}
}

/*
 * Copyright 2010-2017 the original author or authors.
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

package org.springframework.cache;

import java.util.UUID;

import org.junit.Test;

import org.springframework.cache.support.NoOpCacheManager;

import static org.junit.Assert.*;

/**
 * Tests for {@link NoOpCacheManager}.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 */
public class NoOpCacheManagerTests {

	private final CacheManager manager = new NoOpCacheManager();

	@Test
	public void testGetCache() throws Exception {
		Cache cache = this.manager.getCache("bucket");
		assertNotNull(cache);
		assertSame(cache, this.manager.getCache("bucket"));
	}

	@Test
	public void testNoOpCache() throws Exception {
		String name = createRandomKey();
		Cache cache = this.manager.getCache(name);
		assertEquals(name, cache.getName());
		Object key = new Object();
		cache.put(key, new Object());
		assertNull(cache.get(key));
		assertNull(cache.get(key, Object.class));
		assertSame(cache, cache.getNativeCache());
	}

	@Test
	public void testCacheName() throws Exception {
		String name = "bucket";
		assertFalse(this.manager.getCacheNames().contains(name));
		this.manager.getCache(name);
		assertTrue(this.manager.getCacheNames().contains(name));
	}

	@Test
	public void testCacheCallable() throws Exception {
		String name = createRandomKey();
		Cache cache = this.manager.getCache(name);
		Object returnValue = new Object();
		Object value = cache.get(new Object(), () -> returnValue);
		assertEquals(returnValue, value);
	}

	@Test
	public void testCacheGetCallableFail() {
		Cache cache = this.manager.getCache(createRandomKey());
		String key = createRandomKey();
		try {
			cache.get(key, () -> {
				throw new UnsupportedOperationException("Expected exception");
			});
		}
		catch (Cache.ValueRetrievalException ex) {
			assertNotNull(ex.getCause());
			assertEquals(UnsupportedOperationException.class, ex.getCause().getClass());
		}
	}

	private String createRandomKey() {
		return UUID.randomUUID().toString();
	}

}

/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.cache.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cache.AbstractValueAdaptingCacheTests;
import org.springframework.cache.Cache;

import static org.junit.Assert.*;

/**
 * @author Ben Manes
 * @author Stephane Nicoll
 */
public class CaffeineCacheTests extends AbstractValueAdaptingCacheTests<CaffeineCache> {

	private com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache;

	private CaffeineCache cache;

	private CaffeineCache cacheNoNull;

	@Before
	public void setUp() {
		nativeCache = Caffeine.newBuilder().build();
		cache = new CaffeineCache(CACHE_NAME, nativeCache);
		com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCacheNoNull
				= Caffeine.newBuilder().build();
		cacheNoNull =  new CaffeineCache(CACHE_NAME_NO_NULL, nativeCacheNoNull, false);
	}

	@Override
	protected CaffeineCache getCache() {
		return getCache(true);
	}

	@Override
	protected CaffeineCache getCache(boolean allowNull) {
		return allowNull ? this.cache : this.cacheNoNull;
	}

	@Override
	protected Object getNativeCache() {
		return nativeCache;
	}

	@Test
	public void testPutIfAbsentNullValue() throws Exception {
		CaffeineCache cache = getCache();

		Object key = new Object();
		Object value = null;

		assertNull(cache.get(key));
		assertNull(cache.putIfAbsent(key, value));
		assertEquals(value, cache.get(key).get());
		Cache.ValueWrapper wrapper = cache.putIfAbsent(key, "anotherValue");
		assertNotNull(wrapper); // A value is set but is 'null'
		assertEquals(null, wrapper.get());
		assertEquals(value, cache.get(key).get()); // not changed
	}

}

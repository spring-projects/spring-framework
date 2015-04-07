/*
 * Copyright 2002-2014 the original author or authors.
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

import com.google.common.cache.CacheBuilder;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cache.AbstractCacheTests;
import org.springframework.cache.Cache;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class GuavaCacheTests extends AbstractCacheTests<GuavaCache> {

	private com.google.common.cache.Cache<Object, Object> nativeCache;
	private GuavaCache cache;

	@Before
	public void setUp() {
		nativeCache = CacheBuilder.newBuilder().build();
		cache = new GuavaCache(CACHE_NAME, nativeCache);
	}

	@Override
	protected GuavaCache getCache() {
		return cache;
	}

	@Override
	protected Object getNativeCache() {
		return nativeCache;
	}

	@Test
	public void putIfAbsentNullValue() throws Exception {
		GuavaCache cache = getCache();

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

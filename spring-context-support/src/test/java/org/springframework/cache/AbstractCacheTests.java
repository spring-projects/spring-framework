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

package org.springframework.cache;

import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractCacheTests<T extends Cache> {

	protected final static String CACHE_NAME = "testCache";

	protected abstract T getCache();

	protected abstract Object getNativeCache();


	@Test
	public void testCacheName() throws Exception {
		assertEquals(CACHE_NAME, getCache().getName());
	}

	@Test
	public void testNativeCache() throws Exception {
		assertSame(getNativeCache(), getCache().getNativeCache());
	}

	@Test
	public void testCachePut() throws Exception {
		T cache = getCache();

		String key = createRandomKey();
		Object value = "george";

		assertNull(cache.get(key));
		assertNull(cache.get(key, String.class));
		assertNull(cache.get(key, Object.class));

		cache.put(key, value);
		assertEquals(value, cache.get(key).get());
		assertEquals(value, cache.get(key, String.class));
		assertEquals(value, cache.get(key, Object.class));
		assertEquals(value, cache.get(key, null));

		cache.put(key, null);
		assertNotNull(cache.get(key));
		assertNull(cache.get(key).get());
		assertNull(cache.get(key, String.class));
		assertNull(cache.get(key, Object.class));
	}

	@Test
	public void testCachePutIfAbsent() throws Exception {
		T cache = getCache();

		String key = createRandomKey();
		Object value = "initialValue";

		assertNull(cache.get(key));
		assertNull(cache.putIfAbsent(key, value));
		assertEquals(value, cache.get(key).get());
		assertEquals("initialValue", cache.putIfAbsent(key, "anotherValue").get());
		assertEquals(value, cache.get(key).get()); // not changed
	}

	@Test
	public void testCacheRemove() throws Exception {
		T cache = getCache();

		String key = createRandomKey();
		Object value = "george";

		assertNull(cache.get(key));
		cache.put(key, value);
	}

	@Test
	public void testCacheClear() throws Exception {
		T cache = getCache();

		assertNull(cache.get("enescu"));
		cache.put("enescu", "george");
		assertNull(cache.get("vlaicu"));
		cache.put("vlaicu", "aurel");
		cache.clear();
		assertNull(cache.get("vlaicu"));
		assertNull(cache.get("enescu"));
	}


	private String createRandomKey() {
		return UUID.randomUUID().toString();
	}

}

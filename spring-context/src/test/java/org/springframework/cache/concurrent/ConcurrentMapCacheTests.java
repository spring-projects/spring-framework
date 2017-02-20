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

package org.springframework.cache.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cache.AbstractCacheTests;
import org.springframework.core.serializer.support.SerializationDelegate;

import static org.junit.Assert.*;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class ConcurrentMapCacheTests extends AbstractCacheTests<ConcurrentMapCache> {

	protected ConcurrentMap<Object, Object> nativeCache;

	protected ConcurrentMapCache cache;


	@Before
	public void setUp() throws Exception {
		nativeCache = new ConcurrentHashMap<Object, Object>();
		cache = new ConcurrentMapCache(CACHE_NAME, nativeCache, true);
		cache.clear();
	}

	@Override
	protected ConcurrentMapCache getCache() {
		return this.cache;
	}

	@Override
	protected ConcurrentMap<Object, Object> getNativeCache() {
		return this.nativeCache;
	}

	@Test
	public void testIsStoreByReferenceByDefault() {
		assertFalse(this.cache.isStoreByValue());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSerializer() {
		ConcurrentMapCache serializeCache = createCacheWithStoreByValue();
		assertTrue(serializeCache.isStoreByValue());

		Object key = createRandomKey();
		List<String> content = new ArrayList<>();
		content.addAll(Arrays.asList("one", "two", "three"));
		serializeCache.put(key, content);
		content.remove(0);
		List<String> entry = (List<String>) serializeCache.get(key).get();
		assertEquals(3, entry.size());
		assertEquals("one", entry.get(0));
	}

	@Test
	public void testNonSerializableContent() {
		ConcurrentMapCache serializeCache = createCacheWithStoreByValue();

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Failed to serialize");
		thrown.expectMessage(this.cache.getClass().getName());
		serializeCache.put(createRandomKey(), this.cache);
	}

	@Test
	public void testInvalidSerializedContent() {
		ConcurrentMapCache serializeCache = createCacheWithStoreByValue();

		String key = createRandomKey();
		this.nativeCache.put(key, "Some garbage");
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Failed to deserialize");
		thrown.expectMessage("Some garbage");
		serializeCache.get(key);
	}


	private ConcurrentMapCache createCacheWithStoreByValue() {
		return new ConcurrentMapCache(CACHE_NAME, nativeCache, true,
				new SerializationDelegate(ConcurrentMapCacheTests.class.getClassLoader()));
	}

}

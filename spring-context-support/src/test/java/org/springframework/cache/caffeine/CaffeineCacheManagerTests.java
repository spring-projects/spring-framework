/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.cache.caffeine;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Ben Manes
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class CaffeineCacheManagerTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testDynamicMode() {
		CacheManager cm = new CaffeineCacheManager();
		Cache cache1 = cm.getCache("c1");
		assertTrue(cache1 instanceof CaffeineCache);
		Cache cache1again = cm.getCache("c1");
		assertSame(cache1again, cache1);
		Cache cache2 = cm.getCache("c2");
		assertTrue(cache2 instanceof CaffeineCache);
		Cache cache2again = cm.getCache("c2");
		assertSame(cache2again, cache2);
		Cache cache3 = cm.getCache("c3");
		assertTrue(cache3 instanceof CaffeineCache);
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
		CaffeineCacheManager cm = new CaffeineCacheManager("c1", "c2");
		Cache cache1 = cm.getCache("c1");
		assertTrue(cache1 instanceof CaffeineCache);
		Cache cache1again = cm.getCache("c1");
		assertSame(cache1again, cache1);
		Cache cache2 = cm.getCache("c2");
		assertTrue(cache2 instanceof CaffeineCache);
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
		assertTrue(cache1x instanceof CaffeineCache);
		assertTrue(cache1x != cache1);
		Cache cache2x = cm.getCache("c2");
		assertTrue(cache2x instanceof CaffeineCache);
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
	public void changeCaffeineRecreateCache() {
		CaffeineCacheManager cm = new CaffeineCacheManager("c1");
		Cache cache1 = cm.getCache("c1");

		Caffeine<Object, Object> caffeine = Caffeine.newBuilder().maximumSize(10);
		cm.setCaffeine(caffeine);
		Cache cache1x = cm.getCache("c1");
		assertTrue(cache1x != cache1);

		cm.setCaffeine(caffeine); // Set same instance
		Cache cache1xx = cm.getCache("c1");
		assertSame(cache1x, cache1xx);
	}

	@Test
	public void changeCaffeineSpecRecreateCache() {
		CaffeineCacheManager cm = new CaffeineCacheManager("c1");
		Cache cache1 = cm.getCache("c1");

		cm.setCaffeineSpec(CaffeineSpec.parse("maximumSize=10"));
		Cache cache1x = cm.getCache("c1");
		assertTrue(cache1x != cache1);
	}

	@Test
	public void changeCacheSpecificationRecreateCache() {
		CaffeineCacheManager cm = new CaffeineCacheManager("c1");
		Cache cache1 = cm.getCache("c1");

		cm.setCacheSpecification("maximumSize=10");
		Cache cache1x = cm.getCache("c1");
		assertTrue(cache1x != cache1);
	}

	@Test
	public void changeCacheLoaderRecreateCache() {
		CaffeineCacheManager cm = new CaffeineCacheManager("c1");
		Cache cache1 = cm.getCache("c1");

		CacheLoader<Object, Object> loader = mockCacheLoader();
		cm.setCacheLoader(loader);
		Cache cache1x = cm.getCache("c1");
		assertTrue(cache1x != cache1);

		cm.setCacheLoader(loader); // Set same instance
		Cache cache1xx = cm.getCache("c1");
		assertSame(cache1x, cache1xx);
	}

	@Test
	public void setCacheNameNullRestoreDynamicMode() {
		CaffeineCacheManager cm = new CaffeineCacheManager("c1");
		assertNull(cm.getCache("someCache"));
		cm.setCacheNames(null);
		assertNotNull(cm.getCache("someCache"));
	}

	@Test
	public void cacheLoaderUseLoadingCache() {
		CaffeineCacheManager cm = new CaffeineCacheManager("c1");
		cm.setCacheLoader(new CacheLoader<Object, Object>() {
			@Override
			public Object load(Object key) throws Exception {
				if ("ping".equals(key)) {
					return "pong";
				}
				throw new IllegalArgumentException("I only know ping");
			}
		});
		Cache cache1 = cm.getCache("c1");
		Cache.ValueWrapper value = cache1.get("ping");
		assertNotNull(value);
		assertEquals("pong", value.get());

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("I only know ping");
		assertNull(cache1.get("foo"));
	}

	@Test
	public void setTicker() {
		CaffeineCacheManager cm = new CaffeineCacheManager("c1");
		FakeTicker ticker = new FakeTicker();
		cm.setTicker(ticker);

		Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
				.expireAfterWrite(1, TimeUnit.MINUTES);
		cm.setCaffeine(caffeine);

		Cache cache = cm.getCache("c1");
		cache.put("key1", "value1");
		assertEquals("value1", cache.get("key1").get());

		ticker.advance(30, TimeUnit.SECONDS);
		assertEquals("value1", cache.get("key1").get());

		ticker.advance(31, TimeUnit.SECONDS);
		assertNull(cache.get("key1"));
	}

	@SuppressWarnings("unchecked")
	private CacheLoader<Object, Object> mockCacheLoader() {
		return mock(CacheLoader.class);
	}


	static class FakeTicker implements Ticker {
		private long nanos;

		public void advance(long time, TimeUnit timeUnit) {
			nanos += timeUnit.toNanos(time);
		}

		public long read() {
			return this.nanos;
		}
	}
}

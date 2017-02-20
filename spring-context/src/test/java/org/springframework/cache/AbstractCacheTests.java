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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractCacheTests<T extends Cache> {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

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
		assertEquals(value, cache.get(key, (Class<?>) null));

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

	@Test
	public void testCacheGetCallable() {
		doTestCacheGetCallable("test");
	}

	@Test
	public void testCacheGetCallableWithNull() {
		doTestCacheGetCallable(null);
	}

	private void doTestCacheGetCallable(Object returnValue) {
		T cache = getCache();

		String key = createRandomKey();

		assertNull(cache.get(key));
		Object value = cache.get(key, () -> returnValue );
		assertEquals(returnValue, value);
		assertEquals(value, cache.get(key).get());
	}

	@Test
	public void testCacheGetCallableNotInvokedWithHit() {
		doTestCacheGetCallableNotInvokedWithHit("existing");
	}

	@Test
	public void testCacheGetCallableNotInvokedWithHitNull() {
		doTestCacheGetCallableNotInvokedWithHit(null);
	}

	private void doTestCacheGetCallableNotInvokedWithHit(Object initialValue) {
		T cache = getCache();

		String key = createRandomKey();
		cache.put(key, initialValue);

		Object value = cache.get(key, () -> {
			throw new IllegalStateException("Should not have been invoked");
		});
		assertEquals(initialValue, value);
	}

	@Test
	public void testCacheGetCallableFail() {
		T cache = getCache();

		String key = createRandomKey();
		assertNull(cache.get(key));

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

	/**
	 * Test that a call to get with a Callable concurrently properly synchronize the
	 * invocations.
	 */
	@Test
	public void testCacheGetSynchronized() throws InterruptedException {
		T cache = getCache();
		final AtomicInteger counter = new AtomicInteger();
		final List<Object> results = new CopyOnWriteArrayList<>();
		final CountDownLatch latch = new CountDownLatch(10);

		String key = createRandomKey();
		Runnable run = () -> {
			try {
				Integer value = cache.get(key, () -> {
					Thread.sleep(50); // make sure the thread will overlap
					return counter.incrementAndGet();
				});
				results.add(value);
			}
			finally {
				latch.countDown();
			}
		};

		for (int i = 0; i < 10; i++) {
			new Thread(run).start();
		}
		latch.await();

		assertEquals(10, results.size());
		results.forEach(r -> assertThat(r, is(1))); // Only one method got invoked
	}

	protected String createRandomKey() {
		return UUID.randomUUID().toString();
	}

}

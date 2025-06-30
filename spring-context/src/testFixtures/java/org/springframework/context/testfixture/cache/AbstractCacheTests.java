/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.testfixture.cache;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractCacheTests<T extends Cache> {

	protected static final String CACHE_NAME = "testCache";

	protected abstract T getCache();

	protected abstract Object getNativeCache();


	@Test
	protected void testCacheName() {
		assertThat(getCache().getName()).isEqualTo(CACHE_NAME);
	}

	@Test
	protected void testNativeCache() {
		assertThat(getCache().getNativeCache()).isSameAs(getNativeCache());
	}

	@Test
	protected void testCachePut() {
		T cache = getCache();

		String key = createRandomKey();
		Object value = "george";

		assertThat(cache.get(key)).isNull();
		assertThat(cache.get(key, String.class)).isNull();
		assertThat(cache.get(key, Object.class)).isNull();

		cache.put(key, value);
		assertThat(cache.get(key).get()).isEqualTo(value);
		assertThat(cache.get(key, String.class)).isEqualTo(value);
		assertThat(cache.get(key, Object.class)).isEqualTo(value);
		assertThat(cache.get(key, (Class<?>) null)).isEqualTo(value);

		cache.put(key, null);
		assertThat(cache.get(key)).isNotNull();
		assertThat(cache.get(key).get()).isNull();
		assertThat(cache.get(key, String.class)).isNull();
		assertThat(cache.get(key, Object.class)).isNull();
	}

	@Test
	protected void testCachePutIfAbsent() {
		T cache = getCache();

		String key = createRandomKey();
		Object value = "initialValue";

		assertThat(cache.get(key)).isNull();
		assertThat(cache.putIfAbsent(key, value)).isNull();
		assertThat(cache.get(key).get()).isEqualTo(value);
		assertThat(cache.putIfAbsent(key, "anotherValue").get()).isEqualTo("initialValue");
		// not changed
		assertThat(cache.get(key).get()).isEqualTo(value);
	}

	@Test
	protected void testCacheRemove() {
		T cache = getCache();

		String key = createRandomKey();
		Object value = "george";

		assertThat(cache.get(key)).isNull();
		cache.put(key, value);
	}

	@Test
	protected void testCacheClear() {
		T cache = getCache();

		assertThat(cache.get("enescu")).isNull();
		cache.put("enescu", "george");
		assertThat(cache.get("vlaicu")).isNull();
		cache.put("vlaicu", "aurel");
		cache.clear();
		assertThat(cache.get("vlaicu")).isNull();
		assertThat(cache.get("enescu")).isNull();
	}

	@Test
	protected void testCacheGetCallable() {
		doTestCacheGetCallable("test");
	}

	@Test
	protected void testCacheGetCallableWithNull() {
		doTestCacheGetCallable(null);
	}

	private void doTestCacheGetCallable(Object returnValue) {
		T cache = getCache();

		String key = createRandomKey();

		assertThat(cache.get(key)).isNull();
		Object value = cache.get(key, () -> returnValue);
		assertThat(value).isEqualTo(returnValue);
		assertThat(cache.get(key).get()).isEqualTo(value);
	}

	@Test
	protected void testCacheGetCallableNotInvokedWithHit() {
		doTestCacheGetCallableNotInvokedWithHit("existing");
	}

	@Test
	protected void testCacheGetCallableNotInvokedWithHitNull() {
		doTestCacheGetCallableNotInvokedWithHit(null);
	}

	private void doTestCacheGetCallableNotInvokedWithHit(Object initialValue) {
		T cache = getCache();

		String key = createRandomKey();
		cache.put(key, initialValue);

		Object value = cache.get(key, () -> {
			throw new IllegalStateException("Should not have been invoked");
		});
		assertThat(value).isEqualTo(initialValue);
	}

	@Test
	protected void testCacheGetCallableFail() {
		T cache = getCache();

		String key = createRandomKey();
		assertThat(cache.get(key)).isNull();

		try {
			cache.get(key, () -> {
				throw new UnsupportedOperationException("Expected exception");
			});
		}
		catch (Cache.ValueRetrievalException ex) {
			assertThat(ex.getCause()).isNotNull();
			assertThat(ex.getCause().getClass()).isEqualTo(UnsupportedOperationException.class);
		}
	}

	/**
	 * Test that a call to get with a Callable concurrently properly synchronize the
	 * invocations.
	 */
	@Test
	protected void testCacheGetSynchronized() throws InterruptedException {
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

		assertThat(results).hasSize(10);
		results.forEach(r -> assertThat(r).isEqualTo(1)); // Only one method got invoked
	}

	protected String createRandomKey() {
		return UUID.randomUUID().toString();
	}

}

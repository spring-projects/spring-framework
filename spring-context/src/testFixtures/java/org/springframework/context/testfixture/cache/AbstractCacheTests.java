/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.cache;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NullValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractCacheTests<T extends Cache> {

	protected static final String CACHE_NAME = "testCache";

	protected abstract T getCache();

	protected abstract Object getNativeCache();


	@Test
	void cacheName() {
		assertThat(getCache().getName()).isEqualTo(CACHE_NAME);
	}

	@Test
	void nativeCache() {
		assertThat(getCache().getNativeCache()).isSameAs(getNativeCache());
	}

	@Test
	void cachePut() {
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

		cache.put(key, BeanUtils.instantiateClass(NullValue.class));
		assertThat(cache.get(key)).isNotNull();
		assertThat(cache.get(key).get()).isNull();
		assertThat(cache.get(key, String.class)).isNull();
		assertThat(cache.get(key, Object.class)).isNull();
	}

	@Test
	void cachePutIfAbsent() {
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
	void cacheRemove() {
		T cache = getCache();

		String key = createRandomKey();
		Object value = "george";

		assertThat(cache.get(key)).isNull();
		cache.put(key, value);
	}

	@Test
	void cacheClear() {
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
	void cacheGetCallable() {
		doTestCacheGetCallable("test");
	}

	@Test
	void cacheGetCallableWithNull() {
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
	void cacheGetCallableNotInvokedWithHit() {
		doTestCacheGetCallableNotInvokedWithHit("existing");
	}

	@Test
	void cacheGetCallableNotInvokedWithHitNull() {
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
	void cacheGetCallableFail() {
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
	void cacheGetSynchronized() throws InterruptedException {
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

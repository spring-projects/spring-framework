/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.contextsupport.testfixture.jcache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractJCacheAnnotationTests {

	public static final String DEFAULT_CACHE = "default";

	public static final String EXCEPTION_CACHE = "exception";


	protected String keyItem;

	protected ApplicationContext ctx;

	private JCacheableService<?> service;

	private CacheManager cacheManager;

	protected abstract ApplicationContext getApplicationContext();

	@BeforeEach
	protected void setUp(TestInfo testInfo) {
		this.keyItem = testInfo.getTestMethod().get().getName();
		this.ctx = getApplicationContext();
		this.service = this.ctx.getBean(JCacheableService.class);
		this.cacheManager = this.ctx.getBean("cacheManager", CacheManager.class);
	}

	@Test
	protected void cache() {
		Object first = service.cache(this.keyItem);
		Object second = service.cache(this.keyItem);
		assertThat(second).isSameAs(first);
	}

	@Test
	protected void cacheNull() {
		Cache cache = getCache(DEFAULT_CACHE);

		assertThat(cache.get(this.keyItem)).isNull();

		Object first = service.cacheNull(this.keyItem);
		Object second = service.cacheNull(this.keyItem);
		assertThat(second).isSameAs(first);

		Cache.ValueWrapper wrapper = cache.get(this.keyItem);
		assertThat(wrapper).isNotNull();
		assertThat(wrapper.get()).isSameAs(first);
		assertThat(wrapper.get()).as("Cached value should be null").isNull();
	}

	@Test
	protected void cacheException() {
		Cache cache = getCache(EXCEPTION_CACHE);

		Object key = createKey(this.keyItem);
		assertThat(cache.get(key)).isNull();

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.cacheWithException(this.keyItem, true));

		Cache.ValueWrapper result = cache.get(key);
		assertThat(result).isNotNull();
		assertThat(result.get().getClass()).isEqualTo(UnsupportedOperationException.class);
	}

	@Test
	protected void cacheExceptionVetoed() {
		Cache cache = getCache(EXCEPTION_CACHE);

		Object key = createKey(this.keyItem);
		assertThat(cache.get(key)).isNull();

		assertThatNullPointerException().isThrownBy(() ->
				service.cacheWithException(this.keyItem, false));
		assertThat(cache.get(key)).isNull();
	}

	@Test
	protected void cacheCheckedException() {
		Cache cache = getCache(EXCEPTION_CACHE);

		Object key = createKey(this.keyItem);
		assertThat(cache.get(key)).isNull();
		assertThatIOException().isThrownBy(() ->
				service.cacheWithCheckedException(this.keyItem, true));

		Cache.ValueWrapper result = cache.get(key);
		assertThat(result).isNotNull();
		assertThat(result.get().getClass()).isEqualTo(IOException.class);
	}


	@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
	@Test
	protected void cacheExceptionRewriteCallStack() {
		long ref = service.exceptionInvocations();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.cacheWithException(this.keyItem, true))
			.satisfies(first -> {
				// Sanity check, this particular call has called the service
				// First call should not have been cached
				assertThat(service.exceptionInvocations()).isEqualTo(ref + 1);

				UnsupportedOperationException second = methodInCallStack(this.keyItem);
				// Sanity check, this particular call has *not* called the service
				// Second call should have been cached
				assertThat(service.exceptionInvocations()).isEqualTo(ref + 1);

				assertThat(first).hasCause(second.getCause());
				assertThat(first).hasMessage(second.getMessage());
				// Original stack must not contain any reference to methodInCallStack
				assertThat(contain(first, AbstractJCacheAnnotationTests.class.getName(), "methodInCallStack")).isFalse();
				assertThat(contain(second, AbstractJCacheAnnotationTests.class.getName(), "methodInCallStack")).isTrue();
			});
	}

	@Test
	protected void cacheAlwaysInvoke() {
		Object first = service.cacheAlwaysInvoke(this.keyItem);
		Object second = service.cacheAlwaysInvoke(this.keyItem);
		assertThat(second).isNotSameAs(first);
	}

	@Test
	protected void cacheWithPartialKey() {
		Object first = service.cacheWithPartialKey(this.keyItem, true);
		Object second = service.cacheWithPartialKey(this.keyItem, false);
		// second argument not used, see config
		assertThat(second).isSameAs(first);
	}

	@Test
	protected void cacheWithCustomCacheResolver() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		service.cacheWithCustomCacheResolver(this.keyItem);

		// Cache in mock cache
		assertThat(cache.get(key)).isNull();
	}

	@Test
	protected void cacheWithCustomKeyGenerator() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		service.cacheWithCustomKeyGenerator(this.keyItem, "ignored");

		assertThat(cache.get(key)).isNull();
	}

	@Test
	protected void put() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		assertThat(cache.get(key)).isNull();

		service.put(this.keyItem, value);

		Cache.ValueWrapper result = cache.get(key);
		assertThat(result).isNotNull();
		assertThat(result.get()).isEqualTo(value);
	}

	@Test
	protected void putWithException() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		assertThat(cache.get(key)).isNull();

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.putWithException(this.keyItem, value, true));

		Cache.ValueWrapper result = cache.get(key);
		assertThat(result).isNotNull();
		assertThat(result.get()).isEqualTo(value);
	}

	@Test
	protected void putWithExceptionVetoPut() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		assertThat(cache.get(key)).isNull();

		assertThatNullPointerException().isThrownBy(() ->
				service.putWithException(this.keyItem, value, false));
		assertThat(cache.get(key)).isNull();
	}

	@Test
	protected void earlyPut() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		assertThat(cache.get(key)).isNull();

		service.earlyPut(this.keyItem, value);

		Cache.ValueWrapper result = cache.get(key);
		assertThat(result).isNotNull();
		assertThat(result.get()).isEqualTo(value);
	}

	@Test
	protected void earlyPutWithException() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		assertThat(cache.get(key)).isNull();

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.earlyPutWithException(this.keyItem, value, true));

		Cache.ValueWrapper result = cache.get(key);
		assertThat(result).isNotNull();
		assertThat(result.get()).isEqualTo(value);
	}

	@Test
	protected void earlyPutWithExceptionVetoPut() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		assertThat(cache.get(key)).isNull();
		assertThatNullPointerException().isThrownBy(() ->
				service.earlyPutWithException(this.keyItem, value, false));
		// This will be cached anyway as the earlyPut has updated the cache before
		Cache.ValueWrapper result = cache.get(key);
		assertThat(result).isNotNull();
		assertThat(result.get()).isEqualTo(value);
	}

	@Test
	protected void remove() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		cache.put(key, value);

		service.remove(this.keyItem);

		assertThat(cache.get(key)).isNull();
	}

	@Test
	protected void removeWithException() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		cache.put(key, value);

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.removeWithException(this.keyItem, true));

		assertThat(cache.get(key)).isNull();
	}

	@Test
	protected void removeWithExceptionVetoRemove() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		cache.put(key, value);

		assertThatNullPointerException().isThrownBy(() ->
				service.removeWithException(this.keyItem, false));
		Cache.ValueWrapper wrapper = cache.get(key);
		assertThat(wrapper).isNotNull();
		assertThat(wrapper.get()).isEqualTo(value);
	}

	@Test
	protected void earlyRemove() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		cache.put(key, value);

		service.earlyRemove(this.keyItem);

		assertThat(cache.get(key)).isNull();
	}

	@Test
	protected void earlyRemoveWithException() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		cache.put(key, value);

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.earlyRemoveWithException(this.keyItem, true));
		assertThat(cache.get(key)).isNull();
	}

	@Test
	protected void earlyRemoveWithExceptionVetoRemove() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		Object value = new Object();
		cache.put(key, value);

		assertThatNullPointerException().isThrownBy(() ->
				service.earlyRemoveWithException(this.keyItem, false));
		// This will be removed anyway as the earlyRemove has removed the cache before
		assertThat(cache.get(key)).isNull();
	}

	@Test
	protected void removeAll() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		cache.put(key, new Object());

		service.removeAll();

		assertThat(isEmpty(cache)).isTrue();
	}

	@Test
	protected void removeAllWithException() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		cache.put(key, new Object());

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.removeAllWithException(true));

		assertThat(isEmpty(cache)).isTrue();
	}

	@Test
	protected void removeAllWithExceptionVetoRemove() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		cache.put(key, new Object());

		assertThatNullPointerException().isThrownBy(() ->
				service.removeAllWithException(false));
		assertThat(cache.get(key)).isNotNull();
	}

	@Test
	protected void earlyRemoveAll() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		cache.put(key, new Object());

		service.earlyRemoveAll();

		assertThat(isEmpty(cache)).isTrue();
	}

	@Test
	protected void earlyRemoveAllWithException() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		cache.put(key, new Object());

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				service.earlyRemoveAllWithException(true));
		assertThat(isEmpty(cache)).isTrue();
	}

	@Test
	protected void earlyRemoveAllWithExceptionVetoRemove() {
		Cache cache = getCache(DEFAULT_CACHE);

		Object key = createKey(this.keyItem);
		cache.put(key, new Object());

		assertThatNullPointerException().isThrownBy(() ->
				service.earlyRemoveAllWithException(false));
		// This will be removed anyway as the earlyRemove has removed the cache before
		assertThat(isEmpty(cache)).isTrue();
	}

	protected boolean isEmpty(Cache cache) {
		ConcurrentHashMap<?, ?> nativeCache = (ConcurrentHashMap<?, ?>) cache.getNativeCache();
		return nativeCache.isEmpty();
	}


	private Object createKey(Object... params) {
		return SimpleKeyGenerator.generateKey(params);
	}

	private Cache getCache(String name) {
		Cache cache = cacheManager.getCache(name);
		assertThat(cache).as("required cache " + name + " does not exist").isNotNull();
		return cache;
	}

	/**
	 * The only purpose of this method is to invoke a particular method on the
	 * service so that the call stack is different.
	 */
	private UnsupportedOperationException methodInCallStack(String keyItem) {
		try {
			service.cacheWithException(keyItem, true);
			throw new IllegalStateException("Should have thrown an exception");
		}
		catch (UnsupportedOperationException e) {
			return e;
		}
	}

	private boolean contain(Throwable t, String className, String methodName) {
		for (StackTraceElement element : t.getStackTrace()) {
			if (className.equals(element.getClassName()) && methodName.equals(element.getMethodName())) {
				return true;
			}
		}
		return false;
	}

}

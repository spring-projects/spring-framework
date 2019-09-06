/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.cache.config;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.jcache.config.JCacheableService;
import org.springframework.cache.jcache.support.TestableCacheKeyGenerator;
import org.springframework.cache.jcache.support.TestableCacheResolverFactory;

/**
 * Repository sample with a @CacheDefaults annotation
 *
 * <p>Note: copy/pasted from its original compilation because it needs to be
 * processed by the AspectJ compiler to wave the required aspects.
 *
 * @author Stephane Nicoll
 */
@CacheDefaults(cacheName = "default")
public class AnnotatedJCacheableService implements JCacheableService<Long> {

	private final AtomicLong counter = new AtomicLong();
	private final AtomicLong exceptionCounter = new AtomicLong();
	private final Cache defaultCache;

	public AnnotatedJCacheableService(Cache defaultCache) {
		this.defaultCache = defaultCache;
	}

	@Override
	@CacheResult
	public Long cache(String id) {
		return counter.getAndIncrement();
	}

	@Override
	@CacheResult
	public Long cacheNull(String id) {
		return null;
	}

	@Override
	@CacheResult(exceptionCacheName = "exception", nonCachedExceptions = NullPointerException.class)
	public Long cacheWithException(@CacheKey String id, boolean matchFilter) {
		throwException(matchFilter);
		return 0L; // Never reached
	}

	@Override
	@CacheResult(exceptionCacheName = "exception", nonCachedExceptions = NullPointerException.class)
	public Long cacheWithCheckedException(@CacheKey String id, boolean matchFilter) throws IOException {
		throwCheckedException(matchFilter);
		return 0L; // Never reached
	}

	@Override
	@CacheResult(skipGet = true)
	public Long cacheAlwaysInvoke(String id) {
		return counter.getAndIncrement();
	}

	@Override
	@CacheResult
	public Long cacheWithPartialKey(@CacheKey String id, boolean notUsed) {
		return counter.getAndIncrement();
	}

	@Override
	@CacheResult(cacheResolverFactory = TestableCacheResolverFactory.class)
	public Long cacheWithCustomCacheResolver(String id) {
		return counter.getAndIncrement();
	}

	@Override
	@CacheResult(cacheKeyGenerator = TestableCacheKeyGenerator.class)
	public Long cacheWithCustomKeyGenerator(String id, String anotherId) {
		return counter.getAndIncrement();
	}

	@Override
	@CachePut
	public void put(String id, @CacheValue Object value) {
	}

	@Override
	@CachePut(cacheFor = UnsupportedOperationException.class)
	public void putWithException(@CacheKey String id, @CacheValue Object value, boolean matchFilter) {
		throwException(matchFilter);
	}

	@Override
	@CachePut(afterInvocation = false)
	public void earlyPut(String id, @CacheValue Object value) {
		Object key = SimpleKeyGenerator.generateKey(id);
		Cache.ValueWrapper valueWrapper = defaultCache.get(key);
		if (valueWrapper == null) {
			throw new AssertionError("Excepted value to be put in cache with key " + key);
		}
		Object actual = valueWrapper.get();
		if (value != actual) { // instance check on purpose
			throw new AssertionError("Wrong value set in cache with key " + key + ". " +
					"Expected=" + value + ", but got=" + actual);
		}
	}

	@Override
	@CachePut(afterInvocation = false)
	public void earlyPutWithException(@CacheKey String id, @CacheValue Object value, boolean matchFilter) {
		throwException(matchFilter);
	}

	@Override
	@CacheRemove
	public void remove(String id) {
	}

	@Override
	@CacheRemove(noEvictFor = NullPointerException.class)
	public void removeWithException(@CacheKey String id, boolean matchFilter) {
		throwException(matchFilter);
	}

	@Override
	@CacheRemove(afterInvocation = false)
	public void earlyRemove(String id) {
		Object key = SimpleKeyGenerator.generateKey(id);
		Cache.ValueWrapper valueWrapper = defaultCache.get(key);
		if (valueWrapper != null) {
			throw new AssertionError("Value with key " + key + " expected to be already remove from cache");
		}
	}

	@Override
	@CacheRemove(afterInvocation = false, evictFor = UnsupportedOperationException.class)
	public void earlyRemoveWithException(@CacheKey String id, boolean matchFilter) {
		throwException(matchFilter);
	}

	@Override
	@CacheRemoveAll
	public void removeAll() {
	}

	@Override
	@CacheRemoveAll(noEvictFor = NullPointerException.class)
	public void removeAllWithException(boolean matchFilter) {
		throwException(matchFilter);
	}

	@Override
	@CacheRemoveAll(afterInvocation = false)
	public void earlyRemoveAll() {
		ConcurrentHashMap<?, ?> nativeCache = (ConcurrentHashMap<?, ?>) defaultCache.getNativeCache();
		if (!nativeCache.isEmpty()) {
			throw new AssertionError("Cache was expected to be empty");
		}
	}

	@Override
	@CacheRemoveAll(afterInvocation = false, evictFor = UnsupportedOperationException.class)
	public void earlyRemoveAllWithException(boolean matchFilter) {
		throwException(matchFilter);
	}

	@Deprecated
	public void noAnnotation() {
	}

	@Override
	public long exceptionInvocations() {
		return exceptionCounter.get();
	}

	private void throwException(boolean matchFilter) {
		long count = exceptionCounter.getAndIncrement();
		if (matchFilter) {
			throw new UnsupportedOperationException("Expected exception (" + count + ")");
		}
		else {
			throw new NullPointerException("Expected exception (" + count + ")");
		}
	}

	private void throwCheckedException(boolean matchFilter) throws IOException {
		long count = exceptionCounter.getAndIncrement();
		if (matchFilter) {
			throw new IOException("Expected exception (" + count + ")");
		}
		else {
			throw new NullPointerException("Expected exception (" + count + ")");
		}
	}

}

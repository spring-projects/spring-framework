/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

/**
 * Simple cacheable service.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class DefaultCacheableService implements CacheableService<Long> {

	private final AtomicLong counter = new AtomicLong();

	private final AtomicLong nullInvocations = new AtomicLong();


	@Override
	@Cacheable("testCache")
	public Long cache(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Cacheable("testCache")
	public Long cacheNull(Object arg1) {
		return null;
	}

	@Override
	@Cacheable(cacheNames = "testCache", sync = true)
	public Long cacheSync(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Cacheable(cacheNames = "testCache", sync = true)
	public Long cacheSyncNull(Object arg1) {
		return null;
	}

	@Override
	@CacheEvict(cacheNames = "testCache", key = "#p0")
	public void evict(Object arg1, Object arg2) {
	}

	@Override
	@CacheEvict("testCache")
	public void evictWithException(Object arg1) {
		throw new RuntimeException("exception thrown - evict should NOT occur");
	}

	@Override
	@CacheEvict(cacheNames = "testCache", beforeInvocation = true)
	public void evictEarly(Object arg1) {
		throw new RuntimeException("exception thrown - evict should still occur");
	}

	@Override
	@CacheEvict(cacheNames = "testCache", allEntries = true)
	public void evictAll(Object arg1) {
	}

	@Override
	@CacheEvict(cacheNames = "testCache", allEntries = true, beforeInvocation = true)
	public void evictAllEarly(Object arg1) {
		throw new RuntimeException("exception thrown - evict should still occur");
	}

	@Override
	@Cacheable(cacheNames = "testCache", condition = "#p0 == 3")
	public Long conditional(int classField) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Cacheable(cacheNames = "testCache", sync = true, condition = "#p0 == 3")
	public Long conditionalSync(int classField) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Cacheable(cacheNames = "testCache", unless = "#result > 10")
	public Long unless(int arg) {
		return (long) arg;
	}

	@Override
	@Cacheable(cacheNames = "testCache", key = "#p0")
	public Long key(Object arg1, Object arg2) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Cacheable(cacheNames = "testCache")
	public Long varArgsKey(Object... args) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Cacheable(cacheNames = "testCache", key = "#root.methodName")
	public Long name(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Cacheable(cacheNames = "testCache", key = "#root.methodName + #root.method.name + #root.targetClass + #root.target")
	public Long rootVars(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Cacheable(cacheNames = "testCache", keyGenerator = "customKeyGenerator")
	public Long customKeyGenerator(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Cacheable(cacheNames = "testCache", keyGenerator = "unknownBeanName")
	public Long unknownCustomKeyGenerator(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Cacheable(cacheNames = "testCache", cacheManager = "customCacheManager")
	public Long customCacheManager(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Cacheable(cacheNames = "testCache", cacheManager = "unknownBeanName")
	public Long unknownCustomCacheManager(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@CachePut("testCache")
	public Long update(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@CachePut(cacheNames = "testCache", condition = "#arg.equals(3)")
	public Long conditionalUpdate(Object arg) {
		return Long.valueOf(arg.toString());
	}

	@Override
	@Cacheable("testCache")
	public Long nullValue(Object arg1) {
		this.nullInvocations.incrementAndGet();
		return null;
	}

	@Override
	public Number nullInvocations() {
		return this.nullInvocations.get();
	}

	@Override
	@Cacheable("testCache")
	public Long throwChecked(Object arg1) throws Exception {
		throw new IOException(arg1.toString());
	}

	@Override
	@Cacheable("testCache")
	public Long throwUnchecked(Object arg1) {
		throw new UnsupportedOperationException(arg1.toString());
	}

	@Override
	@Cacheable(cacheNames = "testCache", sync = true)
	public Long throwCheckedSync(Object arg1) throws Exception {
		throw new IOException(arg1.toString());
	}

	@Override
	@Cacheable(cacheNames = "testCache", sync = true)
	public Long throwUncheckedSync(Object arg1) {
		throw new UnsupportedOperationException(arg1.toString());
	}

	// multi annotations

	@Override
	@Caching(cacheable = { @Cacheable("primary"), @Cacheable("secondary") })
	public Long multiCache(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Caching(evict = { @CacheEvict("primary"), @CacheEvict(cacheNames = "secondary", key = "#p0"), @CacheEvict(cacheNames = "primary", key = "#p0 + 'A'") })
	public Long multiEvict(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Caching(cacheable = { @Cacheable(cacheNames = "primary", key = "#root.methodName") }, evict = { @CacheEvict("secondary") })
	public Long multiCacheAndEvict(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Caching(cacheable = { @Cacheable(cacheNames = "primary", condition = "#p0 == 3") }, evict = { @CacheEvict("secondary") })
	public Long multiConditionalCacheAndEvict(Object arg1) {
		return this.counter.getAndIncrement();
	}

	@Override
	@Caching(put = { @CachePut("primary"), @CachePut("secondary") })
	public Long multiUpdate(Object arg1) {
		return Long.valueOf(arg1.toString());
	}

	@Override
	@CachePut(cacheNames = "primary", key = "#result.id")
	public TestEntity putRefersToResult(TestEntity arg1) {
		arg1.setId(Long.MIN_VALUE);
		return arg1;
	}

}

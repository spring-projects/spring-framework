/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.cache.config;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

/**
 * @author Costin Leau
 * @author Phillip Webb
 */
@Cacheable("default")
public class AnnotatedClassCacheableService implements CacheableService<Object> {

	private final AtomicLong counter = new AtomicLong();

	public static final AtomicLong nullInvocations = new AtomicLong();

	@Override
	public Object cache(Object arg1) {
		return counter.getAndIncrement();
	}

	@Override
	public Object conditional(int field) {
		return null;
	}

	@Override
	@Cacheable(value = "default", unless = "#result > 10")
	public Object unless(int arg) {
		return arg;
	}

	@Override
	@CacheEvict("default")
	public void invalidate(Object arg1) {
	}

	@Override
	@CacheEvict("default")
	public void evictWithException(Object arg1) {
		throw new RuntimeException("exception thrown - evict should NOT occur");
	}

	@Override
	@CacheEvict(value = "default", allEntries = true)
	public void evictAll(Object arg1) {
	}

	@Override
	@CacheEvict(value = "default", beforeInvocation = true)
	public void evictEarly(Object arg1) {
		throw new RuntimeException("exception thrown - evict should still occur");
	}

	@Override
	@CacheEvict(value = "default", key = "#p0")
	public void evict(Object arg1, Object arg2) {
	}

	@Override
	@CacheEvict(value = "default", key = "#p0", beforeInvocation = true)
	public void invalidateEarly(Object arg1, Object arg2) {
		throw new RuntimeException("exception thrown - evict should still occur");
	}

	@Override
	@Cacheable(value = "default", key = "#p0")
	public Object key(Object arg1, Object arg2) {
		return counter.getAndIncrement();
	}

	@Override
	@Cacheable(value = "default")
	public Object varArgsKey(Object... args) {
		return counter.getAndIncrement();
	}

	@Override
	@Cacheable(value = "default", key = "#root.methodName + #root.caches[0].name")
	public Object name(Object arg1) {
		return counter.getAndIncrement();
	}

	@Override
	@Cacheable(value = "default", key = "#root.methodName + #root.method.name + #root.targetClass + #root.target")
	public Object rootVars(Object arg1) {
		return counter.getAndIncrement();
	}

	@Override
	@CachePut("default")
	public Object update(Object arg1) {
		return counter.getAndIncrement();
	}

	@Override
	@CachePut(value = "default", condition = "#arg.equals(3)")
	public Object conditionalUpdate(Object arg) {
		return arg;
	}

	@Override
	public Object nullValue(Object arg1) {
		nullInvocations.incrementAndGet();
		return null;
	}

	@Override
	public Number nullInvocations() {
		return nullInvocations.get();
	}

	@Override
	public Long throwChecked(Object arg1) throws Exception {
		throw new UnsupportedOperationException(arg1.toString());
	}

	@Override
	public Long throwUnchecked(Object arg1) {
		throw new UnsupportedOperationException();
	}

	// multi annotations

	@Override
	@Caching(cacheable = { @Cacheable("primary"), @Cacheable("secondary") })
	public Object multiCache(Object arg1) {
		return counter.getAndIncrement();
	}

	@Override
	@Caching(evict = { @CacheEvict("primary"), @CacheEvict(value = "secondary", key = "#a0"),  @CacheEvict(value = "primary", key = "#p0 + 'A'") })
	public Object multiEvict(Object arg1) {
		return counter.getAndIncrement();
	}

	@Override
	@Caching(cacheable = { @Cacheable(value = "primary", key = "#root.methodName") }, evict = { @CacheEvict("secondary") })
	public Object multiCacheAndEvict(Object arg1) {
		return counter.getAndIncrement();
	}

	@Override
	@Caching(cacheable = { @Cacheable(value = "primary", condition = "#a0 == 3") }, evict = { @CacheEvict("secondary") })
	public Object multiConditionalCacheAndEvict(Object arg1) {
		return counter.getAndIncrement();
	}

	@Override
	@Caching(put = { @CachePut("primary"), @CachePut("secondary") })
	public Object multiUpdate(Object arg1) {
		return arg1;
	}

	@Override
	@CachePut(value="primary", key="#result.id")
	public TestEntity putRefersToResult(TestEntity arg1) {
		arg1.setId(Long.MIN_VALUE);
		return arg1;
	}
}

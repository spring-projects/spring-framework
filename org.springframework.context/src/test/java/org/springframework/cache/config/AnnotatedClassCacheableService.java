/*
 * Copyright 2010 the original author or authors.
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
import org.springframework.cache.annotation.Cacheable;

/**
 * @author Costin Leau
 */
@Cacheable("default")
public class AnnotatedClassCacheableService implements CacheableService {

	private final AtomicLong counter = new AtomicLong();
	public static final AtomicLong nullInvocations = new AtomicLong();

	public Object cache(Object arg1) {
		return counter.getAndIncrement();
	}

	public Object conditional(int field) {
		return null;
	}

	@CacheEvict("default")
	public void invalidate(Object arg1) {
	}

	@Cacheable(value = "default", key = "#p0")
	public Object key(Object arg1, Object arg2) {
		return counter.getAndIncrement();
	}

	@Cacheable(value = "default", key = "#root.methodName + #root.caches[0].name")
	public Object name(Object arg1) {
		return counter.getAndIncrement();
	}

	public Object nullValue(Object arg1) {
		nullInvocations.incrementAndGet();
		return null;
	}

	public Number nullInvocations() {
		return nullInvocations.get();
	}
}

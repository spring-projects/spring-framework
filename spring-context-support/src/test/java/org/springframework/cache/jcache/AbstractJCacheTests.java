/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.cache.jcache;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.jcache.interceptor.SimpleExceptionCacheResolver;
import org.springframework.cache.support.SimpleCacheManager;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractJCacheTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Rule
	public final TestName name = new TestName();

	protected final CacheManager cacheManager = createSimpleCacheManager("default", "simpleCache");

	protected final CacheResolver defaultCacheResolver = new SimpleCacheResolver(cacheManager);

	protected final CacheResolver defaultExceptionCacheResolver = new SimpleExceptionCacheResolver(cacheManager);

	protected final KeyGenerator defaultKeyGenerator = new SimpleKeyGenerator();

	protected static CacheManager createSimpleCacheManager(String... cacheNames) {
		SimpleCacheManager result = new SimpleCacheManager();
		List<Cache> caches = new ArrayList<>();
		for (String cacheName : cacheNames) {
			caches.add(new ConcurrentMapCache(cacheName));
		}
		result.setCaches(caches);
		result.afterPropertiesSet();
		return result;
	}

}

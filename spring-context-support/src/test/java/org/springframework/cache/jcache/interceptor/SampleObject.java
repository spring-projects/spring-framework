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

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author Stephane Nicoll
 */
class SampleObject {

	// Simple

	@CacheResult(cacheName = "simpleCache")
	public SampleObject simpleGet(Long id) {
		return null;
	}

	@CachePut(cacheName = "simpleCache")
	public void simplePut(Long id, @CacheValue SampleObject instance) {
	}

	@CacheRemove(cacheName = "simpleCache")
	public void simpleRemove(Long id) {
	}

	@CacheRemoveAll(cacheName = "simpleCache")
	public void simpleRemoveAll() {
	}

	@CacheResult(cacheName = "testSimple")
	public SampleObject anotherSimpleGet(String foo, Long bar) {
		return null;
	}

	// @CacheKey

	@CacheResult
	public SampleObject multiKeysGet(@CacheKey Long id, Boolean notUsed,
			@CacheKey String domain) {
		return null;
	}

	// @CacheValue

	@CachePut(cacheName = "simpleCache")
	public void noCacheValue(Long id) {
	}

	@CachePut(cacheName = "simpleCache")
	public void multiCacheValues(Long id, @CacheValue SampleObject instance,
			@CacheValue SampleObject anotherInstance) {
	}

	// Parameter annotation

	@CacheResult(cacheName = "simpleCache")
	public SampleObject annotatedGet(@CacheKey Long id, @Value("${foo}") String foo) {
		return null;
	}

	// Full config

	@CacheResult(cacheName = "simpleCache", skipGet = true,
			cachedExceptions = Exception.class, nonCachedExceptions = RuntimeException.class)
	public SampleObject fullGetConfig(@CacheKey Long id) {
		return null;
	}

	@CachePut(cacheName = "simpleCache", afterInvocation = false,
			cacheFor = Exception.class, noCacheFor = RuntimeException.class)
	public void fullPutConfig(@CacheKey Long id, @CacheValue SampleObject instance) {
	}

}

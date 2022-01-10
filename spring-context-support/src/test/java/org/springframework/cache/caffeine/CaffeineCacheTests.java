/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.cache.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.context.testfixture.cache.AbstractValueAdaptingCacheTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link CaffeineCache}.
 *
 * @author Ben Manes
 * @author Stephane Nicoll
 */
class CaffeineCacheTests extends AbstractValueAdaptingCacheTests<CaffeineCache> {

	private com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache;

	private CaffeineCache cache;

	private CaffeineCache cacheNoNull;

	@BeforeEach
	void setUp() {
		nativeCache = Caffeine.newBuilder().build();
		cache = new CaffeineCache(CACHE_NAME, nativeCache);
		com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCacheNoNull
				= Caffeine.newBuilder().build();
		cacheNoNull = new CaffeineCache(CACHE_NAME_NO_NULL, nativeCacheNoNull, false);
	}

	@Override
	protected CaffeineCache getCache() {
		return getCache(true);
	}

	@Override
	protected CaffeineCache getCache(boolean allowNull) {
		return allowNull ? this.cache : this.cacheNoNull;
	}

	@Override
	protected Object getNativeCache() {
		return nativeCache;
	}

	@Test
	void testLoadingCacheGet() {
		Object value = new Object();
		CaffeineCache loadingCache = new CaffeineCache(CACHE_NAME, Caffeine.newBuilder()
				.build(key -> value));
		ValueWrapper valueWrapper = loadingCache.get(new Object());
		assertThat(valueWrapper).isNotNull();
		assertThat(valueWrapper.get()).isEqualTo(value);
	}

	@Test
	void testLoadingCacheGetWithType() {
		String value = "value";
		CaffeineCache loadingCache = new CaffeineCache(CACHE_NAME, Caffeine.newBuilder()
				.build(key -> value));
		String valueWrapper = loadingCache.get(new Object(), String.class);
		assertThat(valueWrapper).isNotNull();
		assertThat(valueWrapper).isEqualTo(value);
	}

	@Test
	void testLoadingCacheGetWithWrongType() {
		String value = "value";
		CaffeineCache loadingCache = new CaffeineCache(CACHE_NAME, Caffeine.newBuilder()
				.build(key -> value));
		assertThatIllegalStateException().isThrownBy(() -> loadingCache.get(new Object(), Long.class));
	}

	@Test
	void testPutIfAbsentNullValue() {
		CaffeineCache cache = getCache();

		Object key = new Object();
		Object value = null;

		assertThat(cache.get(key)).isNull();
		assertThat(cache.putIfAbsent(key, value)).isNull();
		assertThat(cache.get(key).get()).isEqualTo(value);
		Cache.ValueWrapper wrapper = cache.putIfAbsent(key, "anotherValue");
		// A value is set but is 'null'
		assertThat(wrapper).isNotNull();
		assertThat(wrapper.get()).isNull();
		// not changed
		assertThat(cache.get(key).get()).isEqualTo(value);
	}

}

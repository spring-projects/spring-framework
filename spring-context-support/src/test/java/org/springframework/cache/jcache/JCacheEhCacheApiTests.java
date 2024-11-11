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

package org.springframework.cache.jcache;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.testfixture.cache.AbstractValueAdaptingCacheTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class JCacheEhCacheApiTests extends AbstractValueAdaptingCacheTests<JCacheCache> {

	private CacheManager cacheManager;

	private Cache<Object, Object> nativeCache;

	private JCacheCache cache;

	private JCacheCache cacheNoNull;


	@BeforeEach
	void setup() {
		this.cacheManager = getCachingProvider().getCacheManager();
		this.cacheManager.createCache(CACHE_NAME, new MutableConfiguration<>());
		this.cacheManager.createCache(CACHE_NAME_NO_NULL, new MutableConfiguration<>());
		this.nativeCache = this.cacheManager.getCache(CACHE_NAME);
		this.cache = new JCacheCache(this.nativeCache);
		Cache<Object, Object> nativeCacheNoNull =
				this.cacheManager.getCache(CACHE_NAME_NO_NULL);
		this.cacheNoNull = new JCacheCache(nativeCacheNoNull, false);
	}

	protected CachingProvider getCachingProvider() {
		return Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
	}

	@AfterEach
	void shutdown() {
		if (this.cacheManager != null) {
			this.cacheManager.close();
		}
	}

	@Override
	protected JCacheCache getCache() {
		return getCache(true);
	}

	@Override
	protected JCacheCache getCache(boolean allowNull) {
		return (allowNull ? this.cache : this.cacheNoNull);
	}

	@Override
	protected Object getNativeCache() {
		return this.nativeCache;
	}

	@Test
	void testPutIfAbsentNullValue() {
		JCacheCache cache = getCache(true);

		String key = createRandomKey();
		String value = null;

		assertThat(cache.get(key)).isNull();
		assertThat(cache.putIfAbsent(key, value)).isNull();
		assertThat(cache.get(key).get()).isEqualTo(value);
		org.springframework.cache.Cache.ValueWrapper wrapper = cache.putIfAbsent(key, "anotherValue");
		// A value is set but is 'null'
		assertThat(wrapper).isNotNull();
		assertThat(wrapper.get()).isNull();
		// not changed
		assertThat(cache.get(key).get()).isEqualTo(value);
	}

}

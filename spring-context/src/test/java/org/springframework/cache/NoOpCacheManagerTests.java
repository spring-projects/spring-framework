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

package org.springframework.cache;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.cache.support.NoOpCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoOpCacheManager}.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 */
class NoOpCacheManagerTests {

	private final CacheManager manager = new NoOpCacheManager();

	@Test
	void testGetCache() {
		Cache cache = this.manager.getCache("bucket");
		assertThat(cache).isNotNull();
		assertThat(this.manager.getCache("bucket")).isSameAs(cache);
	}

	@Test
	void testNoOpCache() {
		String name = createRandomKey();
		Cache cache = this.manager.getCache(name);
		assertThat(cache.getName()).isEqualTo(name);
		Object key = new Object();
		cache.put(key, new Object());
		assertThat(cache.get(key)).isNull();
		assertThat(cache.get(key, Object.class)).isNull();
		assertThat(cache.getNativeCache()).isSameAs(cache);
	}

	@Test
	void testCacheName() {
		String name = "bucket";
		assertThat(this.manager.getCacheNames()).doesNotContain(name);
		this.manager.getCache(name);
		assertThat(this.manager.getCacheNames()).contains(name);
	}

	@Test
	void testCacheCallable() {
		String name = createRandomKey();
		Cache cache = this.manager.getCache(name);
		Object returnValue = new Object();
		Object value = cache.get(new Object(), () -> returnValue);
		assertThat(value).isEqualTo(returnValue);
	}

	@Test
	void testCacheGetCallableFail() {
		Cache cache = this.manager.getCache(createRandomKey());
		String key = createRandomKey();
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

	private String createRandomKey() {
		return UUID.randomUUID().toString();
	}

}

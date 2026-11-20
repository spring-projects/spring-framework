/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void getCache() {
		Cache cache = this.manager.getCache("bucket");
		assertThat(cache).isNotNull();
		assertThat(this.manager.getCache("bucket")).isSameAs(cache);
	}

	@Test
	void noOpCache() {
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
	void cacheName() {
		String name = "bucket";
		assertThat(this.manager.getCacheNames()).doesNotContain(name);
		this.manager.getCache(name);
		assertThat(this.manager.getCacheNames()).contains(name);
	}

	@Test
	void cacheCallable() {
		String name = createRandomKey();
		Cache cache = this.manager.getCache(name);
		Object returnValue = new Object();
		Object value = cache.get(new Object(), () -> returnValue);
		assertThat(value).isEqualTo(returnValue);
	}

	@Test
	void cacheGetCallableFail() {
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

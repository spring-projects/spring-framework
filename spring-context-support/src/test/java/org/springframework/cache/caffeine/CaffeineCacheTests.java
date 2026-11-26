/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.caffeine;

import java.util.concurrent.CompletableFuture;

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
	void setup() {
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
		return (allowNull ? this.cache : this.cacheNoNull);
	}

	@Override
	protected Object getNativeCache() {
		return nativeCache;
	}


	@Test
	void loadingCacheGet() {
		Object value = new Object();
		CaffeineCache loadingCache = new CaffeineCache(CACHE_NAME, Caffeine.newBuilder()
				.build(key -> value));
		ValueWrapper cacheValue = loadingCache.get(new Object());
		assertThat(cacheValue).isNotNull();
		assertThat(cacheValue.get()).isSameAs(value);
	}

	@Test
	void loadingCacheGetWithType() {
		String value = "value";
		CaffeineCache loadingCache = new CaffeineCache(CACHE_NAME, Caffeine.newBuilder()
				.build(key -> value));
		String cacheValue = loadingCache.get(new Object(), String.class);
		assertThat(cacheValue).isNotNull();
		assertThat(cacheValue).isSameAs(value);
	}

	@Test
	void loadingCacheGetWithWrongType() {
		String value = "value";
		CaffeineCache loadingCache = new CaffeineCache(CACHE_NAME, Caffeine.newBuilder()
				.build(key -> value));
		assertThatIllegalStateException().isThrownBy(() -> loadingCache.get(new Object(), Long.class));
	}

	@Test
	void loadingCacheGetAsync() {
		Object value = new Object();
		CaffeineCache loadingCache = new CaffeineCache(CACHE_NAME, Caffeine.newBuilder()
				.buildAsync(key -> value), true);
		CompletableFuture<?> cacheValue = loadingCache.retrieve(new Object());
		assertThat(cacheValue).isNotNull();
		assertThat(cacheValue.join()).isSameAs(value);
	}

	@Test
	void putIfAbsentNullValue() {
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

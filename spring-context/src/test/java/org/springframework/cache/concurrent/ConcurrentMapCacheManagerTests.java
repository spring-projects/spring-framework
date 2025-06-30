/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.cache.concurrent;

import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
class ConcurrentMapCacheManagerTests {

	@Test
	void testDynamicMode() {
		CacheManager cm = new ConcurrentMapCacheManager();
		Cache cache1 = cm.getCache("c1");
		assertThat(cache1).isInstanceOf(ConcurrentMapCache.class);
		Cache cache1again = cm.getCache("c1");
		assertThat(cache1).isSameAs(cache1again);
		Cache cache2 = cm.getCache("c2");
		assertThat(cache2).isInstanceOf(ConcurrentMapCache.class);
		Cache cache2again = cm.getCache("c2");
		assertThat(cache2).isSameAs(cache2again);
		Cache cache3 = cm.getCache("c3");
		assertThat(cache3).isInstanceOf(ConcurrentMapCache.class);
		Cache cache3again = cm.getCache("c3");
		assertThat(cache3).isSameAs(cache3again);

		cache1.put("key1", "value1");
		assertThat(cache1.get("key1").get()).isEqualTo("value1");
		cache1.put("key2", 2);
		assertThat(cache1.get("key2").get()).isEqualTo(2);
		cache1.put("key3", null);
		assertThat(cache1.get("key3").get()).isNull();
		cache1.put("key3", null);
		assertThat(cache1.get("key3").get()).isNull();
		cache1.evict("key3");
		assertThat(cache1.get("key3")).isNull();

		assertThat(cache1.putIfAbsent("key1", "value1x").get()).isEqualTo("value1");
		assertThat(cache1.get("key1").get()).isEqualTo("value1");
		assertThat(cache1.putIfAbsent("key2", 2.1).get()).isEqualTo(2);
		assertThat(cache1.putIfAbsent("key3", null)).isNull();
		assertThat(cache1.get("key3").get()).isNull();
		assertThat(cache1.putIfAbsent("key3", null).get()).isNull();
		assertThat(cache1.get("key3").get()).isNull();
		cache1.evict("key3");
		assertThat(cache1.get("key3")).isNull();
	}

	@Test
	void testStaticMode() {
		ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager("c1", "c2");
		Cache cache1 = cm.getCache("c1");
		assertThat(cache1).isInstanceOf(ConcurrentMapCache.class);
		Cache cache1again = cm.getCache("c1");
		assertThat(cache1).isSameAs(cache1again);
		Cache cache2 = cm.getCache("c2");
		assertThat(cache2).isInstanceOf(ConcurrentMapCache.class);
		Cache cache2again = cm.getCache("c2");
		assertThat(cache2).isSameAs(cache2again);
		Cache cache3 = cm.getCache("c3");
		assertThat(cache3).isNull();

		cache1.put("key1", "value1");
		assertThat(cache1.get("key1").get()).isEqualTo("value1");
		cache1.put("key2", 2);
		assertThat(cache1.get("key2").get()).isEqualTo(2);
		cache1.put("key3", null);
		assertThat(cache1.get("key3").get()).isNull();
		cache1.evict("key3");
		assertThat(cache1.get("key3")).isNull();

		cm.setAllowNullValues(false);
		Cache cache1x = cm.getCache("c1");
		assertThat(cache1x).isInstanceOf(ConcurrentMapCache.class);
		assertThat(cache1x).isNotSameAs(cache1);
		Cache cache2x = cm.getCache("c2");
		assertThat(cache2x).isInstanceOf(ConcurrentMapCache.class);
		assertThat(cache2x).isNotSameAs(cache2);
		Cache cache3x = cm.getCache("c3");
		assertThat(cache3x).isNull();

		cache1x.put("key1", "value1");
		assertThat(cache1x.get("key1").get()).isEqualTo("value1");
		cache1x.put("key2", 2);
		assertThat(cache1x.get("key2").get()).isEqualTo(2);

		cm.setAllowNullValues(true);
		Cache cache1y = cm.getCache("c1");

		cache1y.put("key3", null);
		assertThat(cache1y.get("key3").get()).isNull();
		cache1y.evict("key3");
		assertThat(cache1y.get("key3")).isNull();
	}

	@Test
	void testChangeStoreByValue() {
		ConcurrentMapCacheManager cm = new ConcurrentMapCacheManager("c1", "c2");
		assertThat(cm.isStoreByValue()).isFalse();
		Cache cache1 = cm.getCache("c1");
		assertThat(cache1).isInstanceOf(ConcurrentMapCache.class);
		assertThat(((ConcurrentMapCache) cache1).isStoreByValue()).isFalse();
		cache1.put("key", "value");

		cm.setStoreByValue(true);
		assertThat(cm.isStoreByValue()).isTrue();
		Cache cache1x = cm.getCache("c1");
		assertThat(cache1x).isInstanceOf(ConcurrentMapCache.class);
		assertThat(cache1x).isNotSameAs(cache1);
		assertThat(cache1x.get("key")).isNull();
	}

}

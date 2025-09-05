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

package org.springframework.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ConcurrentLruCache}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author JongJun Kim
 */
class ConcurrentLruCacheTests {

	private final ConcurrentLruCache<String, String> cache = new ConcurrentLruCache<>(2, key -> key + "value");


	@Test
	void zeroCapacity() {
		ConcurrentLruCache<String, String> cache = new ConcurrentLruCache<>(0, key -> key + "value");

		assertThat(cache.capacity()).isZero();
		assertThat(cache.size()).isZero();

		assertThat(cache.get("k1")).isEqualTo("k1value");
		assertThat(cache.size()).isZero();
		assertThat(cache.contains("k1")).isFalse();

		assertThat(cache.get("k2")).isEqualTo("k2value");
		assertThat(cache.size()).isZero();
		assertThat(cache.contains("k1")).isFalse();
		assertThat(cache.contains("k2")).isFalse();

		assertThat(cache.get("k3")).isEqualTo("k3value");
		assertThat(cache.size()).isZero();
		assertThat(cache.contains("k1")).isFalse();
		assertThat(cache.contains("k2")).isFalse();
		assertThat(cache.contains("k3")).isFalse();
	}

	@Test
	void getAndSize() {
		assertThat(this.cache.capacity()).isEqualTo(2);
		assertThat(this.cache.size()).isEqualTo(0);
		assertThat(this.cache.get("k1")).isEqualTo("k1value");
		assertThat(this.cache.size()).isEqualTo(1);
		assertThat(this.cache.contains("k1")).isTrue();
		assertThat(this.cache.get("k2")).isEqualTo("k2value");
		assertThat(this.cache.size()).isEqualTo(2);
		assertThat(this.cache.contains("k1")).isTrue();
		assertThat(this.cache.contains("k2")).isTrue();
		assertThat(this.cache.get("k3")).isEqualTo("k3value");
		assertThat(this.cache.size()).isEqualTo(2);
		assertThat(this.cache.contains("k1")).isFalse();
		assertThat(this.cache.contains("k2")).isTrue();
		assertThat(this.cache.contains("k3")).isTrue();
	}

	@Test
	void removeAndSize() {
		assertThat(this.cache.get("k1")).isEqualTo("k1value");
		assertThat(this.cache.get("k2")).isEqualTo("k2value");
		assertThat(this.cache.size()).isEqualTo(2);
		assertThat(this.cache.contains("k1")).isTrue();
		assertThat(this.cache.contains("k2")).isTrue();
		this.cache.remove("k2");
		assertThat(this.cache.size()).isEqualTo(1);
		assertThat(this.cache.contains("k1")).isTrue();
		assertThat(this.cache.contains("k2")).isFalse();
		assertThat(this.cache.get("k3")).isEqualTo("k3value");
		assertThat(this.cache.size()).isEqualTo(2);
		assertThat(this.cache.contains("k1")).isTrue();
		assertThat(this.cache.contains("k2")).isFalse();
		assertThat(this.cache.contains("k3")).isTrue();
	}

	@Test
	void clearAndSize() {
		assertThat(this.cache.get("k1")).isEqualTo("k1value");
		assertThat(this.cache.get("k2")).isEqualTo("k2value");
		assertThat(this.cache.size()).isEqualTo(2);
		assertThat(this.cache.contains("k1")).isTrue();
		assertThat(this.cache.contains("k2")).isTrue();
		this.cache.clear();
		assertThat(this.cache.size()).isEqualTo(0);
		assertThat(this.cache.contains("k1")).isFalse();
		assertThat(this.cache.contains("k2")).isFalse();
		assertThat(this.cache.get("k3")).isEqualTo("k3value");
		assertThat(this.cache.size()).isEqualTo(1);
		assertThat(this.cache.contains("k1")).isFalse();
		assertThat(this.cache.contains("k2")).isFalse();
		assertThat(this.cache.contains("k3")).isTrue();
	}

	@Test
	void statisticsTracking() {
		ConcurrentLruCache.CacheStats initialStats = this.cache.getStats();
		assertThat(initialStats.getHits()).isZero();
		assertThat(initialStats.getMisses()).isZero();
		assertThat(initialStats.getEvictions()).isZero();
		assertThat(initialStats.getHitRatio()).isZero();
		assertThat(initialStats.getRequests()).isZero();
		assertThat(initialStats.getSize()).isZero();
		assertThat(initialStats.getCapacity()).isEqualTo(2);

		this.cache.get("k1");
		ConcurrentLruCache.CacheStats afterFirstGet = this.cache.getStats();
		assertThat(afterFirstGet.getHits()).isZero();
		assertThat(afterFirstGet.getMisses()).isEqualTo(1);
		assertThat(afterFirstGet.getEvictions()).isZero();
		assertThat(afterFirstGet.getHitRatio()).isZero();
		assertThat(afterFirstGet.getRequests()).isEqualTo(1);

		this.cache.get("k1");
		ConcurrentLruCache.CacheStats afterSecondGet = this.cache.getStats();
		assertThat(afterSecondGet.getHits()).isEqualTo(1);
		assertThat(afterSecondGet.getMisses()).isEqualTo(1);
		assertThat(afterSecondGet.getHitRatio()).isEqualTo(0.5);
		assertThat(afterSecondGet.getRequests()).isEqualTo(2);
	}

	@Test
	void statisticsWithEviction() {
		this.cache.get("k1");
		this.cache.get("k2");
		this.cache.get("k3"); // This should evict k1
		
		ConcurrentLruCache.CacheStats stats = this.cache.getStats();
		assertThat(stats.getEvictions()).isGreaterThan(0);
		assertThat(stats.getMisses()).isEqualTo(3);
		assertThat(stats.getHits()).isZero();
		assertThat(stats.getSize()).isEqualTo(2);
	}

	@Test
	void cacheStatsToString() {
		this.cache.get("k1");
		this.cache.get("k1");
		this.cache.get("k2");
		
		ConcurrentLruCache.CacheStats stats = this.cache.getStats();
		String statsString = stats.toString();
		
		assertThat(statsString).contains("CacheStats{");
		assertThat(statsString).contains("hits=");
		assertThat(statsString).contains("misses=");
		assertThat(statsString).contains("evictions=");
		assertThat(statsString).contains("hitRatio=");
		assertThat(statsString).contains("size=");
	}

	@Test
	void disabledStatistics() {
		ConcurrentLruCache<String, String> cacheWithoutStats = 
				new ConcurrentLruCache<>(2, key -> key + "value", false);
		
		assertThat(cacheWithoutStats.isStatsEnabled()).isFalse();
		
		// Cache operations should work normally
		assertThat(cacheWithoutStats.get("k1")).isEqualTo("k1value");
		assertThat(cacheWithoutStats.get("k1")).isEqualTo("k1value");
		
		// Getting stats should throw exception
		assertThatThrownBy(() -> cacheWithoutStats.getStats())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Statistics collection is disabled");
	}

	@Test
	void enabledStatisticsByDefault() {
		ConcurrentLruCache<String, String> defaultCache = 
				new ConcurrentLruCache<>(2, key -> key + "value");
		
		assertThat(defaultCache.isStatsEnabled()).isTrue();
		
		// Should be able to get stats
		assertThat(defaultCache.getStats()).isNotNull();
	}

	@Test
	void statisticsPerformanceOverhead() {
		// Test with statistics disabled
		ConcurrentLruCache<String, String> disabledCache = 
				new ConcurrentLruCache<>(10, key -> key + "value", false);
		
		long startTime = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			disabledCache.get("key" + (i % 100));
		}
		long disabledTime = System.nanoTime() - startTime;
		
		// Test with statistics enabled
		ConcurrentLruCache<String, String> enabledCache = 
				new ConcurrentLruCache<>(10, key -> key + "value", true);
		
		startTime = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			enabledCache.get("key" + (i % 100));
		}
		long enabledTime = System.nanoTime() - startTime;
		
		// Both should complete (no specific performance assertions since it's hardware dependent)
		assertThat(disabledTime).isGreaterThan(0);
		assertThat(enabledTime).isGreaterThan(0);
		assertThat(enabledCache.isStatsEnabled()).isTrue();
		assertThat(disabledCache.isStatsEnabled()).isFalse();
	}

}

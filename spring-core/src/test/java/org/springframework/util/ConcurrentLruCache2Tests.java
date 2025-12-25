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

/**
 * Tests for {@link ConcurrentLruCache2}.
 *
 */
class ConcurrentLruCache2Tests {

	@Test
	void missReturnsNullAndDoesNotPopulate() {
		ConcurrentLruCache2<String, String> cache = new ConcurrentLruCache2<>(2);

		assertThat(cache.get("k1")).isNull();
		assertThat(cache.size()).isZero();
		assertThat(cache.contains("k1")).isFalse();
	}

	@Test
	void manualPutCachesValueAfterMiss() {
		ConcurrentLruCache2<String, String> cache = new ConcurrentLruCache2<>(2);

		assertThat(cache.get("k1")).isNull();
		assertThat(cache.size()).isZero();

		cache.put("k1", "v1");
		cache.put("k2", "v2");

		assertThat(cache.get("k1")).isEqualTo("v1");
		assertThat(cache.get("k2")).isEqualTo("v2");
		assertThat(cache.size()).isEqualTo(2);
		assertThat(cache.contains("k1")).isTrue();
		assertThat(cache.contains("k2")).isTrue();
	}

	@Test
	void differsFromLegacyCacheThatGeneratesOnMiss() {
		ConcurrentLruCache<String, String> legacy = new ConcurrentLruCache<>(2, key -> key + "value");
		ConcurrentLruCache2<String, String> cache = new ConcurrentLruCache2<>(2);

		assertThat(cache.get("k1")).isNull();
		assertThat(cache.size()).isZero();
		assertThat(cache.contains("k1")).isFalse();

		assertThat(legacy.get("k1")).isEqualTo("k1value");
		assertThat(legacy.contains("k1")).isTrue();
		assertThat(legacy.size()).isEqualTo(1);
	}

}


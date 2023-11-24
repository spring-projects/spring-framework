/*
 * Copyright 2002-2023 the original author or authors.
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
 * Tests for {@link ConcurrentLruCache}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
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

}

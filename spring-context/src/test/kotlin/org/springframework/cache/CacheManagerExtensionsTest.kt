/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.cache

import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class CacheManagerExtensionsTest {

	@Test
	fun `Check operator get variant`() {
		val cm = createCacheManager("c")
		assertNotEquals(null, cm["c"])
	}

	private fun createCacheManager(cacheName: String = "c"): CacheManager =
		ConcurrentMapCacheManager(cacheName)
}
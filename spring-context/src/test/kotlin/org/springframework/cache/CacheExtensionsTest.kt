/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class CacheExtensionsTest {

	@Test
	fun `Check inline get variant`() {
		val cache = createCache()
		cache.put("k", "v")
		assertEquals("v", cache.get<String>("k"))
	}

	@Test
	fun `Check operator get variant`() {
		val cache = createCache()
		cache.put("k", "v")
		assertEquals("v", cache["k"]?.get() as String?)
	}

	@Test
	fun `Check operator set variant`() {
		val cache = createCache()
		cache["k"] = "v"
		assertEquals("v", cache.get("k", String::class.java))
	}

	private fun createCache(cacheName: String = "c"): Cache =
		ConcurrentMapCacheManager(cacheName).getCache(cacheName)!!
}
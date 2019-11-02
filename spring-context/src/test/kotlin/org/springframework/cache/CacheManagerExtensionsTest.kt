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

	private fun createCacheManager(cacheName: String = "c"): CacheManager = ConcurrentMapCacheManager(cacheName)
}
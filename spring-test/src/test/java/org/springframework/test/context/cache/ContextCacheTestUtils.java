/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.cache;

import org.springframework.util.StringUtils;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Collection of utility methods for testing scenarios involving the
 * {@link ContextCache}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
public class ContextCacheTestUtils {

	/**
	 * Reset the state of the static context cache in {@link DefaultCacheAwareContextLoaderDelegate}.
	 */
	public static void resetContextCache() {
		DefaultCacheAwareContextLoaderDelegate.defaultContextCache.reset();
	}

	/**
	 * Assert the statistics of the static context cache in {@link DefaultCacheAwareContextLoaderDelegate}.
	 *
	 * @param expectedSize the expected number of contexts in the cache
	 * @param expectedHitCount the expected hit count
	 * @param expectedMissCount the expected miss count
	 */
	public static void assertContextCacheStatistics(int expectedSize, int expectedHitCount, int expectedMissCount) {
		ContextCache contextCache = DefaultCacheAwareContextLoaderDelegate.defaultContextCache;
		String context = (StringUtils.hasText((String) null) ? " (" + null + ")" : "");

		assertSoftly(softly -> {
			softly.assertThat(contextCache.size())
					.as("contexts in cache" + context).isEqualTo(expectedSize);
			softly.assertThat(contextCache.getHitCount())
					.as("cache hits" + context).isEqualTo(expectedHitCount);
			softly.assertThat(contextCache.getMissCount())
					.as("cache misses" + context).isEqualTo(expectedMissCount);
		});
	}

	/**
	 * Assert the statistics of the static context cache in {@link DefaultCacheAwareContextLoaderDelegate}.
	 *
	 * @param usageScenario the scenario in which the statistics are used
	 * @param expectedSize the expected number of contexts in the cache
	 * @param expectedContextUsageCount the expected number of actively running
	 * contexts in the cache
	 * @param expectedHitCount the expected hit count
	 * @param expectedMissCount the expected miss count
	 */
	public static void assertContextCacheStatistics(String usageScenario, int expectedSize,
			int expectedContextUsageCount, int expectedHitCount, int expectedMissCount) {

		assertContextCacheStatistics(DefaultCacheAwareContextLoaderDelegate.defaultContextCache, usageScenario,
			expectedSize, expectedContextUsageCount, expectedHitCount, expectedMissCount);
	}

	/**
	 * Assert the statistics of the supplied context cache.
	 *
	 * @param contextCache the cache to assert against
	 * @param usageScenario the scenario in which the statistics are used
	 * @param expectedSize the expected number of contexts in the cache
	 * @param expectedContextUsageCount the expected number of actively running
	 * contexts in the cache
	 * @param expectedHitCount the expected hit count
	 * @param expectedMissCount the expected miss count
	 */
	public static void assertContextCacheStatistics(ContextCache contextCache, String usageScenario,
			int expectedSize, int expectedContextUsageCount, int expectedHitCount, int expectedMissCount) {

		String context = (StringUtils.hasText(usageScenario) ? " (" + usageScenario + ")" : "");

		assertSoftly(softly -> {
			softly.assertThat(contextCache.size())
					.as("contexts in cache" + context).isEqualTo(expectedSize);
			softly.assertThat(contextCache.getContextUsageCount())
					.as("active contexts in cache" + context).isEqualTo(expectedContextUsageCount);
			softly.assertThat(contextCache.getHitCount())
					.as("cache hits" + context).isEqualTo(expectedHitCount);
			softly.assertThat(contextCache.getMissCount())
					.as("cache misses" + context).isEqualTo(expectedMissCount);
		});
	}

}

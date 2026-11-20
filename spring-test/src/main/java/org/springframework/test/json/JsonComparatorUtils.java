/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import org.springframework.util.ClassUtils;

/**
 * Create {@link JsonComparator} instances for a given {@link JsonCompareMode},
 * using the JSON assertion library available on the classpath.
 *
 * <p><a href="https://github.com/lukas-krecan/JsonUnit">JsonUnit</a> is used
 * if present, otherwise <a href="https://jsonassert.skyscreamer.org/">JSONassert</a>.
 *
 * @author Brian Clozel
 * @since 7.1
 * @see JsonComparator#of(JsonCompareMode)
 */
abstract class JsonComparatorUtils {

	private static final boolean JSON_UNIT_PRESENT;

	private static final boolean JSON_ASSERT_PRESENT;

	static {
		ClassLoader classLoader = JsonComparatorUtils.class.getClassLoader();
		JSON_UNIT_PRESENT = ClassUtils.isPresent("net.javacrumbs.jsonunit.core.internal.Diff", classLoader);
		JSON_ASSERT_PRESENT = ClassUtils.isPresent("org.skyscreamer.jsonassert.JSONCompare", classLoader);
	}


	private JsonComparatorUtils() {
	}


	static JsonComparator forCompareMode(JsonCompareMode compareMode) {
		return forCompareMode(compareMode, JSON_UNIT_PRESENT, JSON_ASSERT_PRESENT);
	}

	static JsonComparator forCompareMode(JsonCompareMode compareMode,
			boolean jsonUnitPresent, boolean jsonAssertPresent) {

		if (jsonUnitPresent) {
			return new JsonUnitJsonComparator(compareMode);
		}
		if (jsonAssertPresent) {
			return new JsonAssertJsonComparator(compareMode);
		}
		throw new IllegalStateException("No JSON assertion library found on the classpath: " +
				"add JsonUnit ('net.javacrumbs.json-unit:json-unit-core') or " +
				"JSONassert ('org.skyscreamer:jsonassert')");
	}

}

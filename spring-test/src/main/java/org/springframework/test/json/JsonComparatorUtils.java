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

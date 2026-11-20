/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import org.jspecify.annotations.Nullable;

import org.springframework.test.json.JsonComparison.Result;

/**
 * Strategy interface used to compare JSON strings.
 *
 * <p>Use {@link #of(JsonCompareMode)} for a comparator backed by the JSON
 * assertion library available on the classpath, or provide a custom
 * implementation to use another library or configuration.
 *
 * @author Phillip Webb
 * @since 6.2
 */
@FunctionalInterface
public interface JsonComparator {

	/**
	 * Compare the given JSON strings.
	 * @param expectedJson the expected JSON
	 * @param actualJson the actual JSON
	 * @return the JSON comparison
	 */
	JsonComparison compare(@Nullable String expectedJson, @Nullable String actualJson);

	/**
	 * Assert that the {@code expectedJson} matches the comparison rules of ths
	 * instance against the {@code actualJson}. Throw an {@link AssertionError}
	 * if the comparison does not match.
	 * @param expectedJson the expected JSON
	 * @param actualJson the actual JSON
	 */
	default void assertIsMatch(@Nullable String expectedJson, @Nullable String actualJson) {
		JsonComparison comparison = compare(expectedJson, actualJson);
		if (comparison.getResult() == Result.MISMATCH) {
			throw new AssertionError(comparison.getMessage());
		}
	}

	/**
	 * Create a {@link JsonComparator} for the given {@link JsonCompareMode},
	 * backed by the JSON assertion library available on the classpath.
	 * <p><a href="https://github.com/lukas-krecan/JsonUnit">JsonUnit</a>
	 * ({@code net.javacrumbs.json-unit:json-unit-core}) is used if present,
	 * otherwise <a href="https://jsonassert.skyscreamer.org/">JSONassert</a>
	 * ({@code org.skyscreamer:jsonassert}).
	 * @param compareMode the compare mode to use
	 * @return a new {@link JsonComparator} instance
	 * @throws IllegalStateException if neither library is present
	 * @since 7.1
	 */
	static JsonComparator of(JsonCompareMode compareMode) {
		return JsonComparatorUtils.forCompareMode(compareMode);
	}

}

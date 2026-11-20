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
 * @author Phillip Webb
 * @since 6.2
 * @see JsonAssert
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

}

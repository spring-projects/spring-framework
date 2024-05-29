/*
 * Copyright 2002-2024 the original author or authors.
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

import org.springframework.lang.Nullable;
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

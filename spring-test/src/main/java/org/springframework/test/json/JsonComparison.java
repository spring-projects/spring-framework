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

/**
 * A comparison of two JSON strings as returned from a {@link JsonComparator}.
 *
 * @author Phillip Webb
 * @since 6.2
 */
public final class JsonComparison {

	private final Result result;

	@Nullable
	private final String message;


	private JsonComparison(Result result, @Nullable String message) {
		this.result = result;
		this.message = message;
	}

	/**
	 * Factory method to create a new {@link JsonComparison} when the JSON
	 * strings match.
	 * @return a new {@link JsonComparison} instance
	 */
	public static JsonComparison match() {
		return new JsonComparison(Result.MATCH, null);
	}

	/**
	 * Factory method to create a new {@link JsonComparison} when the JSON strings
	 * do not match.
	 * @param message a message describing the mismatch
	 * @return a new {@link JsonComparison} instance
	 */
	public static JsonComparison mismatch(String message) {
		return new JsonComparison(Result.MISMATCH, message);
	}

	/**
	 * Return the result of the comparison.
	 */
	public Result getResult() {
		return this.result;
	}

	/**
	 * Return a message describing the comparison.
	 */
	@Nullable
	public String getMessage() {
		return this.message;
	}


	/**
	 * Comparison results.
	 */
	public enum Result {

		/**
		 * The JSON strings match when considering the comparison rules.
		 */
		MATCH,

		/**
		 * The JSON strings do not match when considering the comparison rules.
		 */
		MISMATCH
	}

}

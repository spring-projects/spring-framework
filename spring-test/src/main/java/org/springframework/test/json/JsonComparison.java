/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import org.jspecify.annotations.Nullable;

/**
 * A comparison of two JSON strings as returned from a {@link JsonComparator}.
 *
 * @author Phillip Webb
 * @since 6.2
 */
public final class JsonComparison {

	private final Result result;

	private final @Nullable String message;


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
	public @Nullable String getMessage() {
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

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import org.json.JSONException;
import org.jspecify.annotations.Nullable;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

/**
 * Useful methods that can be used with {@code org.skyscreamer.jsonassert}.
 *
 * @author Phillip Webb
 * @since 6.2
 */
public abstract class JsonAssert {

	/**
	 * Create a {@link JsonComparator} from the given {@link JsonCompareMode}.
	 * @param compareMode the mode to use
	 * @return a new {@link JsonComparator} instance
	 * @see JSONCompareMode#STRICT
	 * @see JSONCompareMode#LENIENT
	 */
	public static JsonComparator comparator(JsonCompareMode compareMode) {
		JSONCompareMode jsonAssertCompareMode = (compareMode != JsonCompareMode.LENIENT ?
				JSONCompareMode.STRICT : JSONCompareMode.LENIENT);
		return comparator(jsonAssertCompareMode);
	}

	/**
	 * Create a new {@link JsonComparator} from the given JSONAssert
	 * {@link JSONComparator}.
	 * @param comparator the JSON Assert {@link JSONComparator}
	 * @return a new {@link JsonComparator} instance
	 */
	public static JsonComparator comparator(JSONComparator comparator) {
		return new JsonAssertJsonComparator(comparator);
	}

	/**
	 * Create a new {@link JsonComparator} from the given JSONAssert
	 * {@link JSONCompareMode}.
	 * @param mode the JSON Assert {@link JSONCompareMode}
	 * @return a new {@link JsonComparator} instance
	 */
	public static JsonComparator comparator(JSONCompareMode mode) {
		return new JsonAssertJsonComparator(mode);
	}

	private static class JsonAssertJsonComparator implements JsonComparator {

		private final JSONComparator jsonAssertComparator;

		JsonAssertJsonComparator(JSONComparator jsonAssertComparator) {
			this.jsonAssertComparator = jsonAssertComparator;
		}

		JsonAssertJsonComparator(JSONCompareMode compareMode) {
			this(new DefaultComparator(compareMode));
		}

		@Override
		public JsonComparison compare(@Nullable String expectedJson, @Nullable String actualJson) {
			if (actualJson == null) {
				return (expectedJson != null) ? JsonComparison.mismatch("Expected null JSON") : JsonComparison.match();
			}
			if (expectedJson == null) {
				return JsonComparison.mismatch("Expected non-null JSON");
			}
			try {
				JSONCompareResult result = JSONCompare.compareJSON(expectedJson, actualJson, this.jsonAssertComparator);
				return (!result.passed()) ? JsonComparison.mismatch(result.getMessage()) : JsonComparison.match();
			}
			catch (JSONException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

}

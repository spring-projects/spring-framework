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

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

import org.springframework.lang.Nullable;

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
		JSONCompareMode jsonAssertCompareMode = (compareMode != JsonCompareMode.LENIENT
				? JSONCompareMode.STRICT : JSONCompareMode.LENIENT);
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
				return (expectedJson != null)
						? JsonComparison.mismatch("Expected null JSON")
						: JsonComparison.match();
			}
			if (expectedJson == null) {
				return JsonComparison.mismatch("Expected non-null JSON");
			}
			try {
				JSONCompareResult result = JSONCompare.compareJSON(expectedJson, actualJson, this.jsonAssertComparator);
				return (!result.passed())
						? JsonComparison.mismatch(result.getMessage())
						: JsonComparison.match();
			}
			catch (JSONException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

}

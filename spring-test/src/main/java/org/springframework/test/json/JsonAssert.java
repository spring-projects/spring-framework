/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

/**
 * Useful methods that can be used with {@code org.skyscreamer.jsonassert}.
 *
 * @author Phillip Webb
 * @since 6.2
 * @deprecated as of 7.1 in favor of {@link JsonComparator#of(JsonCompareMode)},
 * which supports both <a href="https://github.com/lukas-krecan/JsonUnit">JsonUnit</a>
 * and <a href="https://jsonassert.skyscreamer.org/">JSONassert</a>, or a custom
 * {@link JsonComparator} implementation
 */
@Deprecated(since = "7.1", forRemoval = true)
public abstract class JsonAssert {

	/**
	 * Create a {@link JsonComparator} from the given {@link JsonCompareMode}.
	 * @param compareMode the mode to use
	 * @return a new {@link JsonComparator} instance
	 * @see JSONCompareMode#STRICT
	 * @see JSONCompareMode#LENIENT
	 */
	public static JsonComparator comparator(JsonCompareMode compareMode) {
		return new JsonAssertJsonComparator(compareMode);
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

}

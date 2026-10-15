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

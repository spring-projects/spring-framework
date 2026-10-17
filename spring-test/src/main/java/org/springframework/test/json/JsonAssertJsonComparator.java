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

import org.json.JSONException;
import org.jspecify.annotations.Nullable;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

/**
 * {@link JsonComparator} implementation backed by the
 * <a href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 7.1
 */
final class JsonAssertJsonComparator implements JsonComparator {

	private final JSONComparator jsonAssertComparator;


	JsonAssertJsonComparator(JSONComparator jsonAssertComparator) {
		this.jsonAssertComparator = jsonAssertComparator;
	}

	JsonAssertJsonComparator(JSONCompareMode compareMode) {
		this(new DefaultComparator(compareMode));
	}

	JsonAssertJsonComparator(JsonCompareMode compareMode) {
		this(compareMode == JsonCompareMode.LENIENT ? JSONCompareMode.LENIENT : JSONCompareMode.STRICT);
	}


	@Override
	public JsonComparison compare(@Nullable String expectedJson, @Nullable String actualJson) {
		if (actualJson == null) {
			return (expectedJson != null ? JsonComparison.mismatch("Expected null JSON") : JsonComparison.match());
		}
		if (expectedJson == null) {
			return JsonComparison.mismatch("Expected non-null JSON");
		}
		try {
			JSONCompareResult result = JSONCompare.compareJSON(expectedJson, actualJson, this.jsonAssertComparator);
			return (result.passed() ? JsonComparison.match() : JsonComparison.mismatch(result.getMessage()));
		}
		catch (JSONException ex) {
			throw new IllegalStateException(ex);
		}
	}

}

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

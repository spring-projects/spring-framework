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

import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.internal.Diff;
import org.jspecify.annotations.Nullable;

/**
 * {@link JsonComparator} implementation backed by the
 * <a href="https://github.com/lukas-krecan/JsonUnit">JsonUnit</a> library.
 *
 * <p>{@link JsonCompareMode#LENIENT Lenient} comparisons ignore extra fields
 * and array ordering, while {@link JsonCompareMode#STRICT strict} comparisons
 * use the default JsonUnit configuration.
 *
 * @author Brian Clozel
 * @since 7.1
 */
final class JsonUnitJsonComparator implements JsonComparator {

	private final Configuration configuration;


	JsonUnitJsonComparator(Configuration configuration) {
		this.configuration = configuration;
	}

	JsonUnitJsonComparator(JsonCompareMode compareMode) {
		this(compareMode == JsonCompareMode.LENIENT ?
				Configuration.empty().withOptions(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER) :
				Configuration.empty());
	}


	@Override
	public JsonComparison compare(@Nullable String expectedJson, @Nullable String actualJson) {
		if (actualJson == null) {
			return (expectedJson != null ? JsonComparison.mismatch("Expected null JSON") : JsonComparison.match());
		}
		if (expectedJson == null) {
			return JsonComparison.mismatch("Expected non-null JSON");
		}
		Diff diff = Diff.create(expectedJson, actualJson, "actual", "", this.configuration);
		return (diff.similar() ? JsonComparison.match() : JsonComparison.mismatch(diff.differences()));
	}

}

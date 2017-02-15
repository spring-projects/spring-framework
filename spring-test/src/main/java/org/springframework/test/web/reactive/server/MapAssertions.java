/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.web.reactive.server;

import java.util.Arrays;
import java.util.Map;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Assertions on a map of values.
 *
 * @param <K, V> the type of values in map
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MapAssertions<K, V> extends ObjectAssertions<Map<K, V>, MapAssertions<K, V>> {


	protected MapAssertions(ExchangeActions actions, Map<K, V> map, String errorPrefix) {
		super(actions, map, errorPrefix);
	}


	/**
	 * Assert the size of the map.
	 */
	public MapAssertions<K, V> hasSize(int size) {
		assertEquals(getErrorPrefix() + " count", size, getValue().size());
		return this;
	}

	/**
	 * Assert that the map contains all of the given values.
	 */
	@SuppressWarnings("unchecked")
	public MapAssertions<K, V> contains(K key, V value) {
		V actual = getValue().get(key);
		assertEquals(getErrorPrefix() + " do not contain " + value, value, actual);
		return this;
	}

	/**
	 * Assert that the map contains all of the given keys.
	 */
	@SuppressWarnings("unchecked")
	public MapAssertions<K, V> containsKeys(K... keys) {
		Arrays.stream(keys).forEach(key -> {
			boolean result = getValue().containsKey(key);
			assertTrue(getErrorPrefix() + " does not contain key " + key, result);
		});
		return this;
	}

	/**
	 * Assert that the map contains all of the given keys.
	 */
	@SuppressWarnings("unchecked")
	public MapAssertions<K, V> containsValues(V... values) {
		Arrays.stream(values).forEach(value -> {
			boolean result = getValue().containsValue(value);
			assertTrue(getErrorPrefix() + " does not contain value " + value, result);
		});
		return this;
	}

	/**
	 * Assert the map does not contain any of the given keys.
	 */
	@SuppressWarnings("unchecked")
	public MapAssertions<K, V> doesNotContain(K... keys) {
		Arrays.stream(keys).forEach(key -> {
			boolean result = !getValue().containsKey(key);
			assertTrue(getErrorPrefix() + " should not contain " + key, result);
		});
		return this;
	}

}

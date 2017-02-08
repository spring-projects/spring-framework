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
import java.util.List;

import org.springframework.util.MultiValueMap;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * Assertions on the values of a {@link MultiValueMap} entry.
 *
 * @param <V> the type of values in the map.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MultiValueMapEntryAssertions<V> {

	private final ExchangeActions exchangeActions;

	private final String name;

	private final MultiValueMap<String, V> map;

	private final String errorMessagePrefix;


	MultiValueMapEntryAssertions(ExchangeActions actions, String name,
			MultiValueMap<String, V> map, String errorMessagePrefix) {

		this.exchangeActions = actions;
		this.name = name;
		this.map = map;
		this.errorMessagePrefix = errorMessagePrefix + " " + this.name;
	}


	/**
	 * The given values are equal to the actual values.
	 * @param values the values to match
	 */
	@SuppressWarnings("unchecked")
	public ExchangeActions isEqualTo(V... values) {
		List<V> actual = this.map.get(this.name);
		assertEquals(this.errorMessagePrefix, Arrays.asList(values), actual);
		return this.exchangeActions;
	}

	/**
	 * The list of actual values contains the given values.
	 * @param values the values to match
	 */
	@SuppressWarnings("unchecked")
	public ExchangeActions hasValues(V... values) {
		List<V> actual = this.map.get(this.name);
		List<V> expected = Arrays.asList(values);
		String message = getErrorMessagePrefix() + " does not contain " + expected;
		assertTrue(message, actual.containsAll(expected));
		return this.exchangeActions;
	}


	// Protected methods for sub-classes

	protected ExchangeActions getExchangeActions() {
		return this.exchangeActions;
	}

	protected String getName() {
		return this.name;
	}

	protected MultiValueMap<String, V> getMap() {
		return this.map;
	}

	protected String getErrorMessagePrefix() {
		return this.errorMessagePrefix;
	}

	protected V getValue(int index) {
		List<V> actualValues = getMap().get(getName());
		if (actualValues == null || index >= actualValues.size()) {
			fail(getErrorMessagePrefix() + " does not have values at index[" + index + "]: " + actualValues);
		}
		return actualValues.get(index);
	}

}

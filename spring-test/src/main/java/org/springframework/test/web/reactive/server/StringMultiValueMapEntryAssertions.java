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
import java.util.regex.Pattern;

import org.springframework.util.MultiValueMap;

import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Extension of {@link MultiValueMapEntryAssertions} for String values.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class StringMultiValueMapEntryAssertions extends MultiValueMapEntryAssertions<String> {


	public StringMultiValueMapEntryAssertions(ExchangeActions actions, String name,
			MultiValueMap<String, String> map, String errorMessagePrefix) {

		super(actions, name, map, errorMessagePrefix);
	}


	/**
	 * The specified value {@link String#contains contains} the given sub-strings.
	 * @param values the values to match
	 */
	public ExchangeActions valueContains(CharSequence... values) {
		String actual = getValue(0);
		Arrays.stream(values).forEach(value -> {
			String message = getErrorMessagePrefix() + " does not contain " + value;
			assertTrue(message, actual.contains(value));
		});
		return getExchangeActions();
	}

	/**
	 * The specified value does not {@link String#contains contain} the given sub-strings.
	 * @param values the values to match
	 */
	public ExchangeActions valueDoesNotContain(CharSequence... values) {
		String actual = getValue(0);
		Arrays.stream(values).forEach(value -> {
			String message = getErrorMessagePrefix() + " contains " + value + " but shouldn't";
			assertTrue(message, !actual.contains(value));
		});
		return getExchangeActions();
	}

	/**
	 * The specified value matches the given regex pattern value.
	 * @param pattern the values to be compiled with {@link Pattern}
	 */
	public ExchangeActions valueMatches(String pattern) {
		String actual = getValue(0);
		boolean match = Pattern.compile(pattern).matcher(actual).matches();
		String message = getErrorMessagePrefix() + " with value " + actual + " does not match " + pattern;
		assertTrue(message, match);
		return getExchangeActions();
	}

}

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

import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Assertions on a String value.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class StringAssertions extends ObjectAssertions<String, StringAssertions> {


	public StringAssertions(ExchangeActions exchangeActions, String value, String errorPrefix) {
		super(exchangeActions, value, errorPrefix);
	}


	/**
	 * The value {@link String#contains contains} the given sub-strings.
	 * @param values the values to match
	 */
	public StringAssertions contains(CharSequence... values) {
		Arrays.stream(values).forEach(value -> {
			String message = getErrorPrefix() + " does not contain " + value;
			assertTrue(message, getValue().contains(value));
		});
		return this;
	}

	/**
	 * The value does not {@link String#contains contain} the given sub-strings.
	 * @param values the values to match
	 */
	public StringAssertions doesNotContain(CharSequence... values) {
		Arrays.stream(values).forEach(value -> {
			String message = getErrorPrefix() + " contains " + value + " but shouldn't";
			assertTrue(message, !getValue().contains(value));
		});
		return this;
	}

	/**
	 * The value matches the given regex pattern value.
	 * @param pattern the values to be compiled with {@link Pattern}
	 */
	public StringAssertions matches(String pattern) {
		boolean match = Pattern.compile(pattern).matcher(getValue()).matches();
		String message = getErrorPrefix() + " with value " + getValue() + " does not match " + pattern;
		assertTrue(message, match);
		return this;
	}

}

/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.util;

import org.skyscreamer.jsonassert.JSONAssert;

/**
 * A helper class for assertions on JSON content.
 *
 * <p>Use of this class requires the <a
 * href="https://jsonassert.skyscreamer.org/">JSONassert<a/> library.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class JsonExpectationsHelper {

	/**
	 * Parse the expected and actual strings as JSON and assert the two
	 * are "similar" - i.e. they contain the same attribute-value pairs
	 * regardless of formatting with a lenient checking (extensible, and non-strict
	 * array ordering).
	 *
	 * @param expected the expected JSON content
	 * @param actual the actual JSON content
	 * @since 4.1
	 * @see #assertJsonEqual(String, String, boolean)
	 */
	public void assertJsonEqual(String expected, String actual) throws Exception {
		assertJsonEqual(expected, actual, false);
	}

	/**
	 * Parse the expected and actual strings as JSON and assert the two
	 * are "similar" - i.e. they contain the same attribute-value pairs
	 * regardless of formatting.
	 *
	 * <p>Can compare in two modes, depending on {@code strict} parameter value:
	 * <ul>
	 *     <li>{@code true}: strict checking. Not extensible, and strict array ordering.</li>
	 *     <li>{@code false}: lenient checking. Extensible, and non-strict array ordering.</li>
	 * </ul>
	 *
	 * @param expected the expected JSON content
	 * @param actual the actual JSON content
	 * @param strict enables strict checking
	 * @since 4.2
	 */
	public void assertJsonEqual(String expected, String actual, boolean strict) throws Exception {
		JSONAssert.assertEquals(expected, actual, strict);
	}

	/**
	 * Parse the expected and actual strings as JSON and assert the two
	 * are "not similar" - i.e. they contain different attribute-value pairs
	 * regardless of formatting with a lenient checking (extensible, and non-strict
	 * array ordering).
	 *
	 * @param expected the expected JSON content
	 * @param actual the actual JSON content
	 * @since 4.1
	 * @see #assertJsonNotEqual(String, String, boolean)
	 */
	public void assertJsonNotEqual(String expected, String actual) throws Exception {
		assertJsonNotEqual(expected, actual, false);
	}

	/**
	 * Parse the expected and actual strings as JSON and assert the two
	 * are "not similar" - i.e. they contain different attribute-value pairs
	 * regardless of formatting.
	 *
	 * <p>Can compare in two modes, depending on {@code strict} parameter value:
	 * <ul>
	 *     <li>{@code true}: strict checking. Not extensible, and strict array ordering.</li>
	 *     <li>{@code false}: lenient checking. Extensible, and non-strict array ordering.</li>
	 * </ul>
	 *
	 * @param expected the expected JSON content
	 * @param actual the actual JSON content
	 * @param strict enables strict checking
	 * @since 4.2
	 */
	public void assertJsonNotEqual(String expected, String actual, boolean strict) throws Exception {
		JSONAssert.assertNotEquals(expected, actual, strict);
	}

}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.util;

import org.skyscreamer.jsonassert.JSONAssert;

import org.springframework.test.json.JsonComparator;

/**
 * A helper class for assertions on JSON content.
 *
 * <p>Use of this class requires the <a
 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 * @deprecated in favor of using {@link JSONAssert} directly or the
 * {@link JsonComparator} abstraction
 */
@Deprecated(since = "6.2")
public class JsonExpectationsHelper {

	/**
	 * Parse the expected and actual strings as JSON and assert the two
	 * are "similar" - i.e. they contain the same attribute-value pairs
	 * regardless of formatting with lenient checking (extensible content and
	 * non-strict array ordering).
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
	 * <p>Can compare in two modes, depending on the {@code strict} parameter value:
	 * <ul>
	 * <li>{@code true}: strict checking. Not extensible and strict array ordering.</li>
	 * <li>{@code false}: lenient checking. Extensible and non-strict array ordering.</li>
	 * </ul>
	 * @param expected the expected JSON content
	 * @param actual the actual JSON content
	 * @param strict enables strict checking if {@code true}
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
	 * <p>Can compare in two modes, depending on {@code strict} parameter value:
	 * <ul>
	 * <li>{@code true}: strict checking. Not extensible, and strict array ordering.</li>
	 * <li>{@code false}: lenient checking. Extensible, and non-strict array ordering.</li>
	 * </ul>
	 * @param expected the expected JSON content
	 * @param actual the actual JSON content
	 * @param strict enables strict checking
	 * @since 4.2
	 */
	public void assertJsonNotEqual(String expected, String actual, boolean strict) throws Exception {
		JSONAssert.assertNotEquals(expected, actual, strict);
	}

}

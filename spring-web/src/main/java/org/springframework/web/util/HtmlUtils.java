/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.util;

/**
 * Utility class for HTML escaping. Escapes and unescapes
 * based on the W3C HTML 4.01 recommendation, handling
 * character entity references.
 *
 * <p>Reference:
 * <a href="http://www.w3.org/TR/html4/charset.html">http://www.w3.org/TR/html4/charset.html</a>
 *
 * <p>For a comprehensive set of String escaping utilities,
 * consider Jakarta Commons Lang and its StringEscapeUtils class.
 * We are not using that class here to avoid a runtime dependency
 * on Commons Lang just for HTML escaping. Furthermore, Spring's
 * HTML escaping is more flexible and 100% HTML 4.0 compliant.
 *
 * @author Juergen Hoeller
 * @author Martin Kersten
 * @since 01.03.2003
 * @see org.apache.commons.lang.StringEscapeUtils
 */
public abstract class HtmlUtils {

	/**
	 * Shared instance of pre-parsed HTML character entity references.
	 */
	private static final HtmlCharacterEntityReferences characterEntityReferences =
			new HtmlCharacterEntityReferences();


	/**
	 * Turn special characters into HTML character references.
	 * Handles complete character set defined in HTML 4.01 recommendation.
	 * <p>Escapes all special characters to their corresponding
	 * entity reference (e.g. <code>&lt;</code>).
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input the (unescaped) input string
	 * @return the escaped string
	 */
	public static String htmlEscape(String input) {
		if (input == null) {
			return null;
		}
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			String reference = characterEntityReferences.convertToReference(character);
			if (reference != null) {
				escaped.append(reference);
			}
			else {
				escaped.append(character);
			}
		}
		return escaped.toString();
	}

	/**
	 * Turn special characters into HTML character references.
	 * Handles complete character set defined in HTML 4.01 recommendation.
	 * <p>Escapes all special characters to their corresponding numeric
	 * reference in decimal format (&#<i>Decimal</i>;).
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input the (unescaped) input string
	 * @return the escaped string
	 */
	public static String htmlEscapeDecimal(String input) {
		if (input == null) {
			return null;
		}
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			if (characterEntityReferences.isMappedToReference(character)) {
				escaped.append(HtmlCharacterEntityReferences.DECIMAL_REFERENCE_START);
				escaped.append((int) character);
				escaped.append(HtmlCharacterEntityReferences.REFERENCE_END);
			}
			else {
				escaped.append(character);
			}
		}
		return escaped.toString();
	}

	/**
	 * Turn special characters into HTML character references.
	 * Handles complete character set defined in HTML 4.01 recommendation.
	 * <p>Escapes all special characters to their corresponding numeric
	 * reference in hex format (&#x<i>Hex</i>;).
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input the (unescaped) input string
	 * @return the escaped string
	 */
	public static String htmlEscapeHex(String input) {
		if (input == null) {
			return null;
		}
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			if (characterEntityReferences.isMappedToReference(character)) {
				escaped.append(HtmlCharacterEntityReferences.HEX_REFERENCE_START);
				escaped.append(Integer.toString(character, 16));
				escaped.append(HtmlCharacterEntityReferences.REFERENCE_END);
			}
			else {
				escaped.append(character);
			}
		}
		return escaped.toString();
	}

	/**
	 * Turn HTML character references into their plain text UNICODE equivalent.
	 * <p>Handles complete character set defined in HTML 4.01 recommendation
	 * and all reference types (decimal, hex, and entity).
	 * <p>Correctly converts the following formats:
	 * <blockquote>
	 * &amp;#<i>Entity</i>; - <i>(Example: &amp;amp;) case sensitive</i>
	 * &amp;#<i>Decimal</i>; - <i>(Example: &amp;#68;)</i><br>
	 * &amp;#x<i>Hex</i>; - <i>(Example: &amp;#xE5;) case insensitive</i><br>
	 * </blockquote>
	 * Gracefully handles malformed character references by copying original
	 * characters as is when encountered.<p>
	 * <p>Reference:
	 * <a href="http://www.w3.org/TR/html4/sgml/entities.html">
	 * http://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input the (escaped) input string
	 * @return the unescaped string
	 */
	public static String htmlUnescape(String input) {
		if (input == null) {
			return null;
		}
		return new HtmlCharacterEntityDecoder(characterEntityReferences, input).decode();
	}

}

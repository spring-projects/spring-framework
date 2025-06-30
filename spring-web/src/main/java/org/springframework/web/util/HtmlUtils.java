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

package org.springframework.web.util;

import org.springframework.util.Assert;

/**
 * Utility class for HTML escaping.
 *
 * <p>Escapes and unescapes based on the W3C HTML 4.01 recommendation, handling
 * character entity references.
 *
 * <p>Reference:
 * <a href="https://www.w3.org/TR/html4/charset.html">https://www.w3.org/TR/html4/charset.html</a>
 *
 * <p>For a comprehensive set of String escaping utilities, consider
 * <a href="https://commons.apache.org/proper/commons-text/">Apache Commons Text</a>
 * and its {@code StringEscapeUtils} class. We do not use that class here in order
 * to avoid a runtime dependency on Commons Text just for HTML escaping. Furthermore,
 * Spring's HTML escaping is more flexible and 100% HTML 4.0 compliant.
 *
 * @author Juergen Hoeller
 * @author Martin Kersten
 * @author Craig Andrews
 * @since 01.03.2003
 */
public abstract class HtmlUtils {

	/**
	 * Shared instance of pre-parsed HTML character entity references.
	 */
	private static final HtmlCharacterEntityReferences characterEntityReferences =
			new HtmlCharacterEntityReferences();


	/**
	 * Turn special characters into HTML character references.
	 * <p>Handles the complete character set defined in the HTML 4.01 recommendation.
	 * <p>Escapes all special characters to their corresponding
	 * entity reference (for example, {@code &lt;}).
	 * <p>Reference:
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input the (unescaped) input string
	 * @return the escaped string
	 */
	public static String htmlEscape(String input) {
		return htmlEscape(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * Turn special characters into HTML character references.
	 * <p>Handles the complete character set defined in the HTML 4.01 recommendation.
	 * <p>Escapes all special characters to their corresponding
	 * entity reference (for example, {@code &lt;}) at least as required by the
	 * specified encoding. In other words, if a special character does
	 * not have to be escaped for the given encoding, it may not be.
	 * <p>Reference:
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input the (unescaped) input string
	 * @param encoding the name of a supported {@link java.nio.charset.Charset charset}
	 * @return the escaped string
	 * @since 4.1.2
	 */
	public static String htmlEscape(String input, String encoding) {
		Assert.notNull(input, "Input is required");
		Assert.notNull(encoding, "Encoding is required");
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			String reference = characterEntityReferences.convertToReference(character, encoding);
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
	 * <p>Handles the complete character set defined in the HTML 4.01 recommendation.
	 * <p>Escapes all special characters to their corresponding numeric
	 * reference in decimal format (&amp;#<i>Decimal</i>;).
	 * <p>Reference:
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input the (unescaped) input string
	 * @return the escaped string
	 */
	public static String htmlEscapeDecimal(String input) {
		return htmlEscapeDecimal(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * Turn special characters into HTML character references.
	 * <p>Handles the complete character set defined in the HTML 4.01 recommendation.
	 * <p>Escapes all special characters to their corresponding numeric
	 * reference in decimal format (&amp;#<i>Decimal</i>;) at least as required by the
	 * specified encoding. In other words, if a special character does
	 * not have to be escaped for the given encoding, it may not be.
	 * <p>Reference:
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input the (unescaped) input string
	 * @param encoding the name of a supported {@link java.nio.charset.Charset charset}
	 * @return the escaped string
	 * @since 4.1.2
	 */
	public static String htmlEscapeDecimal(String input, String encoding) {
		Assert.notNull(input, "Input is required");
		Assert.notNull(encoding, "Encoding is required");
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			if (characterEntityReferences.isMappedToReference(character, encoding)) {
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
	 * <p>Handles the complete character set defined in the HTML 4.01 recommendation.
	 * <p>Escapes all special characters to their corresponding numeric
	 * reference in hex format (&amp;#x<i>Hex</i>;).
	 * <p>Reference:
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input the (unescaped) input string
	 * @return the escaped string
	 */
	public static String htmlEscapeHex(String input) {
		return htmlEscapeHex(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * Turn special characters into HTML character references.
	 * <p>Handles the complete character set defined in the HTML 4.01 recommendation.
	 * <p>Escapes all special characters to their corresponding numeric
	 * reference in hex format (&amp;#x<i>Hex</i>;) at least as required by the
	 * specified encoding. In other words, if a special character does
	 * not have to be escaped for the given encoding, it may not be.
	 * <p>Reference:
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input the (unescaped) input string
	 * @param encoding the name of a supported {@link java.nio.charset.Charset charset}
	 * @return the escaped string
	 * @since 4.1.2
	 */
	public static String htmlEscapeHex(String input, String encoding) {
		Assert.notNull(input, "Input is required");
		Assert.notNull(encoding, "Encoding is required");
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			if (characterEntityReferences.isMappedToReference(character, encoding)) {
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
	 * <p>Gracefully handles malformed character references by copying original
	 * characters as is when encountered.
	 * <p>Reference:
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 * @param input the (escaped) input string
	 * @return the unescaped string
	 */
	public static String htmlUnescape(String input) {
		return new HtmlCharacterEntityDecoder(characterEntityReferences, input).decode();
	}

}

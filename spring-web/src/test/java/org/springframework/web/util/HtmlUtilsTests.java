/*
 * Copyright 2002-2011 the original author or authors.
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Alef Arendsen
 * @author Martin Kersten
 * @author Rick Evans
 */
public class HtmlUtilsTests {

	@Test
	public void testHtmlEscape() {
		String unescaped = "\"This is a quote'";
		String escaped = HtmlUtils.htmlEscape(unescaped);
		assertEquals("&quot;This is a quote&#39;", escaped);
		escaped = HtmlUtils.htmlEscapeDecimal(unescaped);
		assertEquals("&#34;This is a quote&#39;", escaped);
		escaped = HtmlUtils.htmlEscapeHex(unescaped);
		assertEquals("&#x22;This is a quote&#x27;", escaped);
	}

	@Test
	public void testHtmlUnescape() {
		String escaped = "&quot;This is a quote&#39;";
		String unescaped = HtmlUtils.htmlUnescape(escaped);
		assertEquals(unescaped, "\"This is a quote'");
	}

	@Test
	public void testEncodeIntoHtmlCharacterSet() {
		assertNull("A null string should be converted to a null string",
				HtmlUtils.htmlEscape(null));
		assertEquals("An empty string should be converted to an empty string",
				"", HtmlUtils.htmlEscape(""));
		assertEquals("A string containing no special characters should not be affected",
				"A sentence containing no special characters.",
				HtmlUtils.htmlEscape("A sentence containing no special characters."));

		assertEquals("'< >' should be encoded to '&lt; &gt;'",
				"&lt; &gt;", HtmlUtils.htmlEscape("< >"));
		assertEquals("'< >' should be encoded to '&#60; &#62;'",
				"&#60; &#62;", HtmlUtils.htmlEscapeDecimal("< >"));

		assertEquals("The special character 8709 should be encoded to '&empty;'",
				"&empty;", HtmlUtils.htmlEscape("" + (char) 8709));
		assertEquals("The special character 8709 should be encoded to '&#8709;'",
				"&#8709;", HtmlUtils.htmlEscapeDecimal("" + (char) 8709));

		assertEquals("The special character 977 should be encoded to '&thetasym;'",
				"&thetasym;", HtmlUtils.htmlEscape("" + (char) 977));
		assertEquals("The special character 977 should be encoded to '&#977;'",
				"&#977;", HtmlUtils.htmlEscapeDecimal("" + (char) 977));
	}

	// SPR-9293
	@Test
	public void testEncodeIntoHtmlCharacterSetFromUtf8() {
		String utf8 = ("UTF-8");
		assertNull("A null string should be converted to a null string",
				HtmlUtils.htmlEscape(null, utf8));
		assertEquals("An empty string should be converted to an empty string",
				"", HtmlUtils.htmlEscape("", utf8));
		assertEquals("A string containing no special characters should not be affected",
				"A sentence containing no special characters.",
				HtmlUtils.htmlEscape("A sentence containing no special characters."));

		assertEquals("'< >' should be encoded to '&lt; &gt;'",
				"&lt; &gt;", HtmlUtils.htmlEscape("< >", utf8));
		assertEquals("'< >' should be encoded to '&#60; &#62;'",
				"&#60; &#62;", HtmlUtils.htmlEscapeDecimal("< >", utf8));

		assertEquals("UTF-8 supported chars should not be escaped",
				"Μερικοί Ελληνικοί &quot;χαρακτήρες&quot;",
				HtmlUtils.htmlEscape("Μερικοί Ελληνικοί \"χαρακτήρες\"", utf8));
	}

	@Test
	public void testDecodeFromHtmlCharacterSet() {
		assertNull("A null string should be converted to a null string",
				HtmlUtils.htmlUnescape(null));
		assertEquals("An empty string should be converted to an empty string",
				"", HtmlUtils.htmlUnescape(""));
		assertEquals("A string containing no special characters should not be affected",
				"This is a sentence containing no special characters.",
				HtmlUtils.htmlUnescape("This is a sentence containing no special characters."));

		assertEquals("'A&nbsp;B' should be decoded to 'A B'",
				"A" + (char) 160 + "B", HtmlUtils.htmlUnescape("A&nbsp;B"));

		assertEquals("'&lt; &gt;' should be decoded to '< >'",
				"< >", HtmlUtils.htmlUnescape("&lt; &gt;"));
		assertEquals("'&#60; &#62;' should be decoded to '< >'",
				"< >", HtmlUtils.htmlUnescape("&#60; &#62;"));

		assertEquals("'&#x41;&#X42;&#x43;' should be decoded to 'ABC'",
				"ABC", HtmlUtils.htmlUnescape("&#x41;&#X42;&#x43;"));

		assertEquals("'&phi;' should be decoded to uni-code character 966",
				"" + (char) 966, HtmlUtils.htmlUnescape("&phi;"));

		assertEquals("'&Prime;' should be decoded to uni-code character 8243",
				"" + (char) 8243, HtmlUtils.htmlUnescape("&Prime;"));

		assertEquals("A not supported named reference leads should be ingnored",
				"&prIme;", HtmlUtils.htmlUnescape("&prIme;"));

		assertEquals("An empty reference '&;' should be survive the decoding",
				"&;", HtmlUtils.htmlUnescape("&;"));

		assertEquals("The longest character entity reference '&thetasym;' should be processable",
				"" + (char) 977, HtmlUtils.htmlUnescape("&thetasym;"));

		assertEquals("A malformed decimal reference should survive the decoding",
				"&#notADecimalNumber;", HtmlUtils.htmlUnescape("&#notADecimalNumber;"));
		assertEquals("A malformed hex reference should survive the decoding",
				"&#XnotAHexNumber;", HtmlUtils.htmlUnescape("&#XnotAHexNumber;"));

		assertEquals("The numerical reference '&#1;' should be converted to char 1",
				"" + (char) 1, HtmlUtils.htmlUnescape("&#1;"));

		assertEquals("The malformed hex reference '&#x;' should remain '&#x;'",
				"&#x;", HtmlUtils.htmlUnescape("&#x;"));
	}

}

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

package org.springframework.test.web.servlet.result;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import jakarta.servlet.http.HttpServletResponse;
import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.http.MediaType;
import org.springframework.test.json.JsonAssert;
import org.springframework.test.json.JsonComparator;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.json.JsonComparison;
import org.springframework.test.util.XmlExpectationsHelper;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Factory for response content assertions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#content}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class ContentResultMatchers {

	private final XmlExpectationsHelper xmlHelper;


	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#content()}.
	 */
	protected ContentResultMatchers() {
		this.xmlHelper = new XmlExpectationsHelper();
	}


	/**
	 * Assert the ServletResponse content type. The given content type must
	 * fully match including type, subtype, and parameters. For checking
	 * only the type and subtype see {@link #contentTypeCompatibleWith(String)}.
	 */
	public ResultMatcher contentType(String contentType) {
		return contentType(MediaType.parseMediaType(contentType));
	}

	/**
	 * Assert the ServletResponse content type after parsing it as a MediaType.
	 * The given content type must fully match including type, subtype, and
	 * parameters. For checking only the type and subtype see
	 * {@link #contentTypeCompatibleWith(MediaType)}.
	 */
	public ResultMatcher contentType(MediaType contentType) {
		return result -> {
			String actual = result.getResponse().getContentType();
			assertNotNull("Content type not set", actual);
			assertEquals("Content type", contentType, MediaType.parseMediaType(actual));
		};
	}

	/**
	 * Assert the ServletResponse content type is compatible with the given
	 * content type as defined by {@link MediaType#isCompatibleWith(MediaType)}.
	 */
	public ResultMatcher contentTypeCompatibleWith(String contentType) {
		return contentTypeCompatibleWith(MediaType.parseMediaType(contentType));
	}

	/**
	 * Assert the ServletResponse content type is compatible with the given
	 * content type as defined by {@link MediaType#isCompatibleWith(MediaType)}.
	 */
	public ResultMatcher contentTypeCompatibleWith(MediaType contentType) {
		return result -> {
			String actual = result.getResponse().getContentType();
			assertNotNull("Content type not set", actual);
			MediaType actualContentType = MediaType.parseMediaType(actual);
			assertTrue("Content type [" + actual + "] is not compatible with [" + contentType + "]",
					actualContentType.isCompatibleWith(contentType));
		};
	}

	/**
	 * Assert the character encoding in the ServletResponse.
	 * @since 5.3.10
	 * @see StandardCharsets
	 * @see #encoding(String)
	 */
	public ResultMatcher encoding(Charset characterEncoding) {
		return encoding(characterEncoding.name());
	}

	/**
	 * Assert the character encoding in the ServletResponse.
	 * @see HttpServletResponse#getCharacterEncoding()
	 */
	public ResultMatcher encoding(String characterEncoding) {
		return result -> {
			String actual = result.getResponse().getCharacterEncoding();
			assertEquals("Character encoding", characterEncoding, actual);
		};
	}

	/**
	 * Assert the response body content with a Hamcrest {@link Matcher}.
	 * <pre class="code">
	 * mockMvc.perform(get("/path"))
	 *   .andExpect(content().string(containsString("text")));
	 * </pre>
	 */
	public ResultMatcher string(Matcher<? super String> matcher) {
		return result -> assertThat("Response content", result.getResponse().getContentAsString(), matcher);
	}

	/**
	 * Assert the response body content as a String.
	 */
	public ResultMatcher string(String expectedContent) {
		return result -> assertEquals("Response content", expectedContent, result.getResponse().getContentAsString());
	}

	/**
	 * Assert the response body content as a byte array.
	 */
	public ResultMatcher bytes(byte[] expectedContent) {
		return result -> assertEquals("Response content", expectedContent, result.getResponse().getContentAsByteArray());
	}

	/**
	 * Parse the response content and the given string as XML and assert the two
	 * are "similar" - i.e. they contain the same elements and attributes
	 * regardless of order.
	 * <p>Use of this matcher requires the <a
	 * href="https://www.xmlunit.org/">XMLUnit</a> library.
	 * @param xmlContent the expected XML content
	 * @see MockMvcResultMatchers#xpath(String, Object...)
	 * @see MockMvcResultMatchers#xpath(String, Map, Object...)
	 */
	public ResultMatcher xml(String xmlContent) {
		return result -> {
			String content = result.getResponse().getContentAsString();
			this.xmlHelper.assertXmlEqual(xmlContent, content);
		};
	}

	/**
	 * Parse the response content as {@link Node} and apply the given Hamcrest
	 * {@link Matcher}.
	 */
	public ResultMatcher node(Matcher<? super Node> matcher) {
		return result -> {
			String content = result.getResponse().getContentAsString();
			this.xmlHelper.assertNode(content, matcher);
		};
	}

	/**
	 * Parse the response content as {@link DOMSource} and apply the given
	 * Hamcrest {@link Matcher}.
	 * @see <a href="https://code.google.com/p/xml-matchers/">xml-matchers</a>
	 */
	public ResultMatcher source(Matcher<? super Source> matcher) {
		return result -> {
			String content = result.getResponse().getContentAsString();
			this.xmlHelper.assertSource(content, matcher);
		};
	}

	/**
	 * Parse the expected and actual strings as JSON and assert the two
	 * are "similar" - i.e. they contain the same attribute-value pairs
	 * regardless of formatting with a lenient checking (extensible,
	 * and non-strict array ordering).
	 * <p>Use of this matcher requires the <a
	 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
	 * @param jsonContent the expected JSON content
	 * @since 4.1
	 * @see #json(String, JsonCompareMode)
	 */
	public ResultMatcher json(String jsonContent) {
		return json(jsonContent, JsonCompareMode.LENIENT);
	}

	/**
	 * Parse the response content and the given string as JSON and assert the two are "similar" -
	 * i.e. they contain the same attribute-value pairs regardless of formatting.
	 * <p>Can compare in two modes, depending on {@code strict} parameter value:
	 * <ul>
	 * <li>{@code true}: strict checking. Not extensible, and strict array ordering.</li>
	 * <li>{@code false}: lenient checking. Extensible, and non-strict array ordering.</li>
	 * </ul>
	 * <p>Use of this matcher requires the <a
	 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
	 * @param jsonContent the expected JSON content
	 * @param strict enables strict checking
	 * @since 4.2
	 * @deprecated in favor of {@link #json(String, JsonCompareMode)}
	 */
	@Deprecated(since = "6.2")
	public ResultMatcher json(String jsonContent, boolean strict) {
		JsonCompareMode compareMode = (strict ? JsonCompareMode.STRICT : JsonCompareMode.LENIENT);
		return json(jsonContent, compareMode);
	}

	/**
	 * Parse the response content and the given string as JSON and assert the two
	 * using the given {@linkplain JsonCompareMode mode}. If the comparison failed,
	 * throws an {@link AssertionError} with the message of the {@link JsonComparison}.
	 * <p>Use of this matcher requires the <a
	 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
	 * @param jsonContent the expected JSON content
	 * @param compareMode the compare mode
	 * @since 6.2
	 */
	public ResultMatcher json(String jsonContent, JsonCompareMode compareMode) {
		return json(jsonContent, JsonAssert.comparator(compareMode));
	}

	/**
	 * Parse the response content and the given string as JSON and assert the two
	 * using the given {@link JsonComparator}. If the comparison failed, throws an
	 * {@link AssertionError} with the message of the {@link JsonComparison}.
	 * <p>Use this matcher if you require a custom JSONAssert configuration or
	 * if you desire to use another assertion library.
	 * @param jsonContent the expected JSON content
	 * @param comparator the comparator to use
	 * @since 6.2
	 */
	public ResultMatcher json(String jsonContent, JsonComparator comparator) {
		return result -> {
			String content = result.getResponse().getContentAsString();
			comparator.assertIsMatch(jsonContent, content);
		};
	}

}

/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.http.MediaType;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.test.util.XmlExpectationsHelper;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * Factory for response content assertions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#content}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ContentResultMatchers {

	private final XmlExpectationsHelper xmlHelper;

	private final JsonExpectationsHelper jsonHelper;


	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#content()}.
	 */
	protected ContentResultMatchers() {
		this.xmlHelper = new XmlExpectationsHelper();
		this.jsonHelper = new JsonExpectationsHelper();
	}


	/**
	 * Assert the ServletResponse content type. The given content type must
	 * fully match including type, sub-type, and parameters. For checking
	 * only the type and sub-type see {@link #contentTypeCompatibleWith(String)}.
	 */
	public ResultMatcher contentType(String contentType) {
		return contentType(MediaType.parseMediaType(contentType));
	}

	/**
	 * Assert the ServletResponse content type after parsing it as a MediaType.
	 * The given content type must fully match including type, sub-type, and
	 * parameters. For checking only the type and sub-type see
	 * {@link #contentTypeCompatibleWith(MediaType)}.
	 */
	public ResultMatcher contentType(final MediaType contentType) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String actual = result.getResponse().getContentType();
				assertTrue("Content type not set", actual != null);
				assertEquals("Content type", contentType, MediaType.parseMediaType(actual));
			}
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
	public ResultMatcher contentTypeCompatibleWith(final MediaType contentType) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String actual = result.getResponse().getContentType();
				assertTrue("Content type not set", actual != null);
				MediaType actualContentType = MediaType.parseMediaType(actual);
				assertTrue("Content type [" + actual + "] is not compatible with [" + contentType + "]",
						actualContentType.isCompatibleWith(contentType));
			}
		};
	}

	/**
	 * Assert the character encoding in the ServletResponse.
	 * @see HttpServletResponse#getCharacterEncoding()
	 */
	public ResultMatcher encoding(final String characterEncoding) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				String actual = result.getResponse().getCharacterEncoding();
				assertEquals("Character encoding", characterEncoding, actual);
			}
		};
	}

	/**
	 * Assert the response body content with a Hamcrest {@link Matcher}.
	 * <pre class="code">
	 * mockMvc.perform(get("/path"))
	 *   .andExpect(content().string(containsString("text")));
	 * </pre>
	 */
	public ResultMatcher string(final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				assertThat("Response content", result.getResponse().getContentAsString(), matcher);
			}
		};
	}

	/**
	 * Assert the response body content as a String.
	 */
	public ResultMatcher string(final String expectedContent) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				assertEquals("Response content", expectedContent, result.getResponse().getContentAsString());
			}
		};
	}

	/**
	 * Assert the response body content as a byte array.
	 */
	public ResultMatcher bytes(final byte[] expectedContent) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				assertEquals("Response content", expectedContent, result.getResponse().getContentAsByteArray());
			}
		};
	}

	/**
	 * Parse the response content and the given string as XML and assert the two
	 * are "similar" - i.e. they contain the same elements and attributes
	 * regardless of order.
	 * <p>Use of this matcher requires the <a
	 * href="http://xmlunit.sourceforge.net/">XMLUnit<a/> library.
	 * @param xmlContent the expected XML content
	 * @see MockMvcResultMatchers#xpath(String, Object...)
	 * @see MockMvcResultMatchers#xpath(String, Map, Object...)
	 */
	public ResultMatcher xml(final String xmlContent) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xmlHelper.assertXmlEqual(xmlContent, content);
			}
		};
	}

	/**
	 * Parse the response content as {@link Node} and apply the given Hamcrest
	 * {@link Matcher}.
	 */
	public ResultMatcher node(final Matcher<? super Node> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xmlHelper.assertNode(content, matcher);
			}
		};
	}

	/**
	 * Parse the response content as {@link DOMSource} and apply the given
	 * Hamcrest {@link Matcher}.
	 * @see <a href="https://code.google.com/p/xml-matchers/">xml-matchers</a>
	 */
	public ResultMatcher source(final Matcher<? super Source> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xmlHelper.assertSource(content, matcher);
			}
		};
	}

	/**
	 * Parse the expected and actual strings as JSON and assert the two
	 * are "similar" - i.e. they contain the same attribute-value pairs
	 * regardless of formatting with a lenient checking (extensible, and non-strict array
	 * ordering).
	 * @param jsonContent the expected JSON content
	 * @since 4.1
	 */
	public ResultMatcher json(final String jsonContent) {
		return json(jsonContent, false);
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
	 * href="https://jsonassert.skyscreamer.org/">JSONassert<a/> library.
	 * @param jsonContent the expected JSON content
	 * @param strict enables strict checking
	 * @since 4.2
	 */
	public ResultMatcher json(final String jsonContent, final boolean strict) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				jsonHelper.assertJsonEqual(jsonContent, content, strict);
			}
		};
	}

}

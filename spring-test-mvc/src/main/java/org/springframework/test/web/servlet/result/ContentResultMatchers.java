/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.hamcrest.Matcher;
import org.springframework.http.MediaType;
import org.springframework.test.util.XmlExpectationsHelper;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.w3c.dom.Node;

/**
 * Factory for response content assertions. An instance of this class is
 * typically accessed via {@link MockMvcResultMatchers#content()}.
 *
 * @author Rossen Stoyanchev
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
			public void match(MvcResult result) {
				String actual = result.getResponse().getCharacterEncoding();
				assertEquals("Character encoding", characterEncoding, actual);
			}
		};
	}

	/**
	 * Assert the response body content with a Hamcrest {@link Matcher}.
	 * <pre>
	 * mockMvc.perform(get("/path"))
	 *   .andExpect(content(containsString("text")));
	 * </pre>
	 */
	public ResultMatcher string(final Matcher<? super String> matcher) {
		return new ResultMatcher() {
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
			public void match(MvcResult result) throws Exception {
				assertEquals("Response content", expectedContent, result.getResponse().getContentAsByteArray());
			}
		};
	}

	/**
	 * Parse the response content and the given string as XML and assert the two
	 * are "similar" - i.e. they contain the same elements and attributes
	 * regardless of order.
	 *
	 * <p>Use of this matcher requires the <a
	 * href="http://xmlunit.sourceforge.net/">XMLUnit<a/> library.
	 *
	 * @param xmlContent the expected XML content
	 * @see MockMvcResultMatchers#xpath(String, Object...)
	 * @see MockMvcResultMatchers#xpath(String, Map, Object...)
	 */
	public ResultMatcher xml(final String xmlContent) {
		return new ResultMatcher() {
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
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xmlHelper.assertNode(content, matcher);
			}
		};
	}

	/**
	 * Parse the response content as {@link DOMSource} and apply the given
	 * Hamcrest {@link Matcher}.
	 *
	 * @see <a href="http://code.google.com/p/xml-matchers/">xml-matchers</a>
	 */
	public ResultMatcher source(final Matcher<? super Source> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xmlHelper.assertSource(content, matcher);
			}
		};
	}

}

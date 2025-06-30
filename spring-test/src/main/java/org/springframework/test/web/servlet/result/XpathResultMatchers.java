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

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Factory for assertions on the response content using XPath expressions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#xpath}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class XpathResultMatchers {

	private final XpathExpectationsHelper xpathHelper;


	/**
	 * Protected constructor, not for direct instantiation. Use
	 * {@link MockMvcResultMatchers#xpath(String, Object...)} or
	 * {@link MockMvcResultMatchers#xpath(String, Map, Object...)}.
	 * @param expression the XPath expression
	 * @param namespaces the XML namespaces referenced in the XPath expression, or {@code null}
	 * @param args arguments to parameterize the XPath expression with using the
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 */
	protected XpathResultMatchers(String expression, @Nullable Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		this.xpathHelper = new XpathExpectationsHelper(expression, namespaces, args);
	}


	/**
	 * Evaluate the XPath and assert the {@link Node} content found with the
	 * given Hamcrest {@link Matcher}.
	 */
	public ResultMatcher node(Matcher<? super Node> matcher) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNode(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
		};
	}

	/**
	 * Evaluate the XPath and assert the {@link NodeList} content found with the
	 * given Hamcrest {@link Matcher}.
	 * @since 5.2.2
	 */
	public ResultMatcher nodeList(Matcher<? super NodeList> matcher) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNodeList(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
		};
	}

	/**
	 * Get the response encoding if explicitly defined in the response, {@code null} otherwise.
	 */
	private @Nullable String getDefinedEncoding(MockHttpServletResponse response) {
		return (response.isCharset() ? response.getCharacterEncoding() : null);
	}

	/**
	 * Evaluate the XPath and assert that content exists.
	 */
	public ResultMatcher exists() {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.exists(response.getContentAsByteArray(), getDefinedEncoding(response));
		};
	}

	/**
	 * Evaluate the XPath and assert that content doesn't exist.
	 */
	public ResultMatcher doesNotExist() {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.doesNotExist(response.getContentAsByteArray(), getDefinedEncoding(response));
		};
	}

	/**
	 * Evaluate the XPath and assert the number of nodes found with the given
	 * Hamcrest {@link Matcher}.
	 */
	public ResultMatcher nodeCount(Matcher<? super Integer> matcher) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNodeCount(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
		};
	}

	/**
	 * Evaluate the XPath and assert the number of nodes found.
	 */
	public ResultMatcher nodeCount(int expectedCount) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNodeCount(response.getContentAsByteArray(), getDefinedEncoding(response), expectedCount);
		};
	}

	/**
	 * Apply the XPath and assert the {@link String} value found with the given
	 * Hamcrest {@link Matcher}.
	 */
	public ResultMatcher string(Matcher<? super String> matcher) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertString(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
		};
	}

	/**
	 * Apply the XPath and assert the {@link String} value found.
	 */
	public ResultMatcher string(String expectedValue) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertString(response.getContentAsByteArray(), getDefinedEncoding(response), expectedValue);
		};
	}

	/**
	 * Evaluate the XPath and assert the {@link Double} value found with the
	 * given Hamcrest {@link Matcher}.
	 */
	public ResultMatcher number(Matcher<? super Double> matcher) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNumber(response.getContentAsByteArray(), getDefinedEncoding(response), matcher);
		};
	}

	/**
	 * Evaluate the XPath and assert the {@link Double} value found.
	 */
	public ResultMatcher number(Double expectedValue) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertNumber(response.getContentAsByteArray(), getDefinedEncoding(response), expectedValue);
		};
	}

	/**
	 * Evaluate the XPath and assert the {@link Boolean} value found.
	 */
	public ResultMatcher booleanValue(Boolean value) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			this.xpathHelper.assertBoolean(response.getContentAsByteArray(), getDefinedEncoding(response), value);
		};
	}

}

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

package org.springframework.test.web.servlet.result;

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.w3c.dom.Node;

/**
 * Factory for response content {@code ResultMatcher}'s using an XPath
 * expression. An instance of this class is typically accessed via
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
	 *
	 * @param expression the XPath expression
	 * @param namespaces XML namespaces referenced in the XPath expression, or {@code null}
	 * @param args arguments to parameterize the XPath expression with using the
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 *
	 * @throws XPathExpressionException
	 */
	protected XpathResultMatchers(String expression, Map<String, String> namespaces, Object ... args)
			throws XPathExpressionException {

		this.xpathHelper = new XpathExpectationsHelper(expression, namespaces, args);
	}

	/**
	 * Evaluate the XPath and assert the {@link Node} content found with the
	 * given Hamcrest {@link Matcher}.
	 */
	public ResultMatcher node(final Matcher<? super Node> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xpathHelper.assertNode(content, matcher);
			}
		};
	}

	/**
	 * Evaluate the XPath and assert that content exists.
	 */
	public ResultMatcher exists() {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xpathHelper.exists(content);
			}
		};
	}

	/**
	 * Evaluate the XPath and assert that content doesn't exist.
	 */
	public ResultMatcher doesNotExist() {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xpathHelper.doesNotExist(content);
			}
		};
	}

	/**
	 * Evaluate the XPath and assert the number of nodes found with the given
	 * Hamcrest {@link Matcher}.
	 */
	public ResultMatcher nodeCount(final Matcher<Integer> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xpathHelper.assertNodeCount(content, matcher);
			}
		};
	}

	/**
	 * Evaluate the XPath and assert the number of nodes found.
	 */
	public ResultMatcher nodeCount(final int expectedCount) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xpathHelper.assertNodeCount(content, expectedCount);
			}
		};
	}

	/**
	 * Apply the XPath and assert the {@link String} value found with the given
	 * Hamcrest {@link Matcher}.
	 */
	public ResultMatcher string(final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xpathHelper.assertString(content, matcher);
			}
		};
	}

	/**
	 * Apply the XPath and assert the {@link String} value found.
	 */
	public ResultMatcher string(final String expectedValue) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xpathHelper.assertString(content, expectedValue);
			}
		};
	}

	/**
	 * Evaluate the XPath and assert the {@link Double} value found with the
	 * given Hamcrest {@link Matcher}.
	 */
	public ResultMatcher number(final Matcher<? super Double> matcher) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xpathHelper.assertNumber(content, matcher);
			}
		};
	}

	/**
	 * Evaluate the XPath and assert the {@link Double} value found.
	 */
	public ResultMatcher number(final Double expectedValue) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xpathHelper.assertNumber(content, expectedValue);
			}
		};
	}

	/**
	 * Evaluate the XPath and assert the {@link Boolean} value found.
	 */
	public ResultMatcher booleanValue(final Boolean value) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				String content = result.getResponse().getContentAsString();
				xpathHelper.assertBoolean(content, value);
			}
		};
	}

}
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

package org.springframework.test.util;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * A helper class for applying assertions via XPath expressions.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class XpathExpectationsHelper {

	private final String expression;

	private final XPathExpression xpathExpression;


	/**
	 * Class constructor.
	 *
	 * @param expression the XPath expression
	 * @param namespaces XML namespaces referenced in the XPath expression, or {@code null}
	 * @param args arguments to parameterize the XPath expression with using the
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @throws XPathExpressionException
	 */
	public XpathExpectationsHelper(String expression, Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		this.expression = String.format(expression, args);
		this.xpathExpression = compileXpathExpression(this.expression, namespaces);
	}

	private XPathExpression compileXpathExpression(String expression, Map<String, String> namespaces)
			throws XPathExpressionException {

		SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
		namespaceContext.setBindings((namespaces != null) ? namespaces : Collections.<String, String> emptyMap());
		XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(namespaceContext);
		return xpath.compile(expression);
	}

	/**
	 * @return the compiled XPath expression.
	 */
	protected XPathExpression getXpathExpression() {
		return this.xpathExpression;
	}

	/**
	 * Parse the content, evaluate the XPath expression as a {@link Node}, and
	 * assert it with the given {@code Matcher<Node>}.
	 */
	public void assertNode(String content, final Matcher<? super Node> matcher) throws Exception {
		Document document = parseXmlString(content);
		Node node = evaluateXpath(document, XPathConstants.NODE, Node.class);
		assertThat("Xpath: " + XpathExpectationsHelper.this.expression, node, matcher);
	}

	/**
	 * Parse the given XML content to a {@link Document}.
	 *
	 * @param xml the content to parse
	 * @return the parsed document
	 * @throws Exception in case of errors
	 */
	protected Document parseXmlString(String xml) throws Exception  {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		InputSource inputSource = new InputSource(new StringReader(xml));
		Document document = documentBuilder.parse(inputSource);
		return document;
	}

	/**
	 * Apply the XPath expression to given document.
	 * @throws XPathExpressionException
	 */
	@SuppressWarnings("unchecked")
	protected <T> T evaluateXpath(Document document, QName evaluationType, Class<T> expectedClass)
			throws XPathExpressionException {

		return (T) getXpathExpression().evaluate(document, evaluationType);
	}

	/**
	 * Apply the XPath expression and assert the resulting content exists.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void exists(String content) throws Exception {
		assertNode(content, Matchers.notNullValue());
	}

	/**
	 * Apply the XPath expression and assert the resulting content does not exist.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void doesNotExist(String content) throws Exception {
		assertNode(content, Matchers.nullValue());
	}

	/**
	 * Apply the XPath expression and assert the resulting content with the
	 * given Hamcrest matcher.
	 *
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNodeCount(String content, Matcher<Integer> matcher) throws Exception {
		Document document = parseXmlString(content);
		NodeList nodeList = evaluateXpath(document, XPathConstants.NODESET, NodeList.class);
		String reason = "nodeCount Xpath: " + XpathExpectationsHelper.this.expression;
		assertThat(reason, nodeList.getLength(), matcher);
	}

	/**
	 * Apply the XPath expression and assert the resulting content as an integer.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNodeCount(String content, int expectedCount) throws Exception {
		assertNodeCount(content, Matchers.equalTo(expectedCount));
	}

	/**
	 * Apply the XPath expression and assert the resulting content with the
	 * given Hamcrest matcher.
	 *
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertString(String content, Matcher<? super String> matcher) throws Exception {
		Document document = parseXmlString(content);
		String result = evaluateXpath(document,  XPathConstants.STRING, String.class);
		assertThat("Xpath: " + XpathExpectationsHelper.this.expression, result, matcher);
	}

	/**
	 * Apply the XPath expression and assert the resulting content as a String.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertString(String content, String expectedValue) throws Exception {
		assertString(content, Matchers.equalTo(expectedValue));
	}

	/**
	 * Apply the XPath expression and assert the resulting content with the
	 * given Hamcrest matcher.
	 *
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNumber(String content, Matcher<? super Double> matcher) throws Exception {
		Document document = parseXmlString(content);
		Double result = evaluateXpath(document, XPathConstants.NUMBER, Double.class);
		assertThat("Xpath: " + XpathExpectationsHelper.this.expression, result, matcher);
	}

	/**
	 * Apply the XPath expression and assert the resulting content as a Double.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNumber(String content, Double expectedValue) throws Exception {
		assertNumber(content, Matchers.equalTo(expectedValue));
	}

	/**
	 * Apply the XPath expression and assert the resulting content as a Boolean.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertBoolean(String content, boolean expectedValue) throws Exception {
		Document document = parseXmlString(content);
		String result = evaluateXpath(document, XPathConstants.STRING, String.class);
		assertEquals("Xpath:", expectedValue, Boolean.parseBoolean(result));
	}

}
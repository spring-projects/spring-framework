/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.util;

import java.io.ByteArrayInputStream;
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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.SimpleNamespaceContext;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * A helper class for applying assertions via XPath expressions.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class XpathExpectationsHelper {

	private final String expression;

	private final XPathExpression xpathExpression;

	private final boolean hasNamespaces;


	/**
	 * XpathExpectationsHelper constructor.
	 * @param expression the XPath expression
	 * @param namespaces XML namespaces referenced in the XPath expression, or {@code null}
	 * @param args arguments to parameterize the XPath expression with using the
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @throws XPathExpressionException if expression compilation failed
	 */
	public XpathExpectationsHelper(String expression, @Nullable Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		this.expression = String.format(expression, args);
		this.xpathExpression = compileXpathExpression(this.expression, namespaces);
		this.hasNamespaces = !CollectionUtils.isEmpty(namespaces);
	}


	private XPathExpression compileXpathExpression(String expression, @Nullable Map<String, String> namespaces)
			throws XPathExpressionException {

		SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
		namespaceContext.setBindings(namespaces != null ? namespaces : Collections.emptyMap());
		XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(namespaceContext);
		return xpath.compile(expression);
	}

	/**
	 * Return the compiled XPath expression.
	 */
	protected XPathExpression getXpathExpression() {
		return this.xpathExpression;
	}

	/**
	 * Parse the content, evaluate the XPath expression as a {@link Node},
	 * and assert it with the given {@code Matcher<Node>}.
	 */
	public void assertNode(byte[] content, @Nullable String encoding, final Matcher<? super Node> matcher)
			throws Exception {

		Document document = parseXmlByteArray(content, encoding);
		Node node = evaluateXpath(document, XPathConstants.NODE, Node.class);
		assertThat("XPath " + this.expression, node, matcher);
	}

	/**
	 * Parse the given XML content to a {@link Document}.
	 * @param xml the content to parse
	 * @param encoding optional content encoding, if provided as metadata (e.g. in HTTP headers)
	 * @return the parsed document
	 */
	protected Document parseXmlByteArray(byte[] xml, @Nullable String encoding) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(this.hasNamespaces);
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		InputSource inputSource = new InputSource(new ByteArrayInputStream(xml));
		if (StringUtils.hasText(encoding)) {
			inputSource.setEncoding(encoding);
		}
		return documentBuilder.parse(inputSource);
	}

	/**
	 * Apply the XPath expression to given document.
	 * @throws XPathExpressionException if expression evaluation failed
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected <T> T evaluateXpath(Document document, QName evaluationType, Class<T> expectedClass)
			throws XPathExpressionException {

		return (T) getXpathExpression().evaluate(document, evaluationType);
	}

	/**
	 * Apply the XPath expression and assert the resulting content exists.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void exists(byte[] content, @Nullable String encoding) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		Node node = evaluateXpath(document, XPathConstants.NODE, Node.class);
		assertTrue("XPath " + this.expression + " does not exist", node != null);
	}

	/**
	 * Apply the XPath expression and assert the resulting content does not exist.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void doesNotExist(byte[] content, @Nullable String encoding) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		Node node = evaluateXpath(document, XPathConstants.NODE, Node.class);
		assertTrue("XPath " + this.expression + " exists", node == null);
	}

	/**
	 * Apply the XPath expression and assert the resulting content with the
	 * given Hamcrest matcher.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNodeCount(byte[] content, @Nullable String encoding, Matcher<Integer> matcher) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		NodeList nodeList = evaluateXpath(document, XPathConstants.NODESET, NodeList.class);
		assertThat("nodeCount for XPath " + this.expression,
				(nodeList != null ? nodeList.getLength() : 0), matcher);
	}

	/**
	 * Apply the XPath expression and assert the resulting content as an integer.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNodeCount(byte[] content, @Nullable String encoding, int expectedCount) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		NodeList nodeList = evaluateXpath(document, XPathConstants.NODESET, NodeList.class);
		assertEquals("nodeCount for XPath " + this.expression, expectedCount,
				(nodeList != null ? nodeList.getLength() : 0));
	}

	/**
	 * Apply the XPath expression and assert the resulting content with the
	 * given Hamcrest matcher.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertString(byte[] content, @Nullable String encoding, Matcher<? super String> matcher) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		String result = evaluateXpath(document,  XPathConstants.STRING, String.class);
		assertThat("XPath " + this.expression, result, matcher);
	}

	/**
	 * Apply the XPath expression and assert the resulting content as a String.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertString(byte[] content, @Nullable String encoding, String expectedValue) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		String actual = evaluateXpath(document,  XPathConstants.STRING, String.class);
		assertEquals("XPath " + this.expression, expectedValue, actual);
	}

	/**
	 * Apply the XPath expression and assert the resulting content with the
	 * given Hamcrest matcher.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNumber(byte[] content, @Nullable String encoding, Matcher<? super Double> matcher) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		Double result = evaluateXpath(document, XPathConstants.NUMBER, Double.class);
		assertThat("XPath " + this.expression, result, matcher);
	}

	/**
	 * Apply the XPath expression and assert the resulting content as a Double.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertNumber(byte[] content, @Nullable String encoding, Double expectedValue) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		Double actual = evaluateXpath(document, XPathConstants.NUMBER, Double.class);
		assertEquals("XPath " + this.expression, expectedValue, actual);
	}

	/**
	 * Apply the XPath expression and assert the resulting content as a Boolean.
	 * @throws Exception if content parsing or expression evaluation fails
	 */
	public void assertBoolean(byte[] content, @Nullable String encoding, boolean expectedValue) throws Exception {
		Document document = parseXmlByteArray(content, encoding);
		String actual = evaluateXpath(document, XPathConstants.STRING, String.class);
		assertEquals("XPath " + this.expression, expectedValue, Boolean.parseBoolean(actual));
	}

}

/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.util.MimeType;

/**
 * XPath assertions for a {@link WebTestClient}.
 *
 * @author Eric Deandrea
 * @since 5.1
 */
public class XpathAssertions {

	private final WebTestClient.BodyContentSpec bodySpec;

	private final XpathExpectationsHelper xpathHelper;

	XpathAssertions(WebTestClient.BodyContentSpec spec, String expression, @Nullable Map<String, String> namespaces, Object... args) throws XPathExpressionException {
		this.bodySpec = spec;
		this.xpathHelper = new XpathExpectationsHelper(expression, namespaces, args);
	}

	/**
	 * Applies {@link XpathExpectationsHelper#assertString(byte[], String, String)}
	 */
	public WebTestClient.BodyContentSpec isEqualTo(String expectedValue) {
		return performXmlAssertionAndHandleError(() -> this.xpathHelper.assertString(getResponseBody(), getDefinedEncoding(), expectedValue));
	}

	/**
	 * Applies {@link XpathExpectationsHelper#assertNumber(byte[], String, Double)}
	 */
	public WebTestClient.BodyContentSpec isEqualTo(Double expectedValue) {
		return performXmlAssertionAndHandleError(() -> this.xpathHelper.assertNumber(getResponseBody(), getDefinedEncoding(), expectedValue));
	}

	/**
	 * Applies {@link XpathExpectationsHelper#assertBoolean(byte[], String, boolean)}
	 */
	public WebTestClient.BodyContentSpec isEqualTo(boolean expectedValue) {
		return performXmlAssertionAndHandleError(() -> this.xpathHelper.assertBoolean(getResponseBody(), getDefinedEncoding(), expectedValue));
	}

	/**
	 * Applies {@link XpathExpectationsHelper#exists(byte[], String)}
	 */
	public WebTestClient.BodyContentSpec exists() {
		return performXmlAssertionAndHandleError(() -> this.xpathHelper.exists(getResponseBody(), getDefinedEncoding()));
	}

	/**
	 * Applies {@link XpathExpectationsHelper#doesNotExist(byte[], String)}
	 */
	public WebTestClient.BodyContentSpec doesNotExist() {
		return performXmlAssertionAndHandleError(() -> this.xpathHelper.doesNotExist(getResponseBody(), getDefinedEncoding()));
	}

	/**
	 * Applies {@link XpathExpectationsHelper[#assertNodeCount(byte[], String, int)}
	 */
	public WebTestClient.BodyContentSpec nodeCount(int expectedCount) {
		return performXmlAssertionAndHandleError(() -> this.xpathHelper.assertNodeCount(getResponseBody(), getDefinedEncoding(), expectedCount));
	}

	/**
	 * Applies {@link XpathExpectationsHelper#assertNodeCount(byte[], String, Matcher)}
	 */
	public WebTestClient.BodyContentSpec nodeCount(Matcher<Integer> matcher) {
		return performXmlAssertionAndHandleError(() -> this.xpathHelper.assertNodeCount(getResponseBody(), getDefinedEncoding(), matcher));
	}

	/**
	 * Applies {@link XpathExpectationsHelper#assertNode(byte[], String, Matcher)}
	 */
	public WebTestClient.BodyContentSpec nodeMatches(Matcher<? super Node> matcher) {
		return performXmlAssertionAndHandleError(() -> this.xpathHelper.assertNode(getResponseBody(), getDefinedEncoding(), matcher));
	}

	/**
	 * Applies {@link XpathExpectationsHelper#assertString(byte[], String, Matcher)}
	 */
	public WebTestClient.BodyContentSpec matchesString(Matcher<? super String> matcher) {
		return performXmlAssertionAndHandleError(() -> this.xpathHelper.assertString(getResponseBody(), getDefinedEncoding(), matcher));
	}

	/**
	 * Applies {@link XpathExpectationsHelper#assertNumber(byte[], String, Matcher)}
	 */
	public WebTestClient.BodyContentSpec matchesNumber(Matcher<? super Double> matcher) {
		return performXmlAssertionAndHandleError(() -> this.xpathHelper.assertNumber(getResponseBody(), getDefinedEncoding(), matcher));
	}

	private WebTestClient.BodyContentSpec performXmlAssertionAndHandleError(AssertionThrowingRunnable assertion) {
		assertion.run();
		return this.bodySpec;
	}

	private byte[] getResponseBody() {
		return getResult().getResponseBody();
	}

	private EntityExchangeResult<byte[]> getResult() {
		return this.bodySpec.returnResult();
	}

	private String getDefinedEncoding() {
		return Optional.ofNullable(getResult())
				.map(EntityExchangeResult::getResponseHeaders)
				.map(HttpHeaders::getContentType)
				.map(MimeType::getCharset)
				.orElse(StandardCharsets.UTF_8)
				.name();
	}

	/**
	 * Lets us be able to use lambda expressions that could throw checked exceptions, since
	 * {@link XpathExpectationsHelper} throws {@link Exception} on its methods.
	 */
	@FunctionalInterface
	private interface AssertionThrowingRunnable extends Runnable {
		void runThrows() throws Exception;

		@Override
		default void run() {
			try {
				runThrows();
			}
			catch (Exception ex) {
				throw new AssertionError("XML parsing error", ex);
			}
		}
	}
}
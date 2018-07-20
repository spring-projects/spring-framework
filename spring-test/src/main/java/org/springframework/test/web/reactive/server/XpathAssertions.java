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

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;


/**
 * XPath assertions for the {@link WebTestClient}.
 *
 * @author Eric Deandrea
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class XpathAssertions {

	private final WebTestClient.BodyContentSpec bodySpec;

	private final XpathExpectationsHelper xpathHelper;


	XpathAssertions(WebTestClient.BodyContentSpec spec,
			String expression, @Nullable Map<String, String> namespaces, Object... args) {

		this.bodySpec = spec;
		this.xpathHelper = initXpathHelper(expression, namespaces, args);
	}

	private static XpathExpectationsHelper initXpathHelper(
			String expression, @Nullable Map<String, String> namespaces, Object[] args) {

		try {
			return new XpathExpectationsHelper(expression, namespaces, args);
		}
		catch (XPathExpressionException ex) {
			throw new AssertionError("XML parsing error", ex);
		}
	}


	/**
	 * Delegates to {@link XpathExpectationsHelper#assertString(byte[], String, String)}
	 */
	public WebTestClient.BodyContentSpec isEqualTo(String expectedValue) {
		return assertWith(() -> this.xpathHelper.assertString(getContent(), getCharset(), expectedValue));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#assertNumber(byte[], String, Double)}
	 */
	public WebTestClient.BodyContentSpec isEqualTo(Double expectedValue) {
		return assertWith(() -> this.xpathHelper.assertNumber(getContent(), getCharset(), expectedValue));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#assertBoolean(byte[], String, boolean)}
	 */
	public WebTestClient.BodyContentSpec isEqualTo(boolean expectedValue) {
		return assertWith(() -> this.xpathHelper.assertBoolean(getContent(), getCharset(), expectedValue));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#exists(byte[], String)}
	 */
	public WebTestClient.BodyContentSpec exists() {
		return assertWith(() -> this.xpathHelper.exists(getContent(), getCharset()));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper#doesNotExist(byte[], String)}
	 */
	public WebTestClient.BodyContentSpec doesNotExist() {
		return assertWith(() -> this.xpathHelper.doesNotExist(getContent(), getCharset()));
	}

	/**
	 * Delegates to {@link XpathExpectationsHelper[#assertNodeCount(byte[], String, int)}
	 */
	public WebTestClient.BodyContentSpec nodeCount(int expectedCount) {
		return assertWith(() -> this.xpathHelper.assertNodeCount(getContent(), getCharset(), expectedCount));
	}


	private WebTestClient.BodyContentSpec assertWith(CheckedExceptionTask task) {
		try {
			task.run();
		}
		catch (Exception ex) {
			throw new AssertionError("XML parsing error", ex);
		}
		return this.bodySpec;
	}

	private byte[] getContent() {
		byte[] body = this.bodySpec.returnResult().getResponseBody();
		Assert.notNull(body, "Expected body content");
		return body;
	}

	private String getCharset() {
		return Optional.of(this.bodySpec.returnResult())
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
	private interface CheckedExceptionTask {

		void run() throws Exception;

	}
}